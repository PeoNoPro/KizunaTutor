# Sequence Diagrams - Feature 4 & Feature 5 (KizunaTutor)

Tài liệu này đặc tả các biểu đồ tuần tự (Sequence Diagram) cho các luồng nghiệp vụ cốt lõi của **Feature 4 (Tìm kiếm và khám phá gia sư - FE-04)** và **Feature 5 (Đăng ký học thử 1-1 và kết nối - FE-05)** theo đúng chuẩn mẫu (template) gồm các stereotype phân tầng:
- `<<user interaction>>` Tầng giao diện người dùng (UI)
- `<<control>>` Tầng điều khiển (REST Controller)
- `<<service>>` Tầng nghiệp vụ (Service Layer & Cross-cutting)
- `<<repository>>` Tầng truy xuất dữ liệu (Data Access / Repository)
- `Database` Cơ sở dữ liệu (PostgreSQL)
- `<<entity>>` Đối tượng miền thực thể (Domain Entity)

---

## Danh mục các Sequence Diagram chính

1. **Sequence 1 (FE-04)**: Tìm kiếm và lọc danh sách gia sư (`Search Tutors Sequence`)
2. **Sequence 2 (FE-04)**: Xem chi tiết hồ sơ gia sư (`View Tutor Profile Sequence`)
3. **Sequence 3 (FE-05)**: Học viên đăng ký lịch học thử 1-1 (`Create Trial Booking Sequence`)
4. **Sequence 4 (FE-05)**: Gia sư tiếp nhận và phản hồi yêu cầu học thử (`Respond Trial Booking Sequence`)
5. **Sequence 5 (FE-05)**: Hủy buổi học thử trước giờ học (`Cancel Trial Booking Sequence`)

---

## 1. Sequence 1: Tìm kiếm & Lọc danh sách gia sư (FE-04)

### 1.1. Sơ đồ Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Learner as Học viên / Khách
    participant UI as <<user interaction>><br/>TutorDiscoveryUI
    participant Controller as <<control>><br/>:TutorSearchController
    participant Service as <<service>><br/>:TutorSearchService
    participant Spec as <<specification>><br/>:TutorSearchSpecification
    participant Repo as <<repository>><br/>:TutorSearchRepository
    participant DB as Database<br/>(PostgreSQL)
    participant Entity as <<entity>><br/>:Tutor

    Learner ->> UI: Nhập từ khóa, chọn JLPT, mức giá, khung giờ, nhấn "Tìm kiếm"
    activate UI
    UI ->> Controller: GET /api/tutors?keyword=...&jlptLevel=...&page=0&size=10
    activate Controller

    Controller ->> Service: searchTutors(criteria: TutorSearchCriteriaDTO)
    activate Service

    Service ->> Spec: filter(criteria)
    activate Spec
    Spec -->> Service: return Specification<Tutor>
    deactivate Spec

    Service ->> Repo: findAll(spec, pageable)
    activate Repo
    Repo ->> DB: Execute SQL SELECT ... FROM tutors WHERE ... ORDER BY rating DESC LIMIT 10 OFFSET 0
    activate DB
    DB -->> Repo: Trả về tập dữ liệu bản ghi gia sư
    deactivate DB
    Repo -->> Service: return Page<Tutor>
    deactivate Repo

    loop Với mỗi bản ghi Tutor
        Service ->> Entity: Lấy thông tin hồ sơ, chứng chỉ, khung giờ dạy
        activate Entity
        Entity -->> Service: return Tutor data
        deactivate Entity
        Service ->> Service: Map Tutor -> TutorSummaryResponseDTO
    end

    Service -->> Controller: return TutorSearchPageResponse
    deactivate Service

    Controller -->> UI: HTTP 200 OK (Danh sách gia sư và phân trang)
    deactivate Controller

    UI -->> Learner: Hiển thị danh sách thẻ gia sư (kèm đánh giá, giá, lịch rảnh)
    deactivate UI
```

### 1.2. Mã nguồn PlantUML (Sao chép vào PlantUML / Word)

```plantuml
@startuml
skinparam sequenceMessageAlign center
skinparam shadowing false
autonumber

