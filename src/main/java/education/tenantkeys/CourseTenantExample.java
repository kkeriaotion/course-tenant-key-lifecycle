package education.tenantkeys;

public final class CourseTenantExample {
    private CourseTenantExample() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: CourseTenantExample <tenant-id> <admin-email> <admin-name>");
            System.exit(2);
        }
        InfraiConfig config = InfraiConfig.fromEnvironment();
        TenantLifecycleService lifecycle = new TenantLifecycleService(new InfraiHttpGateway(config));
        TenantCredential credential = lifecycle.onboard(args[0], args[1], args[2]);

        System.out.printf("Tenant %s onboarded: user=%s key_id=%s scopes=%s%n",
                credential.tenantId(), credential.userId(), credential.keyId(), credential.scopes());
        System.out.println("Store the plaintext key now; it cannot be retrieved a second time.");
        System.out.println("Plaintext key: " + credential.plaintextKey());
    }
}
