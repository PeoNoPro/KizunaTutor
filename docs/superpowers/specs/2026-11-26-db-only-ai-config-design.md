# Thiết Kế: Lấy 100% Cấu Hình AI Từ Database (Bỏ Fallback Project)

- **Ngày tạo**: 2026-11-26
- **Trạng thái**: Chờ phê duyệt (Pending Approval)
- **Phạm vi**: Thay đổi nhỏ (`SystemSettingService` + `application.properties` + Unit Test)

---

## 1. Mục tiêu & Ràng buộc
- Loại bỏ hoàn toàn việc đọc `provider`, `model`, `apiKey` AI từ `application.properties` hoặc biến môi trường `GEMINI_API_KEY` / `OPENAI_API_KEY`.
- Toàn bộ các tính năng AI (`AiWritingServiceImpl`, `ReadingAssistantServiceImpl`, `ExerciseAiClient`) tiếp tục gọi qua `SystemSettingService.aiChoice()`, nhưng `SystemSettingService` **chỉ đọc từ bảng `system_configurations`** trong PostgreSQL.
- Nếu DB chưa cấu hình key hoặc lỗi đọc DB, trả về `apiKey = ""` (để các service tự dùng fallback cục bộ / báo chưa cấu hình khóa AI) — không fallback về cấu hình trong project.

## 2. Thiết kế chi tiết
1. **`SystemSettingService.java`**:
   - Xóa các trường `@Value("${app.ai....}")` (`defaultAiProvider`, `defaultGeminiModel`, `defaultOpenAiModel`, `geminiKey`, `openAiKey`) và hàm `serverKey(...)`.
   - `apiKey(provider)`: chỉ đọc `AI_API_KEY_<PROVIDER>` từ DB và giải mã AES-GCM; không có thì trả `""`.
   - `providerNow()`: chỉ đọc `AI_PROVIDER` từ DB (nếu DB chưa có thì trả `""` hoặc `"GEMINI"` khi hiển thị, `model` đọc `AI_MODEL` từ DB, không có thì `""`).
   - `aiChoice()`: gọi `currentAi()`; nếu lỗi DB trả `new AiChoice("", "", "", "")`.
   - `ai()`: `keySource` chỉ trả `"admin"` hoặc `""` (bỏ `"server"`).
2. **`application.properties`**:
   - Xóa mục `# 7. CAU HINH AI ...` (`app.ai.provider`, `app.ai.gemini-api-key`, `app.ai.gemini-model`, `app.ai.openai-api-key`).
3. **Kiểm thử**:
   - Tạo `SystemSettingServiceAiDbOnlyTest.java` kiểm tra đọc từ DB (AES-GCM), trường hợp DB trống và trường hợp lỗi DB đều không lấy giá trị từ `application.properties`.