actor "Học viên / Khách" as Learner
participant "TutorDiscovery UI" as UI <<user interaction>>
participant ":TutorSearchController" as Controller <<control>>
participant ":TutorSearchService" as Service <<service>>
participant ":TutorSearchSpecification" as Spec <<specification>>
participant ":TutorSearchRepository" as Repo <<repository>>
database "Database\n(PostgreSQL)" as DB
participant ":Tutor" as Entity <<entity>>

Learner -> UI: Nhập tiêu chí tìm kiếm, nhấn "Tìm kiếm"
activate UI

UI -> Controller: GET /api/tutors?keyword=...&jlptLevel=...&page=0&size=10
activate Controller

Controller -> Service: searchTutors(criteria: TutorSearchCriteriaDTO)
activate Service

Service -> Spec: filter(criteria)
activate Spec
Spec --> Service: return Specification<Tutor>
deactivate Spec

Service -> Repo: findAll(specification, pageable)
activate Repo

Repo -> DB: Execute SQL SELECT tutors WHERE ... ORDER BY rating DESC
activate DB
DB --> Repo: Trả về dữ liệu kết quả gia sư
deactivate DB

Repo --> Service: return Page<Tutor>
deactivate Repo

loop Với mỗi Tutor trong trang kết quả
    Service -> Entity: Đọc thuộc tính profile, lịch rảnh, review
    activate Entity
    Entity --> Service: return entity data
    deactivate Entity
    Service -> Service: Chuyển đổi sang TutorSummaryResponseDTO
end

Service --> Controller: return TutorSearchPageResponse
deactivate Service

Controller --> UI: HTTP 200 OK (TutorSearchPageResponse)
deactivate Controller

UI --> Learner: Hiển thị danh sách thẻ gia sư kèm bộ lọc kết quả
deactivate UI

@enduml
```

---

## 2. Sequence 2: Xem chi tiết hồ sơ gia sư (FE-04)

### 2.1. Sơ đồ Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Learner as Học viên
    participant UI as <<user interaction>><br/>TutorProfileUI
    participant Controller as <<control>><br/>:TutorSearchController
    participant Service as <<service>><br/>:TutorSearchService
    participant Repo as <<repository>><br/>:TutorSearchRepository
    participant DB as Database<br/>(PostgreSQL)
    participant Entity as <<entity>><br/>:Tutor

    Learner ->> UI: Nhấp vào thẻ gia sư (URL: /gs/10001)
    activate UI
    UI ->> Controller: GET /api/tutors/code/10001
    activate Controller

    Controller ->> Service: getTutorByCode(10001)
    activate Service

    Service ->> Repo: findByTutorCode(10001)
    activate Repo
    Repo ->> DB: Execute SQL SELECT * FROM tutors WHERE tutor_code = 10001
    activate DB
    DB -->> Repo: Bản ghi Tutor
    deactivate DB
    Repo -->> Service: return Optional<Tutor>
    deactivate Repo

    alt Tìm thấy gia sư
        Service ->> Entity: Lấy bio, kinh nghiệm, video giới thiệu, chứng chỉ
        activate Entity
        Entity -->> Service: return full tutor info
        deactivate Entity
        Service ->> Service: Map sang TutorSummaryResponseDTO
        Service -->> Controller: return TutorSummaryResponseDTO
        Controller -->> UI: HTTP 200 OK (Chi tiết hồ sơ gia sư)
        UI -->> Learner: Hiển thị đầy đủ thông tin, lịch rảnh và nút "Đặt lịch học thử"
    else Không tìm thấy
        Service -->> Controller: return null
        Controller -->> UI: HTTP 404 NOT FOUND
        UI -->> Learner: Hiển thị thông báo "Không tìm thấy gia sư yêu cầu"
    end
    deactivate Service
    deactivate Controller
    deactivate UI
```

### 2.2. Mã nguồn PlantUML

