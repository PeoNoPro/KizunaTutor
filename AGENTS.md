# Agent & Coding Guidelines for KizunaTutor

This document provides system-level guidance for all AI assistants (IntelliJ AI Assistant, Cursor, Claude Code, Copilot, Antigravity) working on the **KizunaTutor** codebase.

---

## 1. Design & UI Guidelines (from `pbakaus/impeccable` & `Leonxlnx/taste-skill`)

When creating or modifying frontend components, pages, or templates:
1. **Source of Truth**: Always respect [DESIGN.md](file:///C:/Users/Vuh26/Desktop/KizunaTutor/DESIGN.md) and [UI_LOCALIZATION_RULES.md](file:///C:/Users/Vuh26/Desktop/KizunaTutor/UI_LOCALIZATION_RULES.md).
2. **Palette Compliance**:
   - Primary: Indigo Aizome (`#1B2A4A`)
   - Secondary: Sakura Pink (`#FAD0C4`)
   - Accent/Success: Matcha Green (`#88A064`)
   - Background: Washi Off-white (`#FAF9F6`)
   - Text: Sumi Charcoal (`#1F2421`)
3. **Anti-Slop Protocol**:
   - Do NOT use generic purple AI gradients.
   - Do NOT output repetitive 3-column card layouts without semantic hierarchy.
   - Respect Japanese typography rules: ensure `font-family` supports Japanese glyphs (`"Noto Sans JP"`, `"Zen Kaku Gothic New"`).
   - Kanji practice canvases must strictly align with the `109x109` KanjiVG coordinate system.

---

## 2. Backend & Architecture Guidelines (Spring Boot + PostgreSQL)

1. **Database Schema**:
   - Sourced from [DatabasePgAdmin4.txt](file:///C:/Users/Vuh26/Desktop/KizunaTutor/DatabasePgAdmin4.txt) and [Database_Minigame_KanjiFolder.sql](file:///C:/Users/Vuh26/Desktop/KizunaTutor/Database_Minigame_KanjiFolder.sql).
   - Entities must use standard Spring Data JPA annotations (`@Entity`, `@Table`, `@Id`, UUID keys).
2. **AI Memory Microservice**:
   - Use `com.kizuna.service.ai.AgentMemoryService` to communicate with the `agent-memory` Docker container on port `8081`.
3. **Kanji Asset Integration**:
   - Kanji data is sourced from `data/kanjivg/kanji` and managed via `KanjiDataSeeder`.

---

## 3. Strict UI Localization & Human-First Rule (BẮT BUỘC TUÂN THỦ)

1. **100% TIẾNG VIỆT CHO NGƯỜI DÙNG**:
   - Tất cả nội dung hiển thị cho người học (tiêu đề, nút bấm, nhãn, hướng dẫn, hộp thoại, thông báo, cảnh báo, thống kê) **BẮT BUỘC 100% BẰNG TIẾNG VIỆT**.
   - Ngoại lệ duy nhất: Mặt chữ Kanji, Kana (Hiragana/Katakana), Romaji và bậc JLPT (N5-N1) trong nội dung học tập.
2. **TUYỆT ĐỐI KHÔNG ĐỂ LỘ MÃ CODE HOẶC DATABASE**:
   - Không hiển thị tên biến, tên hàm, tên class, trường database (như ID, UUID, entity, session, request, response, DTO, camelCase, snake_case...).
   - Không dùng tiếng Anh thay thế khi đã có từ tiếng Việt chuẩn xác (Deadline -> Vạch ranh giới nguy hiểm; Game Over -> Kết thúc ván đấu; You Win -> Chiến thắng xuất sắc; Accuracy -> Độ chính xác; Combo -> Chuỗi gõ liên hoàn).
3. **MÃ HÓA UTF-8 TUYỆT ĐỐI**:
   - Tất cả các file HTML, JS, CSS, Java phải được lưu dưới chuẩn UTF-8 không BOM để chống mọi lỗi biến dạng font chữ (Mojibake).
