# Unifying Lookup Modules & Standby State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standardize the UX across Vocabulary (`/vocabulary`), Grammar (`/grammar`), and Kanji (`/kanji-dictionary`) into a cohesive "Tra cứu..." (Lookup & Search) paradigm featuring friendly Standby States with Capybara illustration, clear instructions, and 1-click Quick Action Chips, eliminating initial information clutter.

**Architecture:** 
- Unify Sidebar navigation & Page Headers in `learner-layout.js` under explicit "Tra cứu..." titles.
- Implement reusable, responsive Standby State blocks across `grammar` and `learning-support` mirroring the successful pattern from `kanji-dictionary`.
- Wire seamless transitions: on initial load without query/filter, show Standby State; on query, topic click, or quick chip click, transition smoothly to Cards Grid + Standard Pagination (`pagination.js`). Clearing search or resetting filters restores the Standby State cleanly.

**Tech Stack:** Vanilla JavaScript (ES6 Modules), Tailwind CSS, Material Symbols, HTML5, Spring Boot REST Backend.

---

## Global Constraints

- Never break existing API contracts or backend endpoints (`/api/learner/grammar`, `/api/learner/vocabularies`, `/api/learner/kanji`).
- Zero mock data injection: All dynamic data must come from real backend endpoints or user actions.
- Preserve standard pagination compatibility (`pagination.js` page/number/currentPage).
- Keep Voicevox audio pronunciation, SRS flashcard additions, and deep links functional.

---

## Tasks

### Task 1: Update Sidebar & Shell Titles in `learner-layout.js`
**Files:**
- Modify: `frontend/components/learner-layout/learner-layout.js:70-115`

- [x] Rename `Kho ngữ pháp JLPT` -> `Tra cứu ngữ pháp JLPT` (key: `grammar`, icon: `library_books`).
- [x] Rename `Từ điển chữ Hán` -> `Tra cứu Hán tự & Bút thuận` (key: `kanji-dictionary`, icon: `menu_book`).
- [x] Verify `Tra cứu từ vựng` (key: `learning-support`, icon: `search`) is aligned.
- [x] Test layout navigation: verify links open without broken routes.

---

### Task 2: Implement Standby State for `Tra cứu ngữ pháp JLPT` (`/grammar`)
**Files:**
- Modify: `frontend/pages/grammar/index.html:80-190`
- Modify: `frontend/pages/grammar/grammar.js:20-220`

- [x] In `grammar/index.html`:
  - Add `#grammarStandbyState` container with Capybara illustration, title "Sẵn sàng tra cứu Ngữ pháp JLPT 📖", description, and quick action chips (`🌱 Cấp độ N5`, `🌿 Cấp độ N4`, `〜てください`, `〜てはいけません`, `☕ Đời thường`, `💼 Công việc`).
- [x] In `grammar.js`:
  - Set `initLearnerShell` page title to `Tra cứu ngữ pháp JLPT 📖`.
  - Introduce `isStandbyMode` flag: on initial page load (when no search query `?q=` exists), show `#grammarStandbyState`, hide `#grammarGrid` and `#grammarPagination`.
  - When user clicks a JLPT level tab, domain tab, or types in search: transition out of standby, render cards grid and pagination.
  - Wire quick action chips to trigger respective JLPT level, domain filter, or search pattern.
  - Clear search / reset returns to Standby State.
- [x] Test manual interactions on `http://localhost:3000/grammar`.

---

### Task 3: Implement Standby State for `Tra cứu từ vựng` (`/vocabulary`)
**Files:**
- Modify: `frontend/pages/learning-support/index.html:90-120`
- Modify: `frontend/pages/learning-support/learning-support.js:15-150`

- [x] In `learning-support/index.html`:
  - Add `#vocabStandbyState` container with Capybara illustration, title "Sẵn sàng tra cứu Từ vựng JLPT & Chủ đề 🔤", description, and quick action chips (`🌱 Từ vựng N5`, `🌿 Từ vựng N4`, `☕ Giao tiếp hàng ngày`, `💼 Tiếng Nhật công sở`, `食べる (Ăn)`, `学校 (Trường học)`).
- [x] In `learning-support/js`:
  - Update `initLearnerShell` page title to `Tra cứu từ vựng tiếng Nhật 🔤`.
  - Introduce standby state on initial load: hide `#vocabGrid` and `#vocabPagination`, show `#vocabStandbyState`.
  - When user selects a topic pill, clicks a quick action chip, or inputs a search keyword: hide `#vocabStandbyState`, load and render vocabulary cards grid and pagination.
  - Wire clear search to restore Standby State when no topic is explicitly selected.
- [x] Test manual interactions on `http://localhost:3000/vocabulary`.

---

### Task 4: Verify & Polish `Tra cứu Hán tự & Bút thuận` (`/kanji-dictionary`)
**Files:**
- Modify: `frontend/pages/kanji-dictionary/kanji-dictionary.js:10-25`
- Modify: `frontend/pages/kanji-dictionary/index.html:20-50`

- [x] Ensure `initLearnerShell` page title is `Tra cứu Hán tự & Bút thuận ⛩️`.
- [x] Ensure quick action chips and Capybara standby styling match the unified standard across all 3 pages.
- [x] Verify search, stroke order drawing, and audio synthesis work smoothly without errors.

---

### Task 5: End-to-End Verification & Consistency Review
- [x] Verify all 3 pages load cleanly with zero console errors.
- [x] Verify switching between Standby State and Grid Results works flawlessly.
- [x] Verify standard pagination (`pagination.js`) functions properly once search results are loaded.
- [x] Confirm no regressions across other learner features (AI Writing, Reading, Flashcards).