```plantuml
@startuml
skinparam sequenceMessageAlign center
skinparam shadowing false
autonumber

actor "Học viên" as Learner
participant "TutorProfile UI" as UI <<user interaction>>
participant ":TutorSearchController" as Controller <<control>>
participant ":TutorSearchService" as Service <<service>>
participant ":TutorSearchRepository" as Repo <<repository>>
database "Database" as DB
participant ":Tutor" as Entity <<entity>>

Learner -> UI: Xem hồ sơ gia sư (click mã gia sư 10001)
activate UI

UI -> Controller: GET /api/tutors/code/10001
activate Controller

Controller -> Service: getTutorByCode(10001)
activate Service

Service -> Repo: findByTutorCode(10001)
activate Repo

Repo -> DB: SELECT * FROM tutors WHERE tutor_code = 10001
activate DB
DB --> Repo: Bản ghi Tutor
deactivate DB

Repo --> Service: return Optional<Tutor>
deactivate Repo

alt Gia sư tồn tại
    Service -> Entity: Trích xuất lịch rảnh, chứng chỉ, bài đánh giá
    activate Entity
    Entity --> Service: return tutor data
    deactivate Entity
    Service -> Service: Map sang TutorSummaryResponseDTO
    Service --> Controller: return TutorSummaryResponseDTO
    Controller --> UI: HTTP 200 OK (TutorSummaryResponseDTO)
    UI --> Learner: Hiển thị đầy đủ hồ sơ và lịch rảnh
else Không tìm thấy
    Service --> Controller: return null
    Controller --> UI: HTTP 404 Not Found
    UI --> Learner: Báo lỗi không tìm thấy gia sư
end

deactivate Service
deactivate Controller
deactivate UI

@enduml
```

---

## 3. Sequence 3: Học viên đăng ký học thử 1-1 (FE-05) - Trọng tâm

### 3.1. Sơ đồ Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Learner as Học viên (Learner)
    participant UI as <<user interaction>><br/>BookingModalUI
    participant Controller as <<control>><br/>:TrialBookingController
    participant Auth as <<service>><br/>:JwtTokenProvider
    participant Service as <<service>><br/>:TrialBookingService
    participant BookingRepo as <<repository>><br/>:TrialBookingRepository
    participant ConnRepo as <<repository>><br/>:ConnectionRequestRepository
    participant HistRepo as <<repository>><br/>:TrialBookingHistoryRepository
    participant DB as Database<br/>(PostgreSQL)
    participant Entity as <<entity>><br/>:TrialBooking

    Learner ->> UI: Chọn 25/50 phút, ngày, giờ rảnh và nhấn "Tiếp tục"
    activate UI
    UI ->> Controller: POST /api/v1/trial-bookings (Bearer Token, CreateTrialBookingRequestDTO)
    activate Controller

    Controller ->> Auth: getUsernameFromJWT(token)
    activate Auth
    Auth -->> Controller: return userEmail
    deactivate Auth

    Controller ->> Service: createTrialBooking(userEmail, dto)
    activate Service

    %% Validation & Conflict Checking
    Service ->> Service: validateStartTimeInFuture(dto.scheduledStartTime)
    Service ->> BookingRepo: existsOverlappingBooking(tutorId, startTime, endTime)
    activate BookingRepo
    BookingRepo ->> DB: SELECT COUNT(*) > 0 FROM trial_bookings WHERE tutor_id = ... AND status != 'CANCELLED'
    activate DB
    DB -->> BookingRepo: return boolean (false)
    deactivate DB
    BookingRepo -->> Service: return false (Không bị trùng lịch)
    deactivate BookingRepo

    alt Bị trùng lịch hẹn
        Service -->> Controller: throw ConflictException("Khung giờ này vừa có người đăng ký")
        Controller -->> UI: HTTP 409 CONFLICT
        UI -->> Learner: Hiển thị thông báo chọn khung giờ khác
    else Khung giờ hợp lệ (Thành công)
        %% Tạo hoặc lấy ConnectionRequest
        Service ->> ConnRepo: findByLearnerIdAndTutorId(learnerId, tutorId)
        activate ConnRepo
        ConnRepo ->> DB: SELECT * FROM connection_requests ...
        activate DB
        DB -->> ConnRepo: return Optional<ConnectionRequest>
        deactivate DB
        ConnRepo -->> Service: return connectionRequest
        deactivate ConnRepo

        opt Chưa có kết nối trước đó
            Service ->> DB: INSERT INTO connection_requests (status: 'PENDING')
        end

        %% Tạo TrialBooking Entity
        Service ->> Entity: Khởi tạo thực thể mới (status: 'PENDING', duration: 25/50)
        activate Entity
        Entity -->> Service: return trialBookingEntity
        deactivate Entity

        %% Lưu Database
        Service ->> BookingRepo: save(trialBookingEntity)
        activate BookingRepo
        BookingRepo ->> DB: INSERT INTO trial_bookings VALUES (...)
        activate DB
        DB -->> BookingRepo: return saved TrialBooking (booking_id)
        deactivate DB
        BookingRepo -->> Service: return savedBooking
        deactivate BookingRepo

        %% Lưu Lịch sử (Audit Log)
        Service ->> HistRepo: save(new TrialBookingHistory(from: null, to: 'PENDING'))
        activate HistRepo
        HistRepo ->> DB: INSERT INTO trial_booking_histories VALUES (...)
        deactivate HistRepo

        Service ->> Service: Map savedBooking -> TrialBookingResponseDTO
        Service -->> Controller: return TrialBookingResponseDTO
        Controller -->> UI: HTTP 201 CREATED (TrialBookingResponseDTO)
        UI -->> Learner: Hiển thị thông báo "Đã gửi yêu cầu học thử thành công - Chờ gia sư xác nhận"
    end

    deactivate Service
    deactivate Controller
    deactivate UI
