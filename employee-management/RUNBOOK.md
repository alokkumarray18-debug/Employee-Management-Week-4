# Runbook — get this submitted

Run these on your own machine (this sandbox can't reach Maven Central, so none of this
has actually been executed yet — do it in order).

## 0. Prerequisites
- Java 17+ (`java -version`)
- Maven 3.8+ (`mvn -version`)
- MySQL 8 running locally, OR Docker Desktop if you'd rather use `docker compose`

## 1. Build & run the tests (do this first)
```bash
cd employee-management
mvn clean test
```
Watch for compile errors — this is the first time this exact code has actually been
compiled. If something fails, paste the error back to me and I'll fix it directly.

If it passes, open the coverage report and screenshot it (deliverable requirement):
```bash
# macOS
open target/site/jacoco/index.html
# Linux
xdg-open target/site/jacoco/index.html
# Windows
start target/site/jacoco/index.html
```

## 2. Set up the database
Option A — plain MySQL:
```bash
cp .env.example .env    # fill in your real local MySQL password
export $(grep -v '^#' .env | xargs)   # or use direnv/whatever you prefer
mysql -u root -p < sql/schema.sql
```
Option B — let Hibernate create it on boot (skip schema.sql):
`application-local.properties` already has `ddl-auto=update`.

## 3. Run the app
```bash
mvn spring-boot:run
```
or with Docker (MySQL + app together, no local MySQL needed):
```bash
docker compose up --build
```

## 4. Verify it's alive
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## 5. Exercise the API and take fresh screenshots
Import `postman/Employee-Management.postman_collection.json` into Postman, run through:
create department → create employee → get all (paginated) → search → update → delete.
Replace the old `screenshots/*.png` (those are Week 3, hitting `/api/employees` which
no longer exists) with new ones against `/api/v1/...`.

## 6. Commit
The refactor itself is already committed (12 commits on top of your Week 3 history —
run `git log --oneline` to see them). Just commit your new screenshots:
```bash
git add screenshots/
git commit -m "docs: replace Week 3 screenshots with Week 4 /api/v1 screenshots"
git push
```

## If `mvn clean test` fails
Send me the exact error output — most likely causes, in order of probability:
1. A typo I introduced that only a real compiler would catch
2. A Spring Boot 3.3-specific package path I got slightly wrong (e.g. `RestClientConfig`)
3. A Lombok annotation processing hiccup — try `mvn clean test -U` to force a dependency refresh
