package org.opensagetv.vibe.coremcp;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

final class BridgeHttpServer {
    private static final int MAX_BODY = 64 * 1024;
    private final String bindAddress;
    private final int port;
    private final String token;
    private final ControlService controls;
    private HttpServer server;
    private ExecutorService executor;

    BridgeHttpServer(String bindAddress, int port, String token, ControlService controls) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.token = token;
        this.controls = controls;
    }

    synchronized void start() throws IOException {
        if (server != null) return;
        InetAddress address = InetAddress.getByName(bindAddress);
        server = HttpServer.create(new InetSocketAddress(address, port), 16);
        server.createContext("/health", new HealthHandler());
        server.createContext("/v1/control", new ControlHandler());
        executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            private int sequence;
            public synchronized Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "VibeCoreMcpHttp-" + (++sequence));
                thread.setDaemon(true);
                return thread;
            }
        });
        server.setExecutor(executor);
        server.start();
    }

    synchronized void stop() {
        if (server != null) server.stop(1);
        server = null;
        if (executor != null) executor.shutdownNow();
        executor = null;
    }

    private final class HealthHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, error("method_not_allowed"));
                return;
            }
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("status", "ok");
            out.put("plugin", SageTVCoreMcpPlugin.NAME);
            out.put("version", SageTVCoreMcpPlugin.VERSION);
            out.put("capabilityVersion", Integer.valueOf(ControlService.CAPABILITY_VERSION));
            send(exchange, 200, out);
        }
    }

    private final class ControlHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, error("method_not_allowed"));
                return;
            }
            if (!authenticated(exchange.getRequestHeaders())) {
                send(exchange, 401, error("authentication_required"));
                return;
            }
            try {
                Map<String, String> request = parseForm(readBody(exchange.getRequestBody()));
                send(exchange, 200, controls.execute(request));
            } catch (IllegalArgumentException invalid) {
                send(exchange, 400, error(invalid.getMessage()));
            } catch (Throwable failure) {
                String detail = failure.getMessage();
                if (detail == null || detail.trim().length() == 0) detail = failure.getClass().getSimpleName();
                send(exchange, 500, error(detail));
                SageTVCoreMcpPlugin.log("request failed: " + failure);
            }
        }
    }

    private boolean authenticated(Headers headers) {
        String supplied = headers.getFirst("Authorization");
        if (supplied == null || !supplied.startsWith("Bearer ")) return false;
        byte[] expected = token.getBytes(StandardCharsets.UTF_8);
        byte[] actual = supplied.substring(7).trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] readBody(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BODY) throw new IllegalArgumentException("request_too_large");
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static Map<String, String> parseForm(byte[] body) throws IOException {
        Map<String, String> out = new LinkedHashMap<String, String>();
        String text = new String(body, StandardCharsets.UTF_8);
        if (text.length() == 0) return out;
        for (String part : text.split("&")) {
            int separator = part.indexOf('=');
            String key = separator < 0 ? part : part.substring(0, separator);
            String value = separator < 0 ? "" : part.substring(separator + 1);
            key = URLDecoder.decode(key, "UTF-8");
            value = URLDecoder.decode(value, "UTF-8");
            if (key.length() == 0 || out.containsKey(key)) throw new IllegalArgumentException("invalid_or_duplicate_parameter");
            out.put(key, value);
        }
        return out;
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ok", Boolean.FALSE);
        out.put("error", message == null ? "request_failed" : message);
        return out;
    }

    private static void send(HttpExchange exchange, int status, Map<String, Object> value) throws IOException {
        byte[] payload = (Json.encode(value) + "\n").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, payload.length);
        OutputStream output = exchange.getResponseBody();
        try { output.write(payload); }
        finally { output.close(); }
    }
}

