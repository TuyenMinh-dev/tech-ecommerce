# PHÂN TÍCH & THIẾT KẾ HỆ THỐNG THEO DOMAIN-DRIVEN DESIGN (DDD)
## DỰ ÁN: SÀN THƯƠNG MẠI ĐIỆN TỬ ĐỒ CÔNG NGHỆ (TECH-ECOMMERCE)

### 1. Phân rã Domain & Subdomain (Domain Decomposition)

| Loại Subdomain | Tên Subdomain | Mục đích nghiệp vụ cụ thể với Sàn đồ công nghệ | Service tương ứng | CSDL & Công nghệ |
| :--- | :--- | :--- | :--- | :--- |
| **Core Domain** | **Catalog & Product Specs** | - Quản lý danh mục linh kiện với bộ thông số kỹ thuật động đa dạng theo từng loại (CPU có Socket/Xung nhịp, RAM có Bus/Chuẩn DDR4-DDR5, Chuột có DPI/Switch...).<br>- Hỗ trợ tìm kiếm, lọc đa tiêu chí theo cấu hình phần cứng.<br>- Hỗ trợ stream dữ liệu linh kiện thời gian thực (Server-Sent Events).<br>- **Quy tắc nghiệp vụ:** Kiểm tra tính tương thích linh kiện khi người dùng tự "Build PC" (ví dụ: CPU chuẩn Socket AM5 bắt buộc phải đi cùng Mainboard hỗ trợ AM5 và RAM DDR5). | `catalog-service` | **MongoDB (Reactive)**<br>Framework: **Spring WebFlux** |
| **Core Domain** | **Ordering & Checkout** | - Quản lý giỏ hàng, tạo đơn đặt hàng và điều phối vòng đời trạng thái đơn hàng (`Pending` -> `Confirmed` -> `Paid` -> `Shipping` -> `Completed` / `Cancelled`).<br>- Tính toán tổng tiền, phí vận chuyển và kiểm tra điều kiện áp mã giảm giá / khuyến mãi đi kèm (mua kèm giảm thêm - Bundle Deal).<br>- **Quy tắc nghiệp vụ:** Chốt giá bán (Price Snapshot) tại thời điểm bấm đặt hàng (nếu sau đó `catalog-service` tăng giá linh kiện thì đơn đã đặt không bị đổi giá); tự động hủy đơn nếu quá 15 phút chưa thanh toán. | `order-service` | **PostgreSQL (ACID)**<br>Framework: **Spring Boot MVC + JPA** |
| **Supporting** | **Inventory (Quản lý Kho & Serial)** | - Theo dõi số lượng tồn kho thực tế (`Available Stock`) và số lượng đang giữ chỗ tạm thời (`Reserved Stock`) cho từng mã SKU linh kiện.<br>- Quản lý mã **Serial Number / IMEI** của từng linh kiện xuất kho để phục vụ tra cứu bảo hành chính hãng.<br>- **Quy tắc nghiệp vụ:** Không cho phép tồn kho khả dụng âm (chống bán lố - Overselling khi Flash Sale card đồ họa/CPU); giữ hàng tạm thời khi đơn hàng vừa tạo, trừ kho thật khi thanh toán thành công và hoàn lại tồn kho nếu đơn hàng bị hủy. | `order-service` *(module quản lý kho)* | **PostgreSQL** |
| **Supporting** | **Payment (Thanh toán)** | - Xử lý giao dịch thu tiền đơn hàng (Thanh toán trả trước qua cổng thanh toán/QR Code hoặc COD - thu hộ khi nhận hàng).<br>- Ghi nhận lịch sử giao dịch (`Transaction Log`) và đối soát trạng thái thanh toán.<br>- **Quy tắc nghiệp vụ:** Số tiền thanh toán phải khớp 100% với tổng tiền đơn hàng; các đơn hàng "Build PC" nguyên bộ hoặc giá trị cao (ví dụ > 20 triệu VNĐ) bắt buộc phải thanh toán trước hoặc đặt cọc tối thiểu 10% mới được xác nhận xuất kho; xử lý hoàn tiền (`Refund`) khi đơn đã trả tiền bị hủy. | `order-service` *(module giả lập thanh toán)* | **PostgreSQL** |
| **Generic** | **Identity & Access (Auth)** | - Quản lý đăng ký, đăng nhập, xác thực người dùng bằng JWT (Access Token & Refresh Token).<br>- Phân quyền truy cập theo vai trò (`Customer` mua hàng, `Staff` quản lý kho/đơn, `Admin` quản trị hệ thống) và lưu sổ địa chỉ nhận hàng.<br>- **Quy tắc nghiệp vụ:** Mật khẩu phải được băm (Hash) trước khi lưu; tài khoản bị khóa hoặc chưa kích hoạt không được phép tạo đơn hàng; mỗi người dùng có tối đa 1 địa chỉ nhận hàng mặc định. | `auth-service` | **PostgreSQL**<br>Framework: **Spring Security + JWT** |
| **Generic** | **Notification (Thông báo)** | - Lắng nghe các sự kiện nghiệp vụ (`Domain Events`) từ các service khác (qua RabbitMQ) để gửi thông báo tự động cho khách hàng qua Email/SMS/Console.<br>- **Quy tắc nghiệp vụ:** Gửi thông báo xác nhận ngay khi phát sinh sự kiện `OrderCreated`, gửi hóa đơn điện tử kèm danh sách Serial bảo hành khi `OrderPaid`, và gửi cảnh báo khi đơn hàng bị hủy. Cơ chế gửi tin hoạt động bất đồng bộ: lỗi gửi mail không làm gián đoạn luồng đặt hàng. | `notification-service` | Framework: **Quarkus (Reactive Messaging với RabbitMQ)** |

