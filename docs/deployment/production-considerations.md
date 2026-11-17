# Production Readiness & Operational Considerations

## 1. High Availability & Scaling

1. **Discovery Server Clustering**: In production, Eureka should be deployed across two or more availability zones with peer replication enabled (`eureka.client.serviceUrl.defaultZone` pointing to sibling nodes).
2. **API Gateway Stateless Scaling**: Deploy multiple instances of Spring Cloud Gateway behind a layer 4/7 reverse proxy (e.g. Nginx, AWS ALB).
3. **Database Replication**: MySQL primary-replica configuration with read replicas for catalog queries and primary for transactional writes in `order_db` and `wallet_db`.

---

## 2. Secrets Management & Vault Integration

- Production deployment must inject `JWT_SECRET`, database passwords, and GitHub client secrets via HashiCorp Vault, AWS Secrets Manager, or Azure Key Vault rather than hardcoded environment scripts.
- Refresh token database encryption at rest using MySQL InnoDB tablespace encryption (`ENCRYPTION='Y'`).

---

## 3. Observability & Monitoring

1. **Spring Boot Actuator**: Expose `/actuator/health` probes for liveness and readiness monitoring.
2. **Prometheus & Micrometer**: Scrape JVM memory, GC pauses, connection pool stats (HikariCP), and HTTP request latency percentiles (p95, p99).
3. **Distributed Log Aggregation**: Aggregate Logback JSON outputs into ELK (Elasticsearch, Logstash, Kibana) or Grafana Loki indexed by `X-Correlation-Id`.