```

### 3.2. Mã nguồn PlantUML

```plantuml
@startuml
skinparam sequenceMessageAlign center
skinparam shadowing false
autonumber

actor "Học viên (Learner)" as Learner
participant "BookingModal UI" as UI <<user interaction>>
participant ":TrialBookingController" as Controller <<control>>
participant ":JwtTokenProvider" as Auth <<service>>
participant ":TrialBookingService" as Service <<service>>
participant ":TrialBookingRepository" as BookingRepo <<repository>>
participant ":ConnectionRequestRepository" as ConnRepo <<repository>>
participant ":TrialBookingHistoryRepository" as HistRepo <<repository>>
database "Database\n(PostgreSQL)" as DB
participant ":TrialBooking" as Entity <<entity>>

Learner -> UI: Chọn thời lượng, ngày, giờ rảnh và nhấn "Tiếp tục"
activate UI

UI -> Controller: POST /api/v1/trial-bookings (Bearer Token, dto)
activate Controller

Controller -> Auth: getUsernameFromJWT(token)
activate Auth
Auth --> Controller: return learnerEmail
deactivate Auth

Controller -> Service: createTrialBooking(learnerEmail, dto)
activate Service

Service -> Service: validateStartTimeInFuture(dto.scheduledStartTime)

Service -> BookingRepo: existsOverlappingBooking(tutorId, startTime, endTime)
activate BookingRepo
BookingRepo -> DB: SELECT count(*) FROM trial_bookings WHERE tutor_id = ? ...
activate DB
DB --> BookingRepo: return false (Không trùng)
deactivate DB
BookingRepo --> Service: return false
deactivate BookingRepo

alt Bị trùng lịch gia sư đã có hẹn
    Service --> Controller: throw ConflictException("Khung giờ đã có người đặt")
    Controller --> UI: HTTP 409 Conflict
    UI --> Learner: Báo lỗi trùng lịch và yêu cầu chọn giờ khác