---

### 2. Bounded Contexts & Ubiquitous Language (Ngôn ngữ chung)

Dưới đây là cách cùng một thực thể được mô hình hóa khác nhau qua các ranh giới ngữ cảnh (Bounded Context):

| Khái niệm nghiệp vụ | Trong Product Catalog Context (`catalog-service`) | Trong Order Context (`order-service`) | Trong Inventory Context (`inventory`) |
| :--- | :--- | :--- | :--- |
| **Sản phẩm (Product)** | **`ProductCatalogItem` (Aggregate Root)**<br>- Quản lý: Tên, Hãng sản xuất, Mô tả, Dynamic Attributes (JSON Specs), Hình ảnh.<br>- Hành động: `PublishProduct()`, `UpdateSpecs()`. | **`OrderItemSnapshot` (Value Object)**<br>- Quản lý: `ProductId`, `ProductName`, `UnitPrice` (giá tại thời điểm mua), `Quantity`.<br>- Không chứa thuộc tính kỹ thuật chi tiết. | **`StockItem` (Entity / Aggregate)**<br>- Quản lý: `SkuCode`, `AvailableQuantity`, `ReservedQuantity`, Danh sách `SerialNumber` tồn kho.<br>- Hành động: `ReserveStock()`, `ReleaseStock()`, `DeductStock()`. |
| **Người dùng (User)** | **Khách xem hàng (Guest/Viewer)**<br>- Chỉ đọc dữ liệu, lọc linh kiện. | **`Customer` (Value Object)**<br>- `CustomerId`, Địa chỉ giao hàng (`ShippingAddress`), Số điện thoại nhận hàng. | **Nhân viên kho (Warehouse Keeper)**<br>- Người quét mã vạch xuất/nhập kho. |
| **Trạng thái cốt lõi** | Trạng thái hiển thị sản phẩm: `DRAFT`, `PUBLISHED`, `DISCONTINUED`. | Vòng đời đơn hàng: `PENDING`, `CONFIRMED`, `PAID`, `SHIPPED`, `CANCELLED`. | Trạng thái lưu kho: `IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`, `RESERVED`. |

