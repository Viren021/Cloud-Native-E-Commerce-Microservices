# Cloud-Native E-Commerce Microservices (Security-Hardened Fork)

> **This is a fork** of [hoangtien2k3's project](https://github.com/hoangtien2k3/ecommerce-microservices).
> The original architecture (13 Spring Boot services, Kafka, Kubernetes,
> Next.js frontend) is his work. This fork focuses on **backend security
> hardening**: fixing access-control flaws and adding tests for them.

## What I changed

### Authentication and registration
- **Fixed a privilege-escalation flaw.** Public registration used to accept
  caller-supplied roles, so anyone could register as ADMIN. Registration now
  ignores submitted roles and always assigns the standard customer role.
- Roles stay consistent between Keycloak and the application database, and a
  failed registration does not leave a half-created account behind.
- Sensitive fields (passwords) are no longer logged during registration.

### Authorization
- **Product and category writes are admin-only.** Anonymous callers get
  `401`, regular users get `403`, and public storefront reads still work.
- **Ownership checks (IDOR protection)** on profiles, carts, orders and
  payments. Users can no longer read or modify another user's data by
  changing an ID in the URL or request body.
- The Keycloak JWT `sub` is mapped to the local user instead of being treated
  as the database ID.
- Payment status changes are limited to trusted callbacks and authorized
  workflows.

### Notification service
- Restricted arbitrary email sending to authorized callers.
- Removed attachments based on server filesystem paths in favor of validated
  uploads (size and type limits).
- Notifications go to the correct recipient, with idempotent handling so
  retried or replayed events don't send duplicates.

### Testing
- Added automated security tests, for example:
    - a regular user cannot create or delete products (403)
    - a request with `roles: ["ADMIN"]` cannot create an administrator
    - User A cannot read, update or cancel User B's cart, order or profile
    - forged `userId` values in request bodies are ignored

## Original architecture (from the upstream project)
- 13 Spring Boot services (Java 21, virtual threads)
- Apache APISIX API gateway with JWT validation and rate limiting
- Event-driven design with Apache Kafka (KRaft mode)
- PostgreSQL, Redis, Elasticsearch, S3-compatible storage
- Observability with Micrometer and Prometheus
- Next.js frontend
- Kubernetes (k3d locally, Argo CD for GitOps)

## Tech stack
Java 21, Spring Boot 3, Spring Security, Keycloak (OAuth2/JWT), Kafka,
PostgreSQL, Maven, JUnit

## License
This project is a fork of hoangtien2k3's repository, licensed under the MIT
License. Original copyright (c) 2026 Hoàng Anh Tiến. Modifications
(c) 2026 Viren Hadawale. See [LICENSE](LICENSE) for details.