else Khung giờ hợp lệ
    Service -> ConnRepo: findByLearnerIdAndTutorId(learnerId, tutorId)
    activate ConnRepo
    ConnRepo -> DB: SELECT * FROM connection_requests ...
    activate DB
    DB --> ConnRepo: return Optional<ConnectionRequest>
    deactivate DB
    ConnRepo --> Service: return ConnectionRequest
    deactivate ConnRepo

    opt Chưa có quan hệ kết nối
        Service -> DB: INSERT INTO connection_requests (status = 'PENDING')
    end

    Service -> Entity: new TrialBooking(learner, tutor, startTime, PENDING)
    activate Entity
    Entity --> Service: return entity instance
    deactivate Entity

    Service -> BookingRepo: save(trialBooking)
    activate BookingRepo
    BookingRepo -> DB: INSERT INTO trial_bookings VALUES (...)
    activate DB
    DB --> BookingRepo: return saved TrialBooking
    deactivate DB
    BookingRepo --> Service: return savedEntity
    deactivate BookingRepo

    Service -> HistRepo: save(TrialBookingHistory)
    activate HistRepo
    HistRepo -> DB: INSERT INTO trial_booking_histories VALUES (...)
    deactivate HistRepo

    Service --> Controller: return TrialBookingResponseDTO
    deactivate Service

    Controller --> UI: HTTP 201 Created (TrialBookingResponseDTO)
    deactivate Controller

    UI --> Learner: Hiển thị modal "Chờ gia sư xác nhận"
    deactivate UI
end

@enduml
```

---

## 4. Sequence 4: Gia sư tiếp nhận và phản hồi yêu cầu học thử (FE-05)

### 4.1. Sơ đồ Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Tutor as Gia sư (Tutor)
    participant UI as <<user interaction>><br/>ConnectionRequestsUI
    participant Controller as <<control>><br/>:TrialBookingController
    participant Auth as <<service>><br/>:JwtTokenProvider
    participant Service as <<service>><br/>:TrialBookingService
    participant BookingRepo as <<repository>><br/>:TrialBookingRepository
    participant ConnRepo as <<repository>><br/>:ConnectionRequestRepository
    participant HistRepo as <<repository>><br/>:TrialBookingHistoryRepository
    participant DB as Database<br/>(PostgreSQL)

    Tutor ->> UI: Mở trang Quản lý yêu cầu, bấm "Chấp nhận" (nhập Meet link) hoặc "Từ chối"
    activate UI
    UI ->> Controller: PUT /api/v1/trial-bookings/{id}/respond (RespondTrialBookingRequestDTO)
    activate Controller

    Controller ->> Auth: getUsernameFromJWT(token)
    activate Auth
    Auth -->> Controller: return tutorEmail
    deactivate Auth

    Controller ->> Service: respondTrialBooking(tutorEmail, bookingId, dto)
    activate Service

    Service ->> BookingRepo: findByBookingId(bookingId)
    activate BookingRepo
    BookingRepo ->> DB: SELECT * FROM trial_bookings WHERE booking_id = ...
    activate DB
    DB -->> BookingRepo: return TrialBooking
    deactivate DB
    BookingRepo -->> Service: return TrialBooking
    deactivate BookingRepo

    Service ->> Service: verifyTutorOwnership(trialBooking, tutorEmail)

    alt Gia sư chọn ACCEPT (Chấp nhận)
        Service ->> Service: trialBooking.setStatus(CONFIRMED)<br/>trialBooking.setMeetingLink(dto.meetingLink)
        Service ->> BookingRepo: save(trialBooking)
        activate BookingRepo
        BookingRepo ->> DB: UPDATE trial_bookings SET status = 'CONFIRMED', meeting_link = ...
        deactivate BookingRepo

        Service ->> ConnRepo: updateStatus(ACCEPTED)
        activate ConnRepo
        ConnRepo ->> DB: UPDATE connection_requests SET status = 'ACCEPTED'
        deactivate ConnRepo

        Service ->> HistRepo: save(new History(from: 'PENDING', to: 'CONFIRMED'))
        activate HistRepo
        HistRepo ->> DB: INSERT INTO trial_booking_histories ...
        deactivate HistRepo

    else Gia sư chọn REJECT (Từ chối)
        Service ->> Service: trialBooking.setStatus(CANCELLED)<br/>trialBooking.setCancellationReason(dto.note)
        Service ->> BookingRepo: save(trialBooking)
        activate BookingRepo
        BookingRepo ->> DB: UPDATE trial_bookings SET status = 'CANCELLED'
        deactivate BookingRepo

        Service ->> HistRepo: save(new History(from: 'PENDING', to: 'CANCELLED'))
        activate HistRepo
        HistRepo ->> DB: INSERT INTO trial_booking_histories ...
        deactivate HistRepo
    end

    Service ->> Service: Map updatedBooking -> TrialBookingResponseDTO
    Service -->> Controller: return TrialBookingResponseDTO
    deactivate Service

    Controller -->> UI: HTTP 200 OK (Xử lý thành công)
    deactivate Controller

    UI -->> Tutor: Cập nhật trạng thái thẻ ("Đã xác nhận" / "Đã từ chối")
    deactivate UI
```

