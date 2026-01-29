package com.mcp_asd.burp.engine;

import burp.api.montoya.MontoyaApi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class SseTransport implements McpTransport {
    private final MontoyaApi api;
    private OkHttpClient client;
    private EventSource eventSource;
    private String targetHost;
    private int targetPort;
    private String targetPath;

    public SseTransport(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public void connect(String host, int port, String path, TransportListener listener) {
        this.targetHost = host;
        this.targetPort = port;
        this.targetPath = path;

        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.SECONDS) // Infinite timeout for SSE
                .build();

        Request sseRequest = new Request.Builder()
                .url("http://" + targetHost + ":" + targetPort + targetPath)
                .get()
                .addHeader("Accept", "text/event-stream")
                .build();

        EventSourceListener esListener = new EventSourceListener() {
            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull okhttp3.Response response) {
                listener.onOpen();
            }

            @Override
            public void onEvent(@NotNull EventSource eventSource, String id, String type, @NotNull String data) {
                listener.onMessage(data);
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                listener.onClose();
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, Throwable t, okhttp3.Response response) {
                listener.onError(t);
            }
        };

        EventSource.Factory factory = EventSources.createFactory(client);
        this.eventSource = factory.newEventSource(sseRequest, esListener);
    }

    @Override
    public void send(String message) {
        if (client == null || targetHost == null) return;
        new Thread(() -> {
            try {
                api.logging().logToOutput("SseTransport: Sending POST: " + message);
                Request request = new Request.Builder()
                        .url("http://" + targetHost + ":" + targetPort + targetPath)
                        .post(RequestBody.create(message, okhttp3.MediaType.get("application/json")))
                        .build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        api.logging().logToError("SseTransport: Error " + response.code() + ": " + response.body().string());
                    }
                }
            } catch (Exception e) {
                api.logging().logToError("SseTransport: Send failed: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void close() {
        if (eventSource != null) {
            eventSource.cancel();
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}
