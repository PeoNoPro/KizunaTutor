# Hướng dẫn Deploy KizunaTutor lên VPS (Docker + Cloudflare Tunnel)

Tài liệu này hướng dẫn từng bước đưa hệ thống **KizunaTutor** lên một máy chủ Ubuntu VPS mới (1–2 GB RAM) bằng Docker Compose và mở HTTPS công khai qua Cloudflare Tunnel để phục vụ chạy demo bảo vệ đồ án.

---

## Kiến trúc triển khai Production (`docker-compose.prod.yml`)

Khi chạy với `docker-compose.prod.yml`, hệ thống gồm các thành phần:

- **`backend`** (build từ `backend/Dockerfile`): Spring Boot (Java 17) chạy ở cổng nội bộ `8085`, kết nối tới PostgreSQL trên Supabase, lưu ảnh đại diện và giấy tờ xác minh vào Docker volume **`kizuna_data`** (mount tại `/root/kizuna-data`).
- **`frontend`** (build từ `frontend/Dockerfile`): Node.js 20 (`node server.js`) mở cổng **`3000:3000`**, phục vụ giao diện web và proxy các đường dẫn `/api/*`, `/oauth2/*`, `/login/oauth2/*`, `/memory/*`, `/agent-memory/*` tới `backend:8085`.
- **`cloudflared`** (profile **`tunnel`**): Container `cloudflare/cloudflared:latest` kết nối mạng nội bộ Docker tới `http://frontend:3000` để cấp HTTPS công khai mà không cần mở port 80/443 hay cấu hình Nginx/SSL thủ công.

---

## Bước 1: Chuẩn bị VPS Ubuntu & cài đặt Docker

### 1.1. Yêu cầu tối thiểu
- Hệ điều hành: **Ubuntu 22.04 LTS** hoặc **24.04 LTS**
- Cấu hình đề xuất: **1–2 GB RAM**, 1–2 vCPU, tối thiểu 15–20 GB ổ cứng trống.

### 1.2. Tạo Swap (khuyên dùng cho VPS 1 GB RAM)
Quá trình build Maven (`./mvnw package`) trong stage đầu của `backend/Dockerfile` có thể tiêu tốn RAM đột biến. Nếu VPS chỉ có 1 GB RAM, hãy tạo thêm 2 GB swap để tránh lỗi hết bộ nhớ (OOM):

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

### 1.3. Cài đặt Docker Engine & Docker Compose plugin
Chạy lần lượt các lệnh sau trên VPS:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg git

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

(Tùy chọn) Cho phép user hiện tại chạy `docker` không cần `sudo`:

```bash
sudo usermod -aG docker $USER
newgrp docker
```

Kiểm tra cài đặt thành công:

```bash
docker --version
docker compose version
```

---

## Bước 2: Đưa mã nguồn lên VPS & cấu hình file `.env`

### 2.1. Clone repository
```bash
git clone <URL_REPO_CUA_BAN> KizunaTutor
cd KizunaTutor
```

*(Nếu đẩy code từ máy cá nhân lên VPS, bạn cũng có thể dùng `scp` hoặc `rsync`, miễn sao đứng đúng trong thư mục gốc `KizunaTutor` chứa `docker-compose.prod.yml` và `.env.example`.)*

### 2.2. Tạo và điền file `.env`
Sao chép file mẫu `.env.example` thành `.env`:

```bash
cp .env.example .env
nano .env
```

Điền đầy đủ các biến môi trường theo từng nhóm trong `.env.example`:

1. **Database (Supabase PostgreSQL Pooler)**
   - `SPRING_DATASOURCE_URL`: chuỗi JDBC lấy từ Supabase (**Connect -> Connection Pooler**, cổng `6543`, giữ tham số `?sslmode=require&prepareThreshold=0`), ví dụ:
     `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0`
   - `SPRING_DATASOURCE_USERNAME`: dạng `postgres.<project_ref>`
   - `SPRING_DATASOURCE_PASSWORD`: mật khẩu database Supabase của bạn.

2. **JWT Authentication**
   - `APP_JWT_SECRET`: chuỗi bí mật tối thiểu 256-bit (32+ ký tự). Bạn có thể tạo nhanh bằng lệnh:
     ```bash
     openssl rand -hex 32
     ```
     Sau đó dán kết quả vào `APP_JWT_SECRET`.

