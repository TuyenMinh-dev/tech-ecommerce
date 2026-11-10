# ==============================================================================
# Tech Ecommerce Microservices - Kịch bản kiểm thử toàn diện hệ thống (End-to-End)
# ==============================================================================

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "   BẮT ĐẦU KIỂM THỬ HỆ THỐNG MICROSERVICES SÀN ĐỒ CÔNG NGHỆ (TX2)     " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

$GATEWAY = "http://localhost:8080"
$AUTH = "http://localhost:8082"
$CATALOG = "http://localhost:8081"
$ORDER = "http://localhost:8083"
$NOTIF = "http://localhost:8084"

# 1. Kiểm tra trạng thái các Service
Write-Host "`n[1/5] Kiểm tra Health Checks..." -ForegroundColor Yellow

function Test-Endpoint($name, $url) {
    try {
        $res = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 3 -ErrorAction Stop
        Write-Host "  [OK] $name: Dang hoat dong tot" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "  [WARN] $name: Chua san sang tai $url" -ForegroundColor Red
        return $false
    }
}

Test-Endpoint "API Gateway" "$GATEWAY/actuator/health"
Test-Endpoint "Catalog Service (WebFlux + Mongo)" "$CATALOG/actuator/health"
Test-Endpoint "Auth Service (JWT + PostgreSQL)" "$AUTH/actuator/health"
Test-Endpoint "Order Service (DDD + RabbitMQ)" "$ORDER/actuator/health"
Test-Endpoint "Notification Service (Quarkus)" "$NOTIF/api/notifications/status"

# 2. Xác thực người dùng (Auth Flow)
Write-Host "`n[2/5] Kiem tra Auth Service & Cap phat JWT Token..." -ForegroundColor Yellow
$loginPayload = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "$GATEWAY/api/auth/login" -Method Post -Body $loginPayload -ContentType "application/json"
    $TOKEN = $loginRes.token
    Write-Host "  [OK] Dang nhap thanh cong voi Admin!" -ForegroundColor Green
    Write-Host "  Token prefix: $($TOKEN.Substring(0, 25))..." -ForegroundColor DarkGray
} catch {
    Write-Host "  [FAIL] Khong the dang nhap qua Gateway: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Tra cứu danh mục linh kiện (Catalog WebFlux Flow)
Write-Host "`n[3/5] Kiem tra Catalog Service (WebFlux Reactive)..." -ForegroundColor Yellow
try {
    $products = Invoke-RestMethod -Uri "$GATEWAY/api/products" -Method Get
    Write-Host "  [OK] Da lay duoc $($products.Count) linh kien tu MongoDB reactive:" -ForegroundColor Green
    foreach ($p in $products) {
        Write-Host "    - [$($p.sku)] $($p.name) | Gia: $($p.price) VND | Ton: $($p.stock)" -ForegroundColor Cyan
    }
    $firstProduct = $products[0]
} catch {
    Write-Host "  [FAIL] Khong the lay danh sach linh kien: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Đặt hàng qua Order Service (Yêu cầu JWT Token bảo vệ)
Write-Host "`n[4/5] Kiem tra Dat hang (Order Service with Tactical DDD & ACL)..." -ForegroundColor Yellow
if ($TOKEN -and $firstProduct) {
    $orderPayload = @{
        customerId = 1
        customerName = "Le Minh Tuyen"
        customerEmail = "tuyenlm@haui.edu.vn"
        shippingAddress = "HaUI, Bac Tu Liem, Ha Noi"
        notes = "Giao gio hanh chinh, dong goi chong soc ky"
        items = @(
            @{
                productId = $firstProduct.id
                productName = $firstProduct.name
                unitPrice = $firstProduct.price
                quantity = 1
            }
        )
    } | ConvertTo-Json -Depth 5

    try {
        $headers = @{
            "Authorization" = "Bearer $TOKEN"
        }
        $orderRes = Invoke-RestMethod -Uri "$GATEWAY/api/orders" -Method Post -Body $orderPayload -ContentType "application/json" -Headers $headers
        Write-Host "  [OK] Tao don hang thanh cong! Ma don: $($orderRes.orderCode)" -ForegroundColor Green
        Write-Host "       Tong tien: $($orderRes.totalAmount.amount) VND | Trang thai: $($orderRes.status)" -ForegroundColor Cyan
        $orderCode = $orderRes.orderCode
    } catch {
        Write-Host "  [FAIL] Khong the tao don hang: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "  [SKIP] Bo qua vi thieu Token hoac Danh muc san pham" -ForegroundColor Yellow
}

# 5. Kiểm tra sự kiện tại Notification Service (Quarkus)
Write-Host "`n[5/5] Kiem tra Notification Service (Quarkus AMQP Consumer)..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
try {
    $notifs = Invoke-RestMethod -Uri "$GATEWAY/api/notifications" -Method Get
    Write-Host "  [OK] Quarkus da nhan duoc $($notifs.Count) thong bao tu RabbitMQ:" -ForegroundColor Green
    foreach ($n in $notifs) {
        Write-Host "    - [Ma don: $($n.orderCode)] Gui email cho $($n.customerName) ($($n.customerEmail)) | Tien: $($n.totalAmount) VND" -ForegroundColor Magenta
    }
} catch {
    Write-Host "  [INFO] Co the kiem tra truc tiep tren Notification Service tai $NOTIF/api/notifications" -ForegroundColor DarkGray
}

Write-Host "`n======================================================================" -ForegroundColor Cyan
Write-Host "                     HOAN TAT KIEM THU HE THONG                       " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
