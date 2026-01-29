package com.mcp_asd.burp.engine;

import burp.api.montoya.MontoyaApi;
import com.mcp_asd.burp.ui.ConnectionConfiguration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

public class SseTransport implements McpTransport {
    private final MontoyaApi api;
    private OkHttpClient client;
    private EventSource eventSource;
    private ConnectionConfiguration config;
    private volatile String postEndpointUrl;

    public SseTransport(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public void connect(ConnectionConfiguration config, TransportListener listener) {
        this.config = config;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.SECONDS); // Infinite timeout for SSE

        if (config.isUseMtls()) {
            configureMtls(builder, config);
        }

        client = builder.build();

        String url = "http://" + config.getHost() + ":" + config.getPort() + config.getPath();
        if (config.isUseTls() || config.isUseMtls()) {
            url = url.replace("http://", "https://");
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "text/event-stream");

        // Add custom headers
        config.getHeaders().forEach(requestBuilder::addHeader);

        Request sseRequest = requestBuilder.build();

        EventSourceListener esListener = new EventSourceListener() {
            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull okhttp3.Response response) {
                api.logging().logToOutput("SseTransport: Connection established. Waiting for 'endpoint' event...");
            }

            @Override
            public void onEvent(@NotNull EventSource eventSource, String id, String type, @NotNull String data) {
                if ("endpoint".equals(type)) {
                    String baseUrl = (config.isUseTls() || config.isUseMtls() ? "https://" : "http://") + config.getHost() + ":" + config.getPort();
                    if (data.startsWith("http://") || data.startsWith("https://")) {
                        postEndpointUrl = data;
                    } else if (data.startsWith("/")) {
                        postEndpointUrl = baseUrl + data;
                    } else {
                        // Relative to current path - simple heuristic, append to baseUrl + path's parent? 
                        // Standard says relative to the request URI.
                        // For safety/simplicity with standard MCP, it's usually root relative or absolute.
                        // We'll treat non-slash start as relative to base for now or just append.
                        postEndpointUrl = baseUrl + (data.startsWith("/") ? "" : "/") + data;
                    }
                    api.logging().logToOutput("SseTransport: Received endpoint event. POST URL: " + postEndpointUrl);
                    listener.onOpen();
                } else {
                    listener.onMessage(data);
                }
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                listener.onClose();
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, Throwable t, okhttp3.Response response) {
                if (response != null) {
                    String body = "";
                    try {
                        body = response.body() != null ? response.body().string() : "";
                    } catch (Exception e) {
                        body = " (failed to read body)";
                    }
                    listener.onError(new RuntimeException("HTTP " + response.code() + ": " + response.message() + "\nBody: " + body));
                } else {
                    listener.onError(t);
                }
            }
        };

        EventSource.Factory factory = EventSources.createFactory(client);
        this.eventSource = factory.newEventSource(sseRequest, esListener);
    }

    private void configureMtls(OkHttpClient.Builder builder, ConnectionConfiguration config) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(config.getClientCertPath())) {
                keyStore.load(fis, config.getClientCertPassword().toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, config.getClientCertPassword().toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).getTrustManagers()[0]);
            
            // Note: For testing against local servers with self-signed certs, 
            // you might need to relax hostname verification, but we'll stick to standard for now.
        } catch (Exception e) {
            api.logging().logToError("Failed to configure mTLS: " + e.getMessage());
        }
    }

    @Override
    public void send(String message) {
        if (client == null || config == null) return;
        new Thread(() -> {
            try {
                String url;
                if (postEndpointUrl != null) {
                    url = postEndpointUrl;
                } else {
                    api.logging().logToError("SseTransport: Warning - Sending message before 'endpoint' event received. Using default path.");
                    url = "http://" + config.getHost() + ":" + config.getPort() + config.getPath();
                    if (config.isUseTls() || config.isUseMtls()) {
                        url = url.replace("http://", "https://");
                    }
                }

                api.logging().logToOutput("SseTransport: Sending POST to " + url + ": " + message);
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(message, MediaType.get("application/json")));

                // Add custom headers
                config.getHeaders().forEach(requestBuilder::addHeader);

                try (okhttp3.Response response = client.newCall(requestBuilder.build()).execute()) {
                    if (!response.isSuccessful()) {
                        String body = "";
                        try { body = response.body().string(); } catch (Exception ignored) {}
                        api.logging().logToError("SseTransport: Error " + response.code() + ": " + body);
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