3. **Transactional Email (Brevo SMTP)**
   - `MAIL_HOST=smtp-relay.brevo.com`
   - `MAIL_PORT=587`
   - `MAIL_USERNAME`: email/username đăng nhập SMTP trên Brevo
   - `MAIL_PASSWORD`: SMTP key của Brevo
   - `MAIL_SENDER_EMAIL`: địa chỉ email hiển thị ở mục người gửi (đã xác thực trên Brevo).

4. **Frontend Origins & Public URL (CORS + email mời nhân sự)**
   - `APP_FRONTEND_ORIGINS`: danh sách origin được phép gọi API qua CORS (phân tách bằng dấu phẩy nếu có nhiều domain), ví dụ `https://your-domain.com` (cập nhật theo domain thật ở **Bước 5**).
   - `APP_FRONTEND_URL`: URL công khai của frontend dùng để tạo đường dẫn trong email (ví dụ kích hoạt tài khoản nhân sự), ví dụ `https://your-domain.com`.

5. **Google OAuth2**
   - `GOOGLE_CLIENT_ID`: Client ID từ Google Cloud Console
   - `GOOGLE_CLIENT_SECRET`: Client Secret từ Google Cloud Console
   - `GOOGLE_REDIRECT_URI`: tạm để `https://your-domain.com/login/oauth2/code/google` và cập nhật lại ngay ở **Bước 5** sau khi có domain HTTPS thật.
   - *(Lưu ý: nếu Google OAuth2 đã từng được cấu hình qua giao diện Admin UI — lưu trong bảng `system_configurations` trên Supabase — thì giá trị trong DB sẽ được ưu tiên hơn `.env`.)*

6. **VNPay Payment Gateway (nếu demo thanh toán hội viên)**
   - `VNPAY_TMN_CODE`: mã merchant Sandbox của bạn (để trống nếu chưa dùng)
   - `VNPAY_HASH_SECRET`: chuỗi hash secret Sandbox (để trống nếu chưa dùng)
   - `VNPAY_RETURN_URL`: `https://your-domain.com/tutor/membership/result` (cập nhật theo domain thật ở **Bước 5**).
   - *(Nếu chưa dùng VNPay, giữ trống `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET` — hệ thống vẫn khởi động bình thường và báo cổng thanh toán tạm thời không khả dụng khi bấm thanh toán.)*

7. **Cloudflare R2 Object Storage (tùy chọn)**
   - Mặc định `R2_ENABLED=false`: ảnh đại diện (`AVATAR_STORAGE_DIR`) và file luyện đề (`practice`) được lưu bền vững trực tiếp trên Docker volume `kizuna_data`.
   - **Lưu ý quan trọng:** tính năng tải lên **giấy tờ xác minh gia sư mới** (`VerificationDocumentStorage.java`) yêu cầu bật Cloudflare R2 (`R2_ENABLED=true`) cùng với `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`; nếu `R2_ENABLED=false`, thao tác upload giấy tờ xác minh mới sẽ báo lỗi.

8. **JVM Options**
   - Giữ `JAVA_OPTS=-Xmx512m -Xms256m` để phù hợp với VPS 1–2 GB RAM.

9. **Cloudflare Tunnel (tùy chọn)**
   - `CLOUDFLARE_TUNNEL_TOKEN`: để trống ở bước này nếu chưa tạo Tunnel, sẽ điền ở **Bước 4 (Cách A)**.

> **Bảo mật:** Không commit file `.env` lên Git và không chia sẻ công khai mật khẩu DB, JWT secret, SMTP key hay Client Secret.

---

## Bước 3: Build & khởi chạy Docker Compose

Chạy lệnh build và khởi động 2 service chính (`backend` và `frontend`):

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

*(Lần build đầu tiên sẽ tải dependency Maven và biên dịch Spring Boot JAR nên mất khoảng 2–5 phút tùy tốc độ VPS.)*

Kiểm tra trạng thái các container:

```bash
docker compose -f docker-compose.prod.yml ps
```

Theo dõi log để chắc chắn `backend` (Spring Boot cổng `8085`) và `frontend` (`node server.js` cổng `3000`) đã khởi động không lỗi:

```bash
docker compose -f docker-compose.prod.yml logs -f
```

*(Nhấn `Ctrl + C` để thoát màn hình xem log; các container vẫn chạy ngầm.)*

Kiểm tra nhanh nội bộ trên VPS (dùng `-L` để theo redirect `302` từ `/` sang `/pages/home/`):

