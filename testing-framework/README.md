# testing-framework

API E2E test framework for apilens-mvp. Java 17 + TestNG + REST Assured.
Tests run against the docker-compose stack (gateway on :8080 by default).

## Prerequisites

```bash
docker compose up -d    # from the apilens-mvp root
mvn -v                  # Java 17
```

## Run

```bash
cd testing-framework

mvn test                                                        # parallel suite (default)
mvn test -DsuiteXmlFile=src/test/resources/testng-parallel-classes.xml
mvn test -DsuiteXmlFile=src/test/resources/testng-sequential.xml
mvn test -DsuiteXmlFile=src/test/resources/testng-smoke.xml
```

`BASE_URL` env var overrides the target (default `http://localhost:8080`).

## Design

- `src/main/java/com/apilens/qa/clients` — one client per service (auth, file, prefs)
- `src/main/java/com/apilens/qa/fixtures` — unique emails/file names per test run
- `src/main/java/com/apilens/qa/utils/ThreadContext` — thread-local auth; parallel-safe
- `BaseParallelApiTest` — each test class registers a **fresh user in a fresh org** in `@BeforeClass`,
  so classes can run in parallel against the same stack; tokens/org data never shared

## Notes

- Endpoints are called through the gateway (`/api/...`); it validates the JWT and injects
  `X-User-Id`/`X-Org-Id` for downstream services.
- Tests are self-contained: each class registers its own fresh user (fresh org), so they do
  **not** depend on the seeded demo account.
- Email-change is rate-limited to 1 change / 24h per user; fresh users per class keep runs isolated.

## DevTools workflow (discovery + debugging)

DevTools is for writing tests, not for CI (CI uses REST Assured).

1. Open `http://localhost` → F12 → Network tab → enable **Preserve log**, filter **Fetch/XHR**.
2. Perform the action in the UI (login, upload, slider drag, filter, profile email change).
3. For each request, capture: method + URL, headers (`Authorization`, `Content-Type`), payload,
   response body + status.
4. Turn that into a scenario: add a client method (if missing) + a test asserting the status and
   key response fields. Save the raw capture in a ticket/PR description for reviewers.
5. When a test fails, the captured vs actual response diff is your first debugging step
   (REST Assured logs request+response on assertion failure).

## Extending

Phase roadmap agreed for this framework:

1. **API (current, 18 tests)** — auth / files / prefs happy paths + key negatives
2. **DB integrity** — JDBC assertions like `microservices-tests` (add `postgresql` dep + DB URLs from env)
3. **Security** — missing-auth 401, spoofed `X-User-Id`/`X-Org-Id` on public paths, org isolation
4. **Contract** — JSON schema validation (`json-schema-validator` is already a dependency)
5. **UI / E2E** — separate Playwright module (not part of this one)

CI runs phases via `testng-*.xml` suites; add new suites per phase.
