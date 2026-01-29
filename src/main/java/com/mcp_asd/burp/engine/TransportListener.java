package com.mcp_asd.burp.engine;

public interface TransportListener {
    void onMessage(String message);
    void onOpen();
    void onClose();
    void onError(Throwable t);
}
