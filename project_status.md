# Project Status: MCP Attack Surface Detector

**Current Version:** 0.5.2 (Alpha)
**Build Status:** Stable / Ready for Phase 3

## Development Phases

### Phase 1: Core Infrastructure (Completed)
*   **Transport:** SSE & WebSocket support.
*   **Discovery:** Auto-enumeration of Tools, Resources, Prompts.
*   **Bridging:** Synchronous Burp (Repeater/Intruder) <-> Asynchronous MCP Proxy.

### Phase 2: Automation & Authentication (Completed)
*   **Authentication:**
    *   Custom HTTP Headers (OAuth/API Keys).
    *   mTLS (Client Certificates).
    *   Initialization Parameter Injection.
*   **Automated Scanning:**
    *   Basic Active Scanner Engine.
    *   Checks: Type Confusion, Input Reflection, BOLA (Resources).
*   **Verification:** Verified against local `mcp_server.py`.

### Phase 2.5: Polish & Hardening (Completed - Jan 2026)
*   **Protocol Compliance:** 
    *   Updated method calls to `tools/call` (replacing legacy `tools/invoke`) to match official MCP specification.
    *   Verified compatibility with public servers (DeepWiki, CoinAPI).
*   **Connection Stability:**
    *   Fixed race conditions in SSE transport (handling immediate vs delayed `endpoint` events).
    *   Fixed `405 Method Not Allowed` errors on hybrid stateless/stateful servers.
    *   Fixed Auto-Detector bug for "Auth Required" WebSocket endpoints.
*   **UI/UX Improvements:**
    *   **Server Info:** Added rich HTML dialog to view Server Version, Capabilities, and System Instructions.
    *   **Feedback:** Improved "Enumerating..." status indicators and automated UI clearing on new connections.

### Phase 3: Advanced Capabilities (Planned)
*   **Native Integration:** Report issues to Burp Target/Dashboard (`IScanIssue`).
*   **Deep Fuzzing:** Recursive JSON schema parsing for complex objects.
*   **Intelligent Payloads:** LLM-assisted or context-aware payload selection.