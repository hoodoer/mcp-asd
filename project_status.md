# Project Status and Next Steps: MCP Attack Surface Detector (MCP-ASD)

## Phase 1 Status: COMPLETE ✅

**Key Accomplishments:**
*   **Architecture:** Modular Transport Layer (SSE + WebSocket) fully implemented.
*   **Core Engine:** `EnumerationEngine` successfully discovers Tools, Resources, and Prompts.
*   **Stitching:** `McpProxy` (Internal Server) successfully bridges Burp Intruder/Repeater to asynchronous MCP traffic with ID auto-generation.
*   **UI:** 3-Pane Dashboard, Status Indicators, and robust Connection Dialog with Auto-Detection.
*   **Reliability:** Fixed threading issues, DNS resolution bugs, and socket connection flaws.

## Phase 2: Automation & Authentication (IN PROGRESS)

### 1. Authentication Support
**Priority:** High
**Objective:** Connect to secured MCP servers.
*   [ ] Update `ConnectionDialog` to accept custom HTTP Headers (Authorization).
*   [ ] Propagate headers to `SseTransport` and `WebSocketTransport`.
*   [ ] Support Auth Payloads in JSON-RPC `initialize`.

### 2. Automated Vulnerability Scanning
**Priority:** High
**Objective:** Active audit of MCP Primitives.
*   [ ] **Infrastructure:** Create `Scanner` module.
*   [ ] **Type Confusion:** Fuzz tool arguments with invalid types.
*   [ ] **BOLA:** Iterate Resource URIs.
*   [ ] **Injection:** Probe String arguments for injection markers.

### 3. Test Environment
*   [ ] Update `mcp_server.py` to support Authentication (Header check).
*   [ ] Add vulnerable endpoints to `mcp_server.py` for scanner verification.

---
**Current Build:** Stable
**Version:** 1.0 (Phase 1 Final)