---

### 3. Context Mapping (Bản đồ liên kết ngữ cảnh)

#### 3.1. Sơ đồ Context Map tổng thể
```mermaid
flowchart TD
    subgraph EdgeLayer ["Tầng Biên (Reactive API Gateway)"]
        Gateway["Spring Cloud Gateway<br>(Spring WebFlux + Netty)"]
    end

    subgraph Core ["Miền cốt lõi (Core Domain)"]
        CatalogContext["Product Catalog Context<br><b>Spring WebFlux + Reactive MongoDB</b>"]
        OrderContext["Order Context<br><b>Spring Boot MVC + PostgreSQL (JPA)</b>"]
    end

    subgraph Generic ["Miền chung (Generic Domain)"]
        AuthContext["Identity & Access Context<br><b>Spring Boot + PostgreSQL (JWT)</b>"]
        NotificationContext["Notification Context<br><b>Quarkus (Reactive Messaging)</b>"]
    end

    subgraph Supporting ["Miền hỗ trợ (Supporting Domain)"]
        InventoryPayment["Inventory & Payment<br>(Tích hợp trong Order Context)"]
    end

    %% Client gọi vào Gateway
    Gateway --> AuthContext
    Gateway --> CatalogContext
    Gateway --> OrderContext

    %% Giao tiếp đồng bộ REST (Upstream -> Downstream qua ACL)
    CatalogContext -- "[U] OHS / PL (REST GET)" --> OrderACL["ACL (OpenFeign Adapter)"]
    OrderACL --> OrderContext

    %% Giao tiếp bất đồng bộ qua Domain Events
    OrderContext -- "[U] Published Event: OrderCreated" --> RabbitMQ["Message Broker (RabbitMQ)"]
    RabbitMQ -- "[D] Subscriber" --> NotificationContext
```

#### 3.2. Bảng phân tích quan hệ giữa các Bounded Context

| Chiều liên kết | Loại quan hệ trong DDD | Phương thức truyền thông | Mục đích nghiệp vụ thực tế |
| :--- | :--- | :--- | :--- |
| **Catalog -> Order** | **Upstream [U] -> Downstream [D] qua ACL** | **Đồng bộ (REST qua OpenFeign)** | Khi khách hàng tạo đơn, `order-service` gọi `catalog-service` để xác thực: Sản phẩm có tồn tại không? Trạng thái có đang `PUBLISHED` không? Lấy snapshot tên và giá chuẩn. Sử dụng **ACL** để bọc dữ liệu trả về thành `OrderItem`. |
| **Order -> Notification** | **Published Language / Pub-Sub** | **Bất đồng bộ (RabbitMQ Event)** | Khi đơn hàng tạo thành công (`OrderCreated`), `order-service` phát event lên RabbitMQ exchange. `notification-service` (chạy **Quarkus**) lắng nghe và in log / gửi email. Hai service hoàn toàn lỏng (Loose Coupling), Notification sập thì Order vẫn tạo bình thường. |
| **Auth -> Các Service** | **Open Host Service (OHS)** | **Xác thực phi tập trung (JWT Stateless)** | `auth-service` cấp phát JWT Token. Khi client gửi request qua API Gateway, Gateway và các service nội bộ chỉ cần giải mã chữ ký (Verify Signature) của Public/Secret Key mà **không cần gọi REST ngược lại Auth Service mỗi lần**. |

---

### 4. Tactical Design (Thiết kế tiểu tiết)

#### 4.1. Order Bounded Context (PostgreSQL - Lưu trữ quan hệ & Toàn vẹn ACID)
Áp dụng Clean Architecture, tách biệt tuyệt đối giữa **Domain Model thuần Java** và **JPA Entity (Infrastructure)**.

