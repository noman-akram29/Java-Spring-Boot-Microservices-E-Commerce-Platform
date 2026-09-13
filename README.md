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

1. **Clone the repository**
   ```bash
   git clone https://github.com/noman-akram29/Java-Spring-Boot-Microservices-E-Commerce-Platform.git
   cd Java-Spring-Boot-Microservices-E-Commerce-Platform
   git checkout staging
   ```

2. **Create local secrets (required)**
   ```bash
   mkdir -p secrets
   echo "changeme_local_dev_only" > secrets/mysql_root_password.txt
   echo "changeme_local_dev_only" > secrets/mongo_root_password.txt
   ```

3. **Create `.env` from the example**
   ```bash
   cp .env.example .env
   ```
   Edit `.env` and set a **strong JWT secret** (≥ 32 characters):
   ```bash
   GATEWAY_JWT_SECRET=ThisIsAVerySecure32ByteOrLongerSecretKey!!
   ```
   (Keep other defaults for local development.)

4. **Start everything**
   ```bash
   docker compose up -d --build
   ```

5. **Verify services are healthy**
   ```bash
   docker compose ps
   ```

6. **Smoke test JWT login**
   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"gateway","password":"changeme_local_dev_only"}' | jq -r .accessToken)

   echo "Token: $TOKEN"

   curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/product | jq
   ```

## 🌐 Access Services (correct ports)

| Service                | URL                                      | Port |
|------------------------|------------------------------------------|------|
| API Gateway            | http://localhost:8080                    | 8080 |
| Product Service        | http://localhost:8082                    | 8082 |
| Inventory Service      | http://localhost:8081                    | 8081 |
| Order Service          | http://localhost:8083                    | 8083 |
| Notification Service   | http://localhost:8084                    | 8084 |
| Mailpit UI             | http://localhost:8025                    | 8025 |

> **Note:** Direct service ports are exposed for debugging. In normal use go through the API Gateway on port 8080.

## 📚 API Documentation

- **Aggregated Swagger UI (recommended)**: http://localhost:8080/swagger-ui.html
- Product Service (direct): http://localhost:8082/swagger-ui.html (if enabled)
- Order Service (direct): http://localhost:8083/swagger-ui.html (if enabled)
- Inventory Service (direct): http://localhost:8081/swagger-ui.html (if enabled)

All protected endpoints require a valid JWT obtained from `POST /auth/login`.

## 🧪 Running Tests

Each service has its own Maven wrapper. From the service directory:

```bash
cd product-service && ./mvnw test
cd ../inventory-service && ./mvnw test
# … same for order-service, notification-service, api-gateway
```

Or run them all via the Azure pipeline / CI.

## 🧩 Project Structure

```
.
├── api-gateway/           # API Gateway (Spring Cloud Gateway + JWT)
├── product-service/       # Product catalog (MongoDB)
├── inventory-service/     # Inventory (MySQL)
├── order-service/         # Orders + Idempotency + Outbox (MySQL + Kafka)
├── notification-service/  # Email notifications (Kafka consumer)
├── ecommerce-k8s/         # Kubernetes manifests (base + overlays)
├── mysql/                 # MySQL init scripts for Docker Compose
├── docker-compose.yml
├── .env.example
├── azure-pipelines.yml
└── README.md
```

## 🔄 Service Communication

- **Synchronous**: REST via API Gateway (JWT protected)
- **Asynchronous**: Apache Kafka (Order Placed → Notification)

## 🔒 Authentication

1. Obtain a token:
   ```bash
   POST /auth/login
   { "username": "gateway", "password": "<GATEWAY_AUTH_PASSWORD>" }
   ```
2. Use the token:
   ```
   Authorization: Bearer <accessToken>
   ```

JWT is signed with HS256 using `GATEWAY_JWT_SECRET` (must be ≥ 32 characters).

## 🐛 Debugging

1. Stop a service in Docker:
   ```bash
   docker compose stop <service-name>
   ```

2. Run it locally with debug:
   ```bash
   cd <service-directory>
   ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
   ```

3. Attach your IDE debugger to port 5005.

## 🧹 Clean Up

```bash
docker compose down -v
```

## ☸️ Kubernetes

See `ecommerce-k8s/` for base manifests.  
**Important:** You must create the required Secrets before applying (see `ecommerce-k8s/base/secrets/README.md`).

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details (if present).

## 🙏 Acknowledgments

- Built with ❤️ using Spring Boot and Spring Cloud
- Thanks to all open-source projects used in this project
