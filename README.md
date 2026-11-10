# Sàn Thương mại Điện tử Đồ Công nghệ & Linh kiện Máy tính (Tech Ecommerce)
### Hệ thống Microservices Phân tán Hiệu năng cao (High-Performance Distributed Microservices)

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-green.svg)](https://spring.io/projects/spring-cloud-gateway)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.15%20LTS-blue.svg)](https://quarkus.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green.svg)](https://www.mongodb.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-orange.svg)](https://www.rabbitmq.com/)
[![Docker Compose](https://img.shields.io/badge/Docker%20Compose-v2-blue.svg)](https://docs.docker.com/compose/)

---

## 📌 Giới thiệu Đề tài
- **Tác giả:** Lê Minh Tuyên (Khoa Công nghệ Thông tin - Đại học Công nghiệp Hà Nội - HaUI)
- **Giảng viên hướng dẫn:** ThS. Hà Mạnh Đào
- **Học phần:** Phát triển phần mềm hướng dịch vụ (TX2)
- **Mô hình kiến trúc:** Microservices Architecture, Domain-Driven Design (DDD), Polyglot Persistence, Event-Driven Architecture, Reactive Non-Blocking (Spring WebFlux + Quarkus).

---

## 🏗️ Cấu trúc Hệ thống & Danh mục Dịch vụ

| Microservice | Cổng (Port) | Công nghệ chính | Cơ sở dữ liệu / Broker | Vai trò & Đặc điểm kiến trúc |
| :--- | :---: | :--- | :--- | :--- |
| **`api-gateway`** | **8080** | Spring Cloud Gateway, WebFlux, Netty | In-memory Routing | Điểm truy cập duy nhất, CORS tập trung, bảo vệ route bằng JWT Filter. |
| **`catalog-service`** | **8081** | Spring WebFlux Reactive, Netty | MongoDB (`catalog_db`) | Quản lý danh mục linh kiện, lưu thông số kỹ thuật động (Dynamic Specs JSON), SSE Stream. |
| **`auth-service`** | **8082** | Spring Boot, Spring Security, JJWT | PostgreSQL (`auth_db`) | Quản lý tài khoản, mã hóa BCrypt, cấp phát token JWT phi tập trung (OHS). |
| **`order-service`** | **8083** | Spring Boot MVC, Tactical DDD, AMQP | PostgreSQL (`order_db`), RabbitMQ | Aggregate Root `Order`, Value Object `Money`, Price Snapshot, phát sự kiện `OrderCreatedEvent`. |
| **`notification-service`** | **8084** | Quarkus 3.x, SmallRye Reactive Messaging | RabbitMQ (`order.created`) | Tiêu thụ RAM siêu nhẹ (< 35MB RSS), xử lý sự kiện tạo đơn từ RabbitMQ, mô phỏng gửi email. |

---

## 🗄️ Hạ tầng Cơ sở Dữ liệu & Message Broker

- **PostgreSQL 16:** Port `5432`, tài khoản `postgres` / `postgrespassword`. Tự động tạo 2 CSDL: `auth_db` và `order_db` qua script `scripts/init-postgres.sql`.
- **MongoDB 7.0:** Port `27017`, CSDL `catalog_db`. Tự động nạp sẵn linh kiện mẫu (CPU AMD 7600X, VGA RTX 4070S, RAM DDR5, SSD Samsung 990 Pro).
- **RabbitMQ 3.13:** Port `5672` (AMQP) và `15672` (Management Dashboard tại `http://localhost:15672`, user: `guest` / `guest`).

---

## 🚀 Hướng dẫn Cài đặt & Khởi chạy (1 Click Docker Compose)

### 1. Biên dịch toàn bộ các Microservices
```powershell
mvn clean package -DskipTests
```

### 2. Khởi chạy toàn bộ hệ thống bằng Docker Compose
```powershell
docker compose up -d --build
```

### 3. Kiểm tra trạng thái hoạt động của các container
```powershell
docker compose ps
```

---

## 🧪 Kịch bản Kiểm thử Tự động (End-to-End)

Dự án cung cấp sẵn kịch bản PowerShell kiểm thử từ đầu đến cuối tại thư mục `scripts/`:

```powershell
powershell -ExecutionPolicy Bypass -File ./scripts/test-system.ps1
```

Hoặc thực hiện thủ công bằng lệnh `curl`:

1. **Đăng nhập lấy token Admin:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}'
   ```
2. **Xem danh mục linh kiện (WebFlux + Mongo):**
   ```bash
   curl -X GET http://localhost:8080/api/products
   ```
3. **Tạo đơn hàng linh kiện mới (Bảo vệ bằng Bearer Token):**
   ```bash
   curl -X POST http://localhost:8080/api/orders \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <TOKEN_JWT>" \
     -d '{
       "customerId": 1,
       "customerName": "Le Minh Tuyen",
       "customerEmail": "tuyenlm@haui.edu.vn",
       "shippingAddress": "HaUI, Bac Tu Liem, Ha Noi",
       "items": [
         {
           "productId": "CPU-AMD-R5-7600X",
           "productName": "CPU AMD Ryzen 5 7600X",
           "unitPrice": 5790000,
           "quantity": 1
         }
       ]
     }'
   ```
4. **Kiểm tra thông báo đã gửi tại Quarkus Notification Service:**
   ```bash
   curl -X GET http://localhost:8080/api/notifications
   ```

---

## 📖 Báo cáo Đầy đủ (TX2 Report)
Xem chi tiết báo cáo phân tích thiết kế DDD, thiết kế CSDL, và đánh giá hiệu năng tại file [`BAO_CAO_TX2.md`](./BAO_CAO_TX2.md).
