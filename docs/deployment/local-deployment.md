# Local Deployment & Execution Guide

## 1. Overview

E-ShoppingZone is explicitly designed for straightforward local deployment on bare metal or developer workstations without requiring Docker or Kubernetes.

---

## 2. System Resource Requirements

- **RAM**: Minimum 8 GB available RAM (12+ GB recommended to run all 8 JVM instances and Vite simultaneously).
- **Disk Space**: ~2 GB free disk space for Maven repository dependencies, npm packages, and database logs.
- **Ports Free**: `8761`, `8080`, `8081`, `8082`, `8083`, `8084`, `8085`, `8086`, `5173`, `3306`.

---

## 3. Automated Local Startup Scripts

To streamline local startup across multiple terminal windows, Windows PowerShell helper scripts are provided:

### 3.1 Start All Backend Services (`start-all-services.ps1`)
Launches each Spring Boot application in a separate background process or PowerShell window with appropriate delay intervals for Eureka discovery initialization.

### 3.2 Stop All Backend Services (`stop-all-services.ps1`)
Identifies and gracefully stops Java processes running on the allocated ports (`8761`, `8080`, `8081`-`8086`).