### 4.2. Mã nguồn PlantUML

```plantuml
@startuml
skinparam sequenceMessageAlign center
skinparam shadowing false
autonumber

actor "Gia sư (Tutor)" as Tutor
participant "ConnectionRequests UI" as UI <<user interaction>>
participant ":TrialBookingController" as Controller <<control>>
participant ":JwtTokenProvider" as Auth <<service>>
participant ":TrialBookingService" as Service <<service>>
participant ":TrialBookingRepository" as BookingRepo <<repository>>
participant ":ConnectionRequestRepository" as ConnRepo <<repository>>
participant ":TrialBookingHistoryRepository" as HistRepo <<repository>>
database "Database\n(PostgreSQL)" as DB

Tutor -> UI: Bấm "Chấp nhận" (kèm Google Meet) hoặc "Từ chối"
activate UI

UI -> Controller: PUT /api/v1/trial-bookings/{id}/respond (dto)
activate Controller

Controller -> Auth: getUsernameFromJWT(token)
activate Auth
Auth --> Controller: return tutorEmail
deactivate Auth

Controller -> Service: respondTrialBooking(tutorEmail, id, dto)
activate Service

Service -> BookingRepo: findByBookingId(id)
activate BookingRepo
BookingRepo -> DB: SELECT * FROM trial_bookings WHERE booking_id = ?
activate DB
DB --> BookingRepo: return TrialBooking entity
deactivate DB
BookingRepo --> Service: return entity
deactivate BookingRepo

Service -> Service: verifyTutorOwnership()

alt Hành động: ACCEPT (Chấp nhận)
    Service -> Service: setStatus(CONFIRMED), setMeetingLink(url)
    Service -> BookingRepo: save(trialBooking)
    activate BookingRepo
    BookingRepo -> DB: UPDATE trial_bookings SET status = 'CONFIRMED' ...
    deactivate BookingRepo

    Service -> ConnRepo: updateStatus(ACCEPTED)
    activate ConnRepo
    ConnRepo -> DB: UPDATE connection_requests SET status = 'ACCEPTED' ...
    deactivate ConnRepo

    Service -> HistRepo: save(history: PENDING -> CONFIRMED)
    activate HistRepo
    HistRepo -> DB: INSERT INTO trial_booking_histories ...
    deactivate HistRepo

else Hành động: REJECT (Từ chối)
    Service -> Service: setStatus(CANCELLED), setReason(note)
    Service -> BookingRepo: save(trialBooking)
    activate BookingRepo
    BookingRepo -> DB: UPDATE trial_bookings SET status = 'CANCELLED' ...
    deactivate BookingRepo

    Service -> HistRepo: save(history: PENDING -> CANCELLED)
    activate HistRepo
    HistRepo -> DB: INSERT INTO trial_booking_histories ...
    deactivate HistRepo
end

Service --> Controller: return TrialBookingResponseDTO
deactivate Service

Controller --> UI: HTTP 200 OK (Đã xử lý yêu cầu học thử thành công)
deactivate Controller

UI --> Tutor: Chuyển thẻ vào tab tương ứng và thông báo thành công
deactivate UI

@enduml
```

---

## 5. Sequence 5: Học viên / Gia sư hủy buổi học thử (FE-05)

