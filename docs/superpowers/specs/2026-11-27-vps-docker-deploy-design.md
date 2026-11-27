# Thiết Kế Triển Khai KizunaTutor Lên VPS (Docker)

## 1. Mục tiêu & Lý do
- **Mục tiêu:** Đưa hệ thống KizunaTutor từ `localhost` lên VPS để giảng viên và người dùng có thể truy cập công khai qua HTTPS phục vụ demo và bảo vệ đồ án.
- **Không nằm trong phạm vi (Non-goals):** Không chạy container VOICEVOX Engine (sử dụng Web Speech API fallback sẵn có trong `voicevoxClient.js`) và không chạy container `agent-memory` riêng (sử dụng `AgentMemoryController` tích hợp sẵn trong Spring Boot Backend).
- **Anti-goals:** Không làm hỏng khả năng chạy dev trên `localhost`; không đưa thêm secret/mật khẩu mới vào Git.

## 2. Ràng buộc
- VPS cấu hình nhỏ (1–2GB RAM, Ubuntu/Debian có Docker & Docker Compose).
- Database PostgreSQL giữ nguyên trên **Supabase** hiện tại.
- File upload cục bộ (`avatars`, `verification-docs`) cần được gắn Docker volume để không mất khi restart container.

## 3. Các phương án đã cân nhắc
1. **Render.com (Free 0đ, 1 Docker gộp):** Không tốn tiền nhưng bị sleep sau 15 phút, giới hạn 512MB RAM và mất file upload trên ổ đĩa tạm nếu chưa bật R2.
2. **VPS giá rẻ + Docker Compose + Cloudflare Tunnel (Đã chọn):** Không bị sleep, RAM thoải mái (1–2GB), lưu file upload bền vững trên volume, có HTTPS miễn phí qua Cloudflare Tunnel (dùng link `trycloudflare.com` hoặc gắn tên miền riêng).
3. **Laptop cá nhân + Cloudflare Tunnel:** Nhanh nhưng tắt máy là web ngưng hoạt động, không tiện để giảng viên kiểm tra trước/sau buổi bảo vệ.

## 4. Kiến trúc & Các thay đổi cần thực hiện
- **`backend/Dockerfile`:** Multi-stage build (Maven + Java 17 JDK -> Eclipse Temurin 17 JRE Alpine), chạy JAR cổng `8085`, gắn volume `/root/kizuna-data`.
- **`frontend/Dockerfile`:** Node.js 20 Alpine chạy `server.js` cổng `3000`.
- **`frontend/server.js`:** Hỗ trợ biến môi trường `BACKEND_HOST` (mặc định `127.0.0.1` khi chạy local, đặt `backend` trong Docker network).
- **Frontend API Config (`apiClient.js`, `api.config.js`, `agentMemory.js`):** Tự động dùng đường dẫn tương đối `/api` và `/memory/append` khi truy cập qua domain/IP server thay vì hardcode `http://localhost:8085` hoặc `http://localhost:8081`.
- **`docker-compose.prod.yml` & `.env.example`:** Khởi chạy `backend` + `frontend` (và hướng dẫn gắn `cloudflared` hoặc Nginx), truyền cấu hình qua file `.env` (`GOOGLE_REDIRECT_URI`, `VNPAY_RETURN_URL`, DB, Mail, JWT, R2).
- **`docs/DEPLOY_VPS.md`:** Hướng dẫn từng bước từ lúc thuê VPS, cài Docker, mở HTTPS bằng Cloudflare Tunnel đến cập nhật Google OAuth2 / VNPay callback URL.
