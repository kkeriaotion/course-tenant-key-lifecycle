package education.tenantkeys;

import java.io.IOException;
import java.util.Map;

public interface InfraiGateway {
    Map<String, Object> createUser(Map<String, Object> body) throws IOException, InterruptedException;

    Map<String, Object> createKey(Map<String, Object> body) throws IOException, InterruptedException;

    void revokeKey(String keyId) throws IOException, InterruptedException;

    void deleteUser(String userId) throws IOException, InterruptedException;
}