### 5.1. Sơ đồ Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor User as Học viên / Gia sư
    participant UI as <<user interaction>><br/>ConnectionRequestsUI
    participant Controller as <<control>><br/>:TrialBookingController
    participant Auth as <<service>><br/>:JwtTokenProvider
    participant Service as <<service>><br/>:TrialBookingService
    participant Repo as <<repository>><br/>:TrialBookingRepository
    participant HistRepo as <<repository>><br/>:TrialBookingHistoryRepository
    participant DB as Database<br/>(PostgreSQL)

    User ->> UI: Nhấn nút "Hủy lịch hẹn" và nhập lý do
    activate UI
    UI ->> Controller: PUT /api/v1/trial-bookings/{id}/cancel (CancelTrialBookingRequestDTO)
    activate Controller

    Controller ->> Auth: getUsernameFromJWT(token)
    activate Auth
    Auth -->> Controller: return currentUserEmail
    deactivate Auth

    Controller ->> Service: cancelTrialBooking(userEmail, id, dto)
    activate Service

    Service ->> Repo: findByBookingId(id)
    activate Repo
    Repo ->> DB: SELECT * FROM trial_bookings WHERE booking_id = ...
    activate DB
    DB -->> Repo: return TrialBooking
    deactivate DB
    Repo -->> Service: return TrialBooking
    deactivate Repo

    Service ->> Service: verifyCanCancel(trialBooking, userEmail)

    Service ->> Service: trialBooking.setStatus(CANCELLED)<br/>trialBooking.setCancellationReason(dto.reason)
    Service ->> Repo: save(trialBooking)
    activate Repo
    Repo ->> DB: UPDATE trial_bookings SET status = 'CANCELLED', cancellation_reason = ...
    deactivate Repo

    Service ->> HistRepo: save(new History(from: oldStatus, to: 'CANCELLED'))
    activate HistRepo
    HistRepo ->> DB: INSERT INTO trial_booking_histories ...
    deactivate HistRepo

    Service ->> Service: Map sang TrialBookingResponseDTO
    Service -->> Controller: return TrialBookingResponseDTO
    deactivate Service

    Controller -->> UI: HTTP 200 OK (Đã hủy buổi học thử thành công)
    deactivate Controller

    UI -->> User: Hiển thị badge "Đã hủy" và cập nhật lịch sử
    deactivate UI
```

### 5.2. Mã nguồn PlantUML

```plantuml
@startuml
skinparam sequenceMessageAlign center
skinparam shadowing false
autonumber

actor "Học viên / Gia sư" as User
participant "ConnectionRequests UI" as UI <<user interaction>>
participant ":TrialBookingController" as Controller <<control>>
participant ":JwtTokenProvider" as Auth <<service>>
participant ":TrialBookingService" as Service <<service>>
participant ":TrialBookingRepository" as Repo <<repository>>
participant ":TrialBookingHistoryRepository" as HistRepo <<repository>>
database "Database" as DB

User -> UI: Nhấn "Hủy lịch hẹn", điền lý do và xác nhận
activate UI

UI -> Controller: PUT /api/v1/trial-bookings/{id}/cancel (dto)
activate Controller

Controller -> Auth: getUsernameFromJWT(token)
activate Auth
Auth --> Controller: return currentUserEmail
deactivate Auth

Controller -> Service: cancelTrialBooking(currentUserEmail, id, dto)
activate Service

Service -> Repo: findByBookingId(id)
activate Repo
Repo -> DB: SELECT * FROM trial_bookings WHERE booking_id = ?
activate DB
DB --> Repo: return TrialBooking
deactivate DB
Repo --> Service: return TrialBooking
deactivate Repo

Service -> Service: verifyCanCancel(trialBooking, currentUserEmail)
Service -> Service: setStatus(CANCELLED), setCancellationReason(reason)

Service -> Repo: save(trialBooking)
activate Repo
Repo -> DB: UPDATE trial_bookings SET status = 'CANCELLED' ...
deactivate Repo

Service -> HistRepo: save(history: oldStatus -> CANCELLED)
activate HistRepo
HistRepo -> DB: INSERT INTO trial_booking_histories ...
deactivate HistRepo

Service --> Controller: return TrialBookingResponseDTO
deactivate Service

Controller --> UI: HTTP 200 OK
deactivate Controller

UI --> User: Cập nhật giao diện sang trạng thái "Đã hủy"
deactivate UI

@enduml
```

