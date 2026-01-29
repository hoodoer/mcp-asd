package com.mcp_asd.burp.engine;

public interface McpTransport {
    void connect(String host, int port, String path, TransportListener listener);
    void send(String message);
    void close();
}
