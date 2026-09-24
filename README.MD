# zx-common
Reusable Java 21 and Spring Boot 3 common libraries. The modules contain no company-specific service, registry, credential, or internal network dependency.

## Modules

- `common-base`: JSON, reflection, date, HTTP, AOP, retry, and conversion helpers
- `common-lock`: Redis based distributed lock aspect
- `common-rpc`: annotation driven HTTP clients
- `common-crawl`: Jsoup and HtmlUnit crawling with annotated field mapping
- `common-web`: response wrapping and exception handling
- `common-repository`: Spring Data JPA query and repository extensions

## Requirements

- JDK 21
- Maven 3.9+
- Spring Boot 3.5+

Build all modules:

```bash
mvn -DskipTests verify
```