```bash
curl -IL http://localhost:3000
```

Nếu trả về `HTTP/1.1 200 OK` là ứng dụng đã sẵn sàng.

---

## Bước 4: Mở HTTPS công khai bằng Cloudflare Tunnel

Chọn **Cách A** (khuyên dùng khi bảo vệ đồ án để có URL HTTPS cố định) hoặc **Cách B** (test nhanh không cần domain).

### Cách A: Link cố định với domain riêng / `.id.vn` (Profile `tunnel`)

1. **Đưa domain về Cloudflare:** Đăng nhập [Cloudflare Dashboard](https://dash.cloudflare.com/), thêm domain (ví dụ tên miền `.id.vn` miễn phí cho sinh viên) và trỏ Nameserver về Cloudflare theo hướng dẫn trên màn hình.
2. **Tạo Tunnel trên Cloudflare Zero Trust:**
   - Truy cập **Zero Trust** (`https://one.dash.cloudflare.com/`) -> **Networks** -> **Tunnels** -> **Create a tunnel**.
   - Chọn loại connector là **Cloudflared**, đặt tên tunnel (ví dụ `kizuna-tutor-demo`) và lưu lại.
   - Ở phần cài đặt connector, sao chép chuỗi **Token** (chuỗi dài đứng sau `--token `).
3. **Trỏ Public Hostname về container `frontend`:**
   - Chuyển sang tab **Public Hostname** -> **Add a public hostname**.
   - **Domain**: chọn domain của bạn (có thể thêm subdomain như `demo.yourdomain.id.vn` hoặc dùng domain gốc).
   - **Service**:
     - **Type**: `HTTP`
     - **URL**: `frontend:3000` (hoặc `http://frontend:3000`)
   - Lưu cấu hình hostname.
   > **Lưu ý:** Phải trỏ tới `frontend:3000` (tên service trong `docker-compose.prod.yml`), **không** dùng `localhost:3000` vì `cloudflared` chạy trong container riêng cùng mạng Docker với `frontend`.
4. **Điền token vào `.env` và bật profile `tunnel`:**
   Mở `.env` và dán token vào biến `CLOUDFLARE_TUNNEL_TOKEN`:
   ```env
   CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoi...token_cua_ban...
   ```
   Sau đó chạy lệnh:
   ```bash
   docker compose -f docker-compose.prod.yml --profile tunnel up -d
   ```
   Kiểm tra log của container `cloudflared`:
   ```bash
   docker compose -f docker-compose.prod.yml --profile tunnel logs -f cloudflared
   ```
   Khi thấy log báo `Registered tunnel connection`, bạn đã có thể truy cập `https://<domain-cua-ban>` từ bất kỳ trình duyệt nào.

### Cách B: Test nhanh không cần domain (Quick Tunnel)

Nếu bạn chưa có domain và chỉ muốn lấy nhanh một link `https://*.trycloudflare.com` để kiểm tra:

1. Cài `cloudflared` trực tiếp trên Ubuntu VPS:
   ```bash
   curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb -o cloudflared.deb
   sudo dpkg -i cloudflared.deb
   rm cloudflared.deb
   ```
2. Chạy Quick Tunnel trỏ về cổng `3000` của `frontend`:
   ```bash
   cloudflared tunnel --url http://localhost:3000
   ```
3. Sao chép đường dẫn `https://<chuoi-ngau-nhien>.trycloudflare.com` in ra trên terminal để truy cập thử.

> **Lưu ý:** URL của Quick Tunnel sẽ đổi mỗi lần tắt/bật lại lệnh `cloudflared`, nên **Cách A** vẫn là lựa chọn ổn định nhất cho buổi bảo vệ đồ án.

---

## Bước 5: Cập nhật Google OAuth2 & VNPay theo domain HTTPS

Sau khi đã có URL HTTPS công khai (ví dụ `https://kizuna.yourdomain.id.vn`):

1. **Cập nhật Google Cloud Console:**
   - Vào [Google Cloud Console](https://console.cloud.google.com/) -> **APIs & Services** -> **Credentials** -> chọn **OAuth 2.0 Client ID** của dự án.
   - Trong mục **Authorized redirect URIs**, thêm chính xác URL:
     ```text
     https://<domain>/login/oauth2/code/google
     ```
   - Lưu lại (thay đổi của Google có thể mất 1–5 phút để có hiệu lực).

2. **Cập nhật file `.env` trên VPS:**
   ```bash
   nano .env
   ```
   Sửa lại các biến sau cho khớp với domain thật:
   ```env
   APP_FRONTEND_ORIGINS=https://<domain>
   APP_FRONTEND_URL=https://<domain>
   GOOGLE_REDIRECT_URI=https://<domain>/login/oauth2/code/google
   VNPAY_RETURN_URL=https://<domain>/tutor/membership/result
   ```
   *(Nếu có cấu hình IPN trên trang quản trị merchant VNPay Sandbox, điền `https://<domain>/api/payments/vnpay/ipn`.)*

3. **Áp dụng cấu hình mới cho container:**
   - Nếu chạy không có profile `tunnel` (hoặc dùng Quick Tunnel):
     ```bash
     docker compose -f docker-compose.prod.yml up -d
     ```
   - Nếu chạy bằng Cách A (với container `cloudflared`):
     ```bash
     docker compose -f docker-compose.prod.yml --profile tunnel up -d
     ```

---

## Bước 6: Lưu ý quan trọng khi bảo vệ đồ án

1. **Giọng đọc tiếng Nhật (VOICEVOX fallback):**
   - Trên VPS không chạy container VOICEVOX Engine (`127.0.0.1:50021`) để tiết kiệm RAM/CPU.
   - `frontend/js/voicevoxClient.js` tự động kiểm tra và **fallback mượt mà sang Web Speech API** (`window.speechSynthesis`, giọng `ja-JP`) của trình duyệt.
   - **Mẹo demo:** Hãy kiểm tra trước trên laptop/trình duyệt sẽ dùng để trình chiếu (Chrome hoặc Edge hoạt động ổn định với Web Speech API tiếng Nhật).

2. **Agent Memory lưu bền vững trên Supabase:**
   - Trong `frontend/server.js`, mọi request tới `/memory/*` và `/agent-memory/*` sẽ tự động fallback về `backend:8085` khi microservice cổng `8081` không chạy.
   - Spring Boot (`AgentMemoryController`) lưu trực tiếp bộ nhớ học tập vào bảng `learner_agent_memories` trên **Supabase PostgreSQL**, nên dữ liệu hội thoại/ghi nhớ AI không bị mất khi restart container.

3. **Dữ liệu ảnh đại diện & file luyện đề nằm trong Docker volume `kizuna_data`:**
   - `docker-compose.prod.yml` ánh xạ volume **`kizuna_data`** vào `/root/kizuna-data` trong container `backend` (`AVATAR_STORAGE_DIR=/root/kizuna-data/avatars`, `VERIFICATION_STORAGE_DIR=/root/kizuna-data/verification-docs`).
   - Khi `R2_ENABLED=false`, ảnh đại diện và file luyện đề được lưu trên `kizuna_data` và **vẫn được giữ nguyên** khi chạy `up -d`, `restart`, hoặc `down` (lưu ý: upload giấy tờ xác minh gia sư mới vẫn yêu cầu `R2_ENABLED=true`).
   - **TUYỆT ĐỐI KHÔNG** chạy `docker compose -f docker-compose.prod.yml down -v` vì cờ `-v` sẽ xóa sạch volume `kizuna_data`.

---

## Các lệnh vận hành & xử lý sự cố nhanh

- **Cập nhật code mới từ Git và build lại:**
  ```bash
  git pull
  docker compose -f docker-compose.prod.yml --profile tunnel up -d --build
  ```
- **Xem log riêng của `backend` hoặc `frontend`:**
  ```bash
  docker compose -f docker-compose.prod.yml logs -f backend
  docker compose -f docker-compose.prod.yml logs -f frontend
  ```
- **Khởi động lại nhanh toàn bộ service:**
  ```bash
  docker compose -f docker-compose.prod.yml --profile tunnel restart
  ```
- **Dừng hệ thống (vẫn giữ dữ liệu volume `kizuna_data`):**
  ```bash
  docker compose -f docker-compose.prod.yml --profile tunnel down
  ```
- **Lỗi `502 Bad Gateway` ngay sau khi bật:** `frontend` lên nhanh hơn Spring Boot `backend` khoảng 15–30 giây; đợi thêm nửa phút rồi tải lại trang hoặc kiểm tra `logs -f backend` xem kết nối Supabase có bị sai mật khẩu/URL không.
