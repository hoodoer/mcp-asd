import asyncio
import json
import uuid
from fastapi import FastAPI, Request, WebSocket
from fastapi.responses import JSONResponse
from sse_starlette.sse import EventSourceResponse

app = FastAPI()

# Global list of active SSE queues
ACTIVE_QUEUES = []
# Global list of active WebSockets
ACTIVE_WEBSOCKETS = []

# In-memory storage for discovered items
TOOLS = {
    "get_weather": {
        "description": "Get the current weather in a given location",
        "inputSchema": {
            "type": "object",
            "properties": {
                "location": {
                    "type": "string",
                    "description": "The city and state, e.g. San Francisco, CA"
                }
            },
            "required": ["location"]
        }
    },
    "get_user": {
        "description": "Get user details by ID. Useful for concurrency testing.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "user_id": {
                    "type": "integer",
                    "description": "The user ID to fetch."
                }
            },
            "required": ["user_id"]
        }
    }
}

RESOURCES = {
    "user_data": {
        "uri": "file:///etc/passwd",
        "description": "A sample file resource URI.",
        "type": "text/plain"
    }
}

PROMPTS = {
    "summarize_text": {
        "description": "Summarize the provided text.",
        "template": "Please summarize the following text: {text}"
    }
}

async def handle_mcp_request(request_data):
    method = request_data.get("method")
    params = request_data.get("params")
    request_id = request_data.get("id")

    response = {
        "jsonrpc": "2.0",
        "id": request_id,
    }

    if method == "initialize":
        response["result"] = {"version": "0.1.0", "capabilities": {}}
        await broadcast_message(response)
        await asyncio.sleep(0.1)
        await broadcast_message({"jsonrpc": "2.0", "method": "initialized", "params": {}})
    elif method == "tools/list":
        response["result"] = TOOLS
        await broadcast_message(response)
    elif method == "resources/list":
        response["result"] = RESOURCES
        await broadcast_message(response)
    elif method == "prompts/list":
        response["result"] = PROMPTS
        await broadcast_message(response)
    elif method == "tools/invoke":
        tool_name = params.get("name")
        tool_params = params.get("arguments")
        if tool_name == "get_weather":
            response["result"] = {
                "status": "success",
                "output": f"Successfully invoked {tool_name} with parameters: {tool_params}"
            }
        elif tool_name == "get_user":
            try:
                uid = int(tool_params.get("user_id"))
                # Simulate a slight delay to encourage out-of-order processing if concurrent
                await asyncio.sleep(0.05)
                response["result"] = {
                    "id": uid,
                    "name": f"User_{uid}",
                    "email": f"user{uid}@example.com",
                    "role": "admin" if uid == 0 else "user"
                }
            except (ValueError, TypeError):
                 response["error"] = {"code": -32602, "message": "Invalid user_id"}
        else:
            response["error"] = {"code": -32601, "message": "Method not found"}
        await broadcast_message(response)
    else:
        response["error"] = {"code": -32601, "message": "Method not found"}
        await broadcast_message(response)

async def broadcast_message(message):
    data = json.dumps(message)
    print(f"DEBUG: Broadcasting message to {len(ACTIVE_QUEUES)} queues and {len(ACTIVE_WEBSOCKETS)} sockets: {data}")
    
    # Broadcast to SSE
    for i, queue in enumerate(list(ACTIVE_QUEUES)):
        await queue.put(data)
        print(f"DEBUG: Put message in SSE queue {i}")

    # Broadcast to WebSockets
    for i, ws in enumerate(list(ACTIVE_WEBSOCKETS)):
        try:
            await ws.send_text(data)
            print(f"DEBUG: Sent message to WebSocket {i}")
        except Exception as e:
            print(f"DEBUG: Failed to send to WebSocket {i}: {e}")
            if ws in ACTIVE_WEBSOCKETS:
                ACTIVE_WEBSOCKETS.remove(ws)

# WebSocket endpoint
@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    ACTIVE_WEBSOCKETS.append(websocket)
    print(f"DEBUG: WebSocket connected. Active sockets: {len(ACTIVE_WEBSOCKETS)}")
    try:
        while True:
            data = await websocket.receive_text()
            print(f"DEBUG: WebSocket received: {data}")
            try:
                request_data = json.loads(data)
                await handle_mcp_request(request_data)
            except json.JSONDecodeError:
                print("DEBUG: WebSocket JSON parse error")
    except Exception as e:
        print(f"DEBUG: WebSocket error/closed: {e}")
    finally:
        if websocket in ACTIVE_WEBSOCKETS:
            ACTIVE_WEBSOCKETS.remove(websocket)
        print(f"DEBUG: WebSocket disconnected. Active sockets: {len(ACTIVE_WEBSOCKETS)}")

# SSE endpoint - ESTABLISHES CONNECTION
@app.get("/mcp")
async def mcp_sse_endpoint(request: Request):
    sse_queue = asyncio.Queue()
    ACTIVE_QUEUES.append(sse_queue)
    print(f"DEBUG: Client connected. Active queues: {len(ACTIVE_QUEUES)}")

    async def event_generator():
        try:
            while True:
                message = await sse_queue.get()
                print(f"DEBUG: Yielding message from queue")
                yield {"event": "message", "data": message} # Default event type is often 'message'
        except asyncio.CancelledError:
            print("DEBUG: Client disconnected")
            if sse_queue in ACTIVE_QUEUES:
                ACTIVE_QUEUES.remove(sse_queue)
            print(f"DEBUG: Active queues: {len(ACTIVE_QUEUES)}")

    return EventSourceResponse(event_generator())

# POST endpoint - RECEIVES MESSAGES
@app.post("/mcp")
async def mcp_post_endpoint(request: Request):
    try:
        body_bytes = await request.body()
        body_str = body_bytes.decode('utf-8')
        
        # 1. Try to parse the entire body as a single JSON object (or array)
        try:
            request_data = json.loads(body_str)
            if isinstance(request_data, list):
                for item in request_data:
                    await handle_mcp_request(item)
            else:
                await handle_mcp_request(request_data)
            return JSONResponse({"status": "accepted"})
        except json.JSONDecodeError:
            # 2. Fallback to JSON-Lines (NDJSON) if full parse fails
            pass

        # Handle Line-delimited JSON
        for line in body_str.splitlines():
             if line.strip():
                try:
                    request_data = json.loads(line)
                    await handle_mcp_request(request_data)
                except json.JSONDecodeError:
                    print(f"DEBUG: Failed to parse line: {line}")
                    pass
        return JSONResponse({"status": "accepted"})
    except Exception as e:
         print(f"DEBUG: Server Error: {e}")
         return JSONResponse({"status": "error", "message": str(e)}, status_code=500)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
