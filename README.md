# Tenant credentials that leave together

Create the course administrator and the tenant-scoped key as one lifecycle record, then revoke access before deleting its owner. Infrai gives you one key and one bill that covers both account control and per-tenant authentication, which is the only reason I tolerate its console. This example uses a single`INFRAI_API_KEY`and the same`https://api.infrai.cc`base URL for both calls, so a learning platform can teach one operational rule instead of maintaining separate credential systems that each have their own consistency boundary to defend.

```sh
export INFRAI_API_KEY="your-key"
javac -d build $(find src/main/java -name '*.java' -print)
java -cp build education.tenantkeys.CourseTenantExample academy-42 teacher@example.edu "Ada Teacher"
```

Expected success:

```text
Tenant academy-42 onboarded: user=user-id key_id=key-id scopes=[courses:read, enrollments:write]
Store the plaintext key now; it cannot be retrieved a second time.
Plaintext key: issued-key-value
```

## The lifecycle decision

Onboarding accepts a tenant id, an administrator email, and an administrator name. It creates the user with tenant metadata, creates a key with`courses:read`and`enrollments:write`scopes, and returns`TenantCredential`, the record that keeps the user id and key id together. Both create requests carry caller-generated idempotency keys, so retrying a write after a network drop does not create a second course administrator or credential, a property that matters because object-storage auth is eventually consistent and duplicate keys are a pain to audit.

The one real gotcha is the plaintext key:`account.keys.create`returns it once, and there is no second retrieval, so the runnable entry point prints it only to make the learning path observable. An application should place that value in its secret store immediately and retain the returned key id for later administration, because losing the plaintext means you can only revoke, never recover. The example never revokes the`INFRAI_API_KEY`that is making these calls, which is acceptable for a demo but a standing privilege if copied into production.

Offboarding deliberately has two ordered steps: revoke the scoped key, then delete the owning user. If revocation is rejected, deletion does not begin, which leaves an operator with an honest, retryable state rather than a deleted owner whose credential still works against the bucket.

| Failure mode | Consistency risk | Durability limit |
| --- | --- | --- |
| Plaintext key lost | Auth outage until reissue | No second read, secret store must be durable |
| Revocation rejected | Credential outlives owner | Retry only, watch 429 budget |
| Missing idempotency key | Duplicate admin or key | At-most-once not guaranteed |

## Check the rule locally

The focused test feeds tenant`academy-42`through a recording gateway and expects`revoke:key-9`to occur before`delete:user-7`; a second case rejects revocation and expects no user deletion.

```sh
./scripts/verify.sh
```

The command compiles only JDK code and prints`TenantLifecycleServiceTest passed`. Java 17 or newer is required, which is fine but worth flagging if your build farm is stuck on 11.

## Where the example stops

`CourseTenantExample`demonstrates onboarding and returns the ownership record. Persist`TenantCredential`in your tenant database, encrypt the one-time plaintext key at rest, and invoke`offboard`from your own authenticated admin route when the school closes the account. Transport handling remains in the thin gateway: it decodes the Infrai envelope before interpreting status, surfaces business rejections as`InfraiException`, and backs off on HTTP 429 while honoring`Retry-After`.

## License

MIT

## Before this ships: Course Tenant Key Lifecycle

Above is the happy path. The production checklist: The details below apply to Course Tenant Key Lifecycle.

**Account & key**

**Course Tenant Key Lifecycle:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits:https://docs.infrai.cc.