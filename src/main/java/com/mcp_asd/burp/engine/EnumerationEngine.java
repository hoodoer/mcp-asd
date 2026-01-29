package com.mcp_asd.burp.engine;

import burp.api.montoya.MontoyaApi;
import com.mcp_asd.burp.test.SecurityTester;
import com.mcp_asd.burp.ui.DashboardTab;
import org.json.JSONObject;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EnumerationEngine implements TransportListener {
    private final MontoyaApi api;
    private DashboardTab dashboardTab;
    private final SessionStore sessionStore;
    private final SecurityTester tester;
    private McpTransport transport;
    private CountDownLatch latch;

    public EnumerationEngine(MontoyaApi api, DashboardTab dashboardTab, SecurityTester tester, SessionStore sessionStore) {
        this.api = api;
        this.dashboardTab = dashboardTab;
        this.tester = tester;
        this.sessionStore = sessionStore;
    }

    public void setDashboardTab(DashboardTab dashboardTab) {
        this.dashboardTab = dashboardTab;
    }

    public void start(String host, int port, String transportType, String path) {
        if (dashboardTab != null) {
            dashboardTab.setTarget(host, port);
            dashboardTab.setStatus("🟠 Connecting via " + transportType + "...", java.awt.Color.ORANGE.darker());
        }
        api.logging().logToOutput("Starting enumeration for " + host + ":" + port + " via " + transportType);

        new Thread(() -> {
            try {
                latch = new CountDownLatch(3); // tools, resources, prompts
                
                if ("WebSocket".equals(transportType)) {
                    transport = new WebSocketTransport(api);
                } else {
                    transport = new SseTransport(api);
                }
                
                transport.connect(host, port, path, this);

                if (!latch.await(15, TimeUnit.SECONDS)) {
                    api.logging().logToError("Enumeration timed out waiting for responses.");
                    if (dashboardTab != null) dashboardTab.setStatus("🔴 Connection Timed Out", java.awt.Color.RED);
                } else {
                    api.logging().logToOutput("Enumeration completed successfully.");
                    if (dashboardTab != null) dashboardTab.setStatus("🟢 Connected & Ready", java.awt.Color.GREEN.darker());
                }

            } catch (Exception e) {
                api.logging().logToError("Exception during enumeration: " + e.getMessage(), e);
                if (dashboardTab != null) dashboardTab.setStatus("🔴 Error: " + e.getMessage(), java.awt.Color.RED);
            }
        }).start();
    }

    public void sendRequest(String requestBody) {
        if (transport != null) {
            transport.send(requestBody);
        }
    }

    // --- TransportListener Implementation ---

    @Override
    public void onOpen() {
        api.logging().logToOutput("Transport connected.");
        if (dashboardTab != null) dashboardTab.setStatus("🔵 Enumerating...", java.awt.Color.BLUE.darker());
        
        // Trigger initial discovery
        sendRequest("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"id\":\"" + java.util.UUID.randomUUID() + "\"}");
        sendRequest("{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"" + java.util.UUID.randomUUID() + "\"}");
        sendRequest("{\"jsonrpc\":\"2.0\",\"method\":\"resources/list\",\"id\":\"" + java.util.UUID.randomUUID() + "\"}");
        sendRequest("{\"jsonrpc\":\"2.0\",\"method\":\"prompts/list\",\"id\":\"" + java.util.UUID.randomUUID() + "\"}");
    }

    @Override
    public void onMessage(String data) {
        api.logging().logToOutput("Received event data: " + data);
        try {
            JSONObject json = new JSONObject(data);
            
            // 1. Correlation Logic for Proxy
            if (json.has("id") && !json.isNull("id")) {
                String msgId = json.getString("id");
                if (sessionStore.getRequest(msgId) != null) {
                    api.logging().logToOutput("Engine: Found matching pending request for ID: " + msgId);
                    sessionStore.completeRequest(msgId, json);
                }
            }

            // 2. Enumeration Logic
            if (json.has("result")) {
                 if (data.contains("get_weather")) { // Heuristic to identify tools
                    SwingUtilities.invokeLater(() -> dashboardTab.updateTools(json.getJSONObject("result")));
                    if (latch != null) latch.countDown();
                } else if (data.contains("user_data")) { // Heuristic for resources
                    SwingUtilities.invokeLater(() -> dashboardTab.updateResources(json.getJSONObject("result")));
                    if (latch != null) latch.countDown();
                } else if (data.contains("summarize_text")) { // Heuristic for prompts
                    SwingUtilities.invokeLater(() -> dashboardTab.updatePrompts(json.getJSONObject("result")));
                    if (latch != null) latch.countDown();
                }
            }
        } catch (Exception e) {
            api.logging().logToError("Failed to parse event JSON: " + e.getMessage());
        }
    }

    @Override
    public void onClose() {
        api.logging().logToOutput("Transport closed.");
    }

    @Override
    public void onError(Throwable t) {
        api.logging().logToError("Transport failure: " + (t != null ? t.getMessage() : "Unknown error"));
        if (latch != null) {
            while (latch.getCount() > 0) latch.countDown();
        }
    }
}
