-- ====================================================================================================
-- DỰ ÁN: KIZUNA JAPANESE LEARNING PLATFORM
-- TẬP TIN DDL MỞ RỘNG: BẢNG CHỦ ĐỀ & TỪ VỰNG ĐỜI SỐNG (LEARNING SUPPORT TOPICS)
-- ====================================================================================================

-- 1. BẢNG CHỦ ĐỀ (TOPICS)
CREATE TABLE IF NOT EXISTS topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    title_vn VARCHAR(150) NOT NULL,
    title_jp VARCHAR(150),
    icon VARCHAR(20),
    description VARCHAR(500),
    color_token VARCHAR(50),
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_topics_code ON topics(code);
CREATE INDEX IF NOT EXISTS idx_topics_display_order ON topics(display_order);

-- 2. BẢNG TỪ VỰNG THEO CHỦ ĐỀ (TOPIC VOCABULARIES)
CREATE TABLE IF NOT EXISTS topic_vocabularies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE ON UPDATE CASCADE,
    word VARCHAR(100) NOT NULL,
    furigana VARCHAR(150),
    romaji VARCHAR(150),
    han_viet VARCHAR(150),
    meaning_vn VARCHAR(500) NOT NULL,
    jlpt_level VARCHAR(20),
    example_jp VARCHAR(500),
    example_vn VARCHAR(500),
    kanji_characters VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_topic_vocab_topic ON topic_vocabularies(topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_vocab_word ON topic_vocabularies(word);
