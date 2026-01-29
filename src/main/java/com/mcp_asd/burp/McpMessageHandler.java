package com.mcp_asd.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import com.mcp_asd.burp.engine.SessionStore;

public class McpMessageHandler {

    private final MontoyaApi api;
    private final SessionStore sessionStore;

    public McpMessageHandler(MontoyaApi api, SessionStore sessionStore) {
        this.api = api;
        this.sessionStore = sessionStore;
    }

    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // TODO: Implement MCP message handling
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // TODO: Implement MCP message handling
        return ResponseReceivedAction.continueWith(responseReceived);
    }
}
