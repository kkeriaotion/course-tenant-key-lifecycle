package education.tenantkeys;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TenantLifecycleService {
    private final InfraiGateway infrai;

    public TenantLifecycleService(InfraiGateway infrai) {
        this.infrai = infrai;
    }

    public TenantCredential onboard(String tenantId, String adminEmail, String adminName)
            throws IOException, InterruptedException {
        String operationId = UUID.randomUUID().toString();
        Map<String, Object> user = infrai.createUser(Map.of(
                "email", adminEmail,
                "name", adminName,
                "metadata", Map.of("tenant_id", tenantId, "role", "course_admin"),
                "idempotency_key", operationId + ":user"));
        String userId = required(user, "user_id");

        List<String> scopes = List.of("courses:read", "enrollments:write");
        Map<String, Object> key = infrai.createKey(Map.of(
                "project_id", tenantId,
                "name", "course-admin-" + userId,
                "scopes", scopes,
                "idempotency_key", operationId + ":key"));
        return new TenantCredential(tenantId, userId, required(key, "key_id"), required(key, "key_secret"), scopes);
    }

    public void offboard(TenantCredential credential) throws IOException, InterruptedException {
        // Revoke access before deleting its owner so offboarding cannot report completion halfway through.
        infrai.revokeKey(credential.keyId());
        infrai.deleteUser(credential.userId());
    }

    private static String required(Map<String, Object> data, String field) {
        Object value = data.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Response data is missing " + field);
        }
        return text;
    }
}
