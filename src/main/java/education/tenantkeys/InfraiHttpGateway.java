package education.tenantkeys;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class InfraiHttpGateway implements InfraiGateway {
    private final InfraiConfig config;
    private final HttpClient http;

    public InfraiHttpGateway(InfraiConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    InfraiHttpGateway(InfraiConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public Map<String, Object> createUser(Map<String, Object> body) throws IOException, InterruptedException {
        // infrai.auth.user.create
        return request("POST", "/v1/auth/user/create", body);
    }

    @Override
    public Map<String, Object> createKey(Map<String, Object> body) throws IOException, InterruptedException {
        // infrai.account.keys.create
        return request("POST", "/v1/account/keys/create", body);
    }

    @Override
    public void revokeKey(String keyId) throws IOException, InterruptedException {
        // infrai.account.keys.revoke
        request("DELETE", "/v1/account/keys/revoke/" + encodeSegment(keyId), null);
    }

    @Override
    public void deleteUser(String userId) throws IOException, InterruptedException {
        // infrai.auth.user.delete
        request("DELETE", "/v1/auth/user/delete/" + encodeSegment(userId), null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(String method, String path, Map<String, Object> body)
            throws IOException, InterruptedException {
        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUrl().resolve(path))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(Json.write(body)));
            }

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, Object> envelope = Json.readObject(response.body());
            if (response.statusCode() == 429 && attempt < config.maxAttempts()) {
                pause(response, attempt);
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Map<String, Object> error = envelope.get("error") instanceof Map<?, ?> raw
                        ? (Map<String, Object>) raw : Map.of("message", "Request rejected");
                String code = String.valueOf(error.getOrDefault("code", "REQUEST_REJECTED"));
                throw new InfraiException(code, error, response.statusCode());
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport response " + response.statusCode());
            Object data = envelope.get("data");
            return data instanceof Map<?, ?> raw ? (Map<String, Object>) raw : Map.of();
        }
        throw new IOException("Retry attempts exhausted");
    }

    private static void pause(HttpResponse<?> response, int attempt) throws InterruptedException {
        long seconds = response.headers().firstValue("Retry-After").map(InfraiHttpGateway::seconds)
                .orElse(1L << (attempt - 1));
        Thread.sleep(Duration.ofSeconds(Math.min(seconds, 30)).toMillis());
    }

    private static long seconds(String value) {
        try { return Math.max(0, Long.parseLong(value)); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private static String encodeSegment(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
