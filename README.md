# MCP Attack Surface Detector (MCP-ASD)

**MCP-ASD** is a Burp Suite extension designed to discover, map, and test the attack surface of **Model Context Protocol (MCP)** servers. 

It acts as a bridge between Burp's synchronous testing tools (Repeater, Intruder, Scanner) and the asynchronous, persistent nature of MCP connections (SSE & WebSockets), allowing pentesters to audit Agentic Tools, Resources, and Prompts just like standard REST APIs.

![Status](https://img.shields.io/badge/Status-Phase_1_Complete-success)
![Burp](https://img.shields.io/badge/Burp_Suite-Montoya_API-orange)

## 🚀 Key Features

*   **Multi-Protocol Support:**
    *   Full support for **Server-Sent Events (SSE)** (HTTP POST/GET).
    *   Full support for **WebSockets** (ws://).
    *   **Auto-Detection** engine to identify valid MCP endpoints and transports automatically.
*   **Attack Surface Enumeration:**
    *   Automatically discovers available **Tools**, **Resources**, and **Prompts**.
    *   Visualizes the hierarchy in a structured 3-pane dashboard.
    *   Extracts input schemas and argument structures.
*   **Burp Integration (The "Stitching" Bridge):**
    *   **Repeater Support:** Send prototype JSON-RPC requests to MCP tools and view responses synchronously.
    *   **Intruder Support:** Fully thread-safe integration with Burp Intruder. The extension auto-generates unique JSON-RPC IDs per request, enabling concurrent fuzzing of tool arguments.
*   **Prototype Generator:**
    *   Generates valid JSON-RPC payloads based on the discovered tool schema, ready for fuzzing.

## 🛠️ Installation & Build

### Prerequisites
*   **Java JDK 17+**
*   **Burp Suite Professional or Community** (2023.12.1+)

### Build from Source
```bash
git clone https://github.com/your-repo/mcp-asd.git
cd mcp-asd
./gradlew shadowJar
```
This will create a fat JAR in `build/libs/mcp-asd.jar`.

### Loading into Burp
1.  Open Burp Suite.
2.  Go to **Extensions** -> **Installed**.
3.  Click **Add**.
4.  Select **Java** and browse to `build/libs/mcp-asd.jar`.

## 📖 Usage Guide

### 1. Connecting to an MCP Server
1.  Identify a target URL in Burp's **Proxy History** or **Target** tab.
2.  Right-click the request/host and select **"Send to MCP-ASD"**.
3.  Alternatively, go to the **MCP-ASD** tab and click **"New Connection"**.
4.  In the Connection Dialog:
    *   Click **"Auto-Detect Endpoints"** to scan for common paths (`/mcp`, `/ws`, `/sse`).
    *   Select the desired **Transport** (SSE or WebSocket).
    *   Click **Connect**.

### 2. Mapping the Surface
Once connected, the **MCP-ASD** tab will populate three panes:
*   **🛠️ Tools:** Executable functions exposed to the LLM.
*   **📄 Resources:** Data sources (files, databases) the LLM can read.
*   **💬 Prompts:** Pre-defined templates.

### 3. Testing & Fuzzing
1.  Select an item (e.g., `get_weather`) from the list.
2.  The **Request Prototype** pane will show the JSON-RPC format.
3.  Click **"Send to Repeater"** to manually tweak arguments.
4.  Click **"Send to Intruder"** to fuzz parameters.
    *   *Note:* The extension automatically handles session correlation. You can run Intruder with multiple threads, and the extension will ensure every response is mapped to the correct request using internal ID tracking.

## 🧪 Development Test Server

A mock MCP server is included for testing and development.

### Requirements
```bash
pip install fastapi uvicorn[standard] websockets
```

### Running the Server
```bash
python3 mcp_server.py
```
This starts a server on `0.0.0.0:8000` supporting:
*   **SSE:** `http://localhost:8000/mcp`
*   **WebSocket:** `ws://localhost:8000/ws`

### Vulnerable Tools (For Verification)
*   `get_weather`: Standard tool simulation.
*   `get_user`: Echoes back user details based on ID (useful for concurrency testing).

## 🏗️ Architecture

MCP is inherently asynchronous, while Burp is synchronous. MCP-ASD bridges this gap using a **Transparent Internal Proxy**:

1.  **Burp sends HTTP POST** to `127.0.0.1:<RandomPort>`.
2.  **MCP-ASD intercepts** this request via a raw socket listener.
3.  **Request Decoration:** The extension injects a unique `jsonrpc_id`.
4.  **Transport:** The request is sent over the persistent MCP connection (SSE or WS).
5.  **Correlation:** The extension holds the Burp thread open until the corresponding ID appears in the MCP event stream.
6.  **Response:** The result is "stitched" back into a standard HTTP 200 response and returned to Burp.

## 📅 Roadmap (Phase 2)
*   [ ] **Automated Scanner:** Active vulnerability scanning modules (BOLA, Injection).
*   [ ] **Authentication:** Support for OAuth Bearer tokens and mTLS.
*   [ ] **Deep Schema Parsing:** Better fuzzing prototypes for complex nested objects.
