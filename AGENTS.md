# Agent & Coding Guidelines for KizunaTutor

This document provides system-level guidance for all AI assistants (IntelliJ AI Assistant, Cursor, Claude Code, Copilot, Antigravity) working on the **KizunaTutor** codebase.

---

## 1. Design & UI Guidelines (from `pbakaus/impeccable` & `Leonxlnx/taste-skill`)

When creating or modifying frontend components, pages, or templates:
1. **Source of Truth**: Always respect [DESIGN.md](file:///C:/Users/Vuh26/Desktop/KizunaTutor/DESIGN.md).
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
   - Sourced from [DatabasePgAdmin4.txt](file:///C:/Users/Vuh26/Desktop/KizunaTutor/DatabasePgAdmin4.txt) (29 tables).
   - Entities must use standard Spring Data JPA annotations (`@Entity`, `@Table`, `@Id`, UUID keys).
2. **AI Memory Microservice**:
   - Use `com.kizuna.service.ai.AgentMemoryService` to communicate with the `agent-memory` Docker container on port `8081`.
3. **Kanji Asset Integration**:
   - Kanji data is sourced from `data/kanjivg/kanji` and managed via `KanjiDataSeeder`.
