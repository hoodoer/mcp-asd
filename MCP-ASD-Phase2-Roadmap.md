# Roadmap: MCP Attack Surface Detector (MCP-ASD) - Phase 2

## Overview
Phase 1 successfully established the core infrastructure: a Burp Suite extension capable of enumerating MCP servers (SSE/WebSocket), visualizing the attack surface, and bridging asynchronous MCP traffic to Burp's synchronous tools (Repeater/Intruder).

**Phase 2 focuses on two critical advancements:**
1.  **Automated Security Scanning:** Transitioning from "enabling attacks" to "performing attacks" by implementing specific vulnerability checks.
2.  **Authentication Support:** Ensuring the tool works against secured MCP environments (OAuth, API Keys, mTLS).

---

## 1. Automated Vulnerability Scanner
**Goal:** Implement an "Active Scan" capability that autonomously probes discovered Primitives for common LLM/MCP vulnerabilities.

### 1.1 Architecture
*   **Scanner Interface:** A new `SecurityScanner` module that integrates with Burp's `IScanIssue` API (or Montoya equivalent) to report findings.
*   **Scan Profiles:**
    *   **Passive:** Analyzes `inputSchema` for insecure defaults (e.g., `additionalProperties: true`, weak typing).
    *   **Active:** Sends mutated traffic to `McpProxy`.

### 1.2 Targeted Checks
*   **Prompt Injection (Tools):**
    *   *Attack:* Inject payload markers into string arguments of Tools.
    *   *Check:* Does the tool output reflect the injection raw? (Requires simple taint analysis).
*   **Resource BOLA (Broken Object Level Authorization):**
    *   *Attack:* If a Resource URI contains an ID (e.g., `file:///users/123/report`), iterate the ID (`122`, `124`).
    *   *Check:* Does the server return data for un-enumerated IDs?
*   **Path Traversal (Resources):**
    *   *Attack:* For `file://` resources, attempt `../` sequences in the URI parameters.
*   **Schema Violation / Type Confusion:**
    *   *Attack:* Send `integer` where `string` is expected, or huge arrays.
    *   *Check:* Does the server crash (500) or leak stack traces?

### 1.3 Test Server Updates
*   Update `mcp_server.py` to include:
    *   **Vulnerable Tool:** `echo_input` (vulnerable to injection).
    *   **Vulnerable Resource:** `file:///logs/{id}` (vulnerable to BOLA).
    *   **Crashable Endpoint:** A tool that panics on bad types.

---

## 2. Authentication & Access Control
**Goal:** Support common authentication schemes used in MCP deployments.

### 2.1 Auth Strategies to Support
*   **HTTP Headers (API Keys / OAuth):**
    *   *Mechanism:* Inject global headers (e.g., `Authorization: Bearer <token>`, `X-API-Key: <key>`) into every SSE `GET` or WebSocket handshake.
    *   *OAuth Specifics:* Since OAuth flows (getting the token) are complex and browser-based, we assume the user *has* the token. We provide a UI to easily input it as a Bearer Token.
    *   *UI Update:* Add "Headers" tab to the Connection Dialog with presets for "Bearer" and "Basic".
*   **mTLS (Mutual TLS / Client Certificates):**
    *   *Context:* Burp natively handles mTLS for its own requests, but our extension uses a separate `OkHttp` client for the persistent SSE/WebSocket connection. This client *does not* inherit Burp's certs automatically.
    *   *Implementation:* Add an "mTLS" section to the Connection Dialog.
        *   Option A: Allow user to provide path to PKCS#12 (.p12) file and password.
        *   Option B: Configure `OkHttp` to proxy through Burp (letting Burp handle mTLS), though this is complex to automate. Option A is preferred for reliability.
*   **Initialization Auth (MCP-Specific):**
    *   *Mechanism:* Some servers might require auth data *inside* the JSON-RPC `initialize` message.
    *   *Implementation:* Allow users to customize the `initialize` payload sent by the `EnumerationEngine`.

### 2.2 UI Updates
*   **Connection Dialog v2:**
    *   **Tabs:** Split into "Connection" (Host/Port/Transport) and "Authentication".
    *   **Auth Tab:** 
        *   **Header Manager:** Add/Remove Key-Value pairs.
        *   **mTLS Config:** File picker for Certificate and Password field.

---

## 3. Deep Parameter Analysis (Refinement)
**Goal:** Improve the quality of the "Prototype Request" to facilitate better fuzzing.

*   **Enum Expansion:** If a schema defines `enum: ["A", "B"]`, generate distinct prototypes for each variant.
*   **Recursive Objects:** Better handling of nested JSON objects in tool arguments (currently simplified).

---

## Phase 2 Execution Plan

### Step 1: Authentication (Foundation)
1.  **UI:** Update `ConnectionDialog` to support Tabs (General vs Auth).
2.  **Auth Logic:** Implement Header injection in `SseTransport` and `WebSocketTransport`.
3.  **mTLS Logic:** Implement `SSLSocketFactory` configuration in `OkHttp` using user-provided P12 files.
4.  **Test:** Add a protected endpoint to `mcp_server.py` (checking Header) and simulated mTLS check (optional/mocked).

### Step 2: Scanner Infrastructure
1.  **Scan Controller:** Create a UI context menu action "Scan this Tool".
2.  **Fuzzer Integration:** Programmatically drive the `McpProxy` to send variations.
3.  **Issue Reporting:** Implement the UI to show "Vulnerabilities" found (or log to Burp Dashboard).

### Step 3: Specific Vulnerability Modules
1.  Implement **Schema Fuzzer** (Type Confusion).
2.  Implement **Resource Iterator** (BOLA).

### Step 4: Verification
1.  Update `mcp_server.py` with vulnerabilities.
2.  Run the Scanner against it and verify findings.