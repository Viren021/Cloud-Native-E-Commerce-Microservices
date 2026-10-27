# ecommerce-microservices (my fork)

A production‑grade, cloud‑native e‑commerce platform built with **13 Spring Boot 3 micro‑services**, Kafka, Kubernetes, and a Next.js frontend. This fork contains custom enhancements, a full CI pipeline, and updated documentation.

**Key features**
- 13 Spring Boot services (Java 21, virtual threads)
- Apache APISIX API gateway with JWT validation, rate‑limiting, and OpenID Connect
- Event‑driven architecture using Apache Kafka (KRaft mode)
- PostgreSQL, Redis, Elasticsearch, and RustFS (S3‑compatible) storage
- Observability via Micrometer → Prometheus and distributed correlation IDs
- Frontend built with Next.js 16, React 19, Zustand, and TanStack Query
- Local deployment with k3d and production GitOps with Argo CD
- CI/CD pipeline (GitHub Actions) with SonarCloud quality gates and Docker‑less builds using Jib

*For licensing information, see the [LICENSE](LICENSE) file. This project is based on an MIT‑licensed codebase originally authored by **hoangtien2k3**; the modifications in this repository are my own work (also MIT‑licensed).*