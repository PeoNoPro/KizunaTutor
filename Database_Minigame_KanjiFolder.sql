-- ====================================================================================================
-- DỰ ÁN: KIZUNA JAPANESE LEARNING & KANJI PRACTICE PLATFORM
-- TẬP TIN DDL MỞ RỘNG: QUẢN LÝ BỘ THẺ KANJI & MINIGAME "BẮN CHỮ KANJI" (KANJI RAIN TYPING)
-- MÔI TRƯỜNG THỰC THI: pgAdmin 4 / PostgreSQL 12+
-- ====================================================================================================

-- 1. BẢNG THƯ MỤC / BỘ THẺ KANJI CỦA NGƯỜI DÙNG
DROP TABLE IF EXISTS minigame_mistake_logs CASCADE;
DROP TABLE IF EXISTS minigame_sessions CASCADE;
DROP TABLE IF EXISTS kanji_folder_items CASCADE;
DROP TABLE IF EXISTS kanji_folders CASCADE;

CREATE TABLE kanji_folders (
    folder_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    folder_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(30) DEFAULT '#EF4444',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kanji_folders_user ON kanji_folders(user_id);
CREATE TRIGGER trg_kanji_folders_updated_at 
    BEFORE UPDATE ON kanji_folders 
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();

-- 2. BẢNG LIÊN KẾT CHỮ KANJI TRONG THƯ MỤC
CREATE TABLE kanji_folder_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folder_id UUID NOT NULL REFERENCES kanji_folders(folder_id) ON DELETE CASCADE ON UPDATE CASCADE,
    kanji_id UUID NOT NULL REFERENCES kanji_dictionary(kanji_id) ON DELETE CASCADE ON UPDATE CASCADE,
    order_index INTEGER NOT NULL DEFAULT 0,
    notes VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_folder_kanji UNIQUE (folder_id, kanji_id)
);

CREATE INDEX idx_folder_items_folder ON kanji_folder_items(folder_id);
CREATE INDEX idx_folder_items_kanji ON kanji_folder_items(kanji_id);

-- 3. BẢNG PHIÊN CHƠI MINIGAME & THỐNG KÊ ANALYTICS
CREATE TABLE minigame_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE,
    folder_id UUID REFERENCES kanji_folders(folder_id) ON DELETE SET NULL ON UPDATE CASCADE,
    folder_name VARCHAR(100),
    duration_seconds INTEGER NOT NULL,
    target_duration_seconds INTEGER,
    words_typed_count INTEGER NOT NULL,
    total_mistakes INTEGER NOT NULL DEFAULT 0,
    accuracy NUMERIC(5, 2) NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    is_won BOOLEAN NOT NULL,
    failed_kanji_id UUID REFERENCES kanji_dictionary(kanji_id) ON DELETE SET NULL ON UPDATE CASCADE,
    failed_kanji_character VARCHAR(10),
    played_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_minigame_sessions_user ON minigame_sessions(user_id);
CREATE INDEX idx_minigame_sessions_played ON minigame_sessions(played_at DESC);

-- 4. BẢNG GHI NHẬN TỪ BỊ GÕ SAI NHIỀU NHẤT TRONG PHIÊN
CREATE TABLE minigame_mistake_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES minigame_sessions(session_id) ON DELETE CASCADE ON UPDATE CASCADE,
    kanji_id UUID REFERENCES kanji_dictionary(kanji_id) ON DELETE SET NULL ON UPDATE CASCADE,
    kanji_character VARCHAR(10) NOT NULL,
    reading VARCHAR(50),
    mistake_count INTEGER NOT NULL DEFAULT 1,
    typed_input VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_minigame_mistakes_session ON minigame_mistake_logs(session_id);
