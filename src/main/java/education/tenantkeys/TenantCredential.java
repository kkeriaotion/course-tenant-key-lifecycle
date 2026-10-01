package education.tenantkeys;

import java.util.List;

public record TenantCredential(
        String tenantId,
        String userId,
        String keyId,
        String plaintextKey,
        List<String> scopes) {
    public TenantCredential {
        scopes = List.copyOf(scopes);
    }
}
