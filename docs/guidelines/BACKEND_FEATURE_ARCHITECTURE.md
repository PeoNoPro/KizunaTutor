# QUY HOẠCH KIẾN TRÚC BACKEND THEO TÍNH NĂNG (PACKAGE-BY-FEATURE)
**Dự án:** KizunaTutor  
**Kiến trúc:** Feature-Driven Modular Architecture (Vertical Slice)  
**Phân bổ:** 12 Features nghiệp vụ & Core Infrastructure

---

## 1. NGUYÊN TẮC QUY HOẠCH (ARCHITECTURAL PRINCIPLES)

1. **Vertical Slice / Package-by-Feature (Phân rã theo chiều dọc):**
   - Không đánh số thứ tự (`fe01_`, `fe02_`). Tên package đại diện trực tiếp cho domain/tính năng nghiệp vụ: `auth`, `profile`, `classroom`, `payment`...
   - Mỗi Feature là một module tự thân (Self-contained) chứa đầy đủ các tầng liên quan:
     - `controller/`: REST API & MVC Controller phục vụ tính năng
     - `service/`: Service interfaces và implementation nghiệp vụ
     - `repository/`: Spring Data JPA repositories
     - `entity/`: Các thực thể dữ liệu gắn liền với tính năng
     - `dto/`: Request/Response data transfer objects
     - `mapper/`, `util/`: Tiện ích riêng của tính năng (nếu có)

2. **Core vs Features:**
   - **`core/`**: Chứa hạ tầng kỹ thuật dùng chung toàn hệ thống (Cấu hình Spring, Security, JWT, Global Exception Handlers, Base Entities như `BaseEntity`, `User`, `Role`, và Shared Utils). Tuyệt đối không chứa logic nghiệp vụ đặc thù của riêng một feature nào.
   - **`features/`**: Chứa toàn bộ 12 Feature nghiệp vụ người dùng.

3. **Giao tiếp liên Feature (Inter-Feature Communication):**
   - Không gọi trực tiếp `Repository` của Feature khác.
   - Mọi tương tác nghiệp vụ chéo phải thông qua `Service Interface` được khai báo công khai.

---

## 2. BẢNG DANH MỤC 12 FEATURES CHÍNH THỨC

```text
backend/src/main/java/com/kizuna/
│
├── core/                               # Nền tảng dùng chung
│   ├── config/                         # WebMvc, Async, OpenApiSwagger
│   ├── security/                       # SecurityConfig, JwtFilter, TokenProvider, ContextUtil
│   ├── exception/                      # GlobalExceptionHandler, Base Exceptions
│   ├── entity/                         # BaseEntity, User, Role, Learner, Tutor
│   ├── repository/                     # UserRepository, RoleRepository, LearnerRepository, TutorRepository
│   ├── util/                           # DateTimeUtil, TimeZoneUtil
│   ├── controller/                     # WebHomeController, TestController
│   ├── dto/                            # ApiResponse
│   └── mapper/                         # UserMapper
│
├── features/                           # 12 TÍNH NĂNG NGHIỆP VỤ
│   │
│   ├── auth/                           # Authentication & Authorization (Đăng nhập, OTP, Đăng ký)
│   ├── profile/                        # Learner & Tutor Profile Management
│   ├── verification/                   # Tutor Verification (Xét duyệt bằng cấp gia sư)
│   ├── tutor_search/                   # Tutor Search & Connection (Tìm kiếm gia sư, đánh giá)
│   ├── booking/                        # Trial Lesson Booking & Learning Activation
│   ├── classroom/                      # Classroom & Learning Materials Management (Lớp học, Thư mục Kanji)
│   ├── progress/                       # Homework, Assessment & Progress Tracking (Bài tập, tiến độ)
│   ├── learning_support/               # Learning Supports (Kanji, Minigame, AI Memory, Nét vẽ)
│   ├── payment/                        # Membership & Payment Management (Gói hội viên, VNPay)
│   ├── ticket/                         # Support Ticket & Escalation (Khiếu nại, hỗ trợ CS)
│   ├── admin/                          # System Administration & Operational Reporting (Quản trị hệ thống)
│   └── notification/                   # Notification Management (Thông báo hệ thống)
│
└── KizunaBackendApplication.java       # Root Spring Boot Launcher
```

---

## 3. QUY TẮC DÀNH CHO AI & VIBE CODING

Mọi trợ lý AI (Antigravity, Cursor, Claude Code, Copilot) khi lập trình tính năng mới BẮT BUỘC tuân thủ:
1. **Xác định đúng Feature Domain:** Đặt file mới vào đúng thư mục trong `features/<feature_name>/...`.
2. **Khai báo Package chuẩn:** `package com.kizuna.features.<feature_name>.<subpackage>;`.
3. **Không tạo class tự do ở tầng gốc `com.kizuna`** ngoại trừ `KizunaBackendApplication.java`.
