# E-Commerce Microservices Platform

A scalable, event-driven microservices architecture for an e-commerce platform built with Spring Boot and Spring Cloud.

## 🚀 Features

- **Product Service**: Product catalog management with MongoDB
- **Inventory Service**: Real-time inventory tracking with MySQL
- **Order Service**: Order processing with event-driven architecture (idempotency + transactional outbox)
- **Notification Service**: Email notifications for order updates
- **API Gateway**: Single entry point with JWT-based OAuth2 resource server security, request routing and load balancing
- **Event-Driven Architecture**: Apache Kafka for asynchronous communication
- **Containerized**: Docker and Docker Compose for easy deployment
- **API Documentation**: Integrated Swagger UI (aggregated at the gateway)
- **Observability**: Actuator, Prometheus metrics, OpenTelemetry agent in images

## 🛠️ Tech Stack

- **Java 21**
- **Spring Boot 3.5.16**
- **Spring Cloud 2025.0.3**
- **Spring Data MongoDB & JPA**
- **Spring Kafka**
- **Spring Security + OAuth2 Resource Server (JWT HS256)**
- **Docker & Docker Compose**
- **MySQL 8.4**
- **MongoDB 7**
- **Apache Kafka 3.9** (KRaft)
- **OpenAPI 3.0 / springdoc**

## 📦 Prerequisites

- Java 21 or later
- Docker Desktop (with Docker Compose)
- Maven 3.9.x or later (or use the included `mvnw` wrappers)
- Git

## 🚀 Quick Start (Local with Docker Compose)

### 1. Clone the repository

```bash
git clone https://github.com/noman-akram29/Java-Spring-Boot-Microservices-E-Commerce-Platform.git
cd Java-Spring-Boot-Microservices-E-Commerce-Platform
git checkout staging
```

### 2. Create `.env` from the example

```bash
cp .env.example .env
```

Edit `.env` and set a strong JWT secret (≥ 32 characters):

```bash
GATEWAY_JWT_SECRET=your-secure-jwt-secret-at-least-32-characters
```

Keep the other values at their local-development defaults unless you need to customize them.

> **Security:** Do not commit `.env` or production credentials to Git.

### 3. Start everything

```bash
docker compose up -d --build
```

### 4. Verify services are healthy

```bash
docker compose ps
```

All application and infrastructure containers should report a healthy/running state as applicable.

### 5. Smoke test JWT login

Obtain a JWT using the configured local gateway credentials:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"gateway","password":"<GATEWAY_AUTH_PASSWORD>"}' | jq -r .accessToken)

echo "Token obtained: $([ -n "$TOKEN" ] && echo yes || echo no)"
```

Use the token to access a protected endpoint:

```bash
curl -s \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/product | jq
```

## 🌐 Access Services

| Service              | URL                   | Port |
| -------------------- | --------------------- | ---: |
| API Gateway          | http://localhost:8080 | 8080 |
| Product Service      | http://localhost:8082 | 8082 |
| Inventory Service    | http://localhost:8081 | 8081 |
| Order Service        | http://localhost:8083 | 8083 |
| Notification Service | http://localhost:8084 | 8084 |
| Mailpit UI           | http://localhost:8025 | 8025 |

> **Note:** Direct service ports are exposed for debugging. In normal use, access the application through the API Gateway on port 8080.

## 📚 API Documentation

- **Aggregated Swagger UI (recommended):** http://localhost:8080/swagger-ui.html
- Product Service (direct): http://localhost:8082/swagger-ui.html (if enabled)
- Order Service (direct): http://localhost:8083/swagger-ui.html (if enabled)
- Inventory Service (direct): http://localhost:8081/swagger-ui.html (if enabled)

All protected endpoints require a valid JWT obtained from `POST /auth/login`.

## 🧪 Running Tests

Each service has its own Maven wrapper. From the service directory:

```bash
cd product-service && ./mvnw test

cd ../inventory-service && ./mvnw test

# Repeat for order-service, notification-service, and api-gateway
```

Tests can also be executed through the Azure DevOps pipeline.

## 🧩 Project Structure

```text
.
├── api-gateway/           # API Gateway (Spring Cloud Gateway + JWT)
├── product-service/       # Product catalog (MongoDB)
├── inventory-service/     # Inventory (MySQL)
├── order-service/         # Orders + Idempotency + Outbox (MySQL + Kafka)
├── notification-service/  # Email notifications (Kafka consumer)
├── ecommerce-k8s/         # Kubernetes manifests
├── mysql/                 # MySQL init scripts for Docker Compose
├── docker-compose.yml
├── .env.example
├── azure-pipelines.yml
└── README.md
```

## 🔄 Service Communication

- **Synchronous:** REST via API Gateway (JWT protected)
- **Asynchronous:** Apache Kafka (Order Placed → Notification)

## 🔐 Database Security (Non-Root Users)

Applications **never** connect as database root/admin.

| Database | Application User | Privileges                                       |
| -------- | ---------------- | ------------------------------------------------ |
| MySQL    | `inventory_app`  | SELECT, INSERT, UPDATE, DELETE on `inventory_db` |
| MySQL    | `order_app`      | SELECT, INSERT, UPDATE, DELETE on `order_db`     |
| MongoDB  | `product_app`    | `readWrite` on `product_db`                      |

Root credentials are used only for database/container bootstrap.

For local credential configuration, see `docs/local-secrets-setup.md`.

For Kubernetes Secrets, see `ecommerce-k8s/base/secrets/README.md`.

## 🔒 Authentication

### 1. Obtain a token

```text
POST /auth/login

{
  "username": "gateway",
  "password": "<GATEWAY_AUTH_PASSWORD>"
}
```

### 2. Use the token

```text
Authorization: Bearer <accessToken>
```

JWT is signed with HS256 using `GATEWAY_JWT_SECRET`.

The JWT secret must be at least 32 characters.

## 🐛 Debugging

### 1. Stop a service in Docker

```bash
docker compose stop <service-name>
```

### 2. Run it locally with debug enabled

```bash
cd <service-directory>

./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### 3. Attach your IDE debugger

Attach the IDE debugger to port `5005`.

## 🧹 Clean Up

To stop the application and remove its containers and volumes:

```bash
docker compose down -v
```

> **Warning:** `-v` removes Docker volumes, including local database data. Use `docker compose down` if you want to stop the stack without deleting persistent volumes.

## ☸️ Kubernetes

See `ecommerce-k8s/` for the Kubernetes manifests.

The Kubernetes deployment uses:

- kubeadm self-managed Kubernetes
- AWS EC2 infrastructure
- Kubernetes Secrets for application/database credentials
- Kubernetes Services for internal service discovery
- ECR-hosted application images
- `kubectl`-based deployment through Azure DevOps
- Existing Helm-based observability stack

**Important:** Required Kubernetes Secrets must exist before applying workloads. See:

`ecommerce-k8s/base/secrets/README.md`

Do not commit real credentials or secret values to Git.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch:

```bash
git checkout -b feature/AmazingFeature
```

3. Commit your changes:

```bash
git commit -m "Add some AmazingFeature"
```

4. Push to the branch:

```bash
git push origin feature/AmazingFeature
```

5. Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the `LICENSE` file for details (if present).

## 🙏 Acknowledgments

- Built with ❤️ using Spring Boot and Spring Cloud
- Thanks to all open-source projects used in this project
