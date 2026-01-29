# Design Document: MCP Attack Surface Detector (MCP-ASD)

## 1. Project Overview
**MCP-ASD** is a specialized Burp Suite extension (Montoya API) designed to identify, map, and test the attack surface of **Model Context Protocol (MCP)** servers. By mimicking the philosophy of the original "Attack Surface Detector," this tool moves beyond standard API scanning to specifically target the "Agentic Primitives" (Tools, Resources, and Prompts) exposed to Large Language Models.

---

## 2. Automated Enumeration Engine
The core value of MCP-ASD is its ability to automatically "walk" the MCP protocol to build a comprehensive map of the available attack surface without manual user crawling.

### 2.1 The Discovery Workflow
Upon establishing a transport connection (SSE or WebSocket), the extension must execute a multi-stage discovery sequence:

1.  **Handshake:** Executes `initialize` and `initialized` methods to negotiate protocol versions and server capabilities.
2.  **Tool Discovery (`tools/list`):**
    * **Action:** Fetches all available executable functions.
    * **Parsing:** Extracts the `inputSchema` (JSON Schema) for every tool.
    * **Mapping:** Identifies required vs. optional parameters, data types, and enum constraints.
3.  **Resource Discovery (`resources/list`):**
    * **Action:** Fetches available data URIs (e.g., `file://`, `postgres://`, `s3://`).
    * **Parsing:** Identifies URI templates and MIME types to surface potential path traversal or BOLA targets.
4.  **Prompt Discovery (`prompts/list`):**
    * **Action:** Fetches pre-defined system prompts and templates.
    * **Parsing:** Extracts argument lists to identify fields susceptible to Indirect Prompt Injection.

### 2.2 Deep Parameter Analysis
The engine recursively parses the `inputSchema`. It doesn't just list parameters; it builds a **dependency graph** to understand which parameters are nested, which are mandatory, and which are vulnerable to type-confusion attacks.

---

## 3. Stateful Middleware Architecture
Because MCP is inherently asynchronous (especially over SSE), MCP-ASD acts as a **Synchronous Bridge** to enable seamless use of Burp Repeater and Intruder.

### 3.1 The "Stitching" Logic
1.  **Virtual Endpoint:** The extension listens for traffic at `http://mcp-asd.local/invoke`.
2.  **Request Decoration:** When a tool call is sent from Burp, the extension intercepts it, injects a unique `jsonrpc_id`, and maps it to the current active session.
3.  **Thread Blocking:** The extension pauses the Burp thread using a `CompletableFuture`.
4.  **Asymmetric Capture:** The background SSE/WS listener monitors the server's event stream.
5.  **Correlation:** When a response arrives with the matching `id`, the extension "stitches" the result back into a standard HTTP 200 response body and releases it to the Burp UI.

---

## 4. Attack Surface Dashboard (UI)
The extension adds a new top-level tab to Burp Suite with the following sub-components:

* **Primitive Tree:** A hierarchical view of all enumerated Tools, Resources, and Prompts.
* **Metadata Inspector:** Displays descriptions, MIME types, and JSON Schemas for the selected primitive.
* **Template Generator:** A button to "Generate Prototype Request," which creates a valid JSON-RPC body populated with all identified parameters (including optional ones) for instant testing in Repeater.

---

## 5. Security Testing Framework
The tool leverages the enumerated map to facilitate:
- **BOLA Testing:** Iterative testing of Resource URIs.
- **Input Validation:** Automated fuzzing of Tool arguments using identified types and constraints.
- **Protocol Smuggling:** Testing how the MCP server handles malformed JSON-RPC or out-of-order initialization calls.

---

## 6. Implementation Notes for Developer CLI
- **Framework:** Java 21+ with Burp Montoya API.
- **Transport:** Use a robust HTTP client (e.g., OkHttp) capable of maintaining long-lived SSE connections and WebSocket frames.
- **Concurrency:** Implement a thread-safe `SessionStore` to prevent cross-talk when running high-concurrency Intruder attacks.
- **Format:** Ensure all communication strictly adheres to the **JSON-RPC 2.0** specification.