- **Aggregate Root:** `Order`
  - Quản lý vòng đời đơn hàng và bảo vệ các bất biến nghiệp vụ (Invariants).
  - Thuộc tính: `OrderId` (VO), `CustomerId` (VO), `ShippingAddress` (VO), `OrderStatus` (Enum: PENDING, CONFIRMED, PAID, CANCELLED), `List<OrderItem>` (Entities), `Money totalAmount` (VO).
  - Hành vi nghiệp vụ cốt lõi:
    - `addItem(ProductId, ProductName, Money unitPrice, int quantity)`: Chỉ cho phép thêm khi trạng thái là `PENDING`, tự động tính toán lại tổng tiền.
    - `markAsPaid()`: Chuyển trạng thái sang `PAID`, phát ra sự kiện `OrderPaidEvent`.
    - `cancel(String reason)`: Hủy đơn và phát ra sự kiện `OrderCancelledEvent`.
- **Entities con bên trong Aggregate:**
  - `OrderItem`: Đại diện cho từng dòng hàng trong đơn (gồm `OrderItemId`, `ProductId`, `ProductName`, `UnitPrice`, `Quantity`).
- **Value Objects (Bất biến - Immutable):**
  - `OrderId`: Bọc UUID để đảm bảo Type-safe (tránh truyền nhầm chuỗi String ngẫu nhiên).
  - `Money`: Chứa `BigDecimal amount` và `Currency` (chống lỗi làm tròn số thực của kiểu double/float trong tính toán tài chính).
  - `ShippingAddress`: Chứa địa chỉ chi tiết, phường/xã, quận/huyện, tỉnh/thành.
- **Domain Events:**
  - `OrderCreatedEvent`: Phát sinh khi đơn hàng vừa tạo xong để gửi sang RabbitMQ.

---

#### 4.2. Product Catalog Bounded Context (MongoDB - Reactive & Dữ liệu bán cấu trúc linh hoạt)

- **Aggregate Root:** `Product`
  - Công nghệ: **Spring WebFlux (Reactive Streams với `Mono<Product>` và `Flux<Product>`)**.
  - Quản lý toàn bộ thông tin linh kiện công nghệ và thông số kỹ thuật động.
  - Thuộc tính:
    - `id`: Mã định danh MongoDB (String/ObjectId).
    - `sku`: Mã kho linh kiện duy nhất (ví dụ: `CPU-AMD-R5-7600X`).
    - `name`: Tên sản phẩm.
    - `brand`: Thương hiệu (ASUS, MSI, AMD, Intel, Corsair...).
    - `category`: Danh mục linh kiện (CPU, VGA, RAM, MAINBOARD, PERIPHERALS).
    - `basePrice`: Giá niêm yết (`BigDecimal`).
    - `status`: Enum (`DRAFT`, `PUBLISHED`, `DISCONTINUED`).
    - `attributes`: **`Map<String, Object>`** - Bảng thông số kỹ thuật động lưu dưới dạng JSON linh hoạt:
      - CPU: `{ "socket": "AM5", "cores": 6, "threads": 12, "baseClock": "4.7GHz", "tdp": "105W" }`
      - RAM: `{ "type": "DDR5", "bus": "6000MHz", "capacity": "32GB", "rgb": true }`
      - Mouse: `{ "dpi": 26000, "sensor": "Focus Pro", "connection": "Wireless 2.4GHz" }`
  - Hành vi nghiệp vụ:
    - `publish()`: Đưa sản phẩm lên sàn (chỉ khi có đầy đủ giá và thông số tối thiểu).
    - `updateSpecifications(Map<String, Object> newSpecs)`: Cập nhật thông số kỹ thuật mới mà không cần sửa code hay migrate bảng CSDL.
    - Hỗ trợ endpoint `/stream` (SSE - Server-Sent Events) đẩy dữ liệu cập nhật theo thời gian thực.
