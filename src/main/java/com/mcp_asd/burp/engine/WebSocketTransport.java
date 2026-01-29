package com.mcp_asd.burp.engine;

import burp.api.montoya.MontoyaApi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class WebSocketTransport implements McpTransport {
    private final MontoyaApi api;
    private OkHttpClient client;
    private WebSocket webSocket;
    private String targetHost;
    private int targetPort;

    public WebSocketTransport(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public void connect(String host, int port, String path, TransportListener listener) {
        this.targetHost = host;
        this.targetPort = port;

        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("http://" + targetHost + ":" + targetPort + path) 
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                listener.onOpen();
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                listener.onMessage(text);
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                webSocket.close(1000, null);
                listener.onClose();
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                listener.onError(t);
            }
        });
    }

    @Override
    public void send(String message) {
        if (webSocket != null) {
            api.logging().logToOutput("WebSocketTransport: Sending: " + message);
            webSocket.send(message);
        } else {
            api.logging().logToError("WebSocketTransport: Cannot send, socket is null.");
        }
    }

    @Override
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}
