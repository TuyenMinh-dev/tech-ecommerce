-- Khởi tạo các database cho các microservices
CREATE DATABASE auth_db;
CREATE DATABASE order_db;

GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
