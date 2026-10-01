package education.tenantkeys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TenantLifecycleServiceTest {
    public static void main(String[] args) throws Exception {
        recordsOwnershipAndRemovesAccessBeforeOwner();
        doesNotDeleteOwnerWhenRevocationIsRejected();
        System.out.println("TenantLifecycleServiceTest passed");
    }

    private static void recordsOwnershipAndRemovesAccessBeforeOwner() throws Exception {
        RecordingGateway gateway = new RecordingGateway(false);
        TenantLifecycleService service = new TenantLifecycleService(gateway);
        TenantCredential credential = service.onboard("academy-42", "teacher@example.edu", "Ada Teacher");

        check(credential.userId().equals("user-7"), "credential must retain its owner");
        check(credential.keyId().equals("key-9"), "credential must retain its key id");
        service.offboard(credential);
        check(gateway.calls.equals(List.of("create-user", "create-key", "revoke:key-9", "delete:user-7")),
                "access must be revoked before the owner is deleted");
    }

    private static void doesNotDeleteOwnerWhenRevocationIsRejected() throws Exception {
        RecordingGateway gateway = new RecordingGateway(true);
        TenantLifecycleService service = new TenantLifecycleService(gateway);
        TenantCredential credential = service.onboard("academy-42", "teacher@example.edu", "Ada Teacher");
        try {
            service.offboard(credential);
            throw new AssertionError("expected revocation rejection");
        } catch (InfraiException expected) {
            check(gateway.calls.equals(List.of("create-user", "create-key", "revoke:key-9")),
                    "owner deletion must not run after rejected revocation");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RecordingGateway implements InfraiGateway {
        private final boolean rejectRevocation;
        private final List<String> calls = new ArrayList<>();

        RecordingGateway(boolean rejectRevocation) { this.rejectRevocation = rejectRevocation; }

        public Map<String, Object> createUser(Map<String, Object> body) {
            calls.add("create-user");
            check(body.containsKey("idempotency_key"), "user creation must be retry-safe");
            return Map.of("user_id", "user-7");
        }

        public Map<String, Object> createKey(Map<String, Object> body) {
            calls.add("create-key");
            check(body.get("scopes").equals(List.of("courses:read", "enrollments:write")), "scopes must be tenant-shaped");
            check(body.containsKey("idempotency_key"), "key creation must be retry-safe");
            return Map.of("key_id", "key-9", "key_secret", "issued-once-value");
        }

        public void revokeKey(String keyId) throws IOException {
            calls.add("revoke:" + keyId);
            if (rejectRevocation) throw new InfraiException("REJECTED", Map.of("message", "rejected"), 409);
        }

        public void deleteUser(String userId) { calls.add("delete:" + userId); }
    }
}
