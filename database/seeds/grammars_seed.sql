-- =====================================================================
-- KizunaTutor - Grammars Seed Data
-- Nguon du lieu: Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx
-- TUYET DOI KHONG TAO BANG (Bang grammars da co san trong Database)
-- =====================================================================

BEGIN;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('1', 'N5', '〜は', 'wa', 'Thì; là; đóng vai trò làm chủ đề chính của câu', 'Danh từ + は', 'Viết là chữ は nhưng đọc là ''wa''. Dùng để đưa ra chủ đề đàm thoại của câu; thông tin quan trọng mới sẽ nằm ở phía sau.', '私は学生です。', '{私|わたし}は{学生|がくせい}です。', 'Tôi là một sinh viên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('2', 'N5', '〜が（subject）', 'ga (subject)', 'Chỉ chủ ngữ thực hiện hành động; nhấn mạnh thông tin mới', 'Danh từ + が', 'Từ để hỏi như だれ, 何 khi làm chủ ngữ bắt buộc phải đi với が, tuyệt đối không dùng は. Cũng đi với 好き, ほしい, 上手, わかる.', '雨が降っています。', '{雨|あめ}が{降|ふ}っています。', 'Trời đang mưa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('3', 'N5', '〜を', 'o', 'Chỉ tân ngữ trực tiếp chịu tác động của động từ; nơi chốn đi qua', 'Danh từ + を', 'Viết là を nhưng phát âm là ''o''. Đi với các động từ di chuyển như 歩く, 散歩する để chỉ nơi chốn đi xuyên qua.', '毎朝パンを食べます。', '{毎朝|まいあさ}パンを{食|た}べます。', 'Tôi ăn bánh mì vào mỗi buổi sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('4', 'N5', '〜に（time）', 'ni (time)', 'Vào lúc... (chỉ mốc thời gian cụ thể như giờ, ngày, tháng)', 'Danh từ + に', 'Đi với các mốc thời gian có con số cụ thể như ngày, tháng, giờ. Các từ chỉ thời gian tương đối như 今日, 明日, 毎日 thì không dùng に.', '毎朝7時に起きます。', '{毎朝|まいあさ}7{時|じ}に{起|お}きます。', 'Tôi thức dậy lúc bảy giờ mỗi sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('5', 'N5', '〜に（destination）', 'ni (destination)', 'Đến, tới... (chỉ đích đến cụ thể của hành động di chuyển)', 'Danh từ + に + 行く / 来る / 帰る', 'Nhấn mạnh vào điểm đến (nơi cập bến). Khác với へ nhấn mạnh vào phương hướng di chuyển.', '毎日バスで学校に行きます。', '{毎日|まいにち}バスで{学校|がっこう}に{行|い}きます。', 'Tôi đi học bằng xe buýt mỗi ngày.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('6', 'N5', '〜に（place of existence）', 'ni (location)', 'Ở, tại... (chỉ vị trí tồn tại của người hoặc vật với ある/いる)', 'Danh từ + に + ある / いる', 'Dùng với ある, いる và 住む. Đối với nơi xảy ra hành động (ăn, học, chơi), thay vào đó hãy sử dụng で.', '机の上に辞書があります。', '{机|つくえ}の{上|うえ}に{辞書|じしょ}があります。', 'Có một cuốn từ điển trên bàn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('7', 'N5', '〜に（indirect object）', 'ni (indirect object)', 'Cho, tới, với... (chỉ đối tượng tiếp nhận hành động như tặng, mượn)', 'Danh từ + に', 'Phổ biến với あげる, 貸す, 教える, 見せる, 電話をかける và 会う. Với もらう và 借りる, に đánh dấu người mà bạn nhận được thứ gì đó.', '母に手紙を書きます。', '{母|はは}に{手紙|てがみ}を{書|か}きます。', 'Tôi định viết thư cho mẹ tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('8', 'N5', '〜に（frequency）', 'ni (frequency)', 'Mỗi khoảng thời gian làm... lần (chỉ tần suất thực hiện)', 'Danh từ + に + Danh từ', 'Khoảng thời gian đứng trước, sau đó là に, sau đó là số lần: 月に一回 (mỗi tháng một lần).', '一週間に二回、プールで泳ぎます。', '{一週間|いっしゅうかん}に{二|に}{回|かい}、プールで{泳|およ}ぎます。', 'Tôi bơi ở hồ bơi hai lần một tuần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('9', 'N5', '〜へ', 'e', 'Hướng về, đi về phía... (nhấn mạnh phương hướng di chuyển)', 'Danh từ + へ', 'Viết là へ nhưng phát âm là ''e''. Thường có thể hoán đổi với に để chỉ điểm đến; へ nhấn mạnh về hướng hơn một chút.', '夏休みに北海道へ行きます。', '{夏休|なつやす}みに{北海道|ほっかいどう}へ{行|い}きます。', 'Tôi sẽ đến Hokkaido vào kỳ nghỉ hè.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('10', 'N5', '〜で（place of action）', 'de (place)', 'Tại, ở... (nơi diễn ra một hành động cụ thể)', 'Danh từ + で', 'Phân biệt: 教室にいます (tồn tại ở đâu dùng に) với 教室で勉強します (hành động diễn ra ở đâu dùng で).', '図書館で本を読みます。', '{図書館|としょかん}で{本|ほん}を{読|よ}みます。', 'Tôi đọc sách ở thư viện.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('11', 'N5', '〜で（means）', 'de (means)', 'Bằng, bằng cách... (chỉ phương tiện, công cụ hoặc ngôn ngữ)', 'Danh từ + で', 'Để nói ''đi bộ'', hãy sử dụng 歩いて行きます thay vì で.', '毎日電車で会社へ行きます。', '{毎日|まいにち}{電車|でんしゃ}で{会社|かいしゃ}へ{行|い}きます。', 'Tôi đi làm bằng tàu hỏa hàng ngày.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('12', 'N5', '〜と（and）', 'to (and)', 'Và... (nối tất cả các danh từ trong một danh sách đầy đủ)', 'Danh từ + と + Danh từ', 'と liệt kê mọi mục; để đưa ra một số ví dụ, hãy sử dụng や. Nó chỉ nối danh từ, không nối động từ hoặc tính từ.', 'かばんの中にペンとノートがあります。', 'かばんの{中|なか}にペンとノートがあります。', 'Trong túi của tôi có một cây bút và một cuốn sổ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('13', 'N5', '〜と（with）', 'to (with)', 'Cùng với... (chỉ người hoặc đối tượng cùng làm việc)', 'Danh từ + と', 'Thường theo sau là いっしょに. Với các động từ như 話す, 会う và 結婚する, と đánh dấu người khác.', '週末、友達と映画を見ました。', '{週末|しゅうまつ}、{友達|ともだち}と{映画|えいが}を{見|み}ました。', 'Tôi đã xem một bộ phim với một người bạn vào cuối tuần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('14', 'N5', '〜や', 'ya', 'Và, như là... (liệt kê một vài danh từ tiêu biểu mang tính đại diện)', 'Danh từ + や + Danh từ', 'Đề nghị danh sách chưa đầy đủ. Thường kết thúc bằng など: 本や雑誌など.', '机の上に本や雑誌があります。', '{机|つくえ}の{上|うえ}に{本|ほん}や{雑誌|ざっし}があります。', 'Trên bàn có sách, tạp chí và vân vân.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('15', 'N5', '〜か（or）', 'ka (or)', 'Hoặc là, hay là... (chỉ sự lựa chọn giữa các danh từ)', 'Danh từ + か + Danh từ', 'Có nghĩa là một trong các mục được chọn. Để liệt kê các mục cùng nhau, hãy sử dụng と hoặc や.', '飲み物はコーヒーか紅茶がいいです。', '{飲|の}み{物|もの}はコーヒーか{紅茶|こうちゃ}がいいです。', 'Đối với đồ uống, cà phê hoặc trà sẽ tốt.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('16', 'N5', '〜も', 'mo', 'Cũng, đến cả... (tương tự như đối tượng đã nhắc tới trước đó)', 'Danh từ + も', 'も thay thế は, が và を, nhưng đứng sau các trợ từ khác: 京都にも行きました.', '私も学生です。', '{私|わたし}も{学生|がくせい}です。', 'Tôi cũng là sinh viên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('17', 'N5', '〜の（possessive）', 'no', 'Của... (chỉ sở hữu, trực thuộc, chất liệu hoặc bổ nghĩa danh từ)', 'Danh từ + の + Danh từ', 'Danh từ đầu tiên mô tả danh từ thứ hai: 日本の車 (một chiếc xe hơi Nhật Bản). Danh từ thứ hai có thể lược bỏ khi rõ ràng: それは私のです.', 'これは私のかさです。', 'これは{私|わたし}のかさです。', 'Đây là chiếc ô của tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('18', 'N5', '〜から（from）', 'kara (from)', 'Từ... (chỉ điểm bắt đầu về thời gian hoặc địa điểm)', 'Danh từ + から', 'Thường kết hợp với まで: 月曜日から金曜日まで (từ thứ Hai đến thứ Sáu).', '授業は9時からです。', '{授業|じゅぎょう}は9{時|じ}からです。', 'Lớp học bắt đầu lúc chín giờ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('19', 'N5', '〜まで（until）', 'made', 'Đến, cho đến... (chỉ điểm kết thúc về thời gian hoặc địa điểm)', 'Danh từ + まで', 'Đánh dấu điểm kết thúc của một cái gì đó vẫn tiếp tục. Đối với thời hạn (''by''), hãy sử dụng までに (N4).', '昼休みは1時までです。', '{昼休|ひるやす}みは1{時|じ}までです。', 'Giờ nghỉ trưa kéo dài tới một giờ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('20', 'N5', '〜か（question）', 'ka (question)', 'Có phải không? (trợ từ kết thúc tạo câu hỏi nghi vấn)', 'Mệnh đề / Câu + か', 'Các câu hỏi lịch sự kết thúc bằng か và thường được viết bằng 。. Trong lời nói thông thường, か thường được bỏ đi và thay vào đó sử dụng ngữ điệu lên cao.', 'これはあなたのかばんですか。', NULL, 'Đây có phải là túi của bạn?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('21', 'N5', '〜ね', 'ne', 'Nhé, nhỉ? (tìm kiếm sự đồng tình hoặc chia sẻ cảm xúc)', 'Mệnh đề / Câu + ね', 'Được sử dụng khi người nói mong muốn người nghe chia sẻ cùng quan điểm hoặc kiến ​​thức. So sánh よ, cho người nghe biết điều gì đó mới mẻ.', '今日は暑いですね。', '{今日|きょう}は{暑|あつ}いですね。', 'Hôm nay trời nóng quá phải không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('22', 'N5', '〜よ', 'yo', 'Đấy, nhé! (cung cấp thông tin mới hoặc nhấn mạnh cho người nghe)', 'Mệnh đề / Câu + よ', 'Nói với người nghe điều gì đó mà họ có thể không biết. Việc lạm dụng nó với cấp trên nghe có vẻ tự đề cao.', 'このラーメンはとてもおいしいですよ。', NULL, 'Món ramen này thực sự ngon đấy, bạn biết đấy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('23', 'N5', '〜より', 'yori', 'Hơn so với... (dùng làm mốc so sánh hơn)', 'Danh từ + より', 'Trong X は Y より + tính từ, Y là tiêu chuẩn. Tính từ tiếng Nhật không có dạng ''-er''; より làm công việc đó.', '今日はきのうより寒いです。', '{今日|きょう}はきのうより{寒|さむ}いです。', 'Hôm nay lạnh hơn hôm qua.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('24', 'N5', '〜の方が〜より', 'no hou ga ... yori', 'X thì ... hơn so với Y (so sánh hơn giữa 2 đối tượng)', 'Danh từ + の方が + Danh từ + より', 'Hai phần có thể đến theo thứ tự. Thường dùng để trả lời câu hỏi どちら.', '犬の方が猫より好きです。', '{犬|いぬ}の{方|ほう}が{猫|ねこ}より{好|す}きです。', 'Tôi thích chó hơn mèo.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('25', 'N5', '〜と〜とどちらが', 'to ... to dochira ga', 'X và Y thì cái nào ... hơn? (câu hỏi lựa chọn giữa 2 đối tượng)', 'Danh từ + と + Danh từ + とどちらが', 'So sánh chính xác hai điều. どっち là dạng thông dụng của どちら. Trả lời bằng 〜の方が.', 'コーヒーと紅茶とどちらが好きですか。', 'コーヒーと{紅茶|こうちゃ}とどちらが{好|す}きですか。', 'Bạn thích cái nào hơn, cà phê hay trà?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('26', 'N5', '〜の中で〜が一番', 'no naka de ... ga ichiban', 'Trong số các ... thì ... là nhất (so sánh nhất trong một tập hợp)', 'Danh từ + の中で + Danh từ + が一番', 'Được sử dụng cho ba mặt hàng trở lên. Trong câu hỏi sử dụng 何, だれ, どこ hoặc いつ trước が一番.', '果物の中でりんごが一番好きです。', '{果物|くだもの}の{中|なか}でりんごが{一番|いちばん}{好|す}きです。', 'Trong tất cả các loại trái cây, tôi thích nhất là táo.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('27', 'N5', '〜です', 'desu', 'Là, thì... (trợ động từ kết thúc câu khẳng định lịch sự)', 'Danh từ + です / Tính từ đuôi な + です / Tính từ đuôi い + です', 'Các hình thức: です, ではありません, でした, ではありませんでした. Sau tính từ い, です chỉ thêm tính lịch sự: 高いです.', '私はマリアです。', '{私|わたし}はマリアです。', 'Tôi là Maria.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('28', 'N5', '〜だ', 'da', 'Là, thì... (thể thông thường thân mật của です)', 'Danh từ + だ / Tính từ đuôi な + だ', 'Các dạng: だ, じゃない, だった, じゃなかった. Không bao giờ dùng ngay sau tính từ い (高いだ là sai). Thường rơi vào các câu hỏi thông thường: 学生？', '今日は日曜日だ。', '{今日|きょう}は{日曜日|にちようび}だ。', 'Hôm nay là chủ nhật.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('29', 'N5', '〜ではありません / 〜じゃない', 'de wa arimasen / ja nai', 'Không phải là... (thể phủ định của danh từ và tính từ đuôi な)', 'Danh từ + ではありません / Tính từ đuôi な + ではありません / Danh từ + じゃない / Tính từ đuôi な + じゃない', 'じゃありません và じゃないです là những hình thức đàm thoại lịch sự hơn. Quá khứ: ではありませんでした / じゃなかった. Tính từ い dùng 〜くない thay thế.', '私は先生ではありません。学生です。', '{私|わたし}は{先生|せんせい}ではありません。{学生|がくせい}です。', 'Tôi không phải là giáo viên. Tôi là một sinh viên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('30', 'N5', '〜でした', 'deshita', 'Đã là... (thể quá khứ khẳng định lịch sự của です)', 'Danh từ + でした / Tính từ đuôi な + でした', 'Quá khứ phủ định: ではありませんでした. Tính từ い không dùng でした: nói 高かったです, không dùng 高いでした.', 'きのうは私の誕生日でした。', 'きのうは{私|わたし}の{誕生|たんじょう}{日|び}でした。', 'Hôm qua là sinh nhật của tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('31', 'N5', '〜ます', 'masu', 'Làm gì đó (thể khẳng định hiện tại / tương lai dạng lịch sự)', 'Động từ thể ます (bỏ ます) + ます', 'Bao gồm các thói quen và hành động trong tương lai. 飲む→飲みます, 食べる→食べます, する→します, 来る→来ます.', '毎朝コーヒーを飲みます。', '{毎朝|まいあさ}コーヒーを{飲|の}みます。', 'Tôi uống cà phê mỗi sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('32', 'N5', '〜ません', 'masen', 'Không làm gì đó (thể phủ định lịch sự của động từ)', 'Động từ thể ます (bỏ ます) + ません', 'Tương đương đơn giản là dạng ない: 飲みません = 飲まない.', '私はお酒を飲みません。', '{私|わたし}はお{酒|さけ}を{飲|の}みません。', 'Tôi không uống rượu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('33', 'N5', '〜ました', 'mashita', 'Đã làm gì đó (thể quá khứ khẳng định lịch sự của động từ)', 'Động từ thể ます (bỏ ます) + ました', 'Tương đương đơn giản là dạng た: 会いました = 会った.', 'きのう、友達に会いました。', 'きのう、{友達|ともだち}に{会|あ}いました。', 'Hôm qua tôi đã gặp một người bạn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('34', 'N5', '〜ませんでした', 'masen deshita', 'Đã không làm gì đó (thể quá khứ phủ định lịch sự của động từ)', 'Động từ thể ます (bỏ ます) + ませんでした', 'Tương đương đơn giản là 〜なかった: 食べませんでした = 食べなかった.', 'けさは朝ご飯を食べませんでした。', 'けさは{朝|あさ}ご{飯|はん}を{食|た}べませんでした。', 'Sáng nay tôi đã không ăn sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('35', 'N5', '〜ない', 'nai', 'Không làm gì đó (thể phủ định thông thường ngắn của động từ)', 'Động từ thể ない (bỏ ない) + ない', 'Động từ う đổi âm -u cuối thành -a: 書く→書かない, 買う→買わない. る-động từ thả る: 食べない. する→しない, 来る→こない, ある→ない. Quá khứ: 〜なかった.', '今日は学校へ行かない。', '{今日|きょう}は{学校|がっこう}へ{行|い}かない。', 'Hôm nay tôi không đi học.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('36', 'N5', '〜た', 'ta', 'Đã làm gì đó (thể quá khứ thông thường của động từ)', 'Động từ thể た (quá khứ)', 'Được làm chính xác như dạng て, với て→た và で→だ: 書いて→書いた, 飲んで→飲んだ, 行って→行った. Tương đương lịch sự: 〜ました.', '週末は家で映画を見た。', '{週末|しゅうまつ}は{家|いえ}で{映画|えいが}を{見|み}た。', 'Tôi đã xem một bộ phim ở nhà vào cuối tuần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('37', 'N5', '〜ましょう', 'mashou', 'Cùng nhau làm ... nhé! (lời rủ rê, đề xuất thực hiện hành động)', 'Động từ thể ます (bỏ ます) + ましょう', 'Được sử dụng khi người nói mong đợi người nghe đồng ý. Để mời nhẹ nhàng hơn, hãy sử dụng 〜ませんか.', 'いっしょに行きましょう。', 'いっしょに{行|い}きましょう。', 'Chúng ta hãy đi cùng nhau.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('38', 'N5', '〜ましょうか', 'mashou ka', 'Để tôi làm ... giúp nhé? / Chúng ta cùng làm ... nhé?', 'Động từ thể ます (bỏ ます) + ましょうか', 'Đề nghị làm điều gì đó cho người nghe (tôi có nên làm không?) hoặc yêu cầu họ cùng nhau quyết định điều gì đó (chúng ta có làm không?).', '窓を開けましょうか。', '{窓|まど}を{開|あ}けましょうか。', 'Tôi mở cửa sổ nhé?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('39', 'N5', '〜ませんか', 'masen ka', 'Bạn có muốn cùng làm ... không? (lời mời lịch sự, nhã nhặn)', 'Động từ thể ます (bỏ ます) + ませんか', 'Nhẹ nhàng hơn 〜ましょう vì nó khiến không gian người nghe bị suy giảm. Chấp nhận với ええ、〜ましょう.', 'いっしょに昼ご飯を食べませんか。', 'いっしょに{昼|ひる}ご{飯|はん}を{食|た}べませんか。', 'Bạn có muốn ăn trưa cùng nhau không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('40', 'N5', '〜たい', 'tai', 'Muốn làm gì đó (thể hiện mong muốn, nguyện vọng của bản thân)', 'Động từ thể ます (bỏ ます) + たい', 'Liên hợp như tính từ い: 〜たくない, 〜たかった. Đối tượng có thể lấy が hoặc を. Không dùng để nói trực tiếp mong muốn của người khác.', '夏休みに沖縄へ行きたいです。', '{夏休|なつやす}みに{沖縄|おきなわ}へ{行|い}きたいです。', 'Tôi muốn đến Okinawa vào kỳ nghỉ hè.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('41', 'N5', '〜がほしい', 'ga hoshii', 'Muốn có cái gì đó (thể hiện mong muốn sở hữu đồ vật)', 'Danh từ + がほしい', 'Đối với sự vật, không phải hành động (hành động sử dụng 〜たい). Liên hợp như tính từ い: ほしくない, ほしかった.', '新しいパソコンがほしいです。', '{新|あたら}しいパソコンがほしいです。', 'Tôi muốn một chiếc máy tính mới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('42', 'N5', '〜をください', 'o kudasai', 'Xin vui lòng cho tôi... (dùng khi mua hàng, gọi món)', 'Danh từ + をください', 'Được sử dụng khi mua sắm hoặc đặt hàng. Số lượng theo sau を không có tiểu từ: 切手を二枚ください.', 'すみません、水をください。', 'すみません、{水|みず}をください。', 'Xin lỗi, tôi có thể uống chút nước được không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('43', 'N5', '〜てください', 'te kudasai', 'Xin hãy, vui lòng làm... (yêu cầu, nhờ vả lịch sự)', 'Động từ thể て + ください', 'Dùng để nhờ vả, yêu cầu lịch sự với người cùng cấp hoặc người dưới. Không dùng với cấp trên hoặc đối tác quan trọng (thay bằng ていただけませんか).', 'もう少しゆっくり話してください。', 'もう{少|すこ}しゆっくり{話|はな}してください。', 'Xin hãy nói chậm lại một chút.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('44', 'N5', '〜ないでください', 'naide kudasai', 'Xin đừng làm... (yêu cầu, khuyên bảo không làm việc gì)', 'Động từ thể ない + でください', 'Nhẹ nhàng hơn 〜てはいけません. Trong cách nói thông thường ください được lược bỏ: 見ないで.', '教室で食べないでください。', '{教室|きょうしつ}で{食|た}べないでください。', 'Xin vui lòng không ăn trong lớp học.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('45', 'N5', '〜てもいい', 'te mo ii', 'Được phép làm gì đó (hỏi xin phép hoặc cho phép ai làm gì)', 'Động từ thể て + もいい', 'Thêm ですか để xin phép. Một lời từ chối lịch sự thường chỉ là すみません、ちょっと….', 'この辞書を使ってもいいですか。', 'この{辞書|じしょ}を{使|つか}ってもいいですか。', 'Tôi có thể sử dụng từ điển này được không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('46', 'N5', '〜てはいけない', 'te wa ikenai', 'Không được phép làm gì đó (cấm đoán, quy tắc bắt buộc)', 'Động từ thể て + はいけない', 'Một sự cấm đoán mạnh mẽ, thường dành cho các quy tắc. Hình thức lịch sự: 〜てはいけません. Co rút thông thường: 〜ちゃいけない / 〜じゃいけない.', '図書館で大きい声で話してはいけません。', '{図書館|としょかん}で{大|おお}きい{声|こえ}で{話|はな}してはいけません。', 'Bạn không được nói chuyện lớn tiếng trong thư viện.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('47', 'N5', '〜なければならない / 〜なくてはいけない', 'nakereba naranai / nakute wa ikenai', 'Bắt buộc phải làm gì đó (bổn phận, nghĩa vụ)', 'Động từ thể ない (bỏ ない) + なければならない / Động từ thể ない (bỏ ない) + なくてはいけない', 'Các hình thức lịch sự kết thúc bằng なりません / いけません. Cả hai đều có nghĩa giống nhau; なければならない nghe có vẻ trang trọng hơn một chút.', '明日は6時に起きなければなりません。', '{明日|あした}は6{時|じ}に{起|お}きなければなりません。', 'Ngày mai tôi phải thức dậy lúc sáu giờ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('48', 'N5', '〜なくてもいい', 'nakute mo ii', 'Không cần phải làm gì cũng được (không bắt buộc)', 'Động từ thể ない (bỏ ない) + なくてもいい', 'Ngược lại với 〜なければならない. Hỏi với 〜なくてもいいですか (Tôi không hỏi thì có được không?).', '明日は学校へ来なくてもいいです。', '{明日|あした}は{学校|がっこう}へ{来|こ}なくてもいいです。', 'Ngày mai bạn không cần phải đến trường.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('49', 'N5', '〜ている', 'te iru', 'Đang làm... (hành động tiếp diễn); Đang ở trạng thái... (kết quả)', 'Động từ thể て + いる', 'Với động từ hành động (食べる, 読む) nó thể hiện một hành động đang diễn ra; với động từ thay đổi (結婚する, 開く, 知る) nó hiển thị trạng thái kết quả. Lịch sự: 〜ています.', '弟は今、部屋でゲームをしています。', '{弟|おとうと}は{今|いま}、{部屋|へや}でゲームをしています。', 'Em trai tôi hiện đang chơi game trong phòng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('50', 'N5', '〜て', 'te', 'Và, rồi thì... (nối các động từ, tính từ theo trình tự thời gian)', 'Động từ thể て + Mệnh đề / Câu', 'Động từ cuối cùng thiết lập thì cho cả câu. 書く→書いて, 飲む→飲んで, 話す→話して, 買う→買って, 行く→行って (không đều).', '朝起きて、顔を洗います。', '{朝|あさ}{起|お}きて、{顔|かお}を{洗|あら}います。', 'Tôi thức dậy vào buổi sáng và rửa mặt.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('51', 'N5', '〜てから', 'te kara', 'Sau khi làm việc này xong thì làm tiếp việc khác', 'Động từ thể て + から', 'Nhấn mạnh rằng A kết thúc trước khi B bắt đầu, rõ ràng hơn so với て đơn giản. Với ている trong mệnh đề chính nó thường có nghĩa là ''kể từ''.', '手を洗ってから、ご飯を食べましょう。', '{手|て}を{洗|あら}ってから、ご{飯|はん}を{食|た}べましょう。', 'Chúng ta rửa tay rồi ăn nhé.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('52', 'N5', '〜たり〜たりする', 'tari ... tari suru', 'Nào là làm..., nào là làm... (liệt kê hành động tiêu biểu)', 'Động từ thể た (quá khứ) + り + Động từ thể た (quá khứ) + りする', 'Chọn ra một vài hành động từ nhiều hành động. Căng thẳng và lịch sự diễn ra ở phần cuối cùng する: 〜たりしました.', '日曜日はそうじをしたり、洗濯をしたりします。', '{日曜日|にちようび}はそうじをしたり、{洗濯|せんたく}をしたりします。', 'Vào Chủ nhật, tôi làm những việc như dọn dẹp và giặt giũ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('53', 'N5', '〜たことがある', 'ta koto ga aru', 'Đã từng làm việc gì đó (chỉ kinh nghiệm, trải nghiệm quá khứ)', 'Động từ thể た (quá khứ) + ことがある', 'Nói về kinh nghiệm sống chứ không phải việc vừa làm: きのう食べたことがある là sai. Phủ định 〜たことがありません có nghĩa là ''chưa bao giờ''.', '富士山に登ったことがあります。', '{富士山|ふじさん}に{登|のぼ}ったことがあります。', 'Tôi đã leo núi Phú Sĩ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('54', 'N5', '〜前に', 'mae ni', 'Trước khi làm việc gì đó...', 'Động từ thể từ điển (辞書形) + 前に / Danh từ + の + 前に', 'Động từ trước 前に luôn ở dạng từ điển, ngay cả khi nói về quá khứ. Với khoảng thời gian dài không có の: 3年前に (ba năm trước).', '寝る前に歯をみがきます。', '{寝|ね}る{前|まえ}に{歯|は}をみがきます。', 'Tôi đánh răng trước khi đi ngủ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('55', 'N5', '〜後で', 'ato de', 'Sau khi làm việc gì đó...', 'Động từ thể た (quá khứ) + 後で / Danh từ + の + 後で', 'Động từ trước 後で luôn ở dạng た, ngay cả đối với những kế hoạch trong tương lai. Bản thân 後で có nghĩa là ''sau''.', '晩ご飯を食べた後で、散歩をします。', '{晩|ばん}ご{飯|はん}を{食|た}べた{後|あと}で、{散歩|さんぽ}をします。', 'Tôi đi dạo sau khi ăn tối.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('56', 'N5', '〜ながら（while）', 'nagara', 'Vừa làm việc này vừa làm việc kia (hai việc diễn ra song song)', 'Động từ thể ます (bỏ ます) + ながら', 'Cả hai hành động đều được thực hiện bởi cùng một người. Hành động chính diễn ra sau cùng: trong A-ながら B, B là hành động chính.', '音楽を聞きながら勉強します。', '{音楽|おんがく}を{聞|き}きながら{勉強|べんきょう}します。', 'Tôi vừa học vừa nghe nhạc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('57', 'N5', '〜に行く', 'ni iku', 'Đi đến đâu để làm gì đó (chỉ mục đích của hành động di chuyển)', 'Động từ thể ます (bỏ ます) + に行く / Danh từ + に行く', '来る và 帰る hoạt động giống nhau: 遊びに来る. Với danh từ する, danh từ có thể đi trực tiếp: 買い物に行く, 散歩に行く.', 'デパートへ服を買いに行きます。', 'デパートへ{服|ふく}を{買|か}いに{行|い}きます。', 'Tôi đang đi đến cửa hàng bách hóa để mua quần áo.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('58', 'N5', '〜が好き / 〜が嫌い', 'ga suki / ga kirai', 'Thích / Ghét đối tượng nào đó', 'Danh từ + が好き / Danh từ + が嫌い / Động từ thể từ điển (辞書形) + の + が好き', '好き và 嫌い là tính từ な, nên từ thích dùng が, không phải を. 大好き và 大嫌い mạnh hơn. 嫌い nghe có vẻ thẳng thừng; あまり好きじゃない thì nhẹ nhàng hơn.', '私は甘いものが好きです。', '{私|わたし}は{甘|あま}いものが{好|す}きです。', 'Tôi thích những thứ ngọt ngào.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('59', 'N5', '〜が上手 / 〜が下手', 'ga jouzu / ga heta', 'Giỏi / Kém trong lĩnh vực hoặc kỹ năng nào đó', 'Danh từ + が上手 / Danh từ + が下手', 'Chủ yếu được sử dụng về người khác; tự gọi mình là 上手 nghe có vẻ giống như khoe khoang. Cả hai đều là tính từ な: 料理が上手な人.', '姉はピアノが上手です。', '{姉|あね}はピアノが{上手|じょうず}です。', 'Chị gái tôi chơi piano giỏi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('60', 'N5', '〜がある / 〜がいる', 'ga aru / ga iru', 'Có người / đồ vật ở đâu đó (tồn tại)', 'Danh từ + がある / Danh từ + がいる', 'ある dùng để chỉ đồ vật, thực vật và sự kiện; いる dành cho người và động vật. Phủ định: ない / いない (lịch sự: ありません / いません).', '公園に大きい木があります。', '{公園|こうえん}に{大|おお}きい{木|き}があります。', 'Có một cái cây lớn trong công viên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('61', 'N5', '〜から（reason）', 'kara (reason)', 'Bởi vì, do... (chỉ nguyên nhân mang tính chủ quan của người nói)', 'Mệnh đề / Câu + から', 'Lý do đến trước: A から B (A, vậy B). Trả lời どうして bằng 〜からです. Danh từ và tính từ な cần だ: 雨だから.', '今日は寒いから、コートを着ます。', '{今日|きょう}は{寒|さむ}いから、コートを{着|き}ます。', 'Hôm nay trời lạnh nên tôi sẽ mặc áo khoác.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('62', 'N5', '〜が（but）', 'ga (but)', 'Nhưng, tuy nhiên... (nối hai vế tương phản hoặc mở đầu câu nói)', 'Mệnh đề / Câu + が', 'Cũng được sử dụng chỉ để làm dịu đi một yêu cầu hoặc dẫn dắt vào một chủ đề: すみませんが…. Trang trọng hơn một chút so với けど (N4).', 'この店は安いですが、あまりおいしくないです。', 'この{店|みせ}は{安|やす}いですが、あまりおいしくないです。', 'Nhà hàng này rẻ nhưng đồ ăn không ngon lắm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('63', 'N5', '〜でしょう', 'deshou', 'Có lẽ là... đúng không? (suy đoán hoặc tìm kiếm sự đồng thuận lịch sự)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + でしょう', 'Điển hình của dự báo thời tiết. Nói với ngữ điệu lên cao, nó yêu cầu người nghe đồng ý. Dạng thông thường: だろう (N4).', '明日は雨が降るでしょう。', '{明日|あした}は{雨|あめ}が{降|ふ}るでしょう。', 'Có lẽ ngày mai trời sẽ mưa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('64', 'N5', '〜くなる / 〜になる', 'ku naru / ni naru', 'Trở nên, biến đổi thành... (sự biến đổi về trạng thái)', 'Tính từ đuôi い-ku + なる / Tính từ đuôi な + になる / Danh từ + になる', 'いい trở thành よくなる. Thường được dùng trong quá khứ để mô tả một sự thay đổi đã xảy ra: 大きくなりました.', 'あたたかくなりましたね。', NULL, 'Trời đã ấm hơn rồi phải không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('65', 'N5', '〜にする（decide）', 'ni suru', 'Chọn cái này, quyết định chọn cái này (dùng khi gọi món, lựa chọn)', 'Danh từ + にする', 'Phổ biến khi đặt món ăn hoặc chọn từ các tùy chọn. Quá khứ: 〜にしました (Tôi đã quyết định về...).', '私はカレーにします。', '{私|わたし}はカレーにします。', 'Tôi sẽ ăn cà ri.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('66', 'N5', 'まだ〜ていない', 'mada ... te inai', 'Vẫn chưa làm việc gì đó', 'まだ + Động từ thể て + いない / まだ + Động từ thể て + いません', 'Sử dụng ていない, không phải ませんでした, cho một điều gì đó vẫn có thể xảy ra. Câu trả lời ngắn gọn là いいえ、まだです.', 'まだ昼ご飯を食べていません。', 'まだ{昼|ひる}ご{飯|はん}を{食|た}べていません。', 'Tôi vẫn chưa ăn trưa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('67', 'N5', 'もう〜た', 'mou ... ta', 'Đã làm xong việc gì đó rồi', 'もう + Động từ thể た (quá khứ) / もう + Động từ thể ます (bỏ ます) + ました', 'Trong câu hỏi もう có nghĩa là ''chưa''. Với động từ phủ định, もう có nghĩa là ''không còn nữa'': もう食べません.', '「もう晩ご飯を食べましたか。」「はい、もう食べました。」', '「もう{晩|ばん}ご{飯|はん}を{食|た}べましたか。」「はい、もう{食|た}べました。」', '“Anh đã ăn tối chưa?” "Ừ, tôi đã có rồi."')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('68', 'N5', 'あまり〜ない', 'amari ... nai', 'Không... lắm (đi với thể phủ định biểu thị mức độ thấp)', 'あまり + Động từ thể ない / あまり + Tính từ đuôi い-ku + ない / あまり + Tính từ đuôi な + じゃない', 'Luôn theo sau là dạng phủ định. あまり好きじゃない là một cách nhẹ nhàng hơn để nói rằng bạn không thích điều gì đó.', '私はあまりテレビを見ません。', '{私|わたし}はあまりテレビを{見|み}ません。', 'Tôi không xem TV nhiều.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('69', 'N5', '全然〜ない', 'zenzen ... nai', 'Hoàn toàn không... chút nào (phủ định tuyệt đối)', '全然 + Động từ thể ない / 全然 + Tính từ đuôi い-ku + ない / 全然 + Tính từ đuôi な + じゃない', 'Mạnh hơn あまり〜ない. Trong lời nói thông thường, 全然 đôi khi được sử dụng với những từ tích cực (全然大丈夫), nhưng với từ phủ định là cách sử dụng tiêu chuẩn.', 'この問題は全然わかりません。', 'この{問題|もんだい}は{全然|ぜんぜん}わかりません。', 'Tôi không hiểu câu hỏi này chút nào.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('70', 'N5', '〜だけ', 'dake', 'Chỉ, duy nhất chỉ có... (không có thêm cái khác)', 'Danh từ + だけ / Động từ thể từ điển (辞書形) + だけ', 'Giọng điệu trung tính. So sánh しか〜ない (N4), nhấn mạnh rằng số lượng là nhỏ.', '財布に100円だけあります。', '{財布|さいふ}に100{円|えん}だけあります。', 'Tôi chỉ có 100 yên trong ví.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('71', 'N5', '〜くらい / 〜ぐらい（approximately）', 'kurai / gurai', 'Khoảng chừng, ước lượng mức độ...', 'Danh từ + くらい / Danh từ + ぐらい', 'Được sử dụng với số lượng và thời lượng. Đối với một thời điểm (khoảng ba giờ), hãy sử dụng ごろ: 3時ごろ.', '駅まで歩いて10分ぐらいです。', '{駅|えき}まで{歩|ある}いて10{分|ぷん}ぐらいです。', 'Mất khoảng mười phút đi bộ đến ga.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('72', 'N5', '〜ごろ', 'goro', 'Vào khoảng thời gian... (chỉ mốc thời điểm ước chừng)', 'Danh từ + ごろ', 'Gắn vào đồng hồ thời gian, ngày tháng. に sau ごろ là tùy chọn: 7時ごろ(に)起きます.', '毎晩11時ごろ寝ます。', '{毎晩|まいばん}11{時|じ}ごろ{寝|ね}ます。', 'Tôi đi ngủ vào khoảng mười một giờ mỗi đêm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('73', 'N5', '何も〜ない / 誰も〜ない', 'nani mo ... nai / dare mo ... nai', 'Không có gì cả / Không có ai cả (phủ định toàn bộ)', '何も + Động từ thể ない / 誰も + Động từ thể ない', 'Với địa điểm, trợ từ đứng trước も: どこにも / どこへも行きません (không đi đến đâu). 誰 thường được viết bằng hiragana: だれ.', 'けさは何も食べませんでした。', 'けさは{何|なに}も{食|た}べませんでした。', 'Sáng nay tôi chưa ăn gì cả.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('74', 'N5', '何か / 誰か / どこか', 'nani ka / dare ka / doko ka', 'Cái gì đó / Ai đó / Nơi nào đó (bất định)', 'Từ để hỏi + か', 'Từ để hỏi + か cho từ ''some-''; từ để hỏi + も + phủ định cho từ ''không-''. が và を sau 何か / だれか thường bị lược bỏ.', '何か飲みますか。', '{何|なに}か{飲|の}みますか。', 'Bạn có muốn uống gì không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('75', 'N5', 'これ / それ / あれ / どれ', 'kore / sore / are / dore', 'Cái này / Cái đó / Cái kia / Cái nào (đại từ chỉ định đồ vật)', 'これ / それ / あれ / どれ', 'こ = ở gần người nói, そ = ở gần người nghe, あ = ở xa cả hai, ど = câu hỏi. Họ đứng một mình; làm chủ ngữ どれ lấy が, không phải は.', 'これは私の辞書です。', 'これは{私|わたし}の{辞書|じしょ}です。', 'Đây là từ điển của tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('76', 'N5', 'この / その / あの / どの', 'kono / sono / ano / dono', 'Cái... này / Cái... đó / Cái... kia / Cái... nào (từ bổ nghĩa danh từ)', 'この / その / あの / どの + Danh từ', 'Luôn theo sau bởi một danh từ: この本, không bao giờ この đứng một mình. これ / それ / あれ / どれ được sử dụng khi không có danh từ.', 'このケーキはおいしいです。', NULL, 'Bánh này ngon lắm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('77', 'N5', 'ここ / そこ / あそこ / どこ', 'koko / soko / asoko / doko', 'Ở đây / Ở đó / Ở kia / Ở đâu (chỉ vị trí, nơi chốn)', 'ここ / そこ / あそこ / どこ', 'こちら / そちら / あちら / どちら là phiên bản lịch sự hơn: お国はどちらですか (Bạn đến từ đâu?).', 'ここは私の学校です。', 'ここは{私|わたし}の{学校|がっこう}です。', 'Đây là trường học của tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('78', 'N5', '〜くない / 〜かった / 〜くなかった', 'ku nai / katta / ku nakatta', 'Cách chia phủ định và quá khứ của tính từ đuôi い', 'Tính từ đuôi い stem + くない / Tính từ đuôi い stem + かった / Tính từ đuôi い stem + くなかった', '高い→高くない, 高かった, 高くなかった. Thêm です để lịch sự. いい là không đều: よくない, よかった, よくなかった.', 'この部屋はあまり広くないです。', 'この{部屋|へや}はあまり{広|ひろ}くないです。', 'Căn phòng này không lớn lắm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('79', 'N5', '〜な（na-adjective）', 'na (na-adjective)', 'Cách dùng tính từ đuôi な bổ nghĩa cho danh từ', 'Tính từ đuôi な + な + Danh từ / Tính từ đuôi な + です / Tính từ đuôi な + ではありません / Tính từ đuôi な + でした', 'Trước danh từ, thêm な: 静かな部屋. Ở cuối câu chúng thay đổi như danh từ. きれい và きらい kết thúc bằng い nhưng là tính từ な.', '京都は有名な町です。', '{京都|きょうと}は{有名|ゆうめい}な{町|まち}です。', 'Kyoto là một thành phố nổi tiếng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('80', 'N5', '〜くて / 〜で', 'kute / de', 'Và, vì... (cách nối tính từ đuôi い và tính từ đuôi な)', 'Tính từ đuôi い-ku + て / Tính từ đuôi な + で / Danh từ + で', 'いい trở thành よくて. Kết hợp những phẩm chất chỉ ra cùng một hướng (cả tốt hoặc cả xấu); để tương phản, hãy sử dụng が thay thế.', 'この店は安くておいしいです。', 'この{店|みせ}は{安|やす}くておいしいです。', 'Quán này ngon và rẻ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('81', 'N5', '〜という（named）', 'to iu (named)', 'Có tên gọi là, được gọi là... (giới thiệu tên người, vật, quán ăn)', 'Danh từ + という + Danh từ', 'Giới thiệu một cái tên mà người nghe có thể không biết.', 'これは「すきやき」という料理です。', 'これは「すきやき」という{料理|りょうり}です。', 'Đây là món ăn được gọi là sukiyaki.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('82', 'N4', 'あげる / くれる / もらう', 'ageru / kureru / morau', 'Cho, tặng / Được cho, nhận (hành vi cho nhận ơn huệ)', 'Danh từ (receiver) + に + Danh từ + を + あげる / Danh từ (giver) + が + Danh từ + を + くれる / Danh từ (giver) + に + Danh từ + を + もらう', 'くれる chỉ được sử dụng khi người nhận là người nói hoặc ai đó trong nhóm người nói. Với もらう người tặng có thể được đánh dấu bằng に hoặc から.', '私は弟に古いゲームをあげた。', '{私|わたし}は{弟|おとうと}に{古|ふる}いゲームをあげた。', 'Tôi đã đưa trò chơi cũ của mình cho em trai tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('83', 'N4', '〜てあげる', 'te ageru', 'Làm việc gì đó giúp cho người khác', 'Động từ thể て + あげる', 'Nói thẳng với cấp trên nghe có vẻ trịch thượng; thay vào đó hãy sử dụng お〜しましょうか (お持ちしましょうか) khiêm tốn.', '妹の宿題を手伝ってあげました。', '{妹|いもうと}の{宿題|しゅくだい}を{手伝|てつだ}ってあげました。', 'Tôi đã giúp em gái tôi làm bài tập về nhà.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('84', 'N4', '〜てくれる', 'te kureru', 'Được ai đó làm việc gì giúp cho mình hoặc người thân', 'Động từ thể て + くれる', 'Chủ ngữ là người làm việc giúp đỡ; người được lợi là người nói hoặc người thân thiết với họ.', '友達が引っ越しを手伝ってくれた。', '{友達|ともだち}が{引|ひ}っ{越|こ}しを{手伝|てつだ}ってくれた。', 'Một người bạn đã giúp tôi di chuyển.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('85', 'N4', '〜てもらう', 'te morau', 'Nhận được sự giúp đỡ từ ai đó (mình chủ động nhờ vả)', 'Động từ thể て + もらう', 'Người nhận sự giúp đỡ là chủ ngữ và người thực hiện hành động được đánh dấu bằng に.', '先輩にレポートを見てもらった。', '{先輩|せんぱい}にレポートを{見|み}てもらった。', 'Tôi đã nhờ một sinh viên năm cuối xem qua báo cáo của tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('86', 'N4', '〜てくださる / 〜ていただく', 'te kudasaru / te itadaku', 'Thể kính ngữ lịch sự của てくれる và てもらう', 'Động từ thể て + くださる / Động từ thể て + いただく', 'くださる là phiên bản kính ngữ của くれる (ます-dạng: くださいます); いただく là phiên bản khiêm tốn của もらう.', '先生が私の作文を直してくださいました。', '{先生|せんせい}が{私|わたし}の{作文|さくぶん}を{直|なお}してくださいました。', 'Giáo viên của tôi đã vui lòng sửa bài luận của tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('87', 'N4', '〜ていく', 'te iku', 'Làm gì rồi đi tiếp; Hành động tiếp tục tiến triển về tương lai', 'Động từ thể て + いく', 'Chuyển động hoặc thay đổi khỏi người nói, hoặc từ hiện tại hướng tới tương lai. So sánh 〜てくる.', '雨が降りそうだから、傘を持っていきます。', '{雨|あめ}が{降|ふ}りそうだから、{傘|かさ}を{持|も}っていきます。', 'Có vẻ như trời đang mưa nên tôi sẽ mang theo ô.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('88', 'N4', '〜てくる', 'te kuru', 'Làm gì rồi quay lại; Sự việc bắt đầu xuất hiện hoặc kéo dài tới nay', 'Động từ thể て + くる', 'Sự chuyển động hoặc thay đổi hướng về phía người nói, hoặc từ quá khứ đến hiện tại. 行ってきます nghĩa đen là ''Tôi sẽ đi và quay lại.''', 'ちょっとコンビニに行ってきます。', 'ちょっとコンビニに{行|い}ってきます。', 'Tôi vừa mới đi ra cửa hàng tiện lợi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('89', 'N4', '〜てしまう / 〜ちゃう', 'te shimau / chau', 'Lỡ làm việc gì đó (hối tiếc); Đã hoàn thành dứt điểm toàn bộ', 'Động từ thể て + しまう', 'Trong lời nói, 〜てしまう hợp âm thành 〜ちゃう, và 〜でしまう thành 〜じゃう (飲んじゃう).', '電車に傘を忘れてしまいました。', '{電車|でんしゃ}に{傘|かさ}を{忘|わす}れてしまいました。', 'Tôi để quên chiếc ô của mình trên tàu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('90', 'N4', '〜ておく', 'te oku', 'Làm sẵn, chuẩn bị sẵn việc gì đó trước để phục vụ cho sau này', 'Động từ thể て + おく', 'Trong cách nói thông thường 〜ておく thường trở thành 〜とく (買っとく), và 〜でおく trở thành 〜どく (読んどく).', '旅行の前に、ホテルを予約しておきます。', '{旅行|りょこう}の{前|まえ}に、ホテルを{予約|よやく}しておきます。', 'Tôi sẽ đặt khách sạn trước chuyến đi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('91', 'N4', '〜てみる', 'te miru', 'Thử làm việc gì đó xem sao (trải nghiệm thử)', 'Động từ thể て + みる', 'Có nghĩa là thử làm việc gì đó để xem kết quả, không nỗ lực làm việc gì khó khăn.', 'このシャツ、着てみてもいいですか。', 'このシャツ、{着|き}てみてもいいですか。', 'Tôi có thể thử chiếc áo này được không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('92', 'N4', '〜てある', 'te aru', 'Đang được làm sẵn, ở trạng thái có chủ đích của con người', 'Động từ thể て + ある', 'Được sử dụng với động từ chuyển tiếp và tân ngữ thường dùng が. Nó ngụ ý ai đó đã cố tình làm điều đó, không giống như 〜ている với nội động từ (窓が開いている).', '入り口に地図がはってあります。', '{入|い}り{口|ぐち}に{地図|ちず}がはってあります。', 'Có một tấm bản đồ được dán ở lối vào.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('93', 'N4', '〜とき', 'toki', 'Khi, lúc đang làm việc gì đó hoặc ở vào trạng thái nào đó', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + とき', 'Thì của động từ thể hiện thứ tự: 行くとき nghĩa là trước hoặc trên đường (việc đi vẫn chưa kết thúc); 行ったとき có nghĩa là sau khi đến nơi. Thường được viết 時 bằng chữ Hán.', '子どものとき、よく川で泳ぎました。', '{子|こ}どものとき、よく{川|かわ}で{泳|およ}ぎました。', 'Khi tôi còn nhỏ, tôi thường bơi ở sông.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('94', 'N4', '〜間 / 〜間に', 'aida / aida ni', 'Trong suốt khoảng thời gian... / Trong khi đang... thì có việc khác chen vào', 'Động từ thể て + いる / Động từ thể từ điển (辞書形) / Danh từ + の + 間（に）', '間 có nghĩa là hành động tiếp tục trong suốt thời gian; 間に có nghĩa là nó xảy ra vào một thời điểm nào đó trong một khoảng thời gian.', '夏休みの間、祖父の家にいました。', '{夏休|なつやす}みの{間|あいだ}、{祖父|そふ}の{家|いえ}にいました。', 'Tôi ở nhà ông nội trong kỳ nghỉ hè.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('95', 'N4', '〜そうだ（appearance）', 'sou da (appearance)', 'Trông có vẻ..., sắp sửa... (phán đoán dựa trên vẻ ngoài nhìn thấy)', 'Động từ thể ます (bỏ ます) / Tính từ đuôi い stem / Tính từ đuôi な + そうだ', 'Một sự phán xét từ những gì bạn nhìn thấy. いい → よさそう, ない → なさそう. Không dùng với tính từ chỉ những đặc điểm rõ ràng (赤い, かわいい); かわいそう có nghĩa là ''đáng thương''.', 'そのカレー、辛そうだね。', 'そのカレー、{辛|から}そうだね。', 'Món cà ri đó có vẻ cay.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('96', 'N4', '〜そうだ（hearsay）', 'sou da (hearsay)', 'Nghe nói là... (truyền đạt lại thông tin nghe từ nguồn khác)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + そうだ / Tính từ đuôi な + だ + そうだ / Danh từ + だ + そうだ', 'Gắn vào các dạng đơn giản hoàn toàn, không giống như ''trông giống'' そうだ, gắn vào thân cây. Nó không có hình thức quá khứ hoặc tiêu cực.', '天気予報では、明日は雪が降るそうです。', '{天気|てんき}{予報|よほう}では、{明日|あした}は{雪|ゆき}が{降|ふ}るそうです。', 'Dự báo thời tiết nói ngày mai sẽ có tuyết.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('97', 'N4', '〜らしい（hearsay/conjecture）', 'rashii', 'Nghe nói là, có vẻ như...; Đậm chất, ra dáng...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + らしい', 'Dựa vào nội dung đã nghe hoặc đọc nên loa phát ra âm thanh hơi tách biệt. ''Điển hình'' らしい (男らしい) là một điểm N3 riêng biệt.', '駅の近くに新しいカフェができたらしい。', '{駅|えき}の{近|ちか}くに{新|あたら}しいカフェができたらしい。', 'Có vẻ như một quán cà phê mới đã mở gần nhà ga.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('98', 'N4', '〜ようだ', 'you da', 'Dường như là, hình như là... (suy đoán dựa trên cảm nhận giác quan)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + ようだ / Tính từ đuôi な + な + ようだ / Danh từ + の + ようだ', 'Một cách hơi trang trọng để đưa ra đánh giá dựa trên những gì bạn quan sát hoặc cảm nhận; みたいだ là từ tương đương thông thường. Việc sử dụng ''like, as if'' được đề cập ở N3.', '誰もいないようですね。', '{誰|だれ}もいないようですね。', 'Có vẻ như không có ai ở đây.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('99', 'N4', '〜みたい', 'mitai', 'Giống như là, có vẻ như... (văn nói thân mật của ようだ)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + みたい', 'Thông thường tương đương với ようだ. Nó gắn trực tiếp vào danh từ và tính từ na (静かみたい) mà không cần な hoặc の. Trước danh từ nó trở thành みたいな.', '隣の部屋に誰か来たみたい。', '{隣|となり}の{部屋|へや}に{誰|だれ}か{来|き}たみたい。', 'Hình như có người sang phòng bên cạnh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('100', 'N4', '〜すぎる', 'sugiru', 'Làm gì đó quá mức, quá đà (thường mang sắc thái tiêu cực)', 'Động từ thể ます (bỏ ます) / Tính từ đuôi い stem / Tính từ đuôi な + すぎる', 'Liên hợp như động từ Nhóm 2 (すぎた, すぎて). いい → よすぎる và tính từ ない → なさすぎる.', 'ゆうべ飲みすぎて、頭が痛い。', 'ゆうべ{飲|の}みすぎて、{頭|あたま}が{痛|いた}い。', 'Đêm qua tôi đã uống quá nhiều và bây giờ tôi bị đau đầu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('101', 'N4', '〜方', 'kata', 'Cách làm, phương pháp làm việc gì đó (cách đọc, cách viết, cách đi)', 'Động từ thể ます (bỏ ます) + 方', 'Kết quả là một danh từ nên tân ngữ lấy の thay vì を (日本語の話し方). Với động từ する: 勉強のしかた.', 'この機械の使い方を教えてください。', 'この{機械|きかい}の{使|つか}い{方|かた}を{教|おし}えてください。', 'Xin chỉ cho tôi cách sử dụng chiếc máy này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('102', 'N4', '〜中', 'chuu / juu', 'Đang trong lúc làm... / Trong suốt cả một khoảng thời gian, không gian', 'Danh từ + 中', 'ちゅう đánh dấu một hoạt động đang diễn ra hoặc một khoảng thời gian (会議中, 午前中, 今週中); じゅう có nghĩa là ''xuyên suốt'' một thời gian hoặc địa điểm (一日中, 世界中).', '父は今、電話中です。', '{父|ちち}は{今|いま}、{電話|でんわ}{中|ちゅう}です。', 'Bố tôi hiện đang nghe điện thoại.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('103', 'N4', '〜つもり', 'tsumori', 'Dự định làm gì đó (kế hoạch, ý định của bản thân người nói)', 'Động từ thể từ điển (辞書形) / Động từ thể ない + つもり', 'Nêu rõ ý định cá nhân chắc chắn. 〜つもりはない có nghĩa là ''không có ý định''. Hỏi cấp trên 〜つもりですか nghe có vẻ thẳng thừng.', '夏休みは北海道を旅行するつもりです。', '{夏休|なつやす}みは{北海道|ほっかいどう}を{旅行|りょこう}するつもりです。', 'Tôi dự định đi du lịch vòng quanh Hokkaido trong kỳ nghỉ hè.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('104', 'N4', '〜予定', 'yotei', 'Theo lịch trình, dự kiến sẽ làm gì đó (kế hoạch đã định sẵn)', 'Động từ thể từ điển (辞書形) + 予定 / Danh từ + の + 予定', 'Đề cập đến một lịch trình hoặc sự sắp xếp cố định, thường có sự tham gia của người khác; khách quan hơn つもり.', '来月、大阪に引っ越す予定です。', '{来月|らいげつ}、{大阪|おおさか}に{引|ひ}っ{越|こ}す{予定|よてい}です。', 'Tôi đang có kế hoạch chuyển đến Osaka vào tháng tới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('105', 'N4', '〜ことにする', 'koto ni suru', 'Quyết định sẽ làm (hoặc không làm) việc gì đó', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ことにする', 'Một quyết định của người nói; so sánh 〜ことになる. 〜ことにしている (N3) có nghĩa là ''đặt nó thành quy tắc''.', '健康のために、毎朝歩くことにした。', '{健康|けんこう}のために、{毎朝|まいあさ}{歩|ある}くことにした。', 'Tôi quyết định đi bộ mỗi sáng vì sức khỏe của mình.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('106', 'N4', '〜ことになる', 'koto ni naru', 'Được quy định, quyết định là sẽ làm gì đó (từ bên ngoài quyết định)', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ことになる', 'Trình bày một quyết định được đưa ra bởi người khác hoặc bởi hoàn cảnh. Mọi người cũng sử dụng nó cho các quyết định của riêng họ để có vẻ ít quyết đoán hơn.', '来年の春から、名古屋の店で働くことになりました。', '{来年|らいねん}の{春|はる}から、{名古屋|なごや}の{店|みせ}で{働|はたら}くことになりました。', 'Người ta đã quyết định rằng tôi sẽ làm việc tại cửa hàng Nagoya từ mùa xuân tới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('107', 'N4', '〜ことができる', 'koto ga dekiru', 'Có thể làm được việc gì đó (chỉ năng lực hoặc khả năng thực hiện)', 'Động từ thể từ điển (辞書形) + ことができる', 'Trang trọng hơn một chút so với hình thức tiềm năng. Cũng được dùng cho những việc có thể hoặc được phép (ここでたばこを吸うことはできません).', 'この図書館では、本を二週間借りることができます。', 'この{図書館|としょかん}では、{本|ほん}を{二|に}{週間|しゅうかん}{借|か}りることができます。', 'Tại thư viện này, bạn có thể mượn sách trong hai tuần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('108', 'N4', '〜える / 〜られる（potential）', 'eru / rareru (potential)', 'Động từ thể khả năng: Có thể làm được gì đó', 'Động từ thể từ điển (辞書形) (Group 1: final u → e) + る / Động từ thể ない (bỏ ない) + られる (Group 2)', 'Bất quy tắc: する → できる, 来る → 来られる. Tân ngữ thường lấy が thay vì を. Trong lời nói thông thường, động từ Nhóm 2 thường bỏ ら (見れる).', '私は漢字が二百ぐらい読めます。', '{私|わたし}は{漢字|かんじ}が{二|に}{百|ひゃく}ぐらい{読|よ}めます。', 'Tôi có thể đọc được khoảng hai trăm chữ kanji.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('109', 'N4', '〜れる / 〜られる（passive）', 'reru / rareru (passive)', 'Động từ thể bị động: Bị, được tác động bởi hành động của người khác', 'Động từ thể ない (bỏ ない) + れる (Group 1) / Động từ thể ない (bỏ ない) + られる (Group 2)', 'Bất quy tắc: する → される, 来る → 来られる. Người thực hiện được đánh dấu bằng に. Thường thể hiện rằng đối tượng gặp rắc rối trước hành động của ai đó (雨に降られた).', '先生に作文をほめられました。', '{先生|せんせい}に{作文|さくぶん}をほめられました。', 'Bài luận của tôi được giáo viên khen ngợi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('110', 'N4', '〜せる / 〜させる', 'seru / saseru', 'Động từ thể sai khiến: Bắt buộc hoặc cho phép ai đó làm gì', 'Động từ thể ない (bỏ ない) + せる (Group 1) / Động từ thể ない (bỏ ない) + させる (Group 2)', 'Bất quy tắc: する → させる, 来る → 来させる. Với nội động từ người ta dùng を; với động từ chuyển tiếp, に. Bối cảnh quyết định ''làm'' hoặc ''cho phép''.', '母は毎日弟に野菜を食べさせる。', '{母|はは}は{毎日|まいにち}{弟|おとうと}に{野菜|やさい}を{食|た}べさせる。', 'Mẹ tôi bắt em trai tôi ăn rau mỗi ngày.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('111', 'N4', '〜せられる / 〜させられる', 'serareru / saserareru', 'Động từ thể bị động sai khiến: Bị bắt buộc phải làm việc gì ngoài ý muốn', 'Động từ thể ない (bỏ ない) + せられる (Group 1; short form される) / Động từ thể ない (bỏ ない) + させられる (Group 2)', 'Bất quy tắc: する → させられる, 来る → 来させられる. Dạng ngắn Nhóm 1 (待たされる) phổ biến trong lời nói, nhưng không phổ biến với các động từ kết thúc bằng す (話させられる).', '子どものころ、母に毎日ピアノを練習させられた。', '{子|こ}どものころ、{母|はは}に{毎日|まいにち}ピアノを{練習|れんしゅう}させられた。', 'Khi tôi còn nhỏ, mẹ tôi bắt tôi tập piano hàng ngày.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('112', 'N4', '〜おう / 〜よう', 'ou / you', 'Động từ thể ý chí: Nào chúng ta cùng làm... / Định bụng sẽ làm...', 'Động từ thể từ điển (辞書形) (Group 1: final u → o) + う / Động từ thể ます (bỏ ます) + よう (Group 2)', 'Bất quy tắc: する → しよう, 来る → 来よう. Tương đương với 〜ましょう.', '疲れたね。少し休もう。', '{疲|つか}れたね。{少|すこ}し{休|やす}もう。', 'Điều đó thật mệt mỏi phải không? Chúng ta hãy nghỉ ngơi một chút.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('113', 'N4', '〜(よ)うと思う', 'you to omou', 'Đang có ý định, dự tính sẽ làm việc gì đó', 'Động từ thể ý chí + と思う', '〜と思う thường báo cáo một quyết định vừa được đưa ra; 〜と思っている thể hiện một kế hoạch được thực hiện trong một thời gian. Nhẹ nhàng hơn つもり.', '今年の夏は海に行こうと思っています。', '{今年|ことし}の{夏|なつ}は{海|うみ}に{行|い}こうと{思|おも}っています。', 'Tôi đang nghĩ đến việc đi biển vào mùa hè này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('114', 'N4', '〜え / 〜ろ', 'e / ro', 'Động từ thể mệnh lệnh: Hãy làm ngay! (mệnh lệnh dứt khoát)', 'Động từ thể từ điển (辞書形) (Group 1: final u → e) / Động từ thể ます (bỏ ます) + ろ (Group 2)', 'Bất quy tắc: する → しろ, 来る → 来い, くれる → くれ. Rất thẳng thừng; chủ yếu được nghe từ nam giới, trên các biển báo, trong trường hợp khẩn cấp và trong tiếng cổ vũ (がんばれ).', '早く起きろ！遅刻するぞ。', '{早|はや}く{起|お}きろ！{遅刻|ちこく}するぞ。', 'Đứng dậy đi! Bạn sẽ bị trễ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('115', 'N4', '〜な（prohibition）', 'na (prohibition)', 'Cấm làm việc gì đó! (thể cấm chỉ dứt khoát trong văn nói nam giới/biển báo)', 'Động từ thể từ điển (辞書形) + な', 'Rất mạnh mẽ và thẳng thừng. Đừng nhầm lẫn nó với đuôi câu な diễn tả cảm xúc (いいな).', '危ないから、この川では泳ぐな。', '{危|あぶ}ないから、この{川|かわ}では{泳|およ}ぐな。', 'Nguy hiểm lắm, đừng bơi ở con sông này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('116', 'N4', '〜なさい', 'nasai', 'Hãy làm đi nào! (lời nhắc nhở, ra lệnh thân mật của cha mẹ, thầy cô)', 'Động từ thể ます (bỏ ます) + なさい', 'Nhẹ nhàng hơn dạng mệnh lệnh nhưng vẫn mang tính chỉ dẫn chắc chắn; được phụ huynh, giáo viên và trong các buổi hướng dẫn thi cử, không bao giờ hướng tới cấp trên.', 'もう九時よ。早く寝なさい。', 'もう{九|く}{時|じ}よ。{早|はや}く{寝|ね}なさい。', 'Đã chín giờ rồi. Đi ngủ bây giờ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('117', 'N4', '〜ば', 'ba', 'Nếu... thì... (câu điều kiện giả định thể ば)', 'Động từ thể điều kiện (ば) / Tính từ đuôi い stem + ければ / Tính từ đuôi な + なら / Danh từ + なら', 'Tập trung vào điều kiện cần thiết để đạt được kết quả. Khi mệnh đề chính là một yêu cầu hoặc lời mời, たら thường tự nhiên hơn trừ khi mệnh đề ば là một trạng thái (ある, いる, tính từ).', 'この道をまっすぐ行けば、駅に着きます。', 'この{道|みち}をまっすぐ{行|い}けば、{駅|えき}に{着|つ}きます。', 'Nếu bạn đi thẳng theo con đường này, bạn sẽ đến nhà ga.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('118', 'N4', '〜たら', 'tara', 'Nếu... thì... / Sau khi... thì... (câu điều kiện phổ biến nhất thể たら)', 'Động từ thể た (quá khứ) + ら / Tính từ đuôi い stem + かったら / Tính từ đuôi な + だったら / Danh từ + だったら', 'Câu điều kiện linh hoạt nhất và rất phổ biến trong lời nói. Với những sự kiện chắc chắn sẽ xảy ra, nó có nghĩa là ''khi/sau''.', '駅に着いたら、電話してください。', '{駅|えき}に{着|つ}いたら、{電話|でんわ}してください。', 'Hãy gọi cho tôi khi bạn đến ga.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('119', 'N4', '〜なら', 'nara', 'Nếu là... thì hãy... (đưa ra lời khuyên hoặc gợi ý theo chủ đề người khác nói)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + なら', 'Tiếp thu những gì người nghe đã nói hoặc một tình huống được cho là đúng, sau đó đưa ra lời khuyên hoặc ý kiến. Không giống như たら, hành động chính có thể xảy ra trước mệnh đề なら.', '京都へ行くなら、秋がいいですよ。', '{京都|きょうと}へ{行|い}くなら、{秋|あき}がいいですよ。', 'Nếu bạn định đến Kyoto thì mùa thu là thời điểm thích hợp.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('120', 'N4', '〜と（conditional）', 'to', 'Hễ mà... thì đương nhiên... (điều kiện tự nhiên, thói quen, máy móc)', 'Động từ thể từ điển (辞書形) / Động từ thể ない / Tính từ đuôi い + と / Tính từ đuôi な + だ + と / Danh từ + だ + と', 'Để có kết quả tự nhiên, cách máy móc hoạt động, hướng dẫn và thói quen. Mệnh đề chính không thể là một yêu cầu, mong muốn hay lời mời.', 'このボタンを押すと、お茶が出ます。', 'このボタンを{押|お}すと、お{茶|ちゃ}が{出|で}ます。', 'Khi bạn nhấn nút này, trà sẽ chảy ra.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('121', 'N4', '〜ても / 〜でも', 'te mo / de mo', 'Cho dù... đi chăng nữa thì vẫn...', 'Động từ thể て + も / Tính từ đuôi い-ku + ても / Tính từ đuôi な + でも / Danh từ + でも', 'Với từ để hỏi hoặc いくら nó có nghĩa là ''không có vấn đề gì ...'' (何を食べても, いくら待っても).', '雨が降っても、試合はあります。', '{雨|あめ}が{降|ふ}っても、{試合|しあい}はあります。', 'Ngay cả khi trời mưa, trò chơi vẫn tiếp tục.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('122', 'N4', '〜のに（although）', 'noni (although)', 'Thế mà, vậy mà... (diễn tả sự tiếc nuối, bất mãn trái với mong đợi)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + のに / Tính từ đuôi な + な + のに / Danh từ + な + のに', 'Mang đến sự ngạc nhiên, thất vọng hoặc phàn nàn. Mệnh đề chính không thể là một yêu cầu hay ý định; sử dụng けど hoặc ても ở đó.', '一生懸命勉強したのに、試験に落ちてしまった。', '{一生懸命|いっしょうけんめい}{勉強|べんきょう}したのに、{試験|しけん}に{落|お}ちてしまった。', 'Mặc dù tôi đã học rất chăm chỉ nhưng tôi đã trượt kỳ thi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('123', 'N4', '〜のに（purpose）', 'noni (purpose)', 'Để mà, dùng cho mục đích... (đi kèm từ chỉ thời gian, tiền bạc, công sức)', 'Động từ thể từ điển (辞書形) + のに / Danh từ + に', 'Thường theo sau là các từ như 使う, いい, 便利, 必要 hoặc かかる. Với danh từ, chỉ dùng に (旅行に便利).', 'このかばんは旅行に行くのに便利です。', 'このかばんは{旅行|りょこう}に{行|い}くのに{便利|べんり}です。', 'Túi này tiện dụng khi đi du lịch.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('124', 'N4', '〜ので', 'node', 'Bởi vì, do... (chỉ nguyên nhân khách quan, mang tính lịch sự, trang trọng)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + ので / Tính từ đuôi な + な + ので / Danh từ + な + ので', 'Nghe nhẹ nhàng và khách quan hơn から nên nó được ưa chuộng hơn khi đưa ra lý do một cách lịch sự hoặc khi xin lỗi.', '頭が痛いので、今日は早く帰ります。', '{頭|あたま}が{痛|いた}いので、{今日|きょう}は{早|はや}く{帰|かえ}ります。', 'Hôm nay tôi bị đau đầu nên về nhà sớm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('125', 'N4', '〜て（reason）', 'te (reason)', 'Vì... nên... (dùng thể て để chỉ lý do gây ra cảm xúc hoặc kết quả)', 'Động từ thể て / Tính từ đuôi い stem + くて / Tính từ đuôi な + で / Danh từ + で', 'Phần thứ hai thường là cảm giác, hình thức tiềm ẩn hoặc kết quả, không phải là yêu cầu hay ý định (dùng から hoặc ので cho những điều đó). Phủ định: 〜なくて (会えなくて寂しい).', 'ニュースを聞いて、びっくりしました。', 'ニュースを{聞|き}いて、びっくりしました。', 'Tôi rất ngạc nhiên khi nghe tin này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('126', 'N4', '〜し', 'shi', 'Vừa... lại vừa...; Vì... và vì... (liệt kê nhiều nguyên nhân, lý do)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + し / Tính từ đuôi な + だ + し / Danh từ + だ + し', 'Liệt kê một số lý do hoặc điểm. Một chữ し gợi ý rằng còn có những lý do khác.', 'この部屋は広いし、駅から近いし、とても気に入っています。', 'この{部屋|へや}は{広|ひろ}いし、{駅|えき}から{近|ちか}いし、とても{気|き}に{入|い}っています。', 'Căn hộ này rộng rãi và gần ga nên mình rất thích.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('127', 'N4', '〜けど / 〜けれども', 'kedo / keredomo', 'Nhưng, tuy nhiên... (liên từ nối mang nghĩa tương phản)', 'Mệnh đề / Câu + けど / けれども', 'Mang tính trò chuyện nhiều hơn が; けれども trang trọng hơn けど một chút. Thường được sử dụng như một lời mở đầu nhẹ nhàng trước một câu hỏi hoặc yêu cầu.', 'この店は高いけど、おいしい。', 'この{店|みせ}は{高|たか}いけど、おいしい。', 'Nơi này đắt tiền nhưng đồ ăn thì ngon.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('128', 'N4', '〜ために', 'tame ni', 'Để, nhằm mục đích... / Vì, do nguyên nhân...', 'Động từ thể từ điển (辞書形) / Động từ thể た (quá khứ) / Danh từ + の + ために', 'Vì mục đích, động từ phải là thứ mà chủ ngữ có thể kiểm soát được (so sánh 〜ように). Việc sử dụng ''vì'' nghe có vẻ khá trang trọng.', '日本の大学に入るために、毎日勉強しています。', '{日本|にほん}の{大学|だいがく}に{入|はい}るために、{毎日|まいにち}{勉強|べんきょう}しています。', 'Tôi học hàng ngày để vào được một trường đại học Nhật Bản.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('129', 'N4', '〜ように（purpose）', 'you ni', 'Để mà, nhằm đạt được trạng thái... (chỉ mục đích)', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ように', 'Dùng với các động từ mà chủ ngữ không thể trực tiếp điều khiển: dạng tiềm năng, 分かる, 見える, phủ định. Với những hành động có chủ ý, hãy sử dụng ために.', '後ろの人にも聞こえるように、大きな声で話してください。', '{後|うし}ろの{人|ひと}にも{聞|き}こえるように、{大|おお}きな{声|こえ}で{話|はな}してください。', 'Hãy nói to lên để người ngồi phía sau cũng có thể nghe thấy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('130', 'N4', '〜ようにする', 'you ni suru', 'Cố gắng, nỗ lực tạo thói quen làm gì đó đều đặn', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ようにする', '〜ようにしている mô tả một thói quen mà bạn duy trì. So sánh 〜ようになる, một sự thay đổi xảy ra.', '毎日野菜を食べるようにしています。', '{毎日|まいにち}{野菜|やさい}を{食|た}べるようにしています。', 'Tôi chú trọng việc ăn rau mỗi ngày.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('131', 'N4', '〜ようになる', 'you ni naru', 'Trở nên có thể làm được gì đó (biến đổi về năng lực hoặc thói quen)', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ようになる', 'Thường được sử dụng với các động từ tiềm năng. Đối với ''không còn nữa'', 〜なくなる thường tự nhiên hơn 〜ないようになる.', '練習して、自転車に乗れるようになりました。', '{練習|れんしゅう}して、{自転車|じてんしゃ}に{乗|の}れるようになりました。', 'Sau khi luyện tập, tôi đã có thể đi xe đạp.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('132', 'N4', '〜てほしい', 'te hoshii', 'Muốn người khác làm việc gì đó giúp mình', 'Động từ thể て + ほしい / Động từ thể ない + で + ほしい', 'Người bạn muốn hành động được đánh dấu bằng に. Nói với cấp trên điều đó nghe có vẻ khắt khe; 〜ていただきたいです lịch sự hơn.', 'もう少しゆっくり話してほしいです。', 'もう{少|すこ}しゆっくり{話|はな}してほしいです。', 'Tôi muốn bạn nói chậm lại một chút.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('133', 'N4', '〜たがる', 'tagaru', 'Người thứ ba muốn làm việc gì đó (diễn tả mong muốn của người khác)', 'Động từ thể ます (bỏ ます) + たがる', 'Dùng cho người khác, vì bạn không thể trực tiếp biết được mong muốn của họ. Mong muốn hiện tại thường được thể hiện bằng 〜たがっている.', '子どもたちは外で遊びたがっている。', '{子|こ}どもたちは{外|そと}で{遊|あそ}びたがっている。', 'Bọn trẻ muốn chơi bên ngoài.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('134', 'N4', '〜がる', 'garu', 'Người thứ ba cảm thấy... (nhìn thấy qua biểu hiện bên ngoài)', 'Tính từ đuôi い stem / Tính từ đuôi な + がる', 'Chuyển các tính từ cảm giác (怖い, 寂しい, ほしい) thành động từ mô tả cảm giác của người khác. Đối tượng của cảm giác lấy を.', '妹は暗い所を怖がる。', '{妹|いもうと}は{暗|くら}い{所|ところ}を{怖|こわ}がる。', 'Em gái tôi sợ những nơi tối tăm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('135', 'N4', '〜かもしれない', 'kamo shirenai', 'Có thể, có khả năng là... (suy đoán xác suất khoảng 50%)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + かもしれない', 'Chắc chắn khoảng 50% hoặc ít hơn. Trong cách nói thông thường, nó thường được rút ngắn thành かも.', '午後から雨が降るかもしれません。', '{午後|ごご}から{雨|あめ}が{降|ふ}るかもしれません。', 'Trời có thể mưa vào buổi chiều.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('136', 'N4', '〜だろう', 'darou', 'Có lẽ là... (dự đoán suy đoán ở thể thông thường)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + だろう', 'Phổ biến trong văn viết và lời nói thông thường của nam giới; でしょう là hình thức lịch sự. Thường dùng với たぶん hoặc きっと, hoặc như 〜だろうと思う.', '明日はたぶん晴れるだろう。', '{明日|あした}はたぶん{晴|は}れるだろう。', 'Có lẽ ngày mai trời sẽ nắng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('137', 'N4', '〜はずだ', 'hazu da', 'Chắc chắn là, lẽ ra phải là... (suy luận có căn cứ vững chắc)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + はずだ / Tính từ đuôi な + な + はずだ / Danh từ + の + はずだ', 'Một kỳ vọng tự tin dựa trên sự thật hoặc logic, không phải là phỏng đoán. Nó không có nghĩa là ''nên'' như một nghĩa vụ.', '荷物は明日届くはずです。', '{荷物|にもつ}は{明日|あした}{届|とど}くはずです。', 'Gói hàng sẽ đến vào ngày mai.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('138', 'N4', '〜はずがない', 'hazu ga nai', 'Không lý nào lại..., chắc chắn không thể nào như thế được', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + はずがない / Tính từ đuôi な + な + はずがない / Danh từ + の + はずがない', 'Phủ nhận mạnh mẽ dựa trên lý luận. 〜ないはずだ (''không nên'') là một sự mong đợi nhẹ nhàng hơn.', 'あんなにまじめな人がうそをつくはずがない。', 'あんなにまじめな{人|ひと}がうそをつくはずがない。', 'Không đời nào một người nghiêm túc như vậy lại có thể nói dối được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('139', 'N4', '〜んです / 〜のです', 'n desu / no desu', 'Dùng để nhấn mạnh, giải thích nguyên nhân hoặc hỏi han quan tâm', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + んです / Tính từ đuôi な + な + んです / Danh từ + な + んです', 'Thêm ý nghĩa giải thích một tình huống hoặc hỏi thông tin cơ bản. Các hình thức thông thường là 〜んだ và 〜の. Việc sử dụng quá mức nó có thể nghe có vẻ tự đề cao.', 'どうしたんですか。顔色が悪いですよ。', 'どうしたんですか。{顔色|かおいろ}が{悪|わる}いですよ。', 'Có chuyện gì vậy? Bạn trông nhợt nhạt.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('140', 'N4', '〜かどうか', 'ka dou ka', 'Có... hay không (câu hỏi lồng ghép vào câu chính)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + かどうか', 'Nhúng câu hỏi có-không vào câu. Đối với các câu hỏi có từ để hỏi, chỉ sử dụng 〜か (何時に来るか).', '明日行けるかどうか、まだ分かりません。', '{明日|あした}{行|い}けるかどうか、まだ{分|わ}かりません。', 'Tôi vẫn chưa biết liệu ngày mai tôi có thể đi được không.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('141', 'N4', '〜か（embedded question）', 'ka (embedded question)', 'Nghi vấn từ lồng ghép vào giữa câu trần thuật', 'Từ để hỏi + ... + Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + か', 'Sau danh từ hoặc tính từ na, だ bị lược bỏ (誰の傘か知らない).', '会議が何時に始まるか、教えてください。', '{会議|かいぎ}が{何|なん}{時|じ}に{始|はじ}まるか、{教|おし}えてください。', 'Xin vui lòng cho tôi biết cuộc họp bắt đầu lúc mấy giờ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('142', 'N4', '〜と思う', 'to omou', 'Tôi nghĩ rằng, tôi cho rằng...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + と思う / Tính từ đuôi な + だ + と思う / Danh từ + だ + と思う', 'Người Nhật thường phủ định mệnh đề trích dẫn (来ないと思う) hơn là 思う. Đối với ý kiến ​​của người khác, hãy sử dụng 〜と思っている.', 'この映画はきっと人気が出ると思う。', 'この{映画|えいが}はきっと{人気|にんき}が{出|で}ると{思|おも}う。', 'Tôi chắc chắn bộ phim này sẽ trở nên nổi tiếng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('143', 'N4', '〜と言う', 'to iu (say)', 'Nói rằng, bảo rằng... (trích dẫn lời nói trực tiếp hoặc gián tiếp)', '「Mệnh đề / Câu」 + と言う / Mệnh đề / Câu (plain) + と言う', 'Trích dẫn trực tiếp đi vào bên trong 「」; trích dẫn gián tiếp sử dụng các hình thức đơn giản. 〜と言っていた được dùng phổ biến khi truyền lại điều ai đó đã nói.', '先生は「明日はテストがあります」と言いました。', '{先生|せんせい}は「{明日|あした}はテストがあります」と{言|い}いました。', 'Thầy giáo nói: “Ngày mai có bài kiểm tra.”')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('144', 'N4', '〜しか〜ない', 'shika ... nai', 'Chỉ có thể, không còn cách nào khác ngoài việc...', 'Danh từ + しか + Động từ thể ない', 'Luôn đi đôi với động từ phủ định. So với だけ, nó nhấn mạnh số lượng ít hoặc không đủ.', '財布に五百円しかない。', '{財布|さいふ}に{五|ご}{百|ひゃく}{円|えん}しかない。', 'Tôi chỉ có 500 yên trong ví.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('145', 'N4', '〜ばかり', 'bakari', 'Toàn là, chỉ toàn làm việc gì đó (thường mang nghĩa hơi phàn nàn)', 'Danh từ + ばかり', 'Thường ngụ ý có quá nhiều thứ. 〜てばかりいる (N3) và 〜たばかり (''vừa làm'') là những điểm riêng biệt.', '弟は肉ばかり食べて、野菜を食べない。', '{弟|おとうと}は{肉|にく}ばかり{食|た}べて、{野菜|やさい}を{食|た}べない。', 'Em trai tôi không ăn gì ngoài thịt và không đụng đến rau.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('146', 'N4', '〜たばかり', 'ta bakari', 'Vừa mới làm xong việc gì đó tức thì (cảm nhận chủ quan)', 'Động từ thể た (quá khứ) + ばかり', 'Cho thấy người nói cảm thấy thời gian đã trôi qua rất ít, ngay cả khi đó là vài tuần trước. 〜たところ chỉ đề cập đến khoảnh khắc trước đây.', '日本に来たばかりなので、まだ日本語がよく分かりません。', '{日本|にほん}に{来|き}たばかりなので、まだ{日本語|にほんご}がよく{分|わ}かりません。', 'Mình mới sang Nhật nên chưa hiểu tiếng Nhật lắm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('147', 'N4', '〜ところだ', 'tokoro da', 'Đang chuẩn bị làm / Đang làm dở dang / Vừa mới làm xong', 'Động từ thể từ điển (辞書形) / Động từ thể て + いる / Động từ thể た (quá khứ) + ところだ', 'Dạng từ điển: sắp. 〜ているところ: ở giữa. 〜たところ: vừa xong. Nó xác định chính xác giai đoạn của một hành động.', '今から出かけるところです。', '{今|いま}から{出|で}かけるところです。', 'Tôi vừa định đi ra ngoài.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('148', 'N4', '〜やすい / 〜にくい', 'yasui / nikui', 'Dễ làm... / Khó làm việc gì đó...', 'Động từ thể ます (bỏ ます) + やすい / Động từ thể ます (bỏ ます) + にくい', 'Cả hai liên hợp như tính từ i. やすい cũng có thể có nghĩa là ''có xu hướng'' (この服は汚れやすい).', 'このペンは書きやすいです。', 'このペンは{書|か}きやすいです。', 'Cây bút này rất dễ viết.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('149', 'N4', '〜始める / 〜終わる / 〜続ける', 'hajimeru / owaru / tsuzukeru', 'Bắt đầu làm / Kết thúc làm / Tiếp tục làm việc gì đó', 'Động từ thể ます (bỏ ます) + 始める / Động từ thể ます (bỏ ます) + 終わる / Động từ thể ます (bỏ ます) + 続ける', '〜終わる được dùng với những hành động có kết thúc rõ ràng (読む, 食べる, 書く). 〜終える là một cách thay thế trang trọng hơn.', '去年からギターを習い始めました。', '{去年|きょねん}からギターを{習|なら}い{始|はじ}めました。', 'Tôi bắt đầu học guitar vào năm ngoái.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('150', 'N4', '〜出す', 'dasu', 'Bất thình lình bật ra hành động (bật cười, khóc òa, đổ mưa)', 'Động từ thể ます (bỏ ます) + 出す', 'Ngụ ý sự khởi đầu đột ngột, thường không kiểm soát được. Không giống như 〜始める, nó không được sử dụng cho các hành động đã được lên kế hoạch hoặc trong các yêu cầu.', '赤ちゃんが急に泣き出した。', '{赤|あか}ちゃんが{急|きゅう}に{泣|な}き{出|だ}した。', 'Đứa bé chợt òa khóc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('151', 'N4', '〜ほど〜ない', 'hodo ... nai', 'Không... bằng so với đối tượng kia (so sánh kém)', 'Danh từ / Động từ thể thông thường (V ngắn) + ほど + Tính từ đuôi い-ku + ない / Tính từ đuôi な + ではない', 'Mệnh đề trước ほど là tiêu chuẩn so sánh. Luôn luôn theo sau là một tiêu cực.', '今年の夏は去年ほど暑くない。', '{今年|ことし}の{夏|なつ}は{去年|きょねん}ほど{暑|あつ}くない。', 'Mùa hè năm nay không nóng như năm ngoái.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('152', 'N4', 'なかなか〜ない', 'nakanaka ... nai', 'Mãi mà không... được; Khá là khó để...', 'なかなか + Động từ thể ない', 'Cho thấy điều gì đó không xảy ra nhanh chóng và dễ dàng như mong đợi. Với dạng khẳng định, なかなか có nghĩa là ''khá'' (なかなかおいしい).', 'バスがなかなか来ない。', 'バスがなかなか{来|こ}ない。', 'Xe buýt không đến.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('153', 'N4', '〜ずつ', 'zutsu', 'Từng... một (phân chia đồng đều về số lượng hoặc thời gian)', 'Danh từ (quantity) + ずつ', 'Cấu trúc 〜ずつ thường gặp trong các đề thi JLPT cấp độ N4 và giao tiếp đời sống hàng ngày.', 'りんごを一人に二つずつ配った。', 'りんごを{一人|ひとり}に{二|ふた}つずつ{配|くば}った。', 'Tôi đưa cho mỗi người hai quả táo.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('154', 'N4', '〜も（emphasis）', 'mo (emphasis)', 'Đến tận... (nhấn mạnh số lượng nhiều bất ngờ)', 'Danh từ (number + counter) + も', 'Cho thấy người nói cảm thấy số lượng lớn. Với 一 + counter và động từ phủ định, nó có nghĩa là ''thậm chí không một''.', '昨日は十時間も寝てしまった。', '{昨日|きのう}は{十|じゅう}{時間|じかん}も{寝|ね}てしまった。', 'Hôm qua tôi đã ngủ suốt mười tiếng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('155', 'N4', '〜までに', 'made ni', 'Trước, muộn nhất là vào thời điểm... (chỉ hạn chót deadline)', 'Danh từ / Động từ thể từ điển (辞書形) + までに', 'までに đánh dấu thời hạn cho một hành động diễn ra một lần; まで đánh dấu khoảng thời gian một sự việc tiếp tục diễn ra (六時まで働く).', 'レポートは金曜日までに出してください。', 'レポートは{金曜日|きんようび}までに{出|だ}してください。', 'Vui lòng nộp báo cáo trước thứ Sáu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('156', 'N4', '〜ことがある', 'koto ga aru', 'Thỉnh thoảng làm... / Đôi khi có xảy ra trường hợp...', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ことがある', 'Sử dụng từ điển hoặc dạng ない. Với dạng quá khứ (〜たことがある) nó có nghĩa là ''đã từng làm trước đây''.', '忙しいときは、朝ご飯を食べないことがあります。', '{忙|いそが}しいときは、{朝|あさ}ご{飯|はん}を{食|た}べないことがあります。', 'Khi bận rộn, đôi khi tôi bỏ bữa sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('157', 'N4', '〜ないといけない / 〜なくちゃ', 'nai to ikenai / nakucha', 'Phải làm, bắt buộc phải làm việc gì (văn nói rút gọn)', 'Động từ thể ない + といけない / Động từ thể ない (bỏ ない) + なくちゃ', 'Các lựa chọn thay thế hàng ngày cho なければならない. なくちゃ xuất phát từ なくては; なきゃ (từ なければ) cũng rất phổ biến.', '明日は早いから、もう寝ないといけない。', '{明日|あした}は{早|はや}いから、もう{寝|ね}ないといけない。', 'Ngày mai tôi phải khởi hành sớm nên bây giờ tôi cần phải đi ngủ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('158', 'N4', '〜させてください', 'sasete kudasai', 'Xin hãy cho phép tôi được làm việc này', 'Động từ thể ない (bỏ ない) + せてください (Group 1) / Động từ thể ない (bỏ ない) + させてください (Group 2)', 'Được xây dựng trên dạng nguyên nhân て (する → させて). 〜させていただけませんか thì lịch sự hơn.', '少し考えさせてください。', '{少|すこ}し{考|かんが}えさせてください。', 'Xin hãy để tôi suy nghĩ về nó một chút.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('159', 'N4', '尊敬語', 'sonkeigo', 'Tôn kính ngữ: Nâng cao hành động của đối phương, đối tác', 'いらっしゃる（行く・来る・いる） / 召し上がる（食べる・飲む） / おっしゃる（言う） / なさる（する） / ご覧になる（見る） / くださる（くれる）', 'Nâng cao hành động của những người bạn tôn trọng, chẳng hạn như khách hàng, giáo viên và cấp trên. Đừng bao giờ sử dụng nó cho hành động của riêng bạn.', '社長は何時ごろいらっしゃいますか。', '{社長|しゃちょう}は{何|なん}{時|じ}ごろいらっしゃいますか。', 'Chủ tịch công ty sẽ có mặt ở đây vào khoảng mấy giờ?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('160', 'N4', '謙譲語', 'kenjougo', 'Khiêm nhường ngữ: Hạ thấp hành động của bản thân để tôn kính người nghe', '参る（行く・来る） / おる（いる） / いただく（食べる・飲む・もらう） / 申す（言う） / いたす（する） / 拝見する（見る） / 伺う（聞く・訪ねる）', 'Giảm bớt hành động của chính bạn hoặc của nhóm bạn đối với người mà bạn tôn trọng. 参る, おる, 申す và いたす cũng làm cho lời nói nói chung có vẻ trang trọng.', '明日の午後、そちらに伺います。', '{明日|あした}の{午後|ごご}、そちらに{伺|うかが}います。', 'Tôi sẽ đến văn phòng của bạn vào chiều mai.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('161', 'N4', 'お〜になる', 'o ... ni naru', 'Dạng thức cấu tạo tôn kính ngữ tiêu chuẩn cho động từ', 'お + Động từ thể ます (bỏ ます) + になる', 'Không dùng với gốc một âm tiết (見る, 寝る) hoặc động từ có dạng đặc biệt (行く → いらっしゃる). する-danh từ lấy ご + Danh từ + になる (ご利用になる).', '先生はもうお帰りになりました。', '{先生|せんせい}はもうお{帰|かえ}りになりました。', 'Thầy đã về nhà rồi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('162', 'N4', 'お〜する', 'o ... suru', 'Dạng thức cấu tạo khiêm nhường ngữ tiêu chuẩn cho động từ', 'お + Động từ thể ます (bỏ ます) + する / ご + Danh từ + する', 'Chỉ dành cho những hành động có ảnh hưởng hoặc có lợi cho người khác. お〜いたします thậm chí còn trang trọng hơn.', '重そうですね。お持ちしましょうか。', '{重|おも}そうですね。お{持|も}ちしましょうか。', 'Có vẻ nặng nề đấy. Tôi mang nó cho bạn nhé?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('163', 'N4', '〜でございます', 'de gozaimasu', 'Dạng thức trang trọng lịch sự tối cao của です', 'Danh từ / Tính từ đuôi な + でございます', 'Phiên bản rất lịch sự của です, điển hình của nhân viên cửa hàng và khách sạn nói chuyện với khách hàng. ございます bản thân nó là hình thức lịch sự của あります.', 'お手洗いはあちらでございます。', 'お{手洗|てあら}いはあちらでございます。', 'Nhà vệ sinh ở đằng kia.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('164', 'N4', '〜ても構わない', 'te mo kamawanai', 'Dù có làm... cũng không sao, không hề phiền gì', 'Động từ thể て + も構わない / Tính từ đuôi い-ku + ても構わない / Tính từ đuôi な + でも構わない / Danh từ + でも構わない', 'Trang trọng hơn 〜てもいい một chút. 〜なくても構わない có nghĩa là ''không sao đâu''.', 'ここに荷物を置いても構いませんか。', 'ここに{荷物|にもつ}を{置|お}いても{構|かま}いませんか。', 'Bạn có phiền nếu tôi để túi ở đây không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('165', 'N4', '〜の / 〜こと（nominalizer）', 'no / koto (nominalizer)', 'Sự việc, cái việc... (danh từ hóa mệnh đề động từ)', 'Động từ thể thông thường (V ngắn) + の / Động từ thể thông thường (V ngắn) + こと', 'の được ưa chuộng hơn với các động từ nhận thức (見る, 聞こえる) và trong lời nói thông thường; こと cần được dùng trước です/だ và trong các mẫu cố định như 〜ことができる.', '私は写真を撮るのが好きです。', '{私|わたし}は{写真|しゃしん}を{撮|と}るのが{好|す}きです。', 'Tôi thích chụp ảnh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('166', 'N4', '〜ばよかった', 'ba yokatta', 'Giá mà đã làm... thì tốt biết mấy (hối tiếc điều chưa làm)', 'Động từ thể điều kiện (ば) + よかった / Động từ thể ない (bỏ ない) + なければよかった', 'Thể hiện sự tiếc nuối về quá khứ. 〜たらよかった là một từ thay thế trong hội thoại phổ biến.', 'もっと早く家を出ればよかった。', 'もっと{早|はや}く{家|いえ}を{出|で}ればよかった。', 'Lẽ ra tôi nên rời nhà sớm hơn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('167', 'N4', '〜たらどう', 'tara dou', 'Thử làm... xem sao? (đưa ra lời khuyên hoặc gợi ý)', 'Động từ thể た (quá khứ) + らどう', 'Thường được rút gọn thành 〜たら？. Có thể tỏ ra tự đề cao cấp trên; 〜たらいかがですか là phiên bản lịch sự.', '疲れているなら、少し休んだらどう？', '{疲|つか}れているなら、{少|すこ}し{休|やす}んだらどう？', 'Nếu mệt mỏi, tại sao không nghỉ ngơi một chút?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('168', 'N4', '〜ほうがいい', 'hou ga ii', 'Nên làm... / Không nên làm... thì tốt hơn', 'Động từ thể た (quá khứ) / Động từ thể ない + ほうがいい', 'Để được tư vấn, dạng quá khứ (行ったほうがいい) là lựa chọn thông thường và nghe mạnh mẽ hơn. Thể phủ định sử dụng dạng ない, không phải quá khứ.', '熱があるなら、病院に行ったほうがいいですよ。', '{熱|ねつ}があるなら、{病院|びょういん}に{行|い}ったほうがいいですよ。', 'Nếu bị sốt thì tốt nhất bạn nên đến bệnh viện.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('169', 'N4', '〜とか〜とか', 'toka ... toka', 'Như là..., như là... (liệt kê thân mật trong văn nói)', 'Danh từ + とか + Danh từ + とか / Động từ thể từ điển (辞書形) + とか + Động từ thể từ điển (辞書形) + とか', 'Bình thường hơn 〜や〜など, và nó cũng có thể liệt kê các động từ hoặc cả cụm từ.', '冷蔵庫に卵とか牛乳とかが入っています。', '{冷蔵庫|れいぞうこ}に{卵|たまご}とか{牛乳|ぎゅうにゅう}とかが{入|はい}っています。', 'Có những thứ như trứng và sữa trong tủ lạnh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('170', 'N4', '〜など', 'nado', 'Vân vân, và những thứ tương tự...', 'Danh từ + など', 'Thường kết hợp với や (AやBなど). Sau một danh từ, nó làm dịu đi một gợi ý.', '机の上に本やノートなどがあります。', '{机|つくえ}の{上|うえ}に{本|ほん}やノートなどがあります。', 'Trên bàn có sách, vở, v.v.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('171', 'N4', '〜でも（suggestion）', 'de mo (suggestion)', 'Hay là cái gì đó như... (đưa ra ví dụ mang tính gợi ý nhẹ nhàng)', 'Danh từ + でも', 'Đưa ra một ví dụ trong khi vẫn để ngỏ các tùy chọn khác, điều này làm cho lời mời và lời khuyên nghe có vẻ ít thuyết phục hơn.', 'ちょっと休んで、コーヒーでも飲みませんか。', 'ちょっと{休|やす}んで、コーヒーでも{飲|の}みませんか。', 'Sao chúng ta không nghỉ ngơi và uống cà phê hay gì đó nhỉ?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('172', 'N4', '〜さ', 'sa', 'Độ, mức độ (hậu tố biến tính từ thành danh từ đo lường)', 'Tính từ đuôi い stem + さ / Tính từ đuôi な + さ', 'いい trở thành よさ. Thường dùng cho các phép đo (高さ, 重さ, 大きさ, 長さ).', 'この橋の長さはどのくらいですか。', 'この{橋|はし}の{長|なが}さはどのくらいですか。', 'Cây cầu này dài bao nhiêu?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('173', 'N4', '〜目', 'me', 'Thứ... (hậu tố chỉ số thứ tự như lần thứ nhất, thứ hai)', 'Danh từ (number + counter) + 目', 'Cấu trúc 〜目 thường gặp trong các đề thi JLPT cấp độ N4 và giao tiếp đời sống hàng ngày.', '右から三つ目の建物が図書館です。', '{右|みぎ}から{三|みっ}つ{目|め}の{建物|たてもの}が{図書館|としょかん}です。', 'Tòa nhà thứ ba từ phải sang là thư viện.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('174', 'N4', '〜おきに', 'oki ni', 'Cứ cách mỗi khoảng thời gian/khoảng cách lại...', 'Danh từ (number + counter) + おきに', 'Với ngày (一日おき) nó bỏ qua một đơn vị, nghĩa là ''cách ngày''. Với số phút hoặc khoảng cách, nó thường chỉ có nghĩa là ''mọi X''.', 'このバスは十五分おきに来ます。', 'このバスは{十|じゅう}{五|ご}{分|ふん}おきに{来|き}ます。', 'Xe buýt này đến cứ sau mười lăm phút.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('175', 'N4', '〜くする / 〜にする（make）', 'ku suru / ni suru', 'Làm cho trở nên... (tác động chủ quan làm biến đổi tính chất)', 'Tính từ đuôi い-ku + する / Tính từ đuôi な + に + する / Danh từ + に + する', 'Ai đó cố tình thay đổi một cái gì đó. So sánh 〜くなる / 〜になる, nơi mà sự thay đổi tự nó xảy ra.', 'テレビの音を小さくしてください。', 'テレビの{音|おと}を{小|ちい}さくしてください。', 'Làm ơn tắt TV đi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('176', 'N4', '〜がする', 'ga suru', 'Có cảm giác, ngửi thấy mùi, nghe thấy âm thanh phát ra', 'Danh từ + がする', 'Được sử dụng với におい, 音, 声, 味 và 気 (〜気がする: ''có cảm giác như vậy'').', '台所からいいにおいがする。', '{台所|だいどころ}からいいにおいがする。', 'Có một mùi thơm dễ chịu tỏa ra từ nhà bếp.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('177', 'N4', '〜かな / 〜かしら', 'kana / kashira', 'Liệu có... không nhỉ? (tự hỏi bản thân trong suy nghĩ)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + かな / かしら', 'Một câu hỏi nhẹ nhàng, thường tự nói với chính mình. かしら nghe có vẻ nữ tính và hơi lỗi thời. 〜ないかな có thể diễn tả một điều ước (早く来ないかな).', '明日は晴れるかな。', '{明日|あした}は{晴|は}れるかな。', 'Tôi tự hỏi liệu ngày mai trời có nắng không.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('178', 'N4', '〜てくれませんか / 〜ていただけませんか', 'te kuremasen ka / te itadakemasen ka', 'Bạn có thể làm giúp tôi được không? (nhờ vả lịch sự)', 'Động từ thể て + くれませんか / Động từ thể て + いただけませんか', 'Nhẹ nhàng hơn 〜てください. Sự lịch sự tăng từ 〜てくれませんか lên 〜てもらえませんか đến 〜ていただけませんか.', 'すみません、写真を撮ってくれませんか。', 'すみません、{写真|しゃしん}を{撮|と}ってくれませんか。', 'Xin lỗi, bạn có thể chụp ảnh tôi được không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('179', 'N4', '〜ないで', 'naide', 'Mà không làm việc gì đó (thực hiện hành động khác trong tình trạng không làm)', 'Động từ thể ない + で', '〜ずに (N3) có nghĩa tương tự nhưng trang trọng hơn. Khác với 〜なくて, nó đưa ra lý do.', '今朝は朝ご飯を食べないで学校に行った。', '{今朝|けさ}は{朝|あさ}ご{飯|はん}を{食|た}べないで{学校|がっこう}に{行|い}った。', 'Sáng nay tôi đi học mà không ăn sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('180', 'N3', '〜わけだ', 'wake da', 'Thảo nào, hèn chi; Chẳng trách mà... / Hóa ra là vì nguyên do đó', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + という + わけだ', 'Đưa ra kết luận một cách tự nhiên dựa trên những sự kiện đã biết, thường với cảm giác ''bây giờ tôi đã hiểu''. Thường đứng trước だから, それで hoặc どうりで.', '三年も中国に住んでいたんですか。だから中国語が上手なわけですね。', '{三|さん}{年|ねん}も{中国|ちゅうごく}に{住|す}んでいたんですか。だから{中国|ちゅうごく}{語|ご}が{上手|じょうず}なわけですね。', 'Bạn sống ở Trung Quốc được ba năm? Đó là lý do tại sao tiếng Trung của bạn rất tốt.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('181', 'N3', '〜わけではない', 'wake de wa nai', 'Không hẳn là, không nhất thiết là... (phủ định một phần)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + という + わけではない', 'Sự phủ nhận một phần hoặc nhẹ nhàng những gì người nghe có thể giả định. Thường đi kèm với いつも, 全部 hoặc 必ずしも. Không giống như 〜わけがない, loại trừ một cách thẳng thừng điều gì đó.', '料理が嫌いなわけではないが、毎日作る時間がない。', '{料理|りょうり}が{嫌|きら}いなわけではないが、{毎日|まいにち}{作|つく}る{時間|じかん}がない。', 'Không phải tôi không thích nấu ăn mà là tôi không có thời gian nấu nướng hàng ngày.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('182', 'N3', '〜わけがない', 'wake ga nai', 'Tuyệt đối không thể nào có chuyện... / Không lý nào lại như vậy', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + わけがない', 'Niềm tin chắc chắn của người nói rằng điều gì đó là không thể. Bình thường: 〜わけない. Gần giống với 〜はずがない, nhưng nghe có vẻ cảm xúc hơn và ít dựa trên lý lẽ hơn.', 'こんなに練習したんだから、負けるわけがない。', 'こんなに{練習|れんしゅう}したんだから、{負|ま}けるわけがない。', 'Chúng tôi đã tập luyện chăm chỉ đến thế này nên không đời nào chúng tôi sẽ thua được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('183', 'N3', '〜わけにはいかない', 'wake ni wa ikanai', 'Không thể làm được vì lý do đạo đức, trách nhiệm xã hội hoặc tình thế', 'Động từ thể từ điển (辞書形) / Động từ thể ない + わけにはいかない', 'Hành động này có thể thực hiện được nhưng bị loại trừ bởi nghĩa vụ, hoàn cảnh hoặc lẽ thường. 〜ないわけにはいかない có nghĩa là ''phải; không thể tránh khỏi việc làm''.', '明日は大事な会議があるから、休むわけにはいかない。', '{明日|あした}は{大事|だいじ}な{会議|かいぎ}があるから、{休|やす}むわけにはいかない。', 'Ngày mai có cuộc họp quan trọng nên tôi không thể nghỉ ngày này được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('184', 'N3', '〜ものだ', 'mono da', 'Vốn dĩ là bản chất tự nhiên; Thường hay làm... trong quá khứ; Cảm thán', 'Động từ thể từ điển (辞書形) / Động từ thể た (quá khứ) / Tính từ đuôi い / Tính từ đuôi な + な + ものだ', 'Công dụng chính: sự thật chung hoặc xu hướng tự nhiên, chuẩn mực xã hội (nên), thói quen hoài niệm về quá khứ với 〜たものだ, và cảm giác sâu sắc (時間がたつのは早いものだ). Bình thường: 〜もんだ.', '子どものころは、この川でよく泳いだものだ。', '{子|こ}どものころは、この{川|かわ}でよく{泳|およ}いだものだ。', 'Khi còn nhỏ, tôi thường bơi ở con sông này rất nhiều.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('185', 'N3', '〜ものではない', 'mono de wa nai', 'Không nên làm việc đó (lời khuyên luân lý, lẽ thường)', 'Động từ thể từ điển (辞書形) + ものではない', 'Lời khuyên dựa trên lẽ thường hoặc chuẩn mực xã hội, thường là từ người lớn tuổi. Bình thường: 〜もんじゃない. Nghe có vẻ giống sự hiểu biết thông thường hơn là 〜べきではない.', '人の失敗を笑うものではない。', '{人|ひと}の{失敗|しっぱい}を{笑|わら}うものではない。', 'Bạn không nên cười nhạo lỗi lầm của người khác.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('186', 'N3', '〜もの / 〜もん', 'mono / mon', 'Bởi vì... mà (ngụy biện, làm nũng, giải thích lý do của phụ nữ/trẻ em)', 'Mệnh đề / Câu + もの / もん', 'Nghe có vẻ bĩu môi hoặc tự biện minh; thường bắt đầu bằng だって. もん thông tục hơn. Được sử dụng bởi trẻ em và giữa những người bạn thân; tránh ở nơi làm việc.', '「どうして食べないの？」「だって、ピーマン嫌いなんだもん。」', '「どうして{食|た}べないの？」「だって、ピーマン{嫌|きら}いなんだもん。」', '''Tại sao bạn không ăn?'' ''Bởi vì tôi ghét ớt xanh!''')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('187', 'N3', '〜ものだから / 〜もので', 'mono dakara / mono de', 'Bởi vì, tại vì... (giải thích phân trần lý do bất đắc dĩ)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + な + ものだから / もので', 'Giải thích hoặc bào chữa cho điều gì đó không hoàn toàn nằm trong tầm kiểm soát của người nói. Mệnh đề chính không thể là một mệnh lệnh hoặc yêu cầu. もので thường dùng trong lời xin lỗi lịch sự.', '道がすごく混んでいたものだから、約束の時間に間に合わなかった。', '{道|みち}がすごく{混|こ}んでいたものだから、{約束|やくそく}の{時間|じかん}に{間|ま}に{合|あ}わなかった。', 'Đường rất đông nên tôi không thể đến đúng giờ được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('188', 'N3', '〜ことだ', 'koto da', 'Nên / Không nên làm gì đó (lời khuyên trực tiếp của người có kinh nghiệm)', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ことだ', 'Lời khuyên cá nhân từ một người ở vị trí có thể đưa ra lời khuyên đó (giáo viên, phụ huynh, cấp trên). Không dùng với cấp trên. So sánh 〜ものだ, cái nào phù hợp với những chuẩn mực chung.', '日本語が上手になりたいなら、毎日少しずつでも話すことだ。', '{日本語|にほんご}が{上手|じょうず}になりたいなら、{毎日|まいにち}{少|すこ}しずつでも{話|はな}すことだ。', 'Nếu muốn giỏi tiếng Nhật, bạn nên nói nó mỗi ngày, dù chỉ một chút.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('189', 'N3', '〜ことはない', 'koto wa nai', 'Không cần thiết phải làm gì đó (không đáng phải bận tâm)', 'Động từ thể từ điển (辞書形) + ことはない', 'Trấn an hoặc khuyên nhủ rằng điều gì đó là không cần thiết. Đừng nhầm lẫn với 〜たことはない (''chưa bao giờ làm'').', '簡単な手術ですから、そんなに心配することはありませんよ。', '{簡単|かんたん}な{手術|しゅじゅつ}ですから、そんなに{心配|しんぱい}することはありませんよ。', 'Đó là một thao tác đơn giản nên không cần phải lo lắng nhiều.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('190', 'N3', '〜ことにしている', 'koto ni shite iru', 'Tự mình duy trì thói quen, quy tắc làm gì đó', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ことにしている', 'Một quy tắc cá nhân mà người nói (hoặc chủ đề) đã quyết định và tuân theo. So sánh 〜ことになっている, một quy tắc hoặc kế hoạch do người khác đặt ra.', '健康のために、週に三回はジムに通うことにしている。', '{健康|けんこう}のために、{週|しゅう}に{三|さん}{回|かい}はジムに{通|かよ}うことにしている。', 'Vì sức khỏe của mình, tôi đặt ra quy tắc là đến phòng tập thể dục ít nhất ba lần một tuần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('191', 'N3', '〜ことになっている', 'koto ni natte iru', 'Theo quy định, nội quy là phải... (quy tắc chung đã được đặt ra)', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ことになっている', 'Các quy tắc, phong tục hay kế hoạch do người khác (công ty, trường học, xã hội) quyết định chứ không phải là lựa chọn cá nhân. So sánh 〜ことにしている.', 'この寮では、夜十一時までに戻ることになっている。', 'この{寮|りょう}では、{夜|よる}{十|じゅう}{一|いち}{時|じ}までに{戻|もど}ることになっている。', 'Ở ký túc xá này, cậu phải về trước 11 giờ đêm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('192', 'N3', '〜こと（instruction）', 'koto (instruction)', 'Hãy..., Phải... (dùng trong văn bản viết, bảng thông báo chỉ dẫn)', 'Động từ thể từ điển (辞書形) / Động từ thể ない + こと', 'Kết thúc câu bằng こと để nêu một quy tắc. Được tìm thấy trong các thông báo, quy định và hướng dẫn giữa giáo viên với học sinh. Vô tư; không được sử dụng đối với cấp trên.', 'レポートは、来週の金曜日までに提出すること。', 'レポートは、{来週|らいしゅう}の{金曜日|きんようび}までに{提出|ていしゅつ}すること。', 'Các báo cáo phải được nộp trước thứ Sáu tới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('193', 'N3', '〜ように言う / 〜ように頼む', 'you ni iu / you ni tanomu', 'Bảo, dặn dò hoặc nhờ ai đó hãy làm (hoặc đừng làm) việc gì', 'Động từ thể từ điển (辞書形) / Động từ thể ない + ように言う / ように頼む', 'Báo cáo một hướng dẫn hoặc yêu cầu một cách gián tiếp. Cũng có tác dụng với các động từ khác: 伝える, 注意する, お願いする. Phiên bản trích dẫn trực tiếp: 「〜てください」と言う.', '医者に、お酒を飲みすぎないように言われた。', '{医者|いしゃ}に、お{酒|さけ}を{飲|の}みすぎないように{言|い}われた。', 'Bác sĩ bảo tôi không nên uống quá nhiều.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('194', 'N3', '〜ようとする', 'you to suru', 'Đang định, sắp sửa làm gì đó / Cố gắng nỗ lực thực hiện', 'Động từ thể ý chí + とする', 'Thường thì một nỗ lực không thành công hoặc bị gián đoạn. 〜ようとしない = ''thậm chí sẽ không thử'' (không sẵn lòng). So sánh 〜てみる, có nghĩa là thực sự làm một việc gì đó để xem nó diễn ra như thế nào.', 'ドアを開けようとしたが、鍵がかかっていて開かなかった。', 'ドアを{開|あ}けようとしたが、{鍵|かぎ}がかかっていて{開|あ}かなかった。', 'Tôi cố mở cửa nhưng nó bị khóa và không mở được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('195', 'N3', '〜そうにない / 〜そうもない', 'sou ni nai / sou mo nai', 'Tuyệt đối không có vẻ gì là sẽ xảy ra', 'Động từ thể ます (bỏ ます) + そうにない / そうもない', 'Bề ngoài phủ định 〜そうだ với động từ, dựa trên những gì người nói quan sát được. Nhấn mạnh hơn: 〜そうにもない. Với tính từ dùng 〜なさそうだ thay thế.', 'この雨は、夕方までやみそうにない。', 'この{雨|あめ}は、{夕方|ゆうがた}までやみそうにない。', 'Cơn mưa này có vẻ sẽ không tạnh trước buổi tối.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('196', 'N3', '〜一方だ', 'ippou da', 'Có xu hướng ngày càng... (thường là chiều hướng xấu đi)', 'Động từ thể từ điển (辞書形) + 一方だ', 'Thay đổi ổn định theo một hướng, thường (không phải luôn luôn) theo chiều hướng xấu hơn. Dùng với các động từ thay đổi như 増える, 減る, 上がる, 悪くなる.', 'この町の人口は、減る一方だ。', 'この{町|まち}の{人口|じんこう}は、{減|へ}る{一方|いっぽう}だ。', 'Dân số của thị trấn này ngày càng giảm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('197', 'N3', '〜一方で', 'ippou de', 'Mặt khác, ngược lại... (so sánh 2 mặt của một vấn đề)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + である + 一方で', 'Trình bày hai mặt của một sự vật hoặc hai hoạt động song song. So sánh 〜に対して, sự tương phản giữa hai chủ đề khác nhau.', 'インターネットは便利な一方で、危険な面もある。', 'インターネットは{便利|べんり}な{一方|いっぽう}で、{危険|きけん}な{面|めん}もある。', 'Internet rất tiện lợi nhưng mặt khác nó cũng có những nguy hiểm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('198', 'N3', '〜に対して', 'ni taishite', 'Đối với... (thái độ); Trái ngược với... (so sánh tương phản)', 'Danh từ + に対して / Danh từ + に対する + Danh từ / Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な + のに対して', 'Đánh dấu mục tiêu của một hành động, thái độ hoặc cảm giác, hoặc đối chiếu hai sự vật. So sánh 〜について, đánh dấu chủ đề đang được thảo luận hơn là mục tiêu.', 'お客様に対して、そんな言い方をしてはいけない。', 'お{客様|きゃくさま}に{対|たい}して、そんな{言|い}い{方|かた}をしてはいけない。', 'Bạn không được nói như vậy với khách hàng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('199', 'N3', '〜について', 'ni tsuite', 'Về, liên quan đến chủ đề nào đó', 'Danh từ + について / Danh từ + についての + Danh từ', 'Đánh dấu chủ đề nói, suy nghĩ hoặc nghiên cứu. Trang trọng hơn: 〜に関して. So sánh 〜に対して (mục tiêu của thái độ hoặc hành động).', '今日の授業では、日本の食文化について話し合いました。', '{今日|きょう}の{授業|じゅぎょう}では、{日本|にほん}の{食|しょく}{文化|ぶんか}について{話|はな}し{合|あ}いました。', 'Trong lớp học hôm nay chúng ta đã thảo luận về văn hóa ẩm thực Nhật Bản.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('200', 'N3', '〜によって / 〜による', 'ni yotte / ni yoru', 'Do, bởi... (nguyên nhân/tác giả); Bằng cách... (phương thức); Tùy vào...', 'Danh từ + によって / Danh từ + による + Danh từ', 'Công dụng: tác nhân của thể bị động (trang trọng, để tạo ra hoặc khám phá), nguyên nhân, phương tiện và ''thay đổi tùy theo''. 〜によっては = ''trong một số trường hợp, tùy thuộc vào''.', 'この橋は、有名な建築家によって設計された。', 'この{橋|はし}は、{有名|ゆうめい}な{建築|けんちく}{家|か}によって{設計|せっけい}された。', 'Cây cầu này được thiết kế bởi một kiến ​​trúc sư nổi tiếng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('201', 'N3', '〜によると / 〜によれば', 'ni yoru to / ni yoreba', 'Theo như nguồn thông tin từ...', 'Danh từ + によると / Danh từ + によれば', 'Nêu tên nguồn thông tin. Câu thường kết thúc bằng cách diễn đạt tin đồn như 〜そうだ, 〜らしい hoặc 〜ということだ.', '天気予報によると、週末は大雪になるそうだ。', '{天気|てんき}{予報|よほう}によると、{週末|しゅうまつ}は{大雪|おおゆき}になるそうだ。', 'Theo dự báo thời tiết, cuối tuần này sẽ có tuyết rơi dày đặc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('202', 'N3', '〜ばかりでなく', 'bakari de naku', 'Không chỉ... mà còn cả những đối tượng khác nữa', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + ばかりでなく', 'Ý nghĩa tương tự như 〜だけでなく nhưng trang trọng hơn một chút. Phần thứ hai thường chứa も hoặc まで.', 'この映画は子どもばかりでなく、大人も楽しめる。', 'この{映画|えいが}は{子|こ}どもばかりでなく、{大人|おとな}も{楽|たの}しめる。', 'Không chỉ trẻ em mà cả người lớn cũng có thể thưởng thức bộ phim này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('203', 'N3', '〜だけでなく', 'dake de naku', 'Không chỉ riêng... mà còn cả...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + だけでなく', 'Phổ biến trong cả lời nói và văn viết. Bình thường: 〜だけじゃなくて. Ít trang trọng hơn 〜ばかりでなく.', 'この店は料理がおいしいだけでなく、値段も安い。', 'この{店|みせ}は{料理|りょうり}がおいしいだけでなく、{値段|ねだん}も{安|やす}い。', 'Nhà hàng này không chỉ có đồ ăn ngon mà còn rẻ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('204', 'N3', '〜も〜ば〜も', 'mo ... ba ... mo', 'Vừa... lại vừa...; Cả... lẫn... đều', 'Danh từ + も + Động từ thể điều kiện (ば) + Danh từ + も / Danh từ + も + Tính từ đuôi い stem + ければ + Danh từ + も', 'Liệt kê hai sự kiện cùng loại để nhấn mạnh; các cặp tương phản (いいときも悪いときも) rất phổ biến. So sánh 〜し, trong đó liệt kê lý do.', '彼女は歌も歌えば、ピアノも弾く。', '{彼女|かのじょ}は{歌|うた}も{歌|うた}えば、ピアノも{弾|ひ}く。', 'Cô vừa hát vừa chơi piano.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('205', 'N3', '〜らしい（typical of）', 'rashii (typical of)', 'Đậm chất, đúng phong thái tiêu biểu của...', 'Danh từ + らしい', 'Có những phẩm chất mà bạn mong đợi ở danh từ. Phủ định: 〜らしくない (''không thích''). Khác với tin đồn 〜らしい (N4) và 〜っぽい (trông giống X nhưng không phải).', '今日は春らしい暖かい一日だった。', '{今日|きょう}は{春|はる}らしい{暖|あたた}かい{一|いち}{日|にち}だった。', 'Hôm nay là một ngày ấm áp, thực sự giống như mùa xuân.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('206', 'N3', '〜っぽい', 'ppoi', 'Có vẻ, hay có tính chất... (dễ nổi nóng, hay quên, màu nhờ nhờ)', 'Danh từ / Động từ thể ます (bỏ ます) / Tính từ đuôi い stem + っぽい', 'Danh từ + っぽい: trông giống X nhưng thực ra không phải (大人っぽい). Thân động từ + っぽい: có xu hướng (怒りっぽい). So sánh 〜らしい, có nghĩa là thực sự điển hình của X.', 'この服はちょっと子どもっぽいから、別のにしよう。', 'この{服|ふく}はちょっと{子|こ}どもっぽいから、{別|べつ}のにしよう。', 'Bộ trang phục này hơi trẻ con nên hãy chọn bộ khác nhé.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('207', 'N3', '〜的', 'teki', 'Mang tính chất, về mặt... (hậu tố biến danh từ thành tính từ)', 'Danh từ + 的な + Danh từ / Danh từ + 的に', 'Biến một danh từ (thường là chữ kanji) thành tính từ na: 的な trước danh từ, 的に trước động từ và tính từ. Nhiều từ được học tốt nhất dưới dạng từ: 具体的, 基本的, 積極的.', '日本の伝統的な料理を食べてみたい。', '{日本|にほん}の{伝統|でんとう}{的|てき}な{料理|りょうり}を{食|た}べてみたい。', 'Tôi muốn thử món ăn truyền thống của Nhật Bản.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('208', 'N3', '〜ような / 〜ように（like）', 'you na / you ni (like)', 'Giống như là, như thể là... (so sánh ví von hình tượng)', 'Động từ thể thông thường (V ngắn) / Danh từ + の + ような / ように', 'ような đứng trước danh từ, ように đứng trước động từ và tính từ. AのようなB cũng có thể có nghĩa là ''B chẳng hạn như A''. Bình thường: 〜みたいな / 〜みたいに.', '私も、あなたのような優しい看護師になりたいです。', '{私|わたし}も、あなたのような{優|やさ}しい{看護|かんご}{師|し}になりたいです。', 'Em cũng muốn trở thành một y tá tốt bụng như chị.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('209', 'N3', 'まるで〜ようだ', 'marude ... you da', 'Cứ như thể là, hoàn toàn giống như...', 'まるで + Động từ thể thông thường (V ngắn) / Danh từ + の + ようだ', 'まるで báo hiệu một sự so sánh mang tính biểu tượng: nó chỉ có vẻ như vậy thôi. Bình thường: まるで〜みたいだ. So sánh N2 〜かのようだ.', 'この部屋は広くて明るくて、まるでホテルのようだ。', 'この{部屋|へや}は{広|ひろ}くて{明|あか}るくて、まるでホテルのようだ。', 'Căn phòng này rất rộng và sáng sủa, giống như một khách sạn vậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('210', 'N3', '〜に違いない', 'ni chigai nai', 'Chắc chắn là, không thể sai được (độ tin cậy gần như tuyệt đối)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + に違いない', 'Niềm tin mạnh mẽ dựa trên bằng chứng hoặc trực giác; một giọng điệu hơi viết. So sánh 〜に決まっている, cái nào mang tính chủ quan và mang tính đối thoại hơn.', '隣の家は何日も電気が消えている。家族で旅行に出かけたに違いない。', '{隣|となり}の{家|いえ}は{何|なん}{日|にち}も{電気|でんき}が{消|き}えている。{家族|かぞく}で{旅行|りょこう}に{出|で}かけたに{違|ちが}いない。', 'Đèn bên cạnh đã tắt từ nhiều ngày. Chắc hẳn gia đình đã đi du lịch.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('211', 'N3', '〜に決まっている', 'ni kimatte iru', 'Nhất định là, chắc chắn là... (khẳng định mang tính trực giác)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + に決まっている', 'Một phán đoán rất tự tin, chủ quan, thường gặp trong hội thoại (〜に決まってる). Quyết đoán hơn 〜に違いない và không phù hợp với cách viết trang trọng.', '練習もしないで試合に出たら、負けるに決まっている。', '{練習|れんしゅう}もしないで{試合|しあい}に{出|で}たら、{負|ま}けるに{決|き}まっている。', 'Nếu bạn chơi trận đấu mà không luyện tập, bạn chắc chắn sẽ thua.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('212', 'N3', '〜気がする', 'ki ga suru', 'Có cảm giác là, dường như là...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な + 気がする / Động từ thể thông thường (V ngắn) / Danh từ + の + ような + 気がする', 'Một ấn tượng trực quan, mơ hồ hơn là một phán đoán chắc chắn; ような気がする thậm chí còn nhẹ nhàng hơn. Khác với N4 〜がする (mùi, âm thanh hoặc vị).', 'この人とは、どこかで会ったことがある気がする。', 'この{人|ひと}とは、どこかで{会|あ}ったことがある{気|き}がする。', 'Tôi có cảm giác như đã gặp người này ở đâu rồi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('213', 'N3', '〜ずに', 'zu ni', 'Mà không làm việc gì đó (dạng văn viết trang trọng của ないで)', 'Động từ thể ない (bỏ ない) + ずに', 'Trang trọng hơn, viết theo phong cách tương đương với 〜ないで. Bất quy tắc: する → せずに (không phải しずに), 来る → 来ずに (こずに).', 'けさは朝ご飯を食べずに家を出た。', 'けさは{朝|あさ}ご{飯|はん}を{食|た}べずに{家|いえ}を{出|で}た。', 'Sáng nay tôi rời nhà mà không ăn sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('214', 'N3', '〜まま', 'mama', 'Cứ giữ nguyên tình trạng như thế mà tiếp tục làm việc khác', 'Động từ thể た (quá khứ) / Động từ thể ない / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + まま', 'Một thay đổi dự kiến ​​đã không xảy ra và trạng thái vẫn tiếp tục. Thường gợi ý điều gì đó bất thường hoặc không mong muốn (để đèn sáng, mang giày vào trong nhà).', 'エアコンをつけたまま寝てしまい、風邪をひいた。', 'エアコンをつけたまま{寝|ね}てしまい、{風邪|かぜ}をひいた。', 'Tôi ngủ quên khi bật điều hòa và bị cảm lạnh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('215', 'N3', '〜うちに', 'uchi ni', 'Trong khi còn... (tranh thủ làm trước khi điều kiện thay đổi); Trong lúc đang...', 'Động từ thể từ điển (辞書形) / Động từ thể て + いる / Động từ thể ない / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + うちに', 'Hãy làm điều gì đó khi điều kiện còn tồn tại, trước khi nó thay đổi; hoặc một sự thay đổi xảy ra mà không được chú ý trong một hoạt động. So sánh 〜間に, đánh dấu một khoảng thời gian cố định mà không có tính khẩn cấp.', '若いうちに、いろいろな国を旅行したほうがいい。', '{若|わか}いうちに、いろいろな{国|くに}を{旅行|りょこう}したほうがいい。', 'Bạn nên đi du lịch đến nhiều nước khi còn trẻ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('216', 'N3', '〜ところに / 〜ところへ / 〜ところを', 'tokoro ni / tokoro e / tokoro o', 'Đúng vào thời điểm đang làm việc đó thì có sự việc khác tác động', 'Động từ thể từ điển (辞書形) / Động từ thể て + いる / Động từ thể た (quá khứ) + ところに / ところへ / ところを', 'ところに/へ: có chuyện gì đó xảy ra hoặc có người đến đúng lúc, thường làm gián đoạn. ところを: cảnh là tân ngữ của động từ như 見る, 見つかる, 助ける. Đặt cụm từ: お忙しいところを.', '出かけようとしているところに、友達から電話がかかってきた。', '{出|で}かけようとしているところに、{友達|ともだち}から{電話|でんわ}がかかってきた。', 'Khi tôi vừa định ra ngoài thì một người bạn gọi đến.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('217', 'N3', '〜ば〜ほど', 'ba ... hodo', 'Càng... thì lại càng...', 'Động từ thể điều kiện (ば) + Động từ thể từ điển (辞書形) + ほど / Tính từ đuôi い stem + ければ + Tính từ đuôi い + ほど / Tính từ đuôi な + なら + Tính từ đuôi な + な + ほど', 'Từ đó được lặp lại, đầu tiên ở dạng ば/なら, sau đó ở dạng thông thường. Phần đầu tiên có thể bỏ đi: 考えるほど分からなくなる.', '日本語は、勉強すればするほど面白くなる。', '{日本語|にほんご}は、{勉強|べんきょう}すればするほど{面白|おもしろ}くなる。', 'Càng học tiếng Nhật càng thấy thú vị.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('218', 'N3', '〜ほど', 'hodo', 'Đến mức, đến độ...; Càng... thì càng...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + ほど', 'Hiển thị mức độ với một ví dụ hoặc so sánh. Thường có thể hoán đổi với 〜くらい, nhưng ほど nghe trang trọng hơn và phù hợp với mức độ cao hơn; nó không thể đánh dấu một cái gì đó là tối thiểu.', '昨日は、立っていられないほど疲れていた。', '{昨日|きのう}は、{立|た}っていられないほど{疲|つか}れていた。', 'Hôm qua tôi mệt đến mức gần như không thể đứng vững được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('219', 'N3', '〜くらい / 〜ぐらい（extent）', 'kurai / gurai (extent)', 'Đến mức, cỡ như là... (chỉ mức độ cao)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + くらい', 'Giống như 〜ほど, thể hiện mức độ. くらい cũng có thể đánh dấu một việc gì đó nhỏ nhặt hoặc dễ dàng (''ít nhất; một việc đơn giản như vậy''), cách sử dụng ほど không có.', 'おなかが痛くて、歩けないくらいだった。', 'おなかが{痛|いた}くて、{歩|ある}けないくらいだった。', 'Bụng tôi đau đến nỗi tôi gần như không thể đi lại được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('220', 'N3', '〜さえ', 'sae', 'Ngay cả, thậm chí đến cả... (đưa ra ví dụ cực đoan)', 'Danh từ + さえ / Danh từ + でさえ / Danh từ + にさえ', 'Đưa ra một ví dụ cực đoan, ngụ ý ''tất nhiên những người khác cũng vậy''. Trang trọng hơn 〜も hoặc 〜でも theo nghĩa này. Là chủ ngữ: 〜でさえ. So sánh N1 〜すら.', '忙しくて、昼ご飯を食べる時間さえなかった。', '{忙|いそが}しくて、{昼|ひる}ご{飯|はん}を{食|た}べる{時間|じかん}さえなかった。', 'Tôi bận đến mức không có thời gian ăn trưa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('221', 'N3', '〜さえ〜ば', 'sae ... ba', 'Chỉ cần... là đủ, mọi chuyện sẽ tốt đẹp', 'Danh từ + さえ + Động từ thể điều kiện (ば) / Động từ thể ます (bỏ ます) + さえ + すれば / Động từ thể て + さえ + いれば', 'Nhấn mạnh rằng chỉ riêng điều kiện này là đủ hoặc là điều kiện duy nhất còn thiếu.', 'このアプリさえあれば、どこでも日本語の勉強ができる。', 'このアプリさえあれば、どこでも{日本語|にほんご}の{勉強|べんきょう}ができる。', 'Chỉ cần có ứng dụng này là bạn có thể học tiếng Nhật ở bất cứ đâu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('222', 'N3', '〜こそ', 'koso', 'Chính là... (nhấn mạnh đối tượng, từ chối sự nhầm lẫn)', 'Danh từ + こそ', 'Chọn ra một điều là điều quan trọng nhất. こちらこそ là một câu trả lời cố định: ''cảm ơn/niềm vui là của tôi''. Các dạng N2 liên quan: 〜からこそ, 〜てこそ.', '「いつもありがとう。」「いえ、こちらこそ。」', NULL, '''Cảm ơn vì tất cả mọi thứ.'' ''Không, cảm ơn bạn!''')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('223', 'N3', '〜として', 'to shite', 'Với tư cách là, trên cương vị là...', 'Danh từ + として / Danh từ + としての + Danh từ', 'Nêu rõ vai trò, trạng thái hoặc danh mục. So sánh 〜にとって, thể hiện sự phán xét được đưa ra từ quan điểm của ai.', '兄は、エンジニアとしてアメリカの会社で働いている。', '{兄|あに}は、エンジニアとしてアメリカの{会社|かいしゃ}で{働|はたら}いている。', 'Anh trai tôi làm kỹ sư ở một công ty Mỹ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('224', 'N3', '〜にとって', 'ni totte', 'Đối với bản thân ai đó (nhìn nhận từ góc độ của người đó)', 'Danh từ + にとって / Danh từ + にとっての + Danh từ', 'Tiếp theo là sự đánh giá (大切だ, 難しい, 便利だ). Không dùng cho hành động hay thái độ đối với ai đó; sử dụng 〜に対して cho những điều đó.', '私にとって、家族は何よりも大切なものです。', '{私|わたし}にとって、{家族|かぞく}は{何|なに}よりも{大切|たいせつ}なものです。', 'Với tôi, gia đình quan trọng hơn bất cứ điều gì.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('225', 'N3', '〜にしては', 'ni shite wa', 'Xét theo... thì quả là bất ngờ (trái với mức trung bình dự kiến)', 'Động từ thể thông thường (V ngắn) / Danh từ + にしては', 'Kết quả khác với những gì bạn mong đợi ở X. Không theo sau tính từ; so sánh 〜わりに, có thể và tập trung vào mức độ (giá cả, độ tuổi, nỗ lực).', '初めて作ったにしては、このケーキはよくできている。', '{初|はじ}めて{作|つく}ったにしては、このケーキはよくできている。', 'Lần thử đầu tiên, chiếc bánh này thực sự rất ngon.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('226', 'N3', '〜にしても', 'ni shite mo', 'Cho dù là... đi chăng nữa thì cũng...', 'Động từ thể thông thường (V ngắn) / Danh từ + にしても', 'Chấp nhận một quan điểm và nói rằng điều đó không làm thay đổi quan điểm của người nói. Thường chấp nhận một sự thật; so sánh 〜としても, nó thuần túy mang tính giả thuyết hơn.', '忙しかったにしても、電話一本ぐらいできたでしょう。', '{忙|いそが}しかったにしても、{電話|でんわ}{一本|いっぽん}ぐらいできたでしょう。', 'Ngay cả khi bạn bận, ít nhất bạn cũng có thể gọi một cuộc điện thoại.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('227', 'N3', '〜に関して / 〜に関する', 'ni kanshite / ni kansuru', 'Về, liên quan tới vấn đề... (cách nói trang trọng hơn của について)', 'Danh từ + に関して / Danh từ + に関する + Danh từ', 'Trang trọng hơn 〜について; phổ biến trong kinh doanh, tin tức và văn bản. Trước danh từ, dùng に関する (hoặc に関しての).', '新しいサービスに関してご質問がある方は、受付までどうぞ。', '{新|あたら}しいサービスに{関|かん}してご{質問|しつもん}がある{方|かた}は、{受付|うけつけ}までどうぞ。', 'Mọi thắc mắc về dịch vụ mới vui lòng đến quầy lễ tân.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('228', 'N3', '〜に比べて', 'ni kurabete', 'So với... thì...', 'Danh từ + に比べて / Động từ thể từ điển (辞書形) + のに比べて', 'Ngoài ra còn có 〜と比べて, 〜に比べると và 〜に比べれば. Đặt hai thứ cạnh nhau rõ ràng hơn 〜より.', '今年の夏は、去年に比べてずいぶん涼しい。', '{今年|ことし}の{夏|なつ}は、{去年|きょねん}に{比|くら}べてずいぶん{涼|すず}しい。', 'Mùa hè năm nay mát mẻ hơn nhiều so với năm ngoái.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('229', 'N3', '〜に代わって', 'ni kawatte', 'Thay mặt cho, đại diện cho ai đó làm việc gì', 'Danh từ + に代わって / Danh từ + に代わり', 'Ai đó hành động vì người khác, hoặc một cái gì đó mới thay thế một cái gì đó cũ. Dạng viết: 〜に代わり. So sánh 〜代わりに (thay vì làm; đổi lấy).', '入院中の社長に代わって、副社長があいさつをした。', '{入院|にゅういん}{中|ちゅう}の{社長|しゃちょう}に{代|か}わって、{副|ふく}{社長|しゃちょう}があいさつをした。', 'Phó tổng thống thay mặt tổng thống đang nằm viện phát biểu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('230', 'N3', '〜代わりに', 'kawari ni', 'Thay vì... / Đổi lại, bù lại thì...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + 代わりに', 'Ba cách sử dụng: thay thế (thay vì), trao đổi (đổi lại) và bù đắp (một nhược điểm được cân bằng bởi một nhược điểm).', '今日は外で食べる代わりに、家でカレーを作った。', '{今日|きょう}は{外|そと}で{食|た}べる{代|か}わりに、{家|いえ}でカレーを{作|つく}った。', 'Hôm nay thay vì đi ăn ngoài, tôi nấu cà ri ở nhà.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('231', 'N3', '〜に従って', 'ni shitagatte', 'Cùng với sự biến đổi của... kéo theo; Tuân theo quy định', 'Danh từ + に従って / Động từ thể từ điển (辞書形) + に従って', 'Với những danh từ như quy tắc hay chỉ dẫn: ''theo sau, tuân theo''. Với Verb-dict: thay đổi tỷ lệ như 〜につれて, với giọng trang trọng hơn.', '係の人の指示に従って、落ち着いて避難してください。', '{係|かかり}の{人|ひと}の{指示|しじ}に{従|したが}って、{落|お}ち{着|つ}いて{避難|ひなん}してください。', 'Hãy sơ tán một cách bình tĩnh, làm theo hướng dẫn của nhân viên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('232', 'N3', '〜につれて', 'ni tsurete', 'Cùng với việc... kéo theo... cũng biến đổi song hành', 'Động từ thể từ điển (辞書形) / Danh từ + につれて', 'Cả hai nửa đều mô tả sự thay đổi dần dần. Mệnh đề chính không thể là một yêu cầu hoặc ý định của người nói. Gần với 〜に従って và 〜とともに.', '年をとるにつれて、朝早く目が覚めるようになった。', '{年|とし}をとるにつれて、{朝|あさ}{早|はや}く{目|め}が{覚|さ}めるようになった。', 'Khi tôi già đi, tôi bắt đầu dậy sớm vào buổi sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('233', 'N3', '〜とともに', 'to tomo ni', 'Cùng với...; Đồng thời với việc...', 'Danh từ + とともに / Động từ thể từ điển (辞書形) + とともに', 'Phiên bản chính thức của 〜と一緒に. Đồng thời đánh dấu các sự kiện xảy ra cùng lúc hoặc thay đổi tỷ lệ như 〜につれて.', '社長は、社員とともに新しい工場の完成を喜んだ。', '{社長|しゃちょう}は、{社員|しゃいん}とともに{新|あたら}しい{工場|こうじょう}の{完成|かんせい}を{喜|よろこ}んだ。', 'Chủ tịch công ty đã tổ chức lễ hoàn thành nhà máy mới cùng với các nhân viên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('234', 'N3', '〜たびに', 'tabi ni', 'Mỗi lần, mỗi khi làm việc gì đó thì lại đều xảy ra...', 'Động từ thể từ điển (辞書形) / Danh từ + の + たびに', 'Mỗi lần A xảy ra thì B cũng xảy ra. Nhấn mạnh hơn 〜と hoặc 〜ときはいつも.', 'この写真を見るたびに、大学時代を思い出す。', 'この{写真|しゃしん}を{見|み}るたびに、{大学|だいがく}{時代|じだい}を{思|おも}い{出|だ}す。', 'Mỗi lần nhìn bức ảnh này tôi lại nhớ về thời đại học của mình.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('235', 'N3', '〜ごとに', 'goto ni', 'Cứ mỗi... lại một lần (chu kỳ lặp lại)', 'Danh từ + ごとに / Động từ thể từ điển (辞書形) + ごとに', 'Lặp lại đều đặn hoặc lần lượt từng đơn vị. Với Verb-dict nó gần với 〜たびに. Cẩn thận: 一日ごとに = hàng ngày, nhưng 一日おきに (N4) = cách ngày.', 'このバスは十五分ごとに出ています。', 'このバスは{十|じゅう}{五|ご}{分|ふん}ごとに{出|で}ています。', 'Xe buýt này khởi hành cứ sau mười lăm phút.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('236', 'N3', '〜最中に', 'saichuu ni', 'Đúng vào lúc đang cao điểm làm việc gì đó thì xảy ra sự cố bất ngờ', 'Động từ thể て + いる / Danh từ + の + 最中に', 'Nhấn mạnh mức độ cao của một hoạt động, thường là khi có điều gì đó bất ngờ làm gián đoạn hoạt động đó. Mạnh hơn 〜中に hoặc 〜ている間に. Ngoài ra 〜最中だ.', '大事な会議の最中に、携帯電話が鳴ってしまった。', '{大事|だいじ}な{会議|かいぎ}の{最中|さいちゅう}に、{携帯|けいたい}{電話|でんわ}が{鳴|な}ってしまった。', 'Điện thoại di động của tôi tắt ngay giữa một cuộc họp quan trọng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('237', 'N3', '〜ついでに', 'tsuide ni', 'Nhân tiện làm việc này thì làm thêm việc kia', 'Động từ thể từ điển (辞書形) / Động từ thể た (quá khứ) / Danh từ + の + ついでに', 'Hành động chính vẫn xảy ra; cái thứ hai được thêm vào vì nó thuận tiện. So sánh N1 〜がてら.', '郵便局へ行くついでに、この手紙も出してきてくれる？', '{郵便|ゆうびん}{局|きょく}へ{行|い}くついでに、この{手紙|てがみ}も{出|だ}してきてくれる？', 'Vì bạn sắp đến bưu điện nên bạn có thể gửi lá thư này được không?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('238', 'N3', '〜おかげで', 'okage de', 'Nhờ có, nhờ ơn... (chỉ nguyên nhân đem lại kết quả tốt đẹp)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + おかげで', 'Nguyên nhân của một kết quả tốt đẹp, với một cảm giác biết ơn. Đôi khi được sử dụng một cách mỉa mai khi mang lại kết quả xấu (君のおかげで遅刻したよ). So sánh 〜せいで (đổ lỗi).', '先輩が手伝ってくれたおかげで、仕事が早く終わった。', '{先輩|せんぱい}が{手伝|てつだ}ってくれたおかげで、{仕事|しごと}が{早|はや}く{終|お}わった。', 'Nhờ sự giúp đỡ của đồng nghiệp cấp trên nên công việc đã hoàn thành sớm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('239', 'N3', '〜せいで', 'sei de', 'Tại vì, do... (chỉ nguyên nhân dẫn tới hậu quả xấu, oán trách)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + せいで', 'Nguyên nhân dẫn đến kết quả xấu, đổ lỗi. 〜のせいにする = ''đổ lỗi cho điều gì đó''. Cảm giác trái ngược với 〜おかげで.', '電車が遅れたせいで、面接に遅刻してしまった。', '{電車|でんしゃ}が{遅|おく}れたせいで、{面接|めんせつ}に{遅刻|ちこく}してしまった。', 'Vì chuyến tàu bị trễ nên tôi đã đến cuộc phỏng vấn muộn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('240', 'N3', '〜たとたん / 〜たとたんに', 'ta totan / ta totan ni', 'Ngay sau khi vừa mới... thì bỗng nhiên xảy ra điều bất ngờ ngoài dự kiến', 'Động từ thể た (quá khứ) + とたん / とたんに', 'Sự kiện thứ hai xảy ra đột ngột và nằm ngoài tầm kiểm soát của người nói. Không được sử dụng cho các hành động, yêu cầu hoặc ý định đã được lên kế hoạch của chính người nói.', '立ち上がったとたんに、めまいがした。', '{立|た}ち{上|あ}がったとたんに、めまいがした。', 'Lúc tôi đứng dậy, tôi cảm thấy choáng váng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('241', 'N3', '〜たら〜た', 'tara ... ta', 'Sau khi làm... thì phát hiện ra kết quả bất ngờ', 'Động từ thể た (quá khứ) + ら ... Động từ thể た (quá khứ)', 'Sau một hành động, người nói nhận thấy hoặc phát hiện ra điều gì đó. Mệnh đề thứ hai là quá khứ và nằm ngoài tầm kiểm soát của người nói. 〜と〜た là từ viết tương đương hơn.', '窓を開けたら、外は一面の雪だった。', '{窓|まど}を{開|あ}けたら、{外|そと}は{一|いち}{面|めん}の{雪|ゆき}だった。', 'Khi tôi mở cửa sổ, mọi thứ bên ngoài đều bị tuyết bao phủ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('242', 'N3', '〜たって / 〜だって', 'tatte / datte', 'Cho dù... đi chăng nữa (thể thân mật văn nói của ても/でも)', 'Động từ thể た (quá khứ) + って / Tính từ đuôi い-ku + たって / Tính từ đuôi な / Danh từ + だって', 'Dạng thông tục của 〜ても / 〜でも, thường đi kèm với いくら hoặc từ để hỏi. だって cũng có nghĩa là ''vì'' ở đầu câu và ''thậm chí'' sau danh từ.', '今から急いだって、もう間に合わないよ。', '{今|いま}から{急|いそ}いだって、もう{間|ま}に{合|あ}わないよ。', 'Cho dù bây giờ bạn có nhanh lên, bạn cũng sẽ không thể làm được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('243', 'N3', '〜ないことはない', 'nai koto wa nai', 'Không phải là không... (phủ định kép: thật ra là có thể)', 'Động từ thể ない / Tính từ đuôi い-ku + ない / Tính từ đuôi な + でない + ことはない', 'Một sự phủ định kép đưa ra một tiếng ''có'' do dự. Ngoài ra 〜ないこともない. Đừng nhầm lẫn với Động từ-dict + ことはない (''không cần đâu'').', '辛い料理は食べられないことはないけど、あまり好きじゃない。', '{辛|から}い{料理|りょうり}は{食|た}べられないことはないけど、あまり{好|す}きじゃない。', 'Không phải là tôi không ăn được cay mà là tôi không thích lắm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('244', 'N3', '〜てしょうがない / 〜てしかたがない', 'te shou ga nai / te shikata ga nai', 'Vô cùng, không biết phải làm sao (cảm xúc không kìm nén được)', 'Động từ thể て / Tính từ đuôi い-ku + て / Tính từ đuôi な + で + しょうがない / しかたがない', 'Đối với những cảm giác và cảm giác mà người nói không thể kiểm soát được. しょうがない thì bình thường hơn. Không giống như 〜てたまらない, nó cũng có tác dụng với các động từ tự phát như 気になる hoặc 涙が出る.', '昨日あまり寝ていないので、眠くてしょうがない。', '{昨日|きのう}あまり{寝|ね}ていないので、{眠|ねむ}くてしょうがない。', 'Đêm qua tôi gần như không ngủ được nên buồn ngủ kinh khủng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('245', 'N3', '〜てたまらない', 'te tamaranai', 'Rất, không thể chịu nổi (cảm giác cơ thể hoặc khát khao mãnh liệt)', 'Động từ thể て / Tính từ đuôi い-ku + て / Tính từ đuôi な + で + たまらない', 'Cảm giác mạnh mẽ, trực tiếp hoặc cảm giác cơ thể; rất phổ biến với ham muốn (〜たくてたまらない). Không được sử dụng cho sự thật khách quan. So sánh 〜てしかたがない.', 'この部屋はエアコンがなくて、暑くてたまらない。', 'この{部屋|へや}はエアコンがなくて、{暑|あつ}くてたまらない。', 'Phòng này không có máy lạnh; nóng không chịu nổi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('246', 'N3', '〜てはじめて', 'te hajimete', 'Kể từ sau khi... thì mới nhận ra, mới bắt đầu hiểu được', 'Động từ thể て + はじめて', 'Điều gì đó chỉ được nhận ra hoặc trở thành khả thi sau một trải nghiệm; thường theo sau là 分かる, 気づく hoặc 知る. Cũng được viết là 〜て初めて.', '病気になってはじめて、健康のありがたさが分かった。', '{病気|びょうき}になってはじめて、{健康|けんこう}のありがたさが{分|わ}かった。', 'Chỉ sau khi bị bệnh tôi mới cảm nhận được sức khỏe tốt quý giá như thế nào.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('247', 'N3', '〜てばかりいる', 'te bakari iru', 'Suốt ngày chỉ toàn làm việc gì đó (mang ý trách móc, phàn nàn)', 'Động từ thể て + ばかりいる', 'Một hành động lặp đi lặp lại hoặc liên tục, thường kèm theo sự chỉ trích hoặc không hài lòng. So sánh Danh từ N4 + ばかり (甘い物ばかり食べる).', '弟は休みの日、ゲームをしてばかりいる。', '{弟|おとうと}は{休|やす}みの{日|ひ}、ゲームをしてばかりいる。', 'Vào những ngày nghỉ, em trai tôi không làm gì khác ngoài chơi game.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('248', 'N3', '〜ばいい', 'ba ii', 'Nên làm gì đó thì tốt / Ước gì...', 'Động từ thể điều kiện (ば) + いい / Tính từ đuôi い stem + ければ + いい', 'Yêu cầu hoặc đưa ra lời khuyên (〜ばいいですか = ''tôi nên làm gì?''). 〜ばいいのに = ''Tôi ước gì''. 〜たらいい là từ gần đồng nghĩa; 〜ほうがいい nghe có vẻ kiên quyết hơn.', '「この書類、どこに出せばいいですか。」「二階の受付に出してください。」', '「この{書類|しょるい}、どこに{出|だ}せばいいですか。」「{二|に}{階|かい}の{受付|うけつけ}に{出|だ}してください。」', '''Tôi nên nộp đơn này ở đâu?'' ''Xin vui lòng mang nó đến bàn tiếp tân trên tầng hai.''')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('249', 'N3', '〜場合（は）', 'baai wa', 'Trong trường hợp, khi xảy ra tình huống...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + 場合（は）', 'Điển hình của các hướng dẫn, quy tắc và thông báo về các tình huống có thể xảy ra. Không giống như 〜たら, nó không được sử dụng để kể lại một sự kiện xảy ra một lần trong quá khứ.', '雨の場合は、試合は中止になります。', '{雨|あめ}の{場合|ばあい}は、{試合|しあい}は{中止|ちゅうし}になります。', 'Trong trường hợp trời mưa, trận đấu sẽ bị hủy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('250', 'N3', '〜とすれば / 〜としたら', 'to sureba / to shitara', 'Nếu giả sử là... thì sẽ thế nào?', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + だ / Danh từ + だ + とすれば / としたら', 'Đặt ra một giả thuyết và xem xét những gì tiếp theo. Thường dùng với もし hoặc 仮に. としたら mang tính đàm thoại nhiều hơn; とすれば nghe giống lý luận logic hơn.', 'もし宝くじで一億円当たったとしたら、何に使いますか。', 'もし{宝|たから}くじで{一|いち}{億|おく}{円|えん}{当|あ}たったとしたら、{何|なに}に{使|つか}いますか。', 'Nếu bạn trúng xổ số 100 triệu yên, bạn sẽ tiêu số tiền đó vào việc gì?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('251', 'N3', '〜としても', 'to shite mo', 'Ngay cả khi giả định là... thì cũng...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + だ / Danh từ + だ + としても', 'Nhượng bộ giả định: ngay cả khi giả sử A, B vẫn đúng. Nhiều giả thuyết hơn 〜ても; so sánh 〜にしても, thường chấp nhận một sự thật.', '親に反対されたとしても、私は留学するつもりだ。', '{親|おや}に{反対|はんたい}されたとしても、{私|わたし}は{留学|りゅうがく}するつもりだ。', 'Dù bố mẹ có phản đối thì tôi vẫn có ý định đi du học.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('252', 'N3', '〜とか（hearsay）', 'toka (hearsay)', 'Nghe nói là, hình như là... (truyền đạt thông tin chưa kiểm chứng)', 'Mệnh đề / Câu + とか', 'Tin đồn mơ hồ; nhẹ nhàng hơn và ít chắc chắn hơn 〜そうだ. Thường là 〜とかいう話だ, 〜とか聞いた, hoặc 〜とかで (đưa ra lý do được báo cáo). Khác với việc liệt kê 〜とか〜とか.', '駅前の本屋、今月で閉店するとか。寂しくなりますね。', '{駅前|えきまえ}の{本屋|ほんや}、{今月|こんげつ}で{閉店|へいてん}するとか。{寂|さび}しくなりますね。', 'Tôi nghe nói hiệu sách gần nhà ga sẽ đóng cửa vào cuối tháng này. Sẽ rất buồn khi thấy nó biến mất.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('253', 'N3', '〜って', 'tte', 'Rằng là...; Nói là... (rút gọn của と/というのは/と聞いている)', 'Mệnh đề / Câu + って / Danh từ + って', 'Thay thế thông thường cho と (trích dẫn), そうだ (tin đồn, thường là 〜んだって) và は (chủ đề, đặc biệt là ''X là gì?''). Phổ biến trong lời nói; tránh trong văn bản chính thức.', '彼女、今日は来られないって。', '{彼女|かのじょ}、{今日|きょう}は{来|こ}られないって。', 'Cô ấy nói hôm nay cô ấy không thể đến được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('254', 'N3', '〜ということだ', 'to iu koto da', 'Nghĩa là, nghe nói là... (truyền đạt lại nội dung hoặc giải thích ý)', 'Mệnh đề / Câu + ということだ', 'Hai cách sử dụng: báo cáo thông tin (trang trọng hơn 〜そうだ) và đưa ra kết luận (''vậy điều đó có nghĩa là ...''). Việc sử dụng kết thúc thường kết thúc bằng ということですね.', '駅員さんの話では、事故のため電車は一時間ほど止まるということです。', '{駅員|えきいん}さんの{話|はなし}では、{事故|じこ}のため{電車|でんしゃ}は{一|いち}{時間|じかん}ほど{止|と}まるということです。', 'Theo nhân viên nhà ga, tàu sẽ phải dừng khoảng 1 giờ vì xảy ra tai nạn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('255', 'N3', '〜というのは', 'to iu no wa', 'Cái gọi là... có nghĩa là... (định nghĩa khái niệm)', 'Danh từ / Mệnh đề / Câu + というのは', 'Giới thiệu một từ để định nghĩa hoặc một tình huống để nhận xét. Bình thường: 〜って. Khi bắt đầu câu, というのは giới thiệu lý do kết thúc bằng 〜からだ. Được viết: 〜とは.', '「割り勘」というのは、食事代などをみんなで同じ金額ずつ払うことです。', '「{割|わ}り{勘|かん}」というのは、{食事|しょくじ}{代|だい}などをみんなで{同|おな}じ{金額|きんがく}ずつ{払|はら}うことです。', '''Warikan'' có nghĩa là mọi người đều trả phần như nhau cho một bữa ăn hoặc hóa đơn khác.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('256', 'N3', '〜というより', 'to iu yori', 'Nói đúng hơn là... thay vì nói là...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + というより', 'Sửa hoặc tinh chỉnh mô tả: mô tả thứ hai phù hợp hơn. Thường theo sau là むしろ.', '今日は涼しいというより、寒いくらいだ。', '{今日|きょう}は{涼|すず}しいというより、{寒|さむ}いくらいだ。', 'Hôm nay trời không mát đến mức gần như lạnh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('257', 'N3', '〜わりに / 〜わりには', 'wari ni / wari ni wa', 'So với... thì quả là khác biệt (vượt ngoài dự đoán)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + わりに', 'Kết quả không khớp với mức độ bạn mong đợi từ A (giá, tuổi, nỗ lực). Có thể theo sau tính từ, không giống như 〜にしては.', 'この店は値段のわりに、量が多くておいしい。', 'この{店|みせ}は{値段|ねだん}のわりに、{量|りょう}が{多|おお}くておいしい。', 'Về giá cả, nhà hàng này mang đến cho bạn những phần ăn lớn và ngon.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('258', 'N3', '〜くせに', 'kuse ni', 'Thế mà, vậy mà lại... (mỉa mai, trách móc đối tượng)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + くせに', 'Giống như 〜のに, nhưng với sự khinh miệt hoặc trách móc đối với chủ ngữ. Cả hai mệnh đề đều có cùng một chủ đề; hiếm khi được sử dụng về bản thân. Bình thường.', '自分では何もしないくせに、人の仕事には文句ばかり言う。', '{自分|じぶん}では{何|なに}もしないくせに、{人|ひと}の{仕事|しごと}には{文句|もんく}ばかり{言|い}う。', 'Anh ấy không bao giờ tự mình làm bất cứ điều gì nhưng lại luôn phàn nàn về công việc của người khác.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('259', 'N3', '〜から〜にかけて', 'kara ... ni kakete', 'Từ... đến... (khoảng thời gian hoặc không gian tương đối)', 'Danh từ + から + Danh từ + にかけて', 'Một khoảng thời gian hoặc không gian gồ ghề có các cạnh không chính xác; phổ biến trong các bản tin thời tiết. So sánh 〜から〜まで, đánh dấu điểm bắt đầu và điểm kết thúc rõ ràng.', '今夜から明日の朝にかけて、強い雨が降るでしょう。', '{今夜|こんや}から{明日|あした}の{朝|あさ}にかけて、{強|つよ}い{雨|あめ}が{降|ふ}るでしょう。', 'Dự kiến ​​sẽ có mưa lớn từ đêm nay đến sáng mai.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('260', 'N3', '〜ぶり', 'buri', 'Sau suốt một khoảng thời gian dài mới lại làm việc gì', 'Danh từ + ぶり / Động từ thể ます (bỏ ます) + ぶり', 'Khoảng thời gian + ぶり: lần đầu tiên trong thời gian dài như vậy (〜ぶりに / 〜ぶりの). Danh từ hoặc Thân động từ + ぶり: cách ai đó làm việc gì đó (仕事ぶり, 話しぶり).', '五年ぶりに高校の友達に会って、話が止まらなかった。', '{五|ご}{年|ねん}ぶりに{高校|こうこう}の{友達|ともだち}に{会|あ}って、{話|はなし}が{止|と}まらなかった。', 'Tôi gặp lại những người bạn trung học của mình lần đầu tiên sau 5 năm và chúng tôi không thể ngừng nói chuyện.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('261', 'N3', '〜っけ', 'kke', 'Có phải là... không nhỉ? (hỏi lại khi quên hoặc cố nhớ lại)', 'Động từ thể た (quá khứ) / Danh từ + だ / Danh từ + だった + っけ', 'Kiểm tra điều gì đó bạn đã quên hoặc nhớ lại quá khứ một cách trìu mến (〜たっけ). Hình thức lịch sự: 〜でしたっけ, 〜ましたっけ.', 'あれ、会議って何時からだっけ？', 'あれ、{会議|かいぎ}って{何|なん}{時|じ}からだっけ？', 'Chờ đã, mấy giờ cuộc họp lại bắt đầu?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('262', 'N3', '〜ますように', 'masu you ni', 'Cầu chúc cho, mong sao cho... (lời cầu nguyện, chúc phúc)', 'Động từ thể ます (bỏ ます) + ますように / Động từ thể ます (bỏ ます) + ませんように', 'Được sử dụng cho những lời chúc và lời cầu nguyện, ví dụ: tại một ngôi đền hoặc trên dải tanabata, và ở cuối bức thư chúc ai đó mọi điều tốt lành.', '試験に合格できますように。', '{試験|しけん}に{合格|ごうかく}できますように。', 'Tôi hy vọng tôi vượt qua kỳ thi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('263', 'N3', '〜ふりをする', 'furi o suru', 'Giả vờ như là, làm bộ làm tịch như thể...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + ふりをする', 'Cố tình tạo ấn tượng sai. Động từ-ta phổ biến: 寝たふり, 死んだふり.', '母に呼ばれたが、聞こえないふりをしてゲームを続けた。', '{母|はは}に{呼|よ}ばれたが、{聞|き}こえないふりをしてゲームを{続|つづ}けた。', 'Mẹ gọi điện cho tôi nhưng tôi giả vờ như không nghe thấy và tiếp tục chơi game.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('264', 'N3', '〜向き / 〜向け', 'muki / muke', 'Thích hợp cho... / Thiết kế dành riêng cho đối tượng nào đó', 'Danh từ + 向き / Danh từ + 向け', '向き: rất hợp với X. 向け: được làm nhằm mục đích dành cho X. 向き cũng có nghĩa là ''đối diện'' (南向きの部屋).', 'この料理は作り方が簡単なので、初心者向きだ。', 'この{料理|りょうり}は{作|つく}り{方|かた}が{簡単|かんたん}なので、{初心者|しょしんしゃ}{向|む}きだ。', 'Món ăn này rất dễ làm nên rất phù hợp cho người mới bắt đầu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('265', 'N3', '〜がち', 'gachi', 'Thường hay bị, có xu hướng dễ bị... (chiều hướng tiêu cực)', 'Động từ thể ます (bỏ ます) / Danh từ + がち', 'Có xu hướng thường xuyên hướng tới điều gì đó không mong muốn. So sánh 〜やすい (dễ xảy ra, có thể bình thường) và 〜気味 (dấu hiệu nhẹ của tình trạng hiện tại).', '冬になると、つい家にこもりがちになる。', '{冬|ふゆ}になると、つい{家|いえ}にこもりがちになる。', 'Khi mùa đông đến, tôi có xu hướng nhốt mình trong nhà.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('266', 'N3', '〜気味', 'gimi', 'Có vẻ hơi, có triệu chứng hơi... (hơi sốt, hơi mệt)', 'Danh từ / Động từ thể ます (bỏ ます) + 気味', 'Một dấu hiệu nhẹ của tình trạng (thường là tiêu cực) hiện tại. So sánh 〜がち, có nghĩa là việc gì đó xảy ra thường xuyên.', '少し風邪気味なので、今日は早めに帰ります。', '{少|すこ}し{風邪|かぜ}{気味|ぎみ}なので、{今日|きょう}は{早|はや}めに{帰|かえ}ります。', 'Tôi hơi bị cảm nên hôm nay tôi sẽ về nhà sớm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('267', 'N3', '〜だらけ', 'darake', 'Toàn là, đầy những thứ tiêu cực (đầy bùn, đầy rác, đầy lỗi)', 'Danh từ + だらけ', 'Một lượng lớn thứ gì đó che phủ hoặc lấp đầy một nơi; thường tiêu cực. So sánh N1 〜まみれ (được bao phủ bởi thứ gì đó dính hoặc bẩn).', '子どもたちは、泥だらけになって帰ってきた。', '{子|こ}どもたちは、{泥|どろ}だらけになって{帰|かえ}ってきた。', 'Bọn trẻ về nhà người đầy bùn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('268', 'N3', '〜切る / 〜切れない', 'kiru / kirenai', 'Làm hết sạch hoàn toàn / Không thể nào làm hết xuể được', 'Động từ thể ます (bỏ ます) + 切る / Động từ thể ます (bỏ ます) + 切れない', 'Làm một việc gì đó cho đến tận cùng. Cũng mô tả một trạng thái hoàn toàn: 疲れ切る (''hoàn toàn kiệt sức''). Thường được viết bằng kana: 〜きる / 〜きれない.', '三時間かけて、長い小説を最後まで読み切った。', '{三|さん}{時間|じかん}かけて、{長|なが}い{小説|しょうせつ}を{最後|さいご}まで{読|よ}み{切|き}った。', 'Phải mất ba tiếng đồng hồ nhưng tôi đã đọc hết cuốn tiểu thuyết dài này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('269', 'N3', '〜かける', 'kakeru', 'Đang làm dở dang, chưa hoàn thành trọn vẹn', 'Động từ thể ます (bỏ ます) + かける / Động từ thể ます (bỏ ます) + かけの + Danh từ', 'Một hành động đã bắt đầu nhưng chưa kết thúc, hoặc một việc gì đó sắp xảy ra (死にかける). Thân động từ + かけの + danh từ mô tả sự việc chưa hoàn thành.', '読みかけの本が、机の上に置いてある。', '{読|よ}みかけの{本|ほん}が、{机|つくえ}の{上|うえ}に{置|お}いてある。', 'Có một cuốn sách đang đọc dở trên bàn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('270', 'N3', '〜合う', 'au', 'Cùng nhau làm việc gì đó qua lại lẫn nhau', 'Động từ thể ます (bỏ ます) + 合う', 'Hành động tương hỗ hoặc chia sẻ. Nhiều từ ghép được tạo thành từ: 話し合う, 知り合う, 付き合う, 助け合う.', '困ったときは、お互いに助け合いましょう。', '{困|こま}ったときは、お{互|たが}いに{助|たす}け{合|あ}いましょう。', 'Hãy giúp đỡ lẫn nhau khi mọi việc trở nên khó khăn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('271', 'N3', '〜直す', 'naosu', 'Làm lại việc gì đó một lần nữa cho chuẩn', 'Động từ thể ます (bỏ ます) + 直す', 'Làm lại điều gì đó để sửa chữa hoặc cải thiện nó. Sự lặp lại đơn giản (もう一度〜する) không mang lại cảm giác sửa chữa.', '名前を間違えたので、書類を書き直した。', '{名前|なまえ}を{間違|まちが}えたので、{書類|しょるい}を{書|か}き{直|なお}した。', 'Tôi viết sai tên nên tôi viết lại tài liệu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('272', 'N3', '〜づらい', 'zurai', 'Khó làm việc gì đó (về mặt tâm lý hoặc thao tác)', 'Động từ thể ます (bỏ ます) + づらい', 'Nhấn mạnh sự khó chịu về thể chất hoặc tâm lý của người thực hiện. 〜にくい (N4) khách quan hơn, mô tả bản chất của sự vật. N2 trang trọng: 〜がたい.', 'この靴は少し小さくて、歩きづらい。', 'この{靴|くつ}は{少|すこ}し{小|ちい}さくて、{歩|ある}きづらい。', 'Đôi giày này hơi nhỏ nên đi vào khó khăn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('273', 'N3', '〜べきだ / 〜べきではない', 'beki da / beki de wa nai', 'Nên làm / Không nên làm gì đó theo lẽ thường, đạo lý', 'Động từ thể từ điển (辞書形) + べきだ / Động từ thể từ điển (辞書形) + べきではない', 'Có quan điểm mạnh mẽ về điều gì là đúng hoặc nghĩa vụ của một người; quá thẳng thừng để sử dụng đối với cấp trên. する → すべき hoặc するべき. So sánh 〜ほうがいい (lời khuyên) và 〜ものだ (chuẩn mực xã hội).', '自分が悪いと思ったら、すぐに謝るべきだ。', '{自分|じぶん}が{悪|わる}いと{思|おも}ったら、すぐに{謝|あやま}るべきだ。', 'Nếu bạn cho rằng mình sai thì bạn nên xin lỗi ngay.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('274', 'N3', '〜ほど〜はない', 'hodo ... wa nai', 'Không đến mức phải... như vậy đâu', 'Danh từ + ほど + Tính từ đuôi い / Tính từ đuôi な + な + Danh từ + はない', 'Nói X là ... nhiều nhất, thường là từ kinh nghiệm hoặc cảm nhận cá nhân. Ngoài ra 〜くらい〜はない.', '仕事の後に飲む冷たいビールほどおいしいものはない。', '{仕事|しごと}の{後|あと}に{飲|の}む{冷|つめ}たいビールほどおいしいものはない。', 'Không có gì ngon bằng một ly bia lạnh sau giờ làm việc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('275', 'N3', '〜しかない', 'shika nai (no choice)', 'Chỉ còn cách, không còn lựa chọn nào khác', 'Động từ thể từ điển (辞書形) + しかない', 'Không có lựa chọn nào khác nên người nói chấp nhận lựa chọn này. Khác với Danh từ + しか〜ない (''chỉ''). Tương đương N2 trang trọng: 〜ほかない.', '終電に乗り遅れたから、タクシーで帰るしかない。', '{終電|しゅうでん}に{乗|の}り{遅|おく}れたから、タクシーで{帰|かえ}るしかない。', 'Tôi đã lỡ chuyến tàu cuối cùng nên không còn cách nào khác là bắt taxi về nhà.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('276', 'N3', '〜まで（even）', 'made (even)', 'Đến tận cả, thậm chí đến mức...', 'Danh từ + まで / Danh từ + にまで', 'Thêm một mục cực đoan lên trên những mục khác, thường gây ngạc nhiên hoặc mất tinh thần. So sánh 〜さえ, tự nó chọn ra một ví dụ cực đoan.', '最近は、小学生までスマホを持っている。', '{最近|さいきん}は、{小学生|しょうがくせい}までスマホを{持|も}っている。', 'Ngày nay ngay cả học sinh tiểu học cũng có điện thoại thông minh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('277', 'N3', '〜ところだった', 'tokoro datta', 'Suýt chút nữa thì đã xảy ra chuyện...', 'Động từ thể từ điển (辞書形) + ところだった', 'Một điều gì đó (thường là tồi tệ) đã suýt xảy ra nhưng đã không xảy ra. Thường đi kèm với mệnh đề もう少しで, 危うく hoặc mệnh đề 〜たら. So sánh 〜そうになった.', '考え事をしていて、もう少しで車にひかれるところだった。', '{考|かんが}え{事|ごと}をしていて、もう{少|すこ}しで{車|くるま}にひかれるところだった。', 'Tôi đang mải suy nghĩ và suýt bị ô tô đâm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('278', 'N3', '〜からといって', 'kara to itte', 'Dù nói là... đi chăng nữa thì cũng không được...', 'Mệnh đề / Câu + からといって', 'Phủ nhận rằng một lý do sẽ tự động dẫn đến một kết luận. Thường theo sau là phủ định như 〜わけではない hoặc 〜とは限らない. Bình thường: 〜からって.', '日本人だからといって、みんなが敬語を正しく使えるわけではない。', '{日本人|にほんじん}だからといって、みんなが{敬語|けいご}を{正|ただ}しく{使|つか}えるわけではない。', 'Chỉ vì ai đó là người Nhật không có nghĩa là họ có thể sử dụng keigo một cách chính xác.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('279', 'N3', '〜とおりに / 〜どおりに', 'toori ni / doori ni', 'Đúng theo như, y hệt như...', 'Động từ thể từ điển (辞書形) / Động từ thể た (quá khứ) / Danh từ + の + とおりに / Danh từ + どおりに', 'Làm điều gì đó giống hệt như một mô hình, hướng dẫn hoặc dự đoán. Ngay sau danh từ nó trở thành どおり (予定どおり, 思いどおり). に thường bị bỏ đi. そのとおりだ = ''chính xác''.', '説明書に書いてあるとおりに組み立てたのに、うまく動かない。', '{説明|せつめい}{書|しょ}に{書|か}いてあるとおりに{組|く}み{立|た}てたのに、うまく{動|うご}かない。', 'Mình đã lắp đúng theo hướng dẫn nhưng nó không hoạt động bình thường.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('280', 'N3', '〜なんか / 〜なんて', 'nanka / nante', 'Những thứ như là... (coi nhẹ, khiêm tốn hoặc ngạc nhiên)', 'Danh từ + なんか / Danh từ + なんて / Mệnh đề / Câu + なんて', 'Phiên bản thông thường của など với giọng điệu khinh miệt, khiêm tốn hoặc ngạc nhiên. なんか cũng làm dịu đi những gợi ý (お茶なんかどう？). Câu + なんて thể hiện sự ngạc nhiên hoặc không tin tưởng.', '私なんか、まだまだ初心者です。', '{私|わたし}なんか、まだまだ{初心者|しょしんしゃ}です。', 'Một người như tôi vẫn chỉ là người mới bắt đầu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('281', 'N2', '〜に際して', 'ni saishite', 'Nhân dịp, khi đối mặt với một sự kiện quan trọng trọng đại', 'Danh từ + に際して / Động từ thể từ điển (辞書形) + に際して / Danh từ + に際しての + Danh từ', 'Chính thức; cho những dịp đặc biệt, chỉ diễn ra một lần (nghi lễ, khởi hành, hợp đồng). Gần đồng nghĩa với 〜にあたって, mang lại cảm giác chuẩn bị hoặc đối mặt với sự kiện; に際して chỉ đơn giản là đánh dấu sự kiện.', '新製品の発売に際して、記者会見が開かれた。', '{新|しん}{製品|せいひん}の{発売|はつばい}に{際|さい}して、{記者|きしゃ}{会見|かいけん}が{開|ひら}かれた。', 'Buổi họp báo được tổ chức nhân dịp ra mắt sản phẩm mới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('282', 'N2', '〜に伴って', 'ni tomonatte', 'Cùng với, đi kèm với sự phát triển biến đổi của...', 'Danh từ + に伴って / Động từ thể từ điển (辞書形) + の + に伴って / Danh từ + に伴う + Danh từ', '〜に伴い là dạng viết nhiều hơn. Giống như 〜につれて nó có thể mô tả sự thay đổi dần dần, nhưng nó cũng bao gồm các sự kiện xảy ra một lần dẫn đến một sự kiện khác (ví dụ: chuyển địa điểm).', '高齢化に伴って、医療費は年々増加している。', '{高齢|こうれい}{化|か}に{伴|ともな}って、{医療|いりょう}{費|ひ}は{年々|ねんねん}{増加|ぞうか}している。', 'Cùng với sự già hóa của dân số, chi phí y tế đang tăng lên hàng năm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('283', 'N2', '〜に応じて', 'ni oujite', 'Tùy theo, tương ứng với, để phù hợp với...', 'Danh từ + に応じて / Danh từ + に応じた + Danh từ', 'B được điều chỉnh để phù hợp với A (khả năng, ngân sách, mùa giải, yêu cầu). So sánh 〜次第で, nhấn mạnh rằng kết quả hoàn toàn do A quyết định.', 'この奨学金は、家庭の収入に応じて支給額が変わる。', 'この{奨学|しょうがく}{金|きん}は、{家庭|かてい}の{収入|しゅうにゅう}に{応|おう}じて{支給|しきゅう}{額|がく}が{変|か}わる。', 'Số tiền học bổng này thay đổi tùy theo thu nhập của hộ gia đình.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('284', 'N2', '〜に反して', 'ni hanshite', 'Trái ngược với dự đoán; Vi phạm quy định, nguyên tắc', 'Danh từ + に反して / Danh từ + に反する + Danh từ / Danh từ + に反した + Danh từ', 'Dùng với các danh từ như 予想, 期待, 意思, 規則. Với một quy tắc hoặc luật là danh từ, nó có nghĩa là ''vi phạm''.', '大方の予想に反して、無名の新人が優勝した。', '{大方|おおかた}の{予想|よそう}に{反|はん}して、{無名|むめい}の{新人|しんじん}が{優勝|ゆうしょう}した。', 'Trái với dự đoán của hầu hết mọi người, một tân binh vô danh đã giành chức vô địch.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('285', 'N2', '〜に基づいて', 'ni motozuite', 'Dựa trên cơ sở, căn cứ vào tài liệu, số liệu, pháp luật', 'Danh từ + に基づいて / Danh từ + に基づく + Danh từ / Danh từ + に基づいた + Danh từ', 'Cơ sở là một tiêu chuẩn vững chắc như dữ liệu, luật, sự kiện hoặc kinh nghiệm và B tuân thủ chặt chẽ các tiêu chuẩn đó. So sánh 〜をもとに, được sử dụng cho vật liệu mà từ đó một cái gì đó mới được tạo ra.', 'この映画は実際に起きた事件に基づいて制作された。', 'この{映画|えいが}は{実際|じっさい}に{起|お}きた{事件|じけん}に{基|もと}づいて{制作|せいさく}された。', 'Bộ phim này được thực hiện dựa trên một sự việc có thật đã xảy ra.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('286', 'N2', '〜にわたって', 'ni watatte', 'Suốt trong khoảng thời gian, trên toàn bộ phạm vi không gian', 'Danh từ + にわたって / Danh từ + にわたる + Danh từ', 'Nhấn mạnh toàn bộ khoảng thời gian dài, một khu vực rộng hoặc nhiều đồ vật (多岐にわたる). Không được sử dụng cho nhịp ngắn hoặc nhỏ.', '工事は三か月にわたって行われる予定だ。', '{工事|こうじ}は{三|さん}か{月|げつ}にわたって{行|おこな}われる{予定|よてい}だ。', 'Việc xây dựng dự kiến ​​sẽ diễn ra trong ba tháng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('287', 'N2', '〜に沿って', 'ni sotte', 'Men theo con đường; Bám sát theo kế hoạch, chủ trương', 'Danh từ + に沿って / Danh từ + に沿った + Danh từ', 'Nghĩa đen: dọc theo một con đường, con sông hoặc bờ biển. Cách sử dụng trừu tượng: tuân theo một kế hoạch, chính sách hoặc mong muốn của ai đó.', '川に沿って桜並木が二キロほど続いている。', '{川|かわ}に{沿|そ}って{桜|さくら}{並木|なみき}が{二|に}キロほど{続|つづ}いている。', 'Hàng cây anh đào trải dài khoảng hai cây số dọc theo bờ sông.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('288', 'N2', '〜に加えて', 'ni kuwaete', 'Thêm vào đó, ngoài ra còn có thêm...', 'Danh từ + に加えて', 'Hơi trang trọng; 〜に加え là dạng viết. Mục được thêm vào thường được đánh dấu bằng も.', '今年は猛暑に加えて雨不足が続き、野菜の価格が高騰している。', '{今年|ことし}は{猛暑|もうしょ}に{加|くわ}えて{雨不足|あめぶそく}が{続|つづ}き、{野菜|やさい}の{価格|かかく}が{高騰|こうとう}している。', 'Năm nay, ngoài nắng nóng gay gắt, lại thiếu mưa kéo dài, giá rau tăng vọt.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('289', 'N2', '〜をめぐって', 'o megutte', 'Xoay quanh những tranh cãi, thảo luận về vấn đề nào đó', 'Danh từ + をめぐって / Danh từ + をめぐる + Danh từ', 'Động từ theo sau là về tranh luận, xung đột hoặc cạnh tranh (議論する, 対立する, 争う). Không giống như 〜について, nó không được sử dụng để giải thích một chủ đề đơn giản.', '新しい空港の建設をめぐって、住民の意見が二つに分かれている。', '{新|あたら}しい{空港|くうこう}の{建設|けんせつ}をめぐって、{住民|じゅうみん}の{意見|いけん}が{二|ふた}つに{分|わ}かれている。', 'Người dân bị chia rẽ về việc xây dựng một sân bay mới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('290', 'N2', '〜を通じて / 〜を通して', 'o tsuujite / o tooshite', 'Thông qua phương tiện, người trung gian; Trong suốt cả thời kỳ', 'Danh từ + を通じて / Danh từ + を通して', 'Có thể hoán đổi cho nhau. 〜を通して có xu hướng nhấn mạnh một quá trình hoặc trải nghiệm có chủ ý; 〜を通じて trang trọng hơn một chút và phổ biến hơn với phương tiện truyền thông, địa chỉ liên lạc và thời gian (一年を通じて).', '共通の友人を通じて、今の妻と知り合った。', '{共通|きょうつう}の{友人|ゆうじん}を{通|つう}じて、{今|いま}の{妻|つま}と{知|し}り{合|あ}った。', 'Tôi quen vợ qua một người bạn chung.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('291', 'N2', '〜をきっかけに', 'o kikkake ni', 'Nhân cơ hội, lấy việc đó làm khởi điểm bước ngoặt', 'Danh từ + をきっかけに / Danh từ + をきっかけとして / Động từ thể thông thường (V ngắn) + の + をきっかけに', 'Đánh dấu một sự kiện dẫn đến một sự khởi đầu hoặc thay đổi mới. Từ tương đương chính thức 〜を契機に thường được sử dụng trong văn viết.', '大学時代の留学をきっかけに、国際協力の仕事に興味を持つようになった。', '{大学|だいがく}{時代|じだい}の{留学|りゅうがく}をきっかけに、{国際|こくさい}{協力|きょうりょく}の{仕事|しごと}に{興味|きょうみ}を{持|も}つようになった。', 'Việc học đại học ở nước ngoài khiến tôi hứng thú với công việc hợp tác quốc tế.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('292', 'N2', '〜を除いて', 'o nozoite', 'Ngoại trừ đối tượng này ra', 'Danh từ + を除いて / Danh từ + を除き / Danh từ + を除く + Danh từ', 'Trang trọng hơn 〜以外. 〜を除き là dạng viết.', '年末年始を除いて、図書館は毎日開いている。', '{年末年始|ねんまつねんし}を{除|のぞ}いて、{図書館|としょかん}は{毎日|まいにち}{開|あ}いている。', 'Thư viện mở cửa hàng ngày trừ dịp nghỉ lễ Năm Mới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('293', 'N2', '〜かのようだ', 'ka no you da', 'Cứ y như là, hệt như thể là...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い + かのようだ / Tính từ đuôi な + である + かのようだ / Danh từ + である + かのようだ', 'Mang tính văn học hơn 〜ようだ và báo hiệu rõ ràng rằng sự so sánh này không đúng theo nghĩa đen. Thường kết hợp với まるで. Ngoài ra 〜かのように / 〜かのような.', '彼はまるで何も知らないかのような顔をしていた。', '{彼|かれ}はまるで{何|なに}も{知|し}らないかのような{顔|かお}をしていた。', 'Anh ta làm ra vẻ như không biết gì cả.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('294', 'N2', '〜かねない', 'kanenai', 'Có khả năng, nguy cơ xảy ra hậu quả xấu', 'Động từ thể ます (bỏ ます) + かねない', 'Chỉ cho kết quả không mong muốn. Mặc dù có ない nhưng ý nghĩa vẫn tích cực: ''có nguy cơ thực sự là ...''. Đừng nhầm lẫn với 〜かねる (không thể).', 'そんな無理な運転をしていたら、大事故を起こしかねない。', 'そんな{無理|むり}な{運転|うんてん}をしていたら、{大|だい}{事故|じこ}を{起|お}こしかねない。', 'Nếu bạn tiếp tục lái xe liều lĩnh như vậy, bạn có thể gây ra tai nạn nghiêm trọng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('295', 'N2', '〜かねる', 'kaneru', 'Khó mà, không thể nào làm được (từ chối lịch sự trong kinh doanh)', 'Động từ thể ます (bỏ ます) + かねる', 'Một cách từ chối nhẹ nhàng, trang trọng, rất phổ biến trong dịch vụ khách hàng. Nó thể hiện sự miễn cưỡng hoặc khó khăn của người nói chứ không phải sự bất lực về thể chất.', '申し訳ございませんが、そのご質問にはお答えしかねます。', '{申|もう}し{訳|わけ}ございませんが、そのご{質問|しつもん}にはお{答|こた}えしかねます。', 'Tôi rất xin lỗi, nhưng tôi không thể trả lời câu hỏi đó.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('296', 'N2', '〜ずにはいられない', 'zu ni wa irarenai', 'Không thể không làm, không kìm nén được thôi thúc hành động', 'Động từ thể ない (bỏ ない) + ずにはいられない', 'する trở thành せずにはいられない. 〜ないではいられない là một từ tương đương mang tính đàm thoại hơn một chút. Sự thôi thúc bên trong, không giống như 〜ざるを得ない (do hoàn cảnh ép buộc).', 'あまりにひどいニュースに、怒りを感じずにはいられなかった。', 'あまりにひどいニュースに、{怒|いか}りを{感|かん}じずにはいられなかった。', 'Tôi không thể không cảm thấy tức giận trước tin tức khủng khiếp như vậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('297', 'N2', '〜ざるを得ない', 'zaru o enai', 'Đành phải, buộc lòng phải làm dù bản thân không muốn', 'Động từ thể ない (bỏ ない) + ざるを得ない', 'Hoàn cảnh buộc người nói phải thực hiện một hành động không thực sự muốn thực hiện. する trở thành せざるを得ない. Trang trọng hơn 〜しかない.', '予算が足りないので、計画を縮小せざるを得ない。', '{予算|よさん}が{足|た}りないので、{計画|けいかく}を{縮小|しゅくしょう}せざるを{得|え}ない。', 'Ngân sách không đủ nên chúng tôi không còn lựa chọn nào khác ngoài việc thu hẹp quy mô kế hoạch.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('298', 'N2', '〜てはいられない', 'te wa irarenai', 'Không thể cứ tiếp tục ở tình trạng này được nữa', 'Động từ thể て + はいられない', 'Hoàn cảnh không cho phép người ta ở trạng thái A nên người ta cảm thấy bị thúc đẩy phải hành động. Bình thường: 〜ちゃいられない / 〜てらんない.', '締め切りが迫っているので、のんびりしてはいられない。', '{締|し}め{切|き}りが{迫|せま}っているので、のんびりしてはいられない。', 'Thời hạn đang đến gần nên tôi không thể dễ dàng được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('299', 'N2', '〜ものか', 'mono ka', 'Tuyệt đối không bao giờ làm! (phủ định cực lực, thách thức)', 'Động từ thể từ điển (辞書形) / Tính từ đuôi い / Tính từ đuôi な + な + ものか', 'Từ chối hoặc từ chối cảm xúc mạnh mẽ; không phải là một câu hỏi thực sự Dạng hội thoại: 〜もんか; lịch sự/nữ tính: 〜ものですか.', 'あんな失礼な店には、二度と行くものか。', 'あんな{失礼|しつれい}な{店|みせ}には、{二度|にど}と{行|い}くものか。', 'Không đời nào tôi lại đến cái cửa hàng thô lỗ đó nữa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('300', 'N2', '〜ものの', 'mono no', 'Mặc dù là... nhưng thực tế thì lại...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Tính từ đuôi な + である / Danh từ + である + ものの', 'Thừa nhận sự thật đầu tiên, sau đó nói rằng mọi thứ không diễn ra như nó tưởng. Được viết nhiều hơn 〜けれども.', '免許は取ったものの、怖くてまだ一度も運転していない。', '{免許|めんきょ}は{取|と}ったものの、{怖|こわ}くてまだ{一|いち}{度|ど}も{運転|うんてん}していない。', 'Dù đã có bằng nhưng tôi quá sợ hãi và vẫn chưa lái xe một lần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('301', 'N2', '〜ものなら', 'mono nara', 'Nếu như có thể làm được... (điều kiện khó thành hiện thực)', 'Động từ thể từ điển (辞書形) (potential form) + ものなら', 'Đối với một việc gì đó khó hoặc không thể làm được; mệnh đề chính thường là 〜たい hoặc một lời thách thức. Đừng nhầm lẫn với N1 〜(よ)うものなら (''nếu ai dám..., sẽ có chuyện không hay xảy ra'').', 'できるものなら、学生時代に戻ってもう一度やり直したい。', 'できるものなら、{学生|がくせい}{時代|じだい}に{戻|もど}ってもう{一度|いちど}やり{直|なお}したい。', 'Giá như có thể, tôi sẽ quay lại thời sinh viên và làm lại từ đầu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('302', 'N2', '〜くらいなら / 〜ぐらいなら', 'kurai nara / gurai nara', 'Nếu phải làm việc đó thì thà chọn việc này còn hơn', 'Động từ thể từ điển (辞書形) + くらいなら / Động từ thể từ điển (辞書形) + ぐらいなら', 'A là điều mà người nói cực kỳ không thích; B là lựa chọn ít xấu xa hơn hoặc tốt hơn, thường đi kèm với 〜ほうがいい / 〜ほうがましだ.', 'あんな人に頭を下げるくらいなら、会社を辞めたほうがましだ。', 'あんな{人|ひと}に{頭|あたま}を{下|さ}げるくらいなら、{会社|かいしゃ}を{辞|や}めたほうがましだ。', 'Tôi thà bỏ việc còn hơn cúi đầu trước một kẻ như anh ta.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('303', 'N2', '〜ものがある', 'mono ga aru', 'Có cảm giác thực sự là... (cảm nhận sâu sắc)', 'Động từ thể từ điển (辞書形) / Tính từ đuôi い / Tính từ đuôi な + な + ものがある', 'Diễn tả ấn tượng mạnh mẽ của người nói về chất lượng cảm nhận được ở điều gì đó. Đó là một sự đánh giá, không phải là một tuyên bố về sự tồn tại theo nghĩa đen.', 'この画家の初期の作品には、見る人の心を強く打つものがある。', 'この{画家|がか}の{初期|しょき}の{作品|さくひん}には、{見|み}る{人|ひと}の{心|こころ}を{強|つよ}く{打|う}つものがある。', 'Có điều gì đó trong những tác phẩm đầu tay của họa sĩ này khiến người xem cảm động sâu sắc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('304', 'N2', '〜ことか', 'koto ka', 'Biết bao nhiêu, xiết bao! (cảm thán mức độ cảm xúc)', 'Từ để hỏi + … + Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な + ことか', 'Thường đi kèm với どんなに, どれほど, 何度, v.v. Không phải là một câu hỏi thực sự; nó truyền tải cảm xúc sâu sắc. Giọng điệu nhẹ nhàng hoặc kịch tính.', 'この日が来るのを、どれほど待ち望んだことか。', 'この{日|ひ}が{来|く}るのを、どれほど{待|ま}ち{望|のぞ}んだことか。', 'Tôi đã chờ đợi ngày này bao lâu rồi!')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('305', 'N2', '〜ことに（は）', 'koto ni wa', 'Thật là... làm sao! (nhấn mạnh cảm xúc của người nói)', 'Động từ thể た (quá khứ) / Tính từ đuôi い / Tính từ đuôi な + な + ことに（は）', 'Một từ cảm xúc (驚いた, 残念な, うれしい, 不思議な) thể hiện cảm xúc của người nói trước khi đưa ra sự thật. Viết khá.', '驚いたことに、彼は会社を辞めてパン屋を開いたそうだ。', '{驚|おどろ}いたことに、{彼|かれ}は{会社|かいしゃ}を{辞|や}めてパン{屋|や}を{開|ひら}いたそうだ。', 'Ngạc nhiên thay, tôi nghe nói anh ấy đã nghỉ việc ở công ty và mở một tiệm bánh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('306', 'N2', '〜ことだから', 'koto dakara', 'Vì là đối tượng đó nên chắc chắn sẽ...', 'Danh từ + の + ことだから', 'Từ những gì người nói biết về tính cách của một người, nó sẽ dẫn đến phỏng đoán về hành vi của họ. Thường theo sau là だろう, きっと hoặc に違いない.', '几帳面な彼のことだから、書類はもう準備してあるだろう。', '{几帳面|きちょうめん}な{彼|かれ}のことだから、{書類|しょるい}はもう{準備|じゅんび}してあるだろう。', 'Biết anh ấy tỉ mỉ đến mức nào, có lẽ anh ấy đã chuẩn bị sẵn tài liệu rồi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('307', 'N2', '〜ことなく', 'koto naku', 'Mà không hề làm việc gì đó suốt cả quá trình', 'Động từ thể từ điển (辞書形) + ことなく', 'Một cách viết tương đương với 〜ないで / 〜ずに, thường ngụ ý rằng điều gì đó mà người ta mong đợi sẽ không xảy ra.', '彼は三十年間、一日も休むことなく店を開け続けた。', '{彼|かれ}は{三|さん}{十|じゅう}{年間|ねんかん}、{一|いち}{日|にち}も{休|やす}むことなく{店|みせ}を{開|あ}け{続|つづ}けた。', 'Trong ba mươi năm, ông mở cửa hàng không nghỉ một ngày nào.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('308', 'N2', '〜ことから', 'koto kara', 'Xuất phát từ nguyên nhân, lý do là... mà dẫn tới', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Tính từ đuôi な + である / Danh từ + である + ことから', 'Đưa ra bằng chứng hoặc nguồn gốc đằng sau một cái tên, phán đoán hoặc kết luận. Phổ biến trong việc giải thích tên địa danh và trong các báo cáo.', 'この坂は、昔から桜の名所であることから「桜坂」と呼ばれている。', 'この{坂|さか}は、{昔|むかし}から{桜|さくら}の{名所|めいしょ}であることから「{桜|さくら}{坂|ざか}」と{呼|よ}ばれている。', 'Con dốc này được gọi là ''Sakura-zaka'' vì từ lâu đã là địa điểm nổi tiếng về hoa anh đào.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('309', 'N2', '〜ないことには', 'nai koto ni wa', 'Nếu không làm việc này trước thì tuyệt đối không thể làm việc kia', 'Động từ thể ない / Tính từ đuôi い-ku + ない / Tính từ đuôi な + でない / Danh từ + でない + ことには', 'Mệnh đề chính phủ định (分からない, できない, 始まらない): không có A thì B không thể xảy ra. Nhấn mạnh hơn 〜なければ.', '実際に会って話してみないことには、どんな人か分からない。', '{実際|じっさい}に{会|あ}って{話|はな}してみないことには、どんな{人|ひと}か{分|わ}からない。', 'Bạn không thể biết ai đó như thế nào trừ khi bạn thực sự gặp và nói chuyện với họ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('310', 'N2', '〜ことは〜が', 'koto wa ... ga', 'Làm thì có làm thật đấy nhưng mà...', 'Động từ thể thông thường (V ngắn) + ことは + Động từ thể thông thường (V ngắn) + が / Tính từ đuôi い + ことは + Tính từ đuôi い + が / Tính từ đuôi な + な + ことは + Tính từ đuôi な + だ + が', 'Từ tương tự được lặp lại để thừa nhận điều gì đó chỉ một phần trước khi thêm sự bảo lưu. けど thay thế が trong lời nói.', 'あの映画、見たことは見たけど、途中で寝てしまった。', 'あの{映画|えいが}、{見|み}たことは{見|み}たけど、{途中|とちゅう}で{寝|ね}てしまった。', 'Tôi đã xem bộ phim đó nhưng tôi đã ngủ quên giữa chừng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('311', 'N2', '〜あげく', 'ageku', 'Sau một hồi dây dưa rắc rối cuối cùng lại dẫn tới kết cục tồi tệ', 'Động từ thể た (quá khứ) + あげく / Danh từ + の + あげく', 'Kết quả thường là tiêu cực hoặc đáng thất vọng. 〜末(に) mang tính trung lập và có thể kết thúc tốt đẹp; あげく nhấn mạnh sự thất vọng. あげくの果てに là dạng tăng cường.', 'さんざん迷ったあげく、結局どちらも買わなかった。', 'さんざん{迷|まよ}ったあげく、{結局|けっきょく}どちらも{買|か}わなかった。', 'Sau khi trăn trở rất lâu về nó, cuối cùng tôi cũng không mua.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('312', 'N2', '〜あまり（に）', 'amari ni', 'Vì quá mức... nên dẫn tới kết quả không mong muốn', 'Động từ thể từ điển (辞書形) / Động từ thể た (quá khứ) / Tính từ đuôi な + な / Danh từ + の + あまり（に）', 'Cảm giác hoặc trạng thái ở mức độ cực đoan sẽ dẫn đến một kết quả bất thường, thường không mong muốn. Chủ yếu là với các danh từ chỉ cảm xúc (緊張, 心配, 驚き, 悲しみ).', '彼女は緊張のあまり、スピーチの内容を忘れてしまった。', '{彼女|かのじょ}は{緊張|きんちょう}のあまり、スピーチの{内容|ないよう}を{忘|わす}れてしまった。', 'Cô ấy lo lắng đến mức quên mất mình định nói gì trong bài phát biểu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('313', 'N2', '〜上（に）', 'ue ni', 'Hơn nữa, thêm vào đó (điều tốt thêm tốt, điều xấu thêm xấu)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + である + 上（に）', 'Thêm điểm thứ hai cùng loại (tốt + tốt, xấu + xấu). に có thể được viết ra bằng văn bản. Nó thường không được theo sau bởi một mệnh lệnh hoặc yêu cầu.', 'あのレストランは料理がおいしい上に、値段も手頃だ。', 'あのレストランは{料理|りょうり}がおいしい{上|うえ}に、{値段|ねだん}も{手頃|てごろ}だ。', 'Nhà hàng đó có đồ ăn ngon và hơn hết là giá cả hợp lý.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('314', 'N2', '〜上で', 'ue de', 'Sau khi đã làm việc này kỹ càng thì mới làm tiếp; Xét trên phương diện', 'Động từ thể た (quá khứ) + 上で / Danh từ + の + 上で / Động từ thể từ điển (辞書形) + 上で', 'Động từ-ta + 上で: làm A trước, sau đó làm B. Động từ-dict + 上で: ''trong quá trình / vì mục đích của A''. Văn bản kinh doanh thường rút ngắn nó thành ご確認の上.', '家族とよく相談した上で、お返事いたします。', '{家族|かぞく}とよく{相談|そうだん}した{上|うえ}で、お{返事|へんじ}いたします。', 'Tôi sẽ cho bạn câu trả lời sau khi thảo luận kỹ với gia đình tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('315', 'N2', '〜以上（は）', 'ijou wa', 'Một khi đã... thì buộc lòng phải có trách nhiệm làm đến cùng', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi な + である / Danh từ + である + 以上（は）', 'Từ một tình huống nhất định, rút ​​ra một nghĩa vụ, quyết tâm hoặc phán đoán chắc chắn. Gần đồng nghĩa với 〜からには; 以上は nghe trang trọng và logic hơn, からには giống quyết tâm cá nhân hơn.', '引き受けた以上は、最後まで責任を持ってやり遂げるつもりだ。', '{引|ひ}き{受|う}けた{以上|いじょう}は、{最後|さいご}まで{責任|せきにん}を{持|も}ってやり{遂|と}げるつもりだ。', 'Bây giờ tôi đã thực hiện nó, tôi dự định sẽ thực hiện nó một cách có trách nhiệm đến cùng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('316', 'N2', '〜からには', 'kara ni wa', 'Một khi đã... thì nhất định phải làm', 'Động từ thể thông thường (V ngắn) / Danh từ + である + からには', 'Thể hiện quyết tâm: đã đạt đến tình huống này, người ta nên nhìn thấu nó. Thường kết thúc bằng 〜たい, 〜つもりだ, 〜なければならない. So sánh 〜以上(は), trang trọng và trung lập hơn.', 'オリンピックに出るからには、メダルを狙いたい。', 'オリンピックに{出|で}るからには、メダルを{狙|ねら}いたい。', 'Vì tôi đang thi đấu ở Thế vận hội nên tôi muốn giành huy chương.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('317', 'N2', '〜からこそ', 'kara koso', 'Chính vì lý do đó nên mới càng...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + だ / Danh từ + だ + からこそ', 'Nhấn mạnh rằng lý do này chứ không phải lý do nào khác là lý do thực sự, thường là lý do mà người nghe không mong đợi. Thường kết thúc bằng 〜のだ.', '君のことを思っているからこそ、厳しいことを言うんだ。', '{君|きみ}のことを{思|おも}っているからこそ、{厳|きび}しいことを{言|い}うんだ。', 'Chính vì quan tâm đến bạn nên tôi mới khắt khe với bạn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('318', 'N2', '〜からして', 'kara shite', 'Ngay từ điểm đó đã thấy... (đưa ra dẫn chứng tiêu biểu)', 'Danh từ + からして', 'Hai cách sử dụng: ''đánh giá từ X'' (cơ sở để phỏng đoán) và ''kể cả X, chứ đừng nói đến phần còn lại'', trong đó một ví dụ đại diện cho tổng thể, thường mang tính phê phán.', '空の様子からして、午後は雨になりそうだ。', '{空|そら}の{様子|ようす}からして、{午後|ごご}は{雨|あめ}になりそうだ。', 'Dựa vào vẻ ngoài của bầu trời, có khả năng trời sẽ mưa vào buổi chiều.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('319', 'N2', '〜からすると / 〜からすれば', 'kara suru to / kara sureba', 'Xét từ góc độ, căn cứ vào dữ liệu đó thì suy đoán...', 'Danh từ + からすると / Danh từ + からすれば', 'Đưa ra bằng chứng cho một phán đoán hoặc quan điểm của một người. Gần 〜から見ると; không giống như 〜からいうと, nó có thể cần một người.', 'あの話し方からすると、彼は関西の出身だろう。', 'あの{話|はな}し{方|かた}からすると、{彼|かれ}は{関西|かんさい}の{出身|しゅっしん}だろう。', 'Dựa theo cách nói chuyện thì có lẽ anh ta đến từ vùng Kansai.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('320', 'N2', '〜から見ると / 〜から見れば', 'kara miru to / kara mireba', 'Nhìn từ góc nhìn, lập trường của đối tượng đó thì...', 'Danh từ + から見ると / Danh từ + から見れば', 'Nhìn vào điều gì đó từ vị trí của một người hoặc từ bằng chứng nhất định. 〜から見て và 〜から見ても cũng rất phổ biến.', '外国人から見ると、日本の習慣には不思議なものが多いらしい。', '{外国|がいこく}{人|じん}から{見|み}ると、{日本|にほん}の{習慣|しゅうかん}には{不思議|ふしぎ}なものが{多|おお}いらしい。', 'Rõ ràng, từ góc nhìn của người nước ngoài, nhiều phong tục của người Nhật có vẻ kỳ lạ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('321', 'N2', '〜からいうと / 〜からいって', 'kara iu to / kara itte', 'Xét về mặt, nếu nói từ phương diện đó thì...', 'Danh từ + からいうと / Danh từ + からいって', 'Danh từ là tiêu chí hay quan điểm (立場, 経験, 値段), không phải là người: nói 親からすると, không phải ×親からいうと. 〜からいえば cũng được sử dụng.', '立地からいうと、駅前のマンションのほうが便利だ。', '{立地|りっち}からいうと、{駅前|えきまえ}のマンションのほうが{便利|べんり}だ。', 'Về vị trí thì căn hộ phía trước ga thuận tiện hơn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('322', 'N2', '〜際（に）', 'sai ni', 'Khi, nhân dịp... (dạng trang trọng của とき)', 'Động từ thể từ điển (辞書形) / Động từ thể た (quá khứ) / Danh từ + の + 際（に）', 'Phiên bản trang trọng của 〜とき, phổ biến trong các thông báo và hướng dẫn. Không giống như 〜に際して, nó phù hợp cho những dịp hàng ngày.', 'お降りの際は、お忘れ物のないようご注意ください。', 'お{降|ふ}りの{際|さい}は、お{忘|わす}れ{物|もの}のないようご{注意|ちゅうい}ください。', 'Khi xuống xe, hãy chắc chắn rằng bạn không để lại bất cứ thứ gì.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('323', 'N2', '〜次第', 'shidai', 'Ngay sau khi làm xong thì sẽ lập tức làm bước tiếp theo', 'Động từ thể ます (bỏ ます) + 次第 / Danh từ + 次第', 'Trang trọng, dùng để chỉ một hành động được lên kế hoạch trong tương lai ngay sau A (liên hệ, bắt đầu). Không dùng khi mệnh đề chính ở quá khứ. So sánh 〜次第で (tùy theo).', '詳しい日程が決まり次第、皆様にご連絡いたします。', '{詳|くわ}しい{日程|にってい}が{決|き}まり{次第|しだい}、{皆様|みなさま}にご{連絡|れんらく}いたします。', 'Chúng tôi sẽ liên hệ với mọi người ngay khi lịch trình chi tiết được quyết định.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('324', 'N2', '〜次第だ / 〜次第で', 'shidai da / shidai de', 'Tùy thuộc vào... mà quyết định; Duyên cớ là như thế', 'Danh từ + 次第だ / Danh từ + 次第で / Danh từ + 次第では', 'Kết quả được quyết định bởi A. 〜次第では chỉ ra một kết quả có thể xảy ra trong số nhiều kết quả.', '合格できるかどうかは、これからの努力次第だ。', '{合格|ごうかく}できるかどうかは、これからの{努力|どりょく}{次第|しだい}だ。', 'Việc bạn có vượt qua hay không phụ thuộc vào nỗ lực của bạn kể từ bây giờ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('325', 'N2', '〜末（に）', 'sue ni', 'Sau rất nhiều trăn trở, nỗ lực cuối cùng đã đạt được kết quả', 'Động từ thể た (quá khứ) + 末（に） / Danh từ + の + 末（に）', 'Trung lập: kết quả có thể tốt hoặc xấu và thường là một quyết định hoặc kết luận. Sử dụng 〜あげく khi kết quả không tốt hoặc gây khó chịu.', '半年にわたる交渉の末、両社はようやく合意に達した。', '{半年|はんとし}にわたる{交渉|こうしょう}の{末|すえ}、{両社|りょうしゃ}はようやく{合意|ごうい}に{達|たっ}した。', 'Sau sáu tháng đàm phán, cuối cùng hai công ty đã đạt được thỏa thuận.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('326', 'N2', '〜たところで', 'ta tokoro de', 'Dù cho có làm việc đó đi chăng nữa thì kết quả vẫn vô ích', 'Động từ thể た (quá khứ) + ところで', 'Mệnh đề chính nói rằng nỗ lực đó là vô ích hoặc kết quả sẽ không quan trọng lắm. Thường đi kèm với いくら, どんなに, 今さら. Khác với 〜たところ (khi tôi làm thì tôi đã tìm thấy).', '今から急いだところで、終電には間に合わないだろう。', '{今|いま}から{急|いそ}いだところで、{終電|しゅうでん}には{間|ま}に{合|あ}わないだろう。', 'Ngay cả khi chúng ta vội vàng, có lẽ chúng ta sẽ không bắt được chuyến tàu cuối cùng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('327', 'N2', '〜たところ', 'ta tokoro', 'Sau khi thử làm việc đó xong thì nhận được kết quả', 'Động từ thể た (quá khứ) + ところ', 'Báo cáo những gì xảy ra sau một hành động, thường là một khám phá. Kết quả là một sự kiện trong quá khứ nằm ngoài tầm kiểm soát của người nói. Trang trọng hơn 〜たら.', '駅の窓口で問い合わせたところ、その電車は運休しているとのことだった。', '{駅|えき}の{窓口|まどぐち}で{問|と}い{合|あ}わせたところ、その{電車|でんしゃ}は{運休|うんきゅう}しているとのことだった。', 'Khi tôi hỏi ở cửa sổ nhà ga, tôi được biết là tàu không chạy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('328', 'N2', '〜ばかりか', 'bakari ka', 'Không chỉ dừng lại ở mức đó mà thậm chí còn hơn thế nữa', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + ばかりか', 'Mạnh hơn 〜だけでなく; mục thứ hai thường gây ngạc nhiên hơn và được đánh dấu bằng も, まで hoặc さえ. Không giống như 〜だけでなく, nó không được theo sau bởi mệnh lệnh hoặc yêu cầu.', '彼は約束の時間に遅れたばかりか、謝りもしなかった。', '{彼|かれ}は{約束|やくそく}の{時間|じかん}に{遅|おく}れたばかりか、{謝|あやま}りもしなかった。', 'Anh ta không chỉ đến muộn mà còn không xin lỗi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('329', 'N2', '〜ばかりに', 'bakari ni', 'Chỉ vì một nguyên nhân nhỏ đó mà dẫn tới hậu quả cay đắng', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Tính từ đuôi な + である / Danh từ + である + ばかりに', 'Mệnh đề chính là một kết quả không mong muốn, thường kèm theo sự tiếc nuối. Dạng cố định 〜たいばかりに (''chỉ vì ai đó muốn'') giải thích một động cơ mạnh mẽ.', '確認を一つ怠ったばかりに、大きな損害を出してしまった。', '{確認|かくにん}を{一|ひと}つ{怠|おこた}ったばかりに、{大|おお}きな{損害|そんがい}を{出|だ}してしまった。', 'Đơn giản chỉ vì bỏ qua một lần kiểm tra mà cuối cùng tôi đã gây ra tổn thất nặng nề.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('330', 'N2', '〜ばかりだ', 'bakari da', 'Chỉ việc làm... (mọi khâu chuẩn bị đã xong xuôi); Ngày càng xấu đi', 'Động từ thể từ điển (辞書形) + ばかりだ', 'Thay đổi liên tục theo một hướng, hầu như luôn theo hướng tồi tệ hơn. 〜一方だ (N3) cũng tương tự nhưng cũng có thể diễn tả xu hướng trung lập hoặc tốt.', '駅前の商店街は、客が減るばかりだ。', '{駅前|えきまえ}の{商店|しょうてん}{街|がい}は、{客|きゃく}が{減|へ}るばかりだ。', 'Phố mua sắm trước nhà ga ngày càng vắng khách.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('331', 'N2', '〜はもちろん / 〜はもとより', 'wa mochiron / wa motoyori', 'Đối tượng này là đương nhiên rồi, và ngay cả những cái khác nữa', 'Danh từ + はもちろん / Danh từ + はもとより', 'A là trường hợp hiển nhiên; B được thêm bằng も hoặc まで. 〜はもとより trang trọng và được viết hơn 〜はもちろん.', 'この公園は、週末はもちろん平日も家族連れでにぎわう。', 'この{公園|こうえん}は、{週末|しゅうまつ}はもちろん{平日|へいじつ}も{家族|かぞく}{連|づ}れでにぎわう。', 'Công viên này đông đúc các gia đình vào các ngày trong tuần, chưa kể cuối tuần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('332', 'N2', '〜はともかく', 'wa tomokaku', 'Tạm thời gác chuyện đó sang một bên', 'Danh từ + はともかく / Danh từ + はともかくとして', 'Đặt A sang một bên để tập trung vào B, điều mà người nói cho là quan trọng hơn. So sánh 〜はさておき, được sử dụng nhiều hơn để tạm gác một chủ đề và tiếp tục.', '見た目はともかく、味はなかなかいい。', '{見|み}た{目|め}はともかく、{味|あじ}はなかなかいい。', 'Nhìn sang một bên, nó có vị khá ngon.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('333', 'N2', '〜はさておき', 'wa sateoki', 'Khoan hãy bàn tới chuyện đó, vấn đề quan trọng trước mắt là...', 'Danh từ + はさておき', 'Thường được dùng để chuyển sang ý chính, như trong 冗談はさておき hoặc それはさておき. Trang trọng hơn 〜はともかく một chút.', '冗談はさておき、そろそろ本題に入りましょう。', '{冗談|じょうだん}はさておき、そろそろ{本題|ほんだい}に{入|はい}りましょう。', 'Đùa giỡn sang một bên, hãy bắt tay vào công việc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('334', 'N2', '〜反面 / 〜半面', 'hanmen', 'Ngược lại, mặt khác (so sánh hai khía cạnh đối lập)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Tính từ đuôi な + である / Danh từ + である + 反面', 'Trình bày hai mặt đối lập của cùng một sự việc. 〜一方で (N3) rộng hơn và cũng có thể tương phản hai thứ khác nhau.', '都会の暮らしは便利な反面、ストレスも多い。', '{都会|とかい}の{暮|く}らしは{便利|べんり}な{反面|はんめん}、ストレスも{多|おお}い。', 'Cuộc sống ở thành phố thuận tiện nhưng mặt khác cũng đầy căng thẳng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('335', 'N2', '〜ながら（も）', 'nagara mo', 'Tuy là... dẫu biết là thế nhưng thực tế thì...', 'Động từ thể ます (bỏ ます) / Động từ thể ない / Tính từ đuôi い / Tính từ đuôi な / Danh từ + ながら（も）', 'Không phải ''trong khi làm'' ながら: chủ ngữ ở trạng thái A, nhưng B lại đúng. Đặt các cụm từ: 残念ながら, 知りながら, 及ばずながら.', '狭いながらも、日当たりのいい快適な部屋だ。', '{狭|せま}いながらも、{日当|ひあ}たりのいい{快適|かいてき}な{部屋|へや}だ。', 'Dù nhỏ nhưng đây là một căn phòng đầy nắng và thoải mái.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('336', 'N2', '〜つつ / 〜つつも', 'tsutsu / tsutsu mo', 'Dù biết là không tốt nhưng vẫn cứ làm; Vừa làm vừa...', 'Động từ thể ます (bỏ ます) + つつ / Động từ thể ます (bỏ ます) + つつも', 'riêng つつ là viết 〜ながら (hai hành động cùng một lúc). つつも có nghĩa là ''mặc dù'', thường đi kèm với các động từ chỉ biết hoặc suy nghĩ (思いつつ, 知りつつ).', '今年の業績を振り返りつつ、来年の目標を考えたい。', '{今年|ことし}の{業績|ぎょうせき}を{振|ふ}り{返|かえ}りつつ、{来年|らいねん}の{目標|もくひょう}を{考|かんが}えたい。', 'Khi nhìn lại kết quả năm nay, tôi muốn nghĩ về mục tiêu của năm tới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('337', 'N2', '〜つつある', 'tsutsu aru', 'Đang dần dần trong quá trình biến đổi', 'Động từ thể ます (bỏ ます) + つつある', 'Phong cách viết dành cho sự thay đổi đang được tiến hành hướng tới một kết quả nào đó. Được sử dụng với các động từ thay đổi (増える, 回復する, 変わる), không phải với các hành động đơn giản đang diễn ra như 食べる.', '地球の平均気温は、年々上昇しつつある。', '{地球|ちきゅう}の{平均|へいきん}{気温|きおん}は、{年々|ねんねん}{上昇|じょうしょう}しつつある。', 'Nhiệt độ trung bình của trái đất đang tăng đều đặn hàng năm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('338', 'N2', '〜っぱなし', 'ppanashi', 'Cứ để nguyên trạng thái dở dang như thế không chịu dọn dẹp', 'Động từ thể ます (bỏ ます) + っぱなし', 'Hoặc là có việc gì đó chưa được làm xong mà lẽ ra phải được giải quyết (thường là lời phàn nàn) hoặc là một trạng thái tiếp diễn không ngừng nghỉ. Đàm thoại.', '部屋の電気をつけっぱなしにして出かけてしまった。', '{部屋|へや}の{電気|でんき}をつけっぱなしにして{出|で}かけてしまった。', 'Tôi đi ra ngoài và để đèn trong phòng mình sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('339', 'N2', '〜抜く', 'nuku', 'Kiên trì làm đến cùng dù phải trải qua vô vàn gian khó', 'Động từ thể ます (bỏ ます) + 抜く', 'Nhấn mạnh việc hoàn thành một việc gì đó dù khó khăn hay khó khăn, hoặc làm nó một cách trọn vẹn (考え抜く, 生き抜く).', '足を痛めていたが、彼は最後まで走り抜いた。', '{足|あし}を{痛|いた}めていたが、{彼|かれ}は{最後|さいご}まで{走|はし}り{抜|ぬ}いた。', 'Dù bị thương ở chân nhưng anh ấy vẫn chạy hết mình về đích.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('340', 'N2', '〜得る / 〜得ない', 'uru / enai', 'Có thể xảy ra / Hoàn toàn không thể nào có khả năng xảy ra', 'Động từ thể ます (bỏ ます) + 得る / Động từ thể ます (bỏ ます) + 得ない', 'Khả năng logic, không phải kỹ năng cá nhân (không phải ×泳ぎ得る nghĩa là ''có thể bơi''). Tích cực được đọc là うる hoặc える; các dạng khác sử dụng え- (えない, えた).', 'どんなに注意していても、事故は起こり得る。', 'どんなに{注意|ちゅうい}していても、{事故|じこ}は{起|お}こり{得|え}る。', 'Dù bạn có cẩn thận đến đâu thì tai nạn vẫn có thể xảy ra.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('341', 'N2', '〜がたい', 'gatai', 'Rất khó làm việc gì đó (khó tin, khó tha thứ, khó chấp nhận)', 'Động từ thể ます (bỏ ます) + がたい', 'Dùng với động từ chỉ sự suy nghĩ và cảm xúc (信じる, 理解する, 許す, 離れる). Không giống như 〜にくい, không phải vì khó khăn về thể chất (×この肉は食べがたい).', 'あの誠実な人が不正をしたとは、信じがたい。', 'あの{誠実|せいじつ}な{人|ひと}が{不正|ふせい}をしたとは、{信|しん}じがたい。', 'Thật khó để tin rằng một người trung thực như vậy lại phạm tội lừa đảo.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('342', 'N2', '〜ようがない', 'you ga nai', 'Hoàn toàn không có cách nào để mà làm được', 'Động từ thể ます (bỏ ます) + ようがない / Danh từ + の + しようがない', 'Không có phương pháp nào cả, ngay cả khi người ta muốn. 〜ようもない được nhấn mạnh hơn. Với danh từ suru: 説明のしようがない.', '連絡先が分からないので、知らせようがない。', '{連絡|れんらく}{先|さき}が{分|わ}からないので、{知|し}らせようがない。', 'Tôi không có thông tin liên lạc của anh ấy nên không có cách nào để cho anh ấy biết.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('343', 'N2', '〜ようでは', 'you de wa', 'Nếu cứ trong tình trạng như thế này thì sẽ hỏng việc', 'Động từ thể thông thường (V ngắn) + ようでは', 'Chỉ trích một trạng thái không mong muốn và dự đoán một kết quả xấu; mệnh đề chính là phủ định (〜ない, 困る, だめだ).', 'こんな簡単なミスをするようでは、大きな仕事は任せられない。', 'こんな{簡単|かんたん}なミスをするようでは、{大|おお}きな{仕事|しごと}は{任|まか}せられない。', 'Nếu bạn mắc phải những lỗi đơn giản như thế này, chúng tôi không thể tin tưởng giao cho bạn những công việc lớn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('344', 'N2', '〜てならない', 'te naranai', 'Cứ thấy... không sao ngăn được (cảm xúc tự nhiên dâng trào)', 'Động từ thể て + ならない / Tính từ đuôi い-ku + てならない / Tính từ đuôi な + でならない', 'Dành cho những cảm xúc hoặc ấn tượng tự nảy sinh (気がする, 思われる, 心配だ, 残念だ). Trang trọng và được viết hơn 〜てたまらない / 〜てしょうがない.', '故郷の母のことが心配でならない。', '{故郷|こきょう}の{母|はは}のことが{心配|しんぱい}でならない。', 'Tôi không thể ngừng lo lắng về mẹ tôi ở nhà.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('345', 'N2', '〜てこそ', 'te koso', 'Chính nhờ có việc đó thì mới thực sự hiểu được', 'Động từ thể て + こそ', 'Nhấn mạnh rằng A là điều kiện thiết yếu cho giá trị thực hoặc kết quả tạo ra B. 〜てはじめて (N3) trung tính hơn, tập trung vào thời điểm.', '自分で経験してこそ、本当の意味で理解できるのだ。', '{自分|じぶん}で{経験|けいけん}してこそ、{本当|ほんとう}の{意味|いみ}で{理解|りかい}できるのだ。', 'Chỉ bằng cách tự mình trải nghiệm, bạn mới có thể thực sự hiểu được nó.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('346', 'N2', '〜てからでないと', 'te kara de nai to', 'Nếu chưa làm việc này xong thì không thể làm việc tiếp theo', 'Động từ thể て + からでないと / Động từ thể て + からでなければ', 'Mệnh đề chính mang tính phủ định hoặc thể hiện sự khó khăn: B không thể xảy ra trừ khi A được thực hiện trước.', '実物を見てからでないと、買うかどうか決められない。', '{実物|じつぶつ}を{見|み}てからでないと、{買|か}うかどうか{決|き}められない。', 'Tôi không thể quyết định có nên mua nó hay không cho đến khi tôi nhìn thấy hàng thật.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('347', 'N2', '〜て以来', 'te irai', 'Kể từ sau khi việc đó diễn ra cho tới tận bây giờ', 'Động từ thể て + 以来 / Danh từ + 以来', 'Mệnh đề chính là một trạng thái hoặc một thói quen tiếp diễn từ thời điểm quá khứ đó đến nay, không một sự kiện nào sau đó. Trang trọng hơn 〜てから.', '大学を卒業して以来、彼とは一度も会っていない。', '{大学|だいがく}を{卒業|そつぎょう}して{以来|いらい}、{彼|かれ}とは{一|いち}{度|ど}も{会|あ}っていない。', 'Tôi đã không gặp anh ấy một lần kể từ khi chúng tôi tốt nghiệp đại học.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('348', 'N2', '〜に過ぎない', 'ni suginai', 'Chẳng qua chỉ là, không hơn không kém', 'Động từ thể thông thường (V ngắn) / Danh từ + に過ぎない', 'Hạ thấp một cái gì đó nhỏ hoặc không quan trọng. Thường đi kèm với ただ hoặc 単なる.', 'それは単なるうわさに過ぎない。', 'それは{単|たん}なるうわさに{過|す}ぎない。', 'Đó không gì khác hơn là một tin đồn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('349', 'N2', '〜に限る', 'ni kagiru', 'Cách tốt nhất là..., không gì bằng...', 'Động từ thể từ điển (辞書形) / Động từ thể ない / Danh từ + に限る', 'Niềm tin cá nhân của người nói rằng điều gì đó là sự lựa chọn tốt nhất; đàm thoại. Để nói ''tốt nhất là'' tổng quát hơn, hãy sử dụng 〜に越したことはない.', '疲れたときは、早く寝るに限る。', '{疲|つか}れたときは、{早|はや}く{寝|ね}るに{限|かぎ}る。', 'Khi mệt mỏi, không có gì tốt hơn là đi ngủ sớm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('350', 'N2', '〜に限らず', 'ni kagirazu', 'Không chỉ giới hạn ở... mà cả những đối tượng khác nữa', 'Danh từ + に限らず', 'Mở rộng phạm vi ra ngoài A; phần thứ hai thường có も. Gần giống với 〜だけでなく nhưng trang trọng hơn một chút.', 'この遊園地は、子どもに限らず大人にも人気がある。', 'この{遊園|ゆうえん}{地|ち}は、{子|こ}どもに{限|かぎ}らず{大人|おとな}にも{人気|にんき}がある。', 'Công viên giải trí này không chỉ được trẻ em yêu thích mà cả người lớn nữa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('351', 'N2', '〜に限って', 'ni kagitte', 'Đúng vào cái ngày đó thì xui xẻo; Riêng ai chứ người đó thì không làm thế', 'Danh từ + に限って', 'Hai cách sử dụng: (1) ''đúng lúc'', phàn nàn rằng vận rủi ập đến vào thời điểm tồi tệ nhất; (2) với はずがない / ない, tin tưởng chắc chắn rằng một người cụ thể sẽ không làm điều gì đó.', '傘を持っていない日に限って、雨が降るんだよね。', '{傘|かさ}を{持|も}っていない{日|ひ}に{限|かぎ}って、{雨|あめ}が{降|ふ}るんだよね。', 'Trời luôn mưa vào đúng những ngày tôi không có ô.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('352', 'N2', '〜に限り', 'ni kagiri', 'Chỉ dành riêng cho đối tượng đặc biệt này', 'Danh từ + に限り', 'Chính thức; điển hình của các thông báo, quảng cáo và quy tắc để nêu rõ điều kiện áp dụng cho ai hoặc cái gì.', '本日ご来店のお客様に限り、全商品を二割引きいたします。', '{本日|ほんじつ}ご{来店|らいてん}のお{客様|きゃくさま}に{限|かぎ}り、{全|ぜん}{商品|しょうひん}を{二|に}{割|わり}{引|び}きいたします。', 'Chỉ dành cho khách hàng ghé thăm hôm nay, tất cả các mặt hàng đều được giảm giá 20%.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('353', 'N2', '〜かぎり', 'kagiri', 'Chừng nào mà còn... thì vẫn tiếp tục; Trong toàn bộ giới hạn', 'Động từ thể từ điển (辞書形) / Động từ thể て + いる / Tính từ đuôi い / Tính từ đuôi な + である / Danh từ + である + かぎり', '''Miễn là'' có điều kiện, hoặc ''trong chừng mực'' giới hạn của kiến ​​thức hoặc khả năng (私の知るかぎり, できるかぎり). Cũng được viết là 限り. Khác với 〜に限り (giới hạn).', '私の知るかぎり、彼が約束を破ったことは一度もない。', '{私|わたし}の{知|し}るかぎり、{彼|かれ}が{約束|やくそく}を{破|やぶ}ったことは{一|いち}{度|ど}もない。', 'Theo tôi biết, anh ấy chưa bao giờ thất hứa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('354', 'N2', '〜ないかぎり', 'nai kagiri', 'Nếu không... thì không thể nào...', 'Động từ thể ない + かぎり', 'B giữ được chừng nào A không xảy ra. Nhấn mạnh và trang trọng hơn 〜なければ.', 'よほどのことがないかぎり、明日の会議には出席します。', 'よほどのことがないかぎり、{明日|あした}の{会議|かいぎ}には{出席|しゅっせき}します。', 'Trừ khi có chuyện gì nghiêm trọng xảy ra, tôi sẽ tham dự cuộc họp ngày mai.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('355', 'N2', '〜にかけては', 'ni kakete wa', 'Xét về mặt, riêng về lĩnh vực đó thì không thua kém bất kỳ ai', 'Danh từ + にかけては / Danh từ + にかけても', 'Khen ngợi khả năng của ai đó trong một lĩnh vực cụ thể; thường được theo sau bởi 誰にも負けない, 一番だ, 自信がある.', '料理の腕にかけては、うちの父は誰にも負けない。', '{料理|りょうり}の{腕|うで}にかけては、うちの{父|ちち}は{誰|だれ}にも{負|ま}けない。', 'Về khoản nấu ăn thì bố tôi không ai bằng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('356', 'N2', '〜にこたえて', 'ni kotaete', 'Để đáp ứng, đền đáp lại sự kỳ vọng, mong mỏi, yêu cầu', 'Danh từ + にこたえて / Danh từ + にこたえる + Danh từ', 'Dùng với các danh từ như 期待, 要望, 声援, 要求. Cũng được viết là 〜に応えて. So sánh 〜に応じて, có nghĩa là điều chỉnh theo một điều kiện.', 'ファンの声援にこたえて、選手たちは手を振った。', 'ファンの{声援|せいえん}にこたえて、{選手|せんしゅ}たちは{手|て}を{振|ふ}った。', 'Các cầu thủ vẫy tay chào lại sự cổ vũ của người hâm mộ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('357', 'N2', '〜に先立って', 'ni sakidatte', 'Trước thềm, trước khi tiến hành một sự kiện quan trọng', 'Danh từ + に先立って / Động từ thể từ điển (辞書形) + に先立って / Danh từ + に先立つ + Danh từ', 'Chính thức; để chuẩn bị hoặc sự kiện xảy ra trước một dịp quan trọng. 〜に先立ち là dạng viết.', '新作映画の公開に先立って、完成披露試写会が開かれた。', '{新作|しんさく}{映画|えいが}の{公開|こうかい}に{先立|さきだ}って、{完成|かんせい}{披露|ひろう}{試写|ししゃ}{会|かい}が{開|ひら}かれた。', 'Trước khi phát hành bộ phim mới, một buổi chiếu ra mắt đã được tổ chức.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('358', 'N2', '〜にしろ / 〜にせよ', 'ni shiro / ni seyo', 'Cho dù là cái này hay cái kia thì kết quả vẫn như vậy', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + にしろ・にせよ', 'Cùng nghĩa với 〜にしても (N3) nhưng trang trọng hơn; にせよ được viết nhiều nhất. Dạng ghép AにしろBにしろ có nghĩa là ''A hay B''.', 'たとえ冗談にしろ、そんなことを言うべきではない。', 'たとえ{冗談|じょうだん}にしろ、そんなことを{言|い}うべきではない。', 'Cho dù đó là một trò đùa thì bạn cũng không nên nói những điều như vậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('359', 'N2', '〜にすれば / 〜にしたら', 'ni sureba / ni shitara', 'Nếu đứng trên cương vị, lập trường của người đó', 'Danh từ + にすれば / Danh từ + にしたら', 'Tưởng tượng mọi thứ sẽ cảm thấy như thế nào từ vị trí của người khác (thường không phải của người nói). Không giống như 〜にとって, nó tập trung vào cảm xúc hoặc quan điểm của người đó.', '子どもにすれば、親の小言はうるさいだけなのだろう。', '{子|こ}どもにすれば、{親|おや}の{小言|こごと}はうるさいだけなのだろう。', 'Theo quan điểm của một đứa trẻ, sự cằn nhằn của cha mẹ có lẽ chỉ khiến chúng khó chịu.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('360', 'N2', '〜に相違ない', 'ni soui nai', 'Chắc chắn là như vậy, không còn nghi ngờ gì nữa', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + に相違ない', 'Một dạng viết, trang trọng tương đương với 〜に違いない (N3). Cũng thấy trên các biểu mẫu: 上記の通り相違ありません (''điều trên là chính xác'').', 'この筆跡は、父のものに相違ない。', 'この{筆跡|ひっせき}は、{父|ちち}のものに{相違|そうい}ない。', 'Chữ viết này chắc chắn là của bố tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('361', 'N2', '〜に越したことはない', 'ni koshita koto wa nai', 'Không gì tốt bằng việc làm trước điều đó', 'Động từ thể từ điển (辞書形) / Động từ thể ない / Tính từ đuôi い / Tính từ đuôi な / Danh từ + に越したことはない', 'Thể hiện sự ưa thích thông thường, thường theo sau là が / けれど và một sự dè dặt thực tế. So sánh 〜に限る, đó là sự lựa chọn cá nhân hơn.', 'どんなに健康でも、定期的に検診を受けるに越したことはない。', 'どんなに{健康|けんこう}でも、{定期|ていき}{的|てき}に{検診|けんしん}を{受|う}けるに{越|こ}したことはない。', 'Dù bạn có khỏe mạnh đến đâu thì tốt nhất bạn nên đi khám sức khỏe định kỳ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('362', 'N2', '〜にほかならない', 'ni hoka naranai', 'Chính là, không gì khác ngoài... (nguyên nhân duy nhất)', 'Danh từ + にほかならない / Động từ thể thông thường (V ngắn) + から + にほかならない', 'Khẳng định mạnh mẽ rằng điều này và không có gì khác là trường hợp. 〜からにほかならない nhấn mạnh một lý do. Chính thức/bằng văn bản.', '今回の成功は、社員一人ひとりの努力の結果にほかならない。', '{今回|こんかい}の{成功|せいこう}は、{社員|しゃいん}{一人|ひとり}ひとりの{努力|どりょく}の{結果|けっか}にほかならない。', 'Thành công này không gì khác hơn là kết quả của sự nỗ lực của mỗi nhân viên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('363', 'N2', '〜にもかかわらず', 'ni mo kakawarazu', 'Mặc dù... thế nhưng thực tế lại xảy ra trái ngược', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + である / Danh từ + にもかかわらず', 'Một dạng tương đương trang trọng của 〜のに, phổ biến trong tin tức và văn viết; nhấn mạnh rằng kết quả đi ngược lại với những gì A mong đợi. Đừng nhầm lẫn với 〜にかかわらず (bất kể cái nào).', '悪天候にもかかわらず、大勢の観客が会場に集まった。', '{悪天候|あくてんこう}にもかかわらず、{大勢|おおぜい}の{観客|かんきゃく}が{会場|かいじょう}に{集|あつ}まった。', 'Bất chấp thời tiết xấu, rất đông người dân vẫn tập trung tại địa điểm tổ chức.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('364', 'N2', '〜にあたって', 'ni atatte', 'Nhân dịp, trước thềm một sự kiện trọng đại diễn ra', 'Danh từ + にあたって / Động từ thể từ điển (辞書形) + にあたって / Danh từ + にあたっての + Danh từ', 'Chính thức; được sử dụng khi phải đối mặt với một công việc quan trọng, thường với ý thức chuẩn bị hoặc quyết tâm. 〜に際して cũng tương tự nhưng chỉ đơn giản là đánh dấu sự kiện. Dạng viết: 〜にあたり.', '新生活を始めるにあたって、必要な物をリストにまとめた。', '{新|しん}{生活|せいかつ}を{始|はじ}めるにあたって、{必要|ひつよう}な{物|もの}をリストにまとめた。', 'Khi bắt đầu cuộc sống mới, tôi đã lập danh sách những thứ tôi cần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('365', 'N2', '〜において / 〜における', 'ni oite / ni okeru', 'Ở, tại, trong... (văn viết trang trọng của で)', 'Danh từ + において / Danh từ + における + Danh từ', 'Phiên bản trang trọng của で dùng để chỉ địa điểm, thời gian, lĩnh vực hoặc tình huống. Trước một danh từ, có dạng 〜における.', '卒業式は大講堂において行われます。', '{卒業|そつぎょう}{式|しき}は{大|だい}{講堂|こうどう}において{行|おこな}われます。', 'Lễ tốt nghiệp sẽ được tổ chức tại hội trường chính.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('366', 'N2', '〜にかかわらず', 'ni kakawarazu', 'Bất kể, không phân biệt tuổi tác, giới tính, điều kiện', 'Danh từ + にかかわらず / Động từ thể từ điển (辞書形) + Động từ thể ない + にかかわらず', 'Dùng với các danh từ ám chỉ một phạm vi hoặc một cặp đối lập (年齢, 天候, 有無, 男女). Rất gần với 〜を問わず. Đừng nhầm lẫn với 〜にもかかわらず (mặc dù).', 'このセミナーは、経験の有無にかかわらず、どなたでも参加できます。', 'このセミナーは、{経験|けいけん}の{有無|うむ}にかかわらず、どなたでも{参加|さんか}できます。', 'Bất cứ ai cũng có thể tham gia hội thảo này, bất kể kinh nghiệm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('367', 'N2', '〜にかかわる', 'ni kakawaru', 'Ảnh hưởng nghiêm trọng liên quan tới danh dự, tính mạng', 'Danh từ + にかかわる', 'Dùng cho những việc gây hậu quả nghiêm trọng: 命, 名誉, 信用, 将来. Cũng được viết 〜に関わる.', 'これは人の命にかかわる問題だから、慎重に判断しなければならない。', 'これは{人|ひと}の{命|いのち}にかかわる{問題|もんだい}だから、{慎重|しんちょう}に{判断|はんだん}しなければならない。', 'Đây là vấn đề sinh tử nên chúng ta phải phán xét cẩn thận.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('368', 'N2', '〜につき', 'ni tsuki', 'Bởi vì lý do... (dùng trong văn bản thông báo công khai); Mỗi đơn vị là...', 'Danh từ + につき', 'Hai cách sử dụng riêng biệt: ''per'' với số lượng và ''vì'' trên các thông báo và biển hiệu (工事中につき). Lý do sử dụng là rất trang trọng.', '駐車料金は、一時間につき三百円です。', '{駐車|ちゅうしゃ}{料金|りょうきん}は、{一|いち}{時間|じかん}につき{三百|さんびゃく}{円|えん}です。', 'Bãi đậu xe có giá 300 yên mỗi giờ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('369', 'N2', '〜に向けて', 'ni mukete', 'Hướng tới mục tiêu, chuẩn bị cho sự kiện trong tương lai', 'Danh từ + に向けて / Danh từ + に向けた + Danh từ', 'Đánh dấu một mục tiêu hoặc sự kiện đang được hướng tới hoặc khán giả có điều gì đó hướng tới. Khác với 〜向け (N3), ''được thiết kế cho''.', '来年の全国大会に向けて、毎日練習に励んでいる。', '{来年|らいねん}の{全国|ぜんこく}{大会|たいかい}に{向|む}けて、{毎日|まいにち}{練習|れんしゅう}に{励|はげ}んでいる。', 'Chúng tôi đang luyện tập chăm chỉ mỗi ngày cho giải đấu quốc gia năm tới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('370', 'N2', '〜のもとで / 〜のもとに', 'no moto de / no moto ni', 'Dưới sự dẫn dắt, chỉ bảo, ảnh hưởng của...', 'Danh từ + のもとで / Danh từ + のもとに', 'Dưới sự hướng dẫn, bảo vệ hoặc ảnh hưởng của ai đó, hoặc dưới một quy tắc hoặc điều kiện. Thường được viết là 〜の下で / 〜の下に.', '私は大学院で、有名な教授のもとで研究を続けた。', '{私|わたし}は{大学院|だいがくいん}で、{有名|ゆうめい}な{教授|きょうじゅ}のもとで{研究|けんきゅう}を{続|つづ}けた。', 'Ở trường cao học, tôi tiếp tục nghiên cứu dưới sự hướng dẫn của một giáo sư nổi tiếng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('371', 'N2', '〜抜きで / 〜抜きに', 'nuki de / nuki ni', 'Bỏ qua, không tính đến yếu tố đó', 'Danh từ + 抜きで / Danh từ + 抜きに / Danh từ + 抜きの + Danh từ', 'Bỏ đi một số thứ thường được đưa vào. 〜抜きには〜ない / 〜を抜きにしては〜ない có nghĩa là ''không thể... không có''.', '今日は仕事の話抜きで、ゆっくり楽しみましょう。', '{今日|きょう}は{仕事|しごと}の{話|はなし}{抜|ぬ}きで、ゆっくり{楽|たの}しみましょう。', 'Hôm nay chúng ta hãy gác lại công việc và thư giãn và tận hưởng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('372', 'N2', '〜をこめて', 'o komete', 'Gửi gắm toàn bộ tâm tình, tình cảm vào hành động', 'Danh từ + をこめて', 'Dùng với cảm xúc hoặc ý định (愛, 感謝, 願い, 心). Cũng được viết là 〜を込めて.', '母の誕生日に、感謝の気持ちをこめて手紙を書いた。', '{母|はは}の{誕生|たんじょう}{日|び}に、{感謝|かんしゃ}の{気持|きも}ちをこめて{手紙|てがみ}を{書|か}いた。', 'Nhân dịp sinh nhật của mẹ tôi, tôi đã viết cho mẹ một lá thư đầy lòng biết ơn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('373', 'N2', '〜を中心に', 'o chuushin ni', 'Lấy đối tượng đó làm trung tâm, nòng cốt', 'Danh từ + を中心に / Danh từ + を中心として / Danh từ + を中心とした + Danh từ', 'A là phần cốt lõi hoặc phần chính của một khu vực, nhóm hoặc hoạt động rộng hơn.', '今回の地震では、沿岸部を中心に大きな被害が出た。', '{今回|こんかい}の{地震|じしん}では、{沿岸|えんがん}{部|ぶ}を{中心|ちゅうしん}に{大|おお}きな{被害|ひがい}が{出|で}た。', 'Trong trận động đất này, thiệt hại nghiêm trọng xảy ra chủ yếu ở các khu vực ven biển.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('374', 'N2', '〜をもとに', 'o moto ni', 'Dựa trên chất liệu, dữ liệu thực tế để sáng tạo', 'Danh từ + をもとに / Danh từ + をもとにして / Danh từ + をもとにした + Danh từ', 'A là chất liệu hoặc gợi ý để từ đó tạo ra một điều gì đó mới mẻ. 〜に基づいて được sử dụng khi tuân thủ nghiêm ngặt một quy tắc hoặc thực tế nào đó như một tiêu chuẩn.', 'この小説は、作者自身の体験をもとに書かれている。', 'この{小説|しょうせつ}は、{作者|さくしゃ}{自身|じしん}の{体験|たいけん}をもとに{書|か}かれている。', 'Cuốn tiểu thuyết này dựa trên kinh nghiệm của chính tác giả.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('375', 'N2', '〜を問わず', 'o towazu', 'Không đòi hỏi, không kể bất kỳ điều kiện nào', 'Danh từ + を問わず', 'Dùng với các danh từ hàm ý một phạm vi hoặc một cặp đối lập (男女, 昼夜, 国内外). Rất gần với 〜にかかわらず; を問わず là điển hình của các quảng cáo và thông báo việc làm.', 'この仕事は、年齢や経験を問わず応募できます。', 'この{仕事|しごと}は、{年齢|ねんれい}や{経験|けいけん}を{問|と}わず{応募|おうぼ}できます。', 'Bất cứ ai cũng có thể ứng tuyển vào công việc này bất kể tuổi tác hay kinh nghiệm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('376', 'N2', '〜をはじめ', 'o hajime', 'Trước tiên phải kể đến đối tượng tiêu biểu hàng đầu', 'Danh từ + をはじめ / Danh từ + をはじめとして / Danh từ + をはじめとする + Danh từ', 'Nêu tên thành viên tiêu biểu nhất của nhóm, sau đó đề cập đến cả nhóm. Mệnh đề chính bao gồm nhiều mục, không chỉ A.', '京都には、金閣寺をはじめ、多くの有名な寺がある。', '{京都|きょうと}には、{金閣寺|きんかくじ}をはじめ、{多|おお}くの{有名|ゆうめい}な{寺|てら}がある。', 'Kyoto có nhiều ngôi chùa nổi tiếng, trong đó có Kinkakuji.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('377', 'N2', '〜を〜として', 'o ... to shite', 'Lấy... làm mục tiêu, hình mẫu', 'Danh từ + を + Danh từ + として / Danh từ + を + Danh từ + とする', 'Đặt A vào vai trò hoặc chức năng của B; Các danh từ B phổ biến là 目的, 目標, 中心, 対象, 条件. 〜を〜にして là một biến thể thông thường hơn.', 'この大会は、地域の活性化を目的として開かれている。', 'この{大会|たいかい}は、{地域|ちいき}の{活性|かっせい}{化|か}を{目的|もくてき}として{開|ひら}かれている。', 'Sự kiện này được tổ chức với mục đích hồi sinh khu vực địa phương.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('378', 'N2', '〜かというと', 'ka to iu to', 'Nói như vậy nhưng không hẳn thực tế là như thế', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + かというと / Từ để hỏi + … + かというと', 'Với từ để hỏi (なぜ, どうして) nó đưa ra lời giải thích. Không có ai, nó chỉ đặt ra một câu hỏi để phủ nhận giả định dễ dàng (〜かというと、そうではない). Ngoài ra 〜かといえば.', '高い料理ならおいしいかというと、必ずしもそうではない。', '{高|たか}い{料理|りょうり}ならおいしいかというと、{必|かなら}ずしもそうではない。', 'Đồ ăn đắt tiền có phải lúc nào cũng ngon không? Không nhất thiết phải như vậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('379', 'N2', '〜かと思ったら / 〜かと思うと', 'ka to omottara / ka to omou to', 'Vừa mới thấy... thì lập tức chuyển biến ngay', 'Động từ thể た (quá khứ) + かと思ったら / Động từ thể た (quá khứ) + かと思うと', 'B đi theo A ngay lập tức và bất ngờ. Nó mô tả những gì người nói quan sát được, vì vậy nó không được sử dụng cho hành động của chính người nói.', '空が急に暗くなったかと思ったら、激しい雨が降り出した。', '{空|そら}が{急|きゅう}に{暗|くら}くなったかと{思|おも}ったら、{激|はげ}しい{雨|あめ}が{降|ふ}り{出|だ}した。', 'Ngay khi bầu trời đột nhiên tối sầm thì trời bắt đầu đổ mưa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('380', 'N2', '〜か〜ないかのうちに', 'ka ... nai ka no uchi ni', 'Hành động trước vừa dứt thì hành động sau đã bắt đầu ngay tức khắc', 'Động từ thể từ điển (辞書形) + か + Động từ thể ない + かのうちに / Động từ thể た (quá khứ) + か + Động từ thể ない + かのうちに', 'Động từ tương tự được lặp lại ở dạng tích cực và tiêu cực: không rõ liệu A có hoàn thành trước khi B xảy ra hay không. Mô tả các sự kiện được quan sát.', 'ベルが鳴るか鳴らないかのうちに、生徒たちは教室を飛び出した。', 'ベルが{鳴|な}るか{鳴|な}らないかのうちに、{生徒|せいと}たちは{教室|きょうしつ}を{飛|と}び{出|だ}した。', 'Chuông vừa reo khi học sinh ùa ra khỏi lớp.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('381', 'N2', '〜きり', 'kiri', 'Kể từ lần đó là bặt vô âm tín luôn; Chỉ duy nhất lần đó', 'Động từ thể た (quá khứ) + きり / Danh từ + きり', 'Động từ-ta + きり: điều gì đó đã xảy ra và điều tiếp theo được mong đợi không bao giờ đến (thường đi kèm với ない). Danh từ + きり: ''chỉ'' (一度きり, 二人きり). Bình thường: 〜っきり.', '兄は十年前に家を出たきり、一度も連絡をよこさない。', '{兄|あに}は{十|じゅう}{年|ねん}{前|まえ}に{家|いえ}を{出|で}たきり、{一|いち}{度|ど}も{連絡|れんらく}をよこさない。', 'Anh trai tôi đã rời nhà mười năm trước và không liên lạc với nhau kể từ đó.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('382', 'N2', '〜げ', 'ge', 'Có vẻ, đượm vẻ (tâm trạng biểu lộ qua ánh mắt, nét mặt)', 'Tính từ đuôi い stem + げ / Tính từ đuôi な + げ / Động từ thể ます (bỏ ます) + たげ', 'Mô tả cảm giác hiện rõ trên khuôn mặt hoặc thái độ của ai đó; liên hợp giống như tính từ na (〜げな, 〜げに). Đúng hơn là văn học.', '彼女は窓の外を寂しげに見つめていた。', '{彼女|かのじょ}は{窓|まど}の{外|そと}を{寂|さび}しげに{見|み}つめていた。', 'Cô đang nhìn ra ngoài cửa sổ với vẻ mặt cô đơn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('383', 'N2', '〜っこない', 'kko nai', 'Tuyệt đối không thể nào làm được việc đó đâu', 'Động từ thể ます (bỏ ます) + っこない', 'Chỉ lời nói thông thường; thường đi kèm với động từ tiềm năng (できっこない, 分かりっこない). Một cách thông thường tương đương với 〜わけがない.', 'こんな量の仕事、一日で終わりっこないよ。', 'こんな{量|りょう}の{仕事|しごと}、{一|いち}{日|にち}で{終|お}わりっこないよ。', 'Không có cách nào nhiều công việc này sẽ được thực hiện trong một ngày.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('384', 'N2', '〜どころか', 'dokoro ka', 'Đừng nói tới chuyện đó, ngay cả điều đơn giản hơn còn không làm nổi', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + どころか', 'Hoặc thực tế đối lập với A, hoặc thậm chí không thể có thứ gì nhỏ hơn A (AどころかBも〜ない).', '薬を飲んだが、熱は下がるどころか、ますます高くなった。', '{薬|くすり}を{飲|の}んだが、{熱|ねつ}は{下|さ}がるどころか、ますます{高|たか}くなった。', 'Tôi đã uống thuốc nhưng chưa hề thuyên giảm mà cơn sốt của tôi lại càng cao hơn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('385', 'N2', '〜どころではない', 'dokoro de wa nai', 'Lúc này không phải là lúc rảnh rỗi để làm việc đó', 'Động từ thể từ điển (辞書形) / Danh từ + どころではない', 'Hoàn cảnh khiến A không thể hoặc không thể tưởng tượng được vào lúc này. Bình thường: 〜どころじゃない.', '来週は試験なので、遊びに行くどころではない。', '{来週|らいしゅう}は{試験|しけん}なので、{遊|あそ}びに{行|い}くどころではない。', 'Tuần sau tôi có bài kiểm tra nên giờ không phải lúc để ra ngoài.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('386', 'N2', '〜とは限らない', 'to wa kagiranai', 'Không hẳn nhất thiết lúc nào cũng là như vậy', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + とは限らない', 'Phủ nhận rằng điều gì đó luôn đúng; thường đi kèm với 必ずしも hoặc 〜からといって.', '値段が高いからといって、品質がいいとは限らない。', '{値段|ねだん}が{高|たか}いからといって、{品質|ひんしつ}がいいとは{限|かぎ}らない。', 'Chỉ vì thứ gì đó đắt tiền không nhất thiết có nghĩa là nó có chất lượng tốt.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('387', 'N2', '〜というものだ', 'to iu mono da', 'Đúng thực sự là, đó chính là bản chất của...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + というものだ', 'Một quan điểm chắc chắn về cái gì đó thực sự là gì hoặc mọi thứ diễn ra một cách tự nhiên như thế nào. Thường dùng để chỉ trích một thái độ.', '困っている人がいたら助ける。それが人情というものだ。', '{困|こま}っている{人|ひと}がいたら{助|たす}ける。それが{人情|にんじょう}というものだ。', 'Khi ai đó gặp khó khăn, bạn giúp đỡ họ. Đó chính là lòng tốt của con người.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('388', 'N2', '〜というものではない', 'to iu mono de wa nai', 'Không thể nói tuyệt đối là như thế được', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + というものではない', 'Từ chối một niềm tin đơn giản hóa quá mức; thường đi kèm với 〜ばいい hoặc 〜さえ〜ば. 〜というものでもない là một biến thể nhẹ nhàng hơn.', '練習は長くやればいいというものではない。', '{練習|れんしゅう}は{長|なが}くやればいいというものではない。', 'Thực hành lâu hơn không tự động là thực hành tốt hơn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('389', 'N2', '〜といっても', 'to itte mo', 'Dù nói là... đi chăng nữa nhưng thực tế cũng chỉ ở mức vừa phải', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + といっても', 'Sửa hoặc giới hạn những gì người nghe có thể thừa nhận từ câu nói đầu tiên. Thường theo sau là たった, ただ hoặc 〜しか〜ない.', '料理ができるといっても、簡単なものしか作れません。', '{料理|りょうり}ができるといっても、{簡単|かんたん}なものしか{作|つく}れません。', 'Tôi nói tôi có thể nấu ăn, nhưng tôi chỉ có thể làm những việc đơn giản.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('390', 'N2', '〜といえば / 〜というと', 'to ieba / to iu to', 'Nếu nhắc đến chủ đề đó thì nghĩ ngay đến đặc trưng', 'Danh từ + といえば / Danh từ + というと', 'Giới thiệu điều gì đó liên quan chặt chẽ đến một chủ đề hoặc chọn một từ vừa được đề cập để chuyển cuộc trò chuyện. 〜といったら là một biến thể giàu cảm xúc hơn.', '日本の夏の風物詩といえば、やはり花火大会だろう。', '{日本|にほん}の{夏|なつ}の{風物詩|ふうぶつし}といえば、やはり{花火|はなび}{大会|たいかい}だろう。', 'Khi bạn nghĩ về truyền thống mùa hè ở Nhật Bản, lễ hội pháo hoa sẽ hiện lên trong đầu bạn đầu tiên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('391', 'N2', '〜ほかない', 'hoka nai', 'Không còn cách nào khác ngoài việc...', 'Động từ thể từ điển (辞書形) + ほかない / Động từ thể từ điển (辞書形) + よりほかない', 'Tương đương một cách trang trọng với 〜しかない (N3). Các biến thể: 〜ほかはない, 〜よりほかない, 〜ほかしかたがない.', '終電を逃してしまったので、タクシーで帰るほかない。', '{終電|しゅうでん}を{逃|のが}してしまったので、タクシーで{帰|かえ}るほかない。', 'Tôi đã lỡ chuyến tàu cuối cùng nên không còn cách nào khác là bắt taxi về nhà.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('392', 'N2', '〜まい', 'mai', 'Tuyệt đối không định làm nữa; Có lẽ sẽ không xảy ra', 'Động từ thể từ điển (辞書形) + まい / Động từ thể ます (bỏ ます) (ichidan verbs) + まい', 'Viết/chính thức. Ngôi thứ nhất: kiên quyết không làm (〜ないつもりだ). Khác: đoán tiêu cực (〜ないだろう). する → するまい / すまい. Thường là 〜まいと思う / 〜まいと決心する.', 'あんな恥ずかしい思いは、二度とするまいと心に誓った。', 'あんな{恥|は}ずかしい{思|おも}いは、{二度|にど}とするまいと{心|こころ}に{誓|ちか}った。', 'Tôi tự thề với mình rằng tôi sẽ không bao giờ trải qua cảm giác xấu hổ như vậy nữa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('393', 'N2', '〜（よ）うではないか', 'you de wa nai ka', 'Chúng ta hãy cùng nhau làm việc đó đi nào! (lời kêu gọi hào sảng)', 'Động từ thể ý chí + ではないか / Động từ thể ý chí + ではありませんか', 'Lời kêu gọi mạnh mẽ, trang trọng đối với một nhóm, điển hình trong các bài phát biểu và bài xã luận. Phiên bản thông thường là 〜(よ)うじゃないか.', 'この町の未来のために、皆で力を合わせようではないか。', 'この{町|まち}の{未来|みらい}のために、{皆|みな}で{力|ちから}を{合|あ}わせようではないか。', 'Vì tương lai của thị trấn này, tất cả chúng ta hãy chung sức.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('394', 'N2', '〜もかまわず', 'mo kamawazu', 'Không màng tới, bất chấp ánh nhìn hoặc trở ngại xung quanh', 'Danh từ + もかまわず / Động từ thể thông thường (V ngắn) + の + もかまわず', 'Làm việc gì đó mà không quan tâm đến điều mọi người thường quan tâm (人目, 周囲, 雨). Thường hàm ý không tán thành.', '彼女は人目もかまわず、駅のホームで大声で泣いた。', '{彼女|かのじょ}は{人目|ひとめ}もかまわず、{駅|えき}のホームで{大声|おおごえ}で{泣|な}いた。', 'Cô hét lớn trên sân ga, không quan tâm có ai đang theo dõi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('395', 'N2', '〜上', 'jou', 'Xét về mặt, trên phương diện lý thuyết hoặc đạo đức', 'Danh từ + 上 / Danh từ + 上の + Danh từ', 'Gắn với các danh từ Hán-Nhật (歴史上, 経験上, 健康上, 表面上). Đọc じょう, không phải うえ.', 'この映画には、教育上好ましくない場面が多い。', 'この{映画|えいが}には、{教育|きょういく}{上|じょう}{好|この}ましくない{場面|ばめん}が{多|おお}い。', 'Phim này có nhiều cảnh không mong muốn xét về mặt giáo dục.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('396', 'N2', '〜おそれがある', 'osore ga aru', 'Có mối lo ngại, nguy cơ sẽ xảy ra tai họa', 'Động từ thể từ điển (辞書形) + おそれがある / Danh từ + の + おそれがある', 'Đối với những khả năng không mong muốn; điển hình của tin tức, báo cáo thời tiết và cảnh báo chính thức. Cũng được viết là 〜恐れがある. 〜かねない nghe giống sự đánh giá của chính người nói hơn.', '台風の影響で、今夜から大雨になるおそれがあります。', '{台風|たいふう}の{影響|えいきょう}で、{今夜|こんや}から{大雨|おおあめ}になるおそれがあります。', 'Do ảnh hưởng bão nên có nguy cơ sẽ có mưa lớn từ tối nay.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('397', 'N2', '〜せいか', 'sei ka', 'Chắc có lẽ là do nguyên nhân đó chăng (suy đoán không chắc chắn)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + の + せいか', 'Người nói không chắc chắn về nguyên nhân. Chủ yếu là cho kết quả xấu hoặc trung tính; để có kết quả tốt, おかげか cũng được sử dụng.', '年のせいか、最近疲れが取れにくくなった。', '{年|とし}のせいか、{最近|さいきん}{疲|つか}れが{取|と}れにくくなった。', 'Có lẽ đó là độ tuổi của tôi, nhưng gần đây tôi phải mất nhiều thời gian hơn để hồi phục sau cơn mệt mỏi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('398', 'N2', '〜だけあって / 〜だけに', 'dake atte / dake ni', 'Quả đúng xứng danh với danh tiếng, năng lực ấy', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + だけあって・だけに', 'だけあって khen ngợi một kết quả phù hợp với mong đợi (thường đi kèm với さすが). だけに còn có nghĩa là ''thêm vì'' và có thể dẫn đến những kết quả tiêu cực.', 'さすが一流ホテルだけあって、サービスが行き届いている。', 'さすが{一流|いちりゅう}ホテルだけあって、サービスが{行|い}き{届|とど}いている。', 'Đúng như bạn mong đợi từ một khách sạn hạng nhất, dịch vụ này hoàn hảo.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('399', 'N2', '〜だけのことはある', 'dake no koto wa aru', 'Thật bõ công, quả xứng đáng với công sức bỏ ra', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な / Danh từ + だけのことはある', 'Đánh giá một kết quả được chứng minh hoàn toàn bằng công sức, giá cả hoặc danh tiếng; thường đi kèm với さすが. Giống như 〜だけあって nhưng đặt ở cuối câu.', 'この料理は本当においしい。人気店だけのことはある。', 'この{料理|りょうり}は{本当|ほんとう}においしい。{人気|にんき}{店|てん}だけのことはある。', 'Món ăn này thực sự rất ngon. Không có gì ngạc nhiên khi nơi này rất nổi tiếng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('400', 'N2', 'たとえ〜ても', 'tatoe ... te mo', 'Cho dù giả sử có xảy ra chuyện gì đi chăng nữa thì...', 'たとえ + Động từ thể て + も / たとえ + Tính từ đuôi い-ku + ても / たとえ + Tính từ đuôi な + でも / たとえ + Danh từ + でも', 'たとえ báo hiệu trước một giả thuyết ''ngay cả khi'' và củng cố nó. Một biến thể được viết nhiều hơn là たとえ〜としても.', 'たとえ反対されても、自分の夢を諦めるつもりはない。', 'たとえ{反対|はんたい}されても、{自分|じぶん}の{夢|ゆめ}を{諦|あきら}めるつもりはない。', 'Dù mọi người có phản đối thì tôi cũng không có ý định từ bỏ ước mơ của mình.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('401', 'N2', '〜やら〜やら', 'yara ... yara', 'Nào là chuyện này, nào là chuyện kia (rối rắm, bận rộn)', 'Danh từ + やら + Danh từ + やら / Động từ thể từ điển (辞書形) + やら + Động từ thể từ điển (辞書形) + やら / Tính từ đuôi い + やら + Tính từ đuôi い + やら', 'Liệt kê một vài ví dụ trong số rất nhiều ví dụ, thường gợi ý sự hỗn loạn hoặc cảm xúc lẫn lộn. Thường được theo sau bởi で + 大変だ / 忙しい.', '引っ越しの前は、荷造りやら手続きやらで目が回るほど忙しかった。', '{引|ひ}っ{越|こ}しの{前|まえ}は、{荷造|にづく}りやら{手続|てつづ}きやらで{目|め}が{回|まわ}るほど{忙|いそが}しかった。', 'Trước khi chuyển đi, với việc đóng gói và giấy tờ, tôi đã rất bối rối.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('402', 'N2', '〜ずに済む / 〜ないで済む', 'zu ni sumu / naide sumu', 'May mắn không cần phải làm việc phiền phức mà vẫn giải quyết xong', 'Động từ thể ない (bỏ ない) + ずに済む / Động từ thể ない + で済む', 'Một cái gì đó khó chịu hoặc tốn kém hóa ra lại không cần thiết. する → せずに済む. So sánh Danh từ + で済む, ''chỉ cần ...''.', '友人が車で送ってくれたので、タクシーを使わずに済んだ。', '{友人|ゆうじん}が{車|くるま}で{送|おく}ってくれたので、タクシーを{使|つか}わずに{済|す}んだ。', 'Bạn tôi đã chở tôi đi nên tôi không phải đi taxi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('403', 'N2', '〜ようによっては', 'you ni yotte wa', 'Tùy thuộc vào cách nhìn nhận, cách làm mà sẽ ra kết quả khác nhau', 'Động từ thể ます (bỏ ます) + ようによっては', 'Chủ yếu với 考え, 見, 言い, 使い: kết quả thay đổi tùy theo cách thực hiện A. Thường đưa ra cách giải thích tích cực.', '考えようによっては、今回の失敗はいい教訓になったとも言える。', '{考|かんが}えようによっては、{今回|こんかい}の{失敗|しっぱい}はいい{教訓|きょうくん}になったとも{言|い}える。', 'Tùy thuộc vào cách nhìn nhận, bạn có thể nói thất bại này đã dạy cho chúng ta một bài học tốt.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('404', 'N1', '〜がてら', 'gatera', 'Nhân tiện, tiện thể trên đường làm việc chính thì làm việc phụ', 'Động từ thể ます (bỏ ます) / Danh từ + がてら', 'Một chuyến đi chơi phục vụ hai mục đích; mệnh đề chính thường là chuyển động (行く, 寄る, 歩く). So sánh 〜かたがた (trang trọng, chào hỏi và thăm hỏi) và 〜かたわら (hai hoạt động dài hạn).', '散歩がてら、駅前の本屋に寄って新刊をのぞいてきた。', '{散歩|さんぽ}がてら、{駅前|えきまえ}の{本屋|ほんや}に{寄|よ}って{新刊|しんかん}をのぞいてきた。', 'Khi ra ngoài đi dạo, tôi ghé qua hiệu sách trước nhà ga để xem những cuốn sách mới phát hành.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('405', 'N1', '〜かたわら', 'katawara', 'Bên cạnh công việc chính thì đồng thời còn kiêm thêm việc khác', 'Động từ thể từ điển (辞書形) / Danh từ + の + かたわら', 'Hai hoạt động được theo đuổi song song trong một thời gian dài (một công việc và một việc theo đuổi bên lề), không phải là hành động chỉ thực hiện một lần. Là một danh từ đơn giản, かたわら cũng có nghĩa là ''bên cạnh''.', '彼女は会社勤めのかたわら、週末は日本語教室でボランティアをしている。', '{彼女|かのじょ}は{会社|かいしゃ}{勤|づと}めのかたわら、{週末|しゅうまつ}は{日本語|にほんご}{教室|きょうしつ}でボランティアをしている。', 'Ngoài công việc văn phòng, cô còn tham gia tình nguyện tại một lớp học tiếng Nhật vào cuối tuần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('406', 'N1', '〜が早いか', 'ga hayai ka', 'Vừa mới dứt hành động thì lập tức sự việc khác ập đến ngay tức thì', 'Động từ thể từ điển (辞書形) + が早いか', 'Hành động thứ hai diễn ra gần như ngay lập tức; nó nêu lên một sự kiện trong quá khứ, không bao giờ là một yêu cầu hay ý định. Từ gần đồng nghĩa: 〜や否や (trang trọng nhất), 〜なり (cùng chủ đề, thường gây ngạc nhiên), 〜そばから (lặp lại).', '子どもたちは玄関を開けるが早いか、ランドセルを放り出して遊びに行ってしまった。', '{子|こ}どもたちは{玄関|げんかん}を{開|あ}けるが{早|はや}いか、ランドセルを{放|ほう}り{出|だ}して{遊|あそ}びに{行|い}ってしまった。', 'Bọn trẻ vừa mở cửa trước đã vứt cặp sách sang một bên và chạy đi chơi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('407', 'N1', '〜すら', 'sura', 'Ngay cả, thậm chí đến cả điều đơn giản nhất cũng không...', 'Danh từ (+ particle) + すら', 'Một 〜さえ mang tính văn học hơn. Không giống như さえ, nó không được sử dụng trong cấu trúc 〜さえ〜ば (''giá như'').', '高熱が三日も続き、水を飲むことすらつらかった。', '{高熱|こうねつ}が{三日|みっか}も{続|つづ}き、{水|みず}を{飲|の}むことすらつらかった。', 'Sốt cao kéo dài ba ngày, ngay cả nước uống cũng khó khăn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('408', 'N1', '〜ずくめ', 'zukume', 'Toàn là điều tốt lành / Toàn là chuyện xui rủi nối tiếp', 'Danh từ + ずくめ', 'Hoạt động với một số danh từ hạn chế (黒, いいこと, 規則, ごちそう, 結構). Không giống như だらけ hay まみれ nó có thể mang nghĩa tích cực và không có nghĩa là được che chắn về mặt vật lý.', '昇進に結婚、新居の購入と、今年はいいことずくめの一年だった。', '{昇進|しょうしん}に{結婚|けっこん}、{新居|しんきょ}の{購入|こうにゅう}と、{今年|ことし}はいいことずくめの{一|いち}{年|ねん}だった。', 'Một sự thăng tiến, một đám cưới và một ngôi nhà mới - năm nay không mang lại điều gì ngoài những điều tốt đẹp.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('409', 'N1', '〜たら最後 / 〜が最後', 'tara saigo / ga saigo', 'Một khi đã lỡ... thì coi như xong đời, không thể cứu vãn', 'Động từ thể た (quá khứ) + ら最後 / Động từ thể た (quá khứ) + が最後', 'Kết quả là xấu hoặc không thể đảo ngược và được tuyên bố một cách chắc chắn. 〜が最後 mang tính văn chương hơn một chút.', 'この子は一度泣き出したら最後、何をしても一時間は泣きやまない。', 'この{子|こ}は{一度|いちど}{泣|な}き{出|だ}したら{最後|さいご}、{何|なに}をしても{一|いち}{時間|じかん}は{泣|な}きやまない。', 'Một khi đứa trẻ này bắt đầu khóc, không có gì có thể ngăn cản nó trong một giờ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('410', 'N1', '〜てからというもの', 'te kara to iu mono', 'Kể từ thời khắc sự kiện đó xảy ra thì cuộc sống hoàn toàn đổi khác', 'Động từ thể て + からというもの', 'Nhấn mạnh một sự thay đổi lâu dài, rõ rệt bắt đầu từ sự kiện đó; nhiều cảm xúc hơn so với 〜て以来 trung lập.', '犬を飼い始めてからというもの、父は毎朝六時に起きるようになった。', '{犬|いぬ}を{飼|か}い{始|はじ}めてからというもの、{父|ちち}は{毎朝|まいあさ}{六|ろく}{時|じ}に{起|お}きるようになった。', 'Kể từ khi chúng tôi có một con chó, bố tôi luôn thức dậy lúc sáu giờ sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('411', 'N1', '〜てやまない', 'te yamanai', 'Không ngừng tha thiết cầu chúc từ tận đáy lòng', 'Động từ thể て + やまない', 'Dùng với các động từ chỉ cảm giác như 願う, 祈る, 愛する, 尊敬する, 期待する. Điển hình của các bài phát biểu, thư từ và văn bản trang trọng.', '皆様のご健康とご多幸を心より願ってやみません。', '{皆様|みなさま}のご{健康|けんこう}とご{多幸|たこう}を{心|こころ}より{願|ねが}ってやみません。', 'Tôi chân thành chúc tất cả các bạn sức khỏe và hạnh phúc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('412', 'N1', '〜と相まって', 'to aimatte', 'Kết hợp cùng với yếu tố khác tạo nên hiệu ứng tổng hòa', 'Danh từ + と相まって / Danh từ + も相まって', 'Hai yếu tố củng cố lẫn nhau để tạo ra hiệu ứng mạnh mẽ hơn. Ngoài ra còn có AとBが相まって và 〜も相まって.', '色づいた紅葉が澄んだ秋空と相まって、息をのむような景色をつくり出していた。', '{色|いろ}づいた{紅葉|こうよう}が{澄|す}んだ{秋空|あきぞら}と{相|あい}まって、{息|いき}をのむような{景色|けしき}をつくり{出|だ}していた。', 'Những chiếc lá mùa thu đỏ rực cùng với bầu trời mùa thu trong trẻo đã tạo nên một khung cảnh ngoạn mục.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('413', 'N1', '〜ともなると / 〜ともなれば', 'tomo naru to / tomo nareba', 'Một khi đã đạt đến tầm cỡ, vị thế cao như vậy thì đương nhiên', 'Danh từ / Động từ thể từ điển (辞書形) + ともなると / Danh từ / Động từ thể từ điển (辞書形) + ともなれば', 'Danh từ đánh dấu một địa vị, thời gian hoặc số lượng cao đáng kể; phần còn lại nêu rõ những gì tự nhiên đi kèm với nó.', '大企業の社長ともなると、分刻みのスケジュールで動かなければならない。', '{大|だい}{企業|きぎょう}の{社長|しゃちょう}ともなると、{分|ふん}{刻|きざ}みのスケジュールで{動|うご}かなければならない。', 'Khi bạn là chủ tịch của một công ty lớn, bạn phải làm việc theo lịch trình từng phút một.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('414', 'N1', '〜ならでは', 'nara de wa', 'Chỉ duy nhất nơi đó / Người đó mới có được nét độc đáo này', 'Danh từ + ならではの + Danh từ / Danh từ + ならではだ', 'Hầu như luôn luôn khen ngợi. Văn học AならではBない có nghĩa là ''chỉ A mới có thể B''.', '港町ならではの新鮮な魚介料理を、心ゆくまで味わった。', '{港町|みなとまち}ならではの{新鮮|しんせん}な{魚介|ぎょかい}{料理|りょうり}を、{心|こころ}ゆくまで{味|あじ}わった。', 'Chúng tôi thưởng thức tận tình những món hải sản tươi sống mà bạn chỉ có thể có được ở một thị trấn cảng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('415', 'N1', '〜べく', 'beku', 'Nhằm mục đích, với quyết tâm thực hiện việc gì', 'Động từ thể từ điển (辞書形) + べく (する → すべく)', 'Phong cách viết trang trọng; する thường trở thành すべく. Mệnh đề chính không thể là một yêu cầu hoặc mệnh lệnh.', '同社は海外市場に進出すべく、現地に新たな拠点を設けた。', '{同社|どうしゃ}は{海外|かいがい}{市場|しじょう}に{進出|しんしゅつ}すべく、{現地|げんち}に{新|あら}たな{拠点|きょてん}を{設|もう}けた。', 'Công ty đã thành lập một cơ sở mới tại địa phương để mở rộng ra thị trường nước ngoài.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('416', 'N1', '〜(よ)うものなら', 'you mono nara', 'Nếu chẳng may lỡ xảy ra chuyện đó thì hậu quả khôn lường', 'Động từ thể ý chí + ものなら', 'Phóng đại sự nguy hiểm của một hành động. Đừng nhầm lẫn với 〜ものなら (N2), có dạng tiềm năng cho một điều ước không thể xảy ra: 行けるものなら行きたい.', '父の前で母の料理に文句でも言おうものなら、大変なことになる。', '{父|ちち}の{前|まえ}で{母|はは}の{料理|りょうり}に{文句|もんく}でも{言|い}おうものなら、{大変|たいへん}なことになる。', 'Nếu tôi phàn nàn về việc mẹ nấu ăn trước mặt bố thì tôi sẽ phải trả giá đắt lắm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('417', 'N1', '〜に至る / 〜に至って / 〜に至っては', 'ni itaru / ni itatte / ni itatte wa', 'Phát triển tới mức nghiêm trọng; Đạt tới giai đoạn', 'Danh từ / Động từ thể từ điển (辞書形) + に至る / Danh từ / Động từ thể từ điển (辞書形) + に至って / Danh từ + に至っては', 'に至る: đạt đến điểm (nghiêm trọng). に至って: chỉ một lần mọi việc đến mức đó. に至っては: giới thiệu ví dụ cực đoan nhất, thường là tiêu cực. Liên quan: 〜に至るまで ''xuống, thậm chí''.', '小さな行き違いが、ついには両社の提携解消にまで至った。', '{小|ちい}さな{行|い}き{違|ちが}いが、ついには{両社|りょうしゃ}の{提携|ていけい}{解消|かいしょう}にまで{至|いた}った。', 'Một sự hiểu lầm nhỏ cuối cùng đã dẫn đến việc hai công ty giải thể mối quan hệ đối tác của họ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('418', 'N1', '〜を禁じ得ない', 'o kinjienai', 'Không thể nào kìm nén được cảm xúc xao xuyến, tức giận, xót xa', 'Danh từ + を禁じ得ない', 'Dùng với các danh từ chỉ cảm xúc như 怒り, 憤り, 同情, 驚き, 涙, 疑問. Chính thức; kỳ lạ đối với những cảm xúc tầm thường hàng ngày.', '罪のない子どもたちが犠牲になったことに、深い憤りを禁じ得ない。', '{罪|つみ}のない{子|こ}どもたちが{犠牲|ぎせい}になったことに、{深|ふか}い{憤|いきどお}りを{禁|きん}じ{得|え}ない。', 'Tôi không khỏi cảm thấy phẫn nộ sâu sắc trước việc những đứa trẻ vô tội lại trở thành nạn nhân.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('419', 'N1', '〜を余儀なくされる', 'o yogi naku sareru', 'Bị ép buộc rơi vào tình thế không thể không làm', 'Danh từ + を余儀なくされる', 'Danh từ này thường là danh từ suru chỉ một hành động không mong muốn (中止, 撤退, 変更, 入院). Chủ động 〜を余儀なくさせる có nghĩa là ''ép buộc (ai đó) phải''.', '大型台風の接近により、多くの便が欠航を余儀なくされた。', '{大型|おおがた}{台風|たいふう}の{接近|せっきん}により、{多|おお}くの{便|びん}が{欠航|けっこう}を{余儀|よぎ}なくされた。', 'Khi một cơn bão lớn đang đến gần, nhiều chuyến bay phải hủy bỏ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('420', 'N1', '〜あっての', 'atte no', 'Chính nhờ có sự tồn tại của đối tượng đó thì mới có được thành quả hôm nay', 'Danh từ + あっての + Danh từ / Danh từ + あってのことだ', 'AあってのB: B chỉ tồn tại nhờ có A nên A là tất yếu. Thường bày tỏ lòng biết ơn.', 'お客様あっての商売ですから、どんな小さな声にも耳を傾けなければならない。', 'お{客様|きゃくさま}あっての{商売|しょうばい}ですから、どんな{小|ちい}さな{声|こえ}にも{耳|みみ}を{傾|かたむ}けなければならない。', 'Hoạt động kinh doanh của chúng tôi tồn tại chỉ vì khách hàng, vì vậy chúng tôi phải lắng nghe ngay cả những tiếng nói nhỏ nhất.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('421', 'N1', '〜いかんで / 〜いかんによらず', 'ikan de / ikan ni yorazu', 'Tùy thuộc vào... / Bất kể tình hình ra sao đi chăng nữa', 'Danh từ (+ の) + いかんで / Danh từ (+ の) + いかんによらず', 'Rất trang trọng (いかん = ''như thế nào, cái gì''). Các biến thể: 〜いかんによって / いかんでは (tùy theo); 〜いかんにかかわらず / いかんを問わず (bất kể).', '来季の契約は、今シーズンの成績いかんで決まる。', '{来季|らいき}の{契約|けいやく}は、{今|こん}シーズンの{成績|せいせき}いかんで{決|き}まる。', 'Hợp đồng mùa tới sẽ phụ thuộc vào thành tích mùa này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('422', 'N1', '〜(よ)うが / 〜(よ)うと', 'you ga / you to', 'Cho dù có làm hay không thì kết quả vẫn chẳng thay đổi', 'Động từ thể ý chí + が / Động từ thể ý chí + と / Tính từ đuôi い stem + かろうが / Tính từ đuôi な + だろうが / Danh từ + だろうが', 'Nhấn mạnh hơn 〜ても; thường đi kèm với các từ để hỏi (何を, どこへ, いくら) hoặc どんなに. Mệnh đề chính thể hiện kết quả không bị ảnh hưởng.', '周りに何と言われようが、自分で決めた道を進むつもりだ。', '{周|まわ}りに{何|なん}と{言|い}われようが、{自分|じぶん}で{決|き}めた{道|みち}を{進|すす}むつもりだ。', 'Dù mọi người xung quanh có nói gì thì tôi vẫn quyết tâm đi theo con đường mình đã chọn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('423', 'N1', '〜(よ)うが〜まいが / 〜(よ)うと〜まいと', 'you ga ... mai ga / you to ... mai to', 'Dù có xảy ra hay không xảy ra thì quyết định vẫn giữ nguyên', 'Động từ thể ý chí + が + Động từ thể từ điển (辞書形) + まいが / Động từ thể ý chí + と + Động từ thể từ điển (辞書形) + まいと', 'Động từ tương tự được lặp lại. Đối với động từ ichidan và する/来る, まい có thể gắn vào gốc nai: 認めまい, しまい (hoặc すまい), こまい.', '君が賛成しようがしまいが、この計画はもう動き出している。', '{君|きみ}が{賛成|さんせい}しようがしまいが、この{計画|けいかく}はもう{動|うご}き{出|だ}している。', 'Cho dù bạn có đồng ý hay không, kế hoạch này đã được thực hiện.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('424', 'N1', '〜(よ)うにも〜ない', 'you ni mo ... nai', 'Dù rất muốn làm nhưng hoàn toàn không thể làm nổi', 'Động từ thể ý chí + にも + Động từ thể ない (potential of the same verb)', 'Một số hoàn cảnh bên ngoài cản trở hành động dự định. Động từ thứ hai thường là thế âm của cùng một động từ, hoặc 〜ようがない.', '財布を家に忘れてきたので、昼ご飯を買おうにも買えなかった。', '{財布|さいふ}を{家|いえ}に{忘|わす}れてきたので、{昼|ひる}ご{飯|はん}を{買|か}おうにも{買|か}えなかった。', 'Tôi để quên ví ở nhà nên dù rất muốn nhưng tôi cũng không thể mua được bữa trưa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('425', 'N1', '〜かぎりだ', 'kagiri da', 'Vô cùng, hết sức (cảm xúc tột độ của người nói)', 'Tính từ đuôi い / Tính từ đuôi な + な + かぎりだ', 'Thể hiện cảm xúc mạnh mẽ của người nói bằng các tính từ cảm xúc (うれしい, 寂しい, 心細い, うらやましい, 残念な). Không sử dụng cho cảm xúc của người khác.', '長年の夢だった自分の店をついに開くことができ、うれしいかぎりだ。', '{長年|ながねん}の{夢|ゆめ}だった{自分|じぶん}の{店|みせ}をついに{開|ひら}くことができ、うれしいかぎりだ。', 'Cuối cùng tôi đã mở được cửa hàng của riêng mình, một giấc mơ bấy lâu nay và tôi không thể hạnh phúc hơn được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('426', 'N1', '〜かたがた', 'katagata', 'Tiện thể kết hợp thực hiện hai mục đích trang trọng cùng lúc', 'Danh từ + かたがた', 'Chính thức; thường đi kèm với お礼, ご挨拶, ご報告, お見舞い cộng với động từ thăm viếng (伺う, 訪ねる). So sánh 〜がてら thông thường và 〜かたわら (hoạt động dài hạn song song).', '先日のお礼かたがた、ご挨拶に伺いたいと存じます。', '{先日|せんじつ}のお{礼|れい}かたがた、ご{挨拶|あいさつ}に{伺|うかが}いたいと{存|ぞん}じます。', 'Tôi muốn kêu gọi các bạn bày tỏ lòng kính trọng của tôi và cũng để cảm ơn các bạn về ngày hôm đó.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('427', 'N1', '〜かと思いきや', 'ka to omoikiya', 'Cứ ngỡ là sẽ diễn ra như thế, nào ngờ thực tế lại đảo ngược hoàn toàn', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + かと思いきや', 'Kết quả trái ngược với những gì được mong đợi. Ngoài ra 〜と思いきや. Được tìm thấy trong bài viết và bài tiểu luận sống động hơn là trong bài phát biểu.', '今年の冬は暖かいかと思いきや、二月に入って記録的な大雪に見舞われた。', '{今年|ことし}の{冬|ふゆ}は{暖|あたた}かいかと{思|おも}いきや、{二月|にがつ}に{入|はい}って{記録|きろく}{的|てき}な{大雪|おおゆき}に{見舞|みま}われた。', 'Chúng tôi tưởng mùa đông năm nay sẽ ôn hòa nhưng rồi tháng Hai lại mang đến lượng tuyết kỷ lục.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('428', 'N1', '〜きらいがある', 'kirai ga aru', 'Có tật xấu, có xu hướng dễ mắc phải thói quen xấu', 'Động từ thể từ điển (辞書形) / Động từ thể ない / Danh từ + の + きらいがある', 'Luôn phê phán; phổ biến trong bình luận về thói quen hoặc thể chế của mọi người. So sánh 〜がち đàm thoại và 〜傾向がある trung tính.', '彼は何事も自分一人で抱え込むきらいがある。', '{彼|かれ}は{何事|なにごと}も{自分|じぶん}{一人|ひとり}で{抱|かか}え{込|こ}むきらいがある。', 'Anh ấy có xu hướng tự mình gánh vác mọi việc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('429', 'N1', '〜極まる / 〜極まりない', 'kiwamaru / kiwamari nai', 'Cực kỳ, vô cùng (mức độ chạm đỉnh, nguy hiểm cực điểm)', 'Tính từ đuôi な + 極まる / Tính từ đuôi な + 極まりない', 'Cả hai đều có nghĩa giống nhau; 極まりない ngày nay phổ biến hơn. Chủ yếu đi kèm với tính từ na phủ định (失礼, 危険, 不愉快, 退屈). 感極まる ''vượt qua cảm xúc'' là một cụm từ cố định.', '公共の場で大声で電話をするなんて、迷惑極まりない。', '{公共|こうきょう}の{場|ば}で{大声|おおごえ}で{電話|でんわ}をするなんて、{迷惑|めいわく}{極|きわ}まりない。', 'Nói to trên điện thoại ở nơi công cộng là vô cùng thiếu cân nhắc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('430', 'N1', '〜の極み', 'no kiwami', 'Đỉnh điểm của sự vinh quang / Đỉnh điểm của sự đau đớn', 'Danh từ + の極み', 'Phổ biến với 光栄, 感激, 痛恨, ぜいたく, 疲労. So sánh 〜の至り, vốn được giới hạn ở một vài cụm từ cố định về cảm xúc của chính người nói.', '決勝戦であのような凡ミスをしたことは、まさに痛恨の極みだ。', '{決勝|けっしょう}{戦|せん}であのような{凡|ぼん}ミスをしたことは、まさに{痛恨|つうこん}の{極|きわ}みだ。', 'Việc mắc phải lỗi bất cẩn như vậy trong trận chung kết là sự hối tiếc cay đắng nhất có thể tưởng tượng được.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('431', 'N1', '〜ごとき / 〜ごとく', 'gotoki / gotoku', 'Như là, hệt như là... (văn phong cổ điển, trang trọng)', 'Danh từ + の + ごとく / Động từ thể thông thường (V ngắn) + (が) + ごとく / Danh từ + (の) + ごとき + Danh từ', 'Văn học tương đương với 〜ように / 〜ような. ごとき ngay sau khi một người tỏ ra khinh thường (お前ごとき) hoặc, đối với bản thân, sự khiêm tốn (私ごとき).', '彼女は何事もなかったかのごとく、静かに微笑んでいた。', '{彼女|かのじょ}は{何事|なにごと}もなかったかのごとく、{静|しず}かに{微笑|ほほえ}んでいた。', 'Cô ấy chỉ cười nhẹ như không có chuyện gì xảy ra.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('432', 'N1', '〜こととて', 'koto tote', 'Bởi vì lý do đó nên mong được lượng thứ bỏ qua', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi な + な / Danh từ + の + こととて', 'Cổ hủ và trang trọng, chủ yếu là trong lời xin lỗi hoặc yêu cầu sự hiểu biết. Thường dùng với ぬ (知らぬこととて ''vì tôi không biết'').', '何分にも不慣れなこととて、至らぬ点があればご容赦ください。', '{何分|なにぶん}にも{不慣|ふな}れなこととて、{至|いた}らぬ{点|てん}があればご{容赦|ようしゃ}ください。', 'Vì tôi vẫn chưa quen với điều này, xin vui lòng tha thứ cho bất kỳ thiếu sót nào.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('433', 'N1', '〜ことなしに', 'koto nashi ni', 'Mà không hề có hành động nào xảy ra', 'Động từ thể từ điển (辞書形) + ことなしに', 'Thường theo sau là điều không thể xảy ra (“không có A thì B không thể xảy ra”). Gần với 〜ことなく (N2), nghĩa đơn giản là “không làm gì”.', '基礎を固めることなしに、応用力は身につかない。', '{基礎|きそ}を{固|かた}めることなしに、{応用|おうよう}{力|りょく}は{身|み}につかない。', 'Nếu không xây dựng nền tảng vững chắc, bạn sẽ không bao giờ phát triển được khả năng áp dụng những gì mình biết.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('434', 'N1', '〜始末だ', 'shimatsu da', 'Rốt cuộc lại dẫn tới kết cục thảm hại, đáng chê trách', 'Động từ thể từ điển (辞書形) + 始末だ / この + 始末だ', 'Một kết cục tồi tệ sau một loạt sự kiện, được kể lại với vẻ bực tức. Thường đi kèm với とうとう, ついには, 挙げ句の果てに.', '弟は遊んでばかりいて、とうとう単位が足りずに留年する始末だ。', '{弟|おとうと}は{遊|あそ}んでばかりいて、とうとう{単位|たんい}が{足|た}りずに{留年|りゅうねん}する{始末|しまつ}だ。', 'Anh trai tôi không làm gì khác ngoài việc chơi đùa và cuối cùng anh ấy phải học lại một năm vì thiếu tín chỉ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('435', 'N1', '〜ずじまい', 'zu jimai', 'Rốt cuộc đến cuối cùng vẫn không kịp làm việc đó', 'Động từ thể ない (bỏ ない) + ずじまい (する → せずじまい)', 'Tiếc nuối vì một hành động dự định đã không bao giờ xảy ra trước khi cơ hội trôi qua. Thông thường là 〜ずじまいだった / 〜ずじまいになった.', '旅行中は雨続きで、楽しみにしていた山には登らずじまいだった。', '{旅行|りょこう}{中|ちゅう}は{雨|あめ}{続|つづ}きで、{楽|たの}しみにしていた{山|やま}には{登|のぼ}らずじまいだった。', 'Trời mưa suốt chuyến đi nên tôi chưa bao giờ được leo lên ngọn núi mà mình hằng mong đợi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('436', 'N1', '〜ずにはおかない / 〜ないではおかない', 'zu ni wa okanai / nai de wa okanai', 'Nhất định sẽ trừng phạt, nhất định phải làm cho bằng được', 'Động từ thể ない (bỏ ない) + ずにはおかない (する → せずにはおかない) / Động từ thể ない + ではおかない', 'Hai cách sử dụng: quyết tâm kiên quyết, hoặc điều gì đó chắc chắn gây ra cảm giác hoặc phản ứng. So sánh 〜ずにはいられない (N2), sự thôi thúc không thể cưỡng lại của bản thân.', '彼女の力強い歌声は、聴く者の心を揺さぶらずにはおかない。', '{彼女|かのじょ}の{力強|ちからづよ}い{歌声|うたごえ}は、{聴|き}く{者|もの}の{心|こころ}を{揺|ゆ}さぶらずにはおかない。', 'Giọng hát đầy nội lực của cô không thể không lay động trái tim người nghe.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('437', 'N1', '〜ずにはすまない / 〜ないではすまない', 'zu ni wa sumanai / nai de wa sumanai', 'Không thể nào xong chuyện nếu không làm việc xin lỗi/đền bù', 'Động từ thể ない (bỏ ない) + ずにはすまない (する → せずにはすまない) / Động từ thể ない + ではすまない', 'Nghĩa vụ xuất phát từ các chuẩn mực xã hội hoặc hoàn cảnh chứ không phải mong muốn cá nhân. 〜ずには済まされない là một biến thể mạnh hơn.', '大切な花瓶を割ってしまったのだから、弁償せずにはすまないだろう。', '{大切|たいせつ}な{花瓶|かびん}を{割|わ}ってしまったのだから、{弁償|べんしょう}せずにはすまないだろう。', 'Vì tôi đã làm vỡ chiếc bình quý giá đó nên tôi sẽ không thể rời đi mà không phải trả tiền.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('438', 'N1', '〜そばから', 'soba kara', 'Vừa mới làm xong thì ngay lập tức lại bị xóa/quên mất', 'Động từ thể từ điển (辞書形) / Động từ thể た (quá khứ) + そばから', 'Một mô hình lặp đi lặp lại trong đó hành động thứ hai hủy bỏ hành động đầu tiên, thường gây thất vọng. Không giống như 〜や否や hay 〜なり, nó không được sử dụng cho một sự kiện diễn ra một lần.', '片付けるそばから、子どもたちがおもちゃを散らかしていく。', '{片付|かたづ}けるそばから、{子|こ}どもたちがおもちゃを{散|ち}らかしていく。', 'Ngay khi tôi dọn dẹp xong, bọn trẻ lại rải đồ chơi của chúng ra xung quanh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('439', 'N1', '〜たりとも', 'tari tomo', 'Dù chỉ là một chút, một giây, một người cũng tuyệt đối không...', 'Danh từ (一 + counter) + たりとも', 'Gắn vào 一 + bộ đếm (一円, 一日, 一人, 一瞬) và theo sau là một câu phủ định hoặc cấm đoán. Ngoài ra 何人たりとも ''không có ai cả''.', '税金を一円たりとも無駄にしてはならない。', '{税金|ぜいきん}を{一|いち}{円|えん}たりとも{無駄|むだ}にしてはならない。', 'Không một đồng tiền thuế nào bị lãng phí.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('440', 'N1', '〜だに', 'dani', 'Thậm chí ngay cả trong suy nghĩ, trong mơ cũng chưa từng...', 'Động từ thể từ điển (辞書形) + だに / Danh từ + だに', 'Cổ xưa; tồn tại chủ yếu trong các cụm từ cố định: 想像するだに / 考えるだに (''chỉ nghĩ về nó thôi''), 微動だにしない, 想像だにしない /夢想だにしない (''thậm chí không bao giờ tưởng tượng được'').', 'あの事故の瞬間は、思い出すだに身震いがする。', 'あの{事故|じこ}の{瞬間|しゅんかん}は、{思|おも}い{出|だ}すだに{身震|みぶる}いがする。', 'Chỉ cần nhớ lại khoảnh khắc xảy ra tai nạn đó thôi cũng khiến tôi rùng mình.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('441', 'N1', '〜たる', 'taru', 'Xứng danh là, trên tư cách là người có vị thế cao', 'Danh từ + たる + Danh từ / Danh từ + たるもの', 'Nhấn mạnh các nhiệm vụ hoặc phẩm giá được mong đợi ở một vai trò. 〜たる者 / 〜たるもの được theo sau bởi những gì một người như vậy nên (không) làm.', '教師たる者、生徒の前で模範を示さなければならない。', '{教師|きょうし}たる{者|もの}、{生徒|せいと}の{前|まえ}で{模範|もはん}を{示|しめ}さなければならない。', 'Bất cứ ai là giáo viên đều phải làm gương trước mặt học sinh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('442', 'N1', '〜つ〜つ', 'tsu ... tsu', 'Lúc thì làm việc này, lúc lại làm việc kia sóng đôi liên tiếp', 'Động từ thể ます (bỏ ます) + つ + Động từ thể ます (bỏ ます) + つ', 'Chỉ trong các cụm từ có động từ trái nghĩa hoặc cặp chủ động/thụ động: 行きつ戻りつ, 持ちつ持たれつ, 抜きつ抜かれつ, 浮きつ沈みつ.', '近所の人たちとは、持ちつ持たれつの関係でやってきた。', '{近所|きんじょ}の{人|ひと}たちとは、{持|も}ちつ{持|も}たれつの{関係|かんけい}でやってきた。', 'Hàng xóm của tôi và tôi luôn giúp đỡ lẫn nhau.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('443', 'N1', '〜であれ / 〜であれ〜であれ', 'de are / de are ... de are', 'Dù cho là... hay là... thì bản chất vẫn không đổi', 'Danh từ + であれ / Từ để hỏi + であれ / Danh từ + であれ + Danh từ + であれ', 'Chữ viết tương đương với 〜でも / 〜だろうと. 〜であろうと và 〜であろうが là những biến thể gần giống nhau.', '理由が何であれ、暴力は決して許されない。', '{理由|りゆう}が{何|なん}であれ、{暴力|ぼうりょく}は{決|けっ}して{許|ゆる}されない。', 'Dù lý do là gì đi nữa, bạo lực không bao giờ được chấp nhận.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('444', 'N1', '〜てしかるべきだ', 'te shikaru beki da', 'Lẽ dĩ nhiên phải làm việc đó theo chuẩn mực đạo lý', 'Động từ thể て + しかるべきだ / Tính từ đuôi い-ku + て + しかるべきだ / Tính từ đuôi な + であって + しかるべきだ', 'Nói rõ điều gì là đúng đắn, thường ở dạng thụ động và thường hàm ý chỉ trích rằng điều đó đã không xảy ra. Tính từ しかるべき cũng có nghĩa là ''thích hợp''.', 'これだけの損害を出したのだから、経営陣は責任を問われてしかるべきだ。', 'これだけの{損害|そんがい}を{出|だ}したのだから、{経営|けいえい}{陣|じん}は{責任|せきにん}を{問|と}われてしかるべきだ。', 'Gây ra thiệt hại lớn như vậy, ban quản lý phải chịu trách nhiệm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('445', 'N1', '〜ではあるまいし', 'de wa aru mai shi', 'Đâu phải là trẻ con nữa mà lại hành xử như thế!', 'Danh từ + ではあるまいし / Động từ thể từ điển (辞書形) + の + ではあるまいし', 'Tiếp theo là một bản án, lời khuyên hoặc mệnh lệnh. Mặc dù là まい trang trọng nhưng nó vẫn phổ biến trong lời nói, đặc biệt là 〜じゃあるまいし.', '子どもではあるまいし、自分のことくらい自分で決めなさい。', '{子|こ}どもではあるまいし、{自分|じぶん}のことくらい{自分|じぶん}で{決|き}めなさい。', 'Bạn không phải là một đứa trẻ, vì vậy hãy tự đưa ra quyết định về bản thân.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('446', 'N1', '〜手前', 'temae', 'Vì đã lỡ hứa / Vì danh dự trước mặt người khác nên buộc phải làm', 'Động từ thể thông thường (V ngắn) / Danh từ + の + 手前', 'Bởi vì những gì một người đã nói hoặc vị trí của một người trước mặt người khác, người ta cảm thấy buộc phải hành động theo một cách nhất định. Thường theo sau là 〜わけにはいかない hoặc 〜しかない.', '部下の手前、上司の私が弱音を吐くことはできなかった。', '{部下|ぶか}の{手前|てまえ}、{上司|じょうし}の{私|わたし}が{弱音|よわね}を{吐|は}くことはできなかった。', 'Trước mặt cấp dưới của mình, tôi không thể để mình phàn nàn với tư cách là cấp trên của họ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('447', 'N1', '〜てもさしつかえない', 'te mo sashitsukae nai', 'Dù có làm... thì cũng không gây trở ngại gì', 'Động từ thể て + もさしつかえない / Tính từ đuôi い-ku + て + もさしつかえない / Tính từ đuôi な + でもさしつかえない / Danh từ + でもさしつかえない', 'Một 〜てもいい / 〜ても構わない trang trọng hơn, phổ biến trong kinh doanh, thông báo và hướng dẫn chính thức. Thường được viết 差し支えない.', '書類は鉛筆で記入してもさしつかえありません。', '{書類|しょるい}は{鉛筆|えんぴつ}で{記入|きにゅう}してもさしつかえありません。', 'Bạn có thể điền vào mẫu bằng bút chì.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('448', 'N1', '〜とあって', 'to atte', 'Vì là trong hoàn cảnh đặc biệt như thế nên việc đó diễn ra là tất yếu', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + とあって', 'Nguyên nhân là một tình huống đặc biệt, đáng chú ý và mệnh đề chính tường thuật kết quả tự nhiên, được quan sát thấy của nó. Phổ biến trong tin tức; không được dùng cho mong muốn hoặc yêu cầu của chính người nói.', '三連休の初日とあって、高速道路はどこも大渋滞だった。', '{三|さん}{連休|れんきゅう}の{初日|しょにち}とあって、{高速|こうそく}{道路|どうろ}はどこも{大|だい}{渋滞|じゅうたい}だった。', 'Hôm nay là ngày đầu tiên của kỳ nghỉ cuối tuần kéo dài ba ngày, đường cao tốc khắp nơi đều tắc nghẽn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('449', 'N1', '〜とあれば', 'to areba', 'Nếu rơi vào tình huống đặc biệt như vậy thì đành phải chấp nhận', 'Động từ thể thông thường (V ngắn) / Danh từ + とあれば', 'Một điều kiện đặc biệt mà theo đó một người sẽ có một ngoại lệ hoặc làm điều gì đó khó khăn. Thường xuyên 〜のためとあれば.', '子どもの命を救うためとあれば、どんな犠牲も惜しまない。', '{子|こ}どもの{命|いのち}を{救|すく}うためとあれば、どんな{犠牲|ぎせい}も{惜|お}しまない。', 'Nếu muốn cứu mạng con tôi, tôi sẽ không tiếc hy sinh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('450', 'N1', '〜といい〜といい', 'to ii ... to ii', 'Dù nhìn về mặt này hay nhìn về mặt kia đều hoàn hảo / tồi tệ', 'Danh từ + といい + Danh từ + といい', 'Chọn ra hai khía cạnh làm bằng chứng cho đánh giá tổng thể, tích cực hoặc tiêu cực. Không giống như 〜も〜も thông thường, nó mang ý nghĩa phán đoán của người nói.', 'この旅館は、料理といい接客といい、申し分ない。', 'この{旅館|りょかん}は、{料理|りょうり}といい{接客|せっきゃく}といい、{申|もう}し{分|ぶん}ない。', 'Nhà trọ này là hoàn hảo, cả về đồ ăn lẫn dịch vụ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('451', 'N1', '〜といったところだ', 'to itta tokoro da', 'Cao nhất cũng chỉ ở mức tầm tầm đó mà thôi', 'Danh từ / Động từ thể từ điển (辞書形) + といったところだ', 'Ước tính sơ bộ, khiêm tốn: số lượng hoặc mức độ không lớn. Thường đi kèm với せいぜい hoặc まあ. Ngoài ra 〜というところだ.', '私の料理の腕前は、せいぜいカレーが作れるといったところだ。', '{私|わたし}の{料理|りょうり}の{腕前|うでまえ}は、せいぜいカレーが{作|つく}れるといったところだ。', 'Kỹ năng nấu nướng của tôi tốt nhất là làm món cà ri.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('452', 'N1', '〜といえども', 'to iedomo', 'Dẫu là bậc vĩ nhân, dẫu là chuyên gia đi chăng nữa thì vẫn có lúc sơ suất', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + といえども', 'Phiên bản chính thức của 〜といっても / 〜であっても: ngay cả người có địa vị đó cũng không được miễn. Thường đi kèm với いかなる hoặc たとえ.', '子どもといえども、一人の人間として尊重されなければならない。', '{子|こ}どもといえども、{一人|ひとり}の{人間|にんげん}として{尊重|そんちょう}されなければならない。', 'Trẻ em cũng phải được tôn trọng với tư cách cá nhân.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('453', 'N1', '〜といったらない', 'to ittara nai', 'Không từ ngữ nào có thể diễn tả xiết được mức độ ấy!', 'Tính từ đuôi い / Tính từ đuôi な / Danh từ + といったらない', 'Hoạt động cho cả cảm giác tích cực và tiêu cực. Hình thức thông thường: 〜ったらない, và 〜といったらありゃしない (chỉ phủ định).', '決勝戦で一点差で負けたときの悔しさといったらなかった。', '{決勝|けっしょう}{戦|せん}で{一点|いってん}{差|さ}で{負|ま}けたときの{悔|くや}しさといったらなかった。', 'Sự thất vọng khi thua trận chung kết chỉ với một điểm không thể diễn tả bằng lời.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('454', 'N1', '〜ときたら', 'to kitara', 'Cứ hễ nhắc đến kẻ đó là lại bực mình!', 'Danh từ + ときたら', 'Giới thiệu một người hoặc vật quen thuộc mà người nói đã chán ngán; những gì tiếp theo là một lời phàn nàn. Đàm thoại.', 'うちの夫ときたら、休みの日は一日中ソファでごろごろしている。', 'うちの{夫|おっと}ときたら、{休|やす}みの{日|ひ}は{一日中|いちにちじゅう}ソファでごろごろしている。', 'Còn chồng tôi, những ngày nghỉ anh ấy đều nằm dài trên sofa.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('455', 'N1', '〜としたところで / 〜にしたところで', 'to shita tokoro de / ni shita tokoro de', 'Ngay cả khi đứng trên lập trường của họ thì cũng bất lực', 'Động từ thể thông thường (V ngắn) / Danh từ + としたところで / Danh từ + にしたところで', 'Giống như 〜としても / 〜にしても, nhưng nhấn mạnh rằng kết quả sẽ không thay đổi. Bình thường: 〜としたって / 〜にしたって.', '今から急いだとしたところで、最終電車には間に合わないだろう。', '{今|いま}から{急|いそ}いだとしたところで、{最終|さいしゅう}{電車|でんしゃ}には{間|ま}に{合|あ}わないだろう。', 'Ngay cả khi chúng ta vội vã bây giờ, có lẽ chúng ta sẽ không bắt được chuyến tàu cuối cùng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('456', 'N1', '〜とは', 'to wa', 'Cái chuyện bất ngờ như thế mà lại xảy ra được ư! (kinh ngạc)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + とは', 'Có thể kết thúc câu, không để lại phản ứng, hoặc theo sau là 思わなかった, 驚いた, v.v. Khác với định nghĩa 〜とは (''X có nghĩa là ...'').', 'あの真面目な彼が、会社の金に手をつけていたとは。', 'あの{真面目|まじめ}な{彼|かれ}が、{会社|かいしゃ}の{金|かね}に{手|て}をつけていたとは。', 'Nghĩ đến việc một người nghiêm túc như anh ta lại nhúng tay vào tiền của công ty!')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('457', 'N1', '〜とばかりに', 'to bakari ni', 'Tựa như muốn nói rằng, dường như muốn ra hiệu rằng...', 'Mệnh đề / Câu + とばかりに', 'Mô tả thái độ hoặc cử chỉ của người khác dường như muốn nói điều gì đó mà không cần lời nói; không được sử dụng về bản thân. So sánh 〜んばかり (gần đạt đến trạng thái cực đoan).', '店員は「早く帰れ」とばかりに、私たちのテーブルを片付け始めた。', '{店員|てんいん}は「{早|はや}く{帰|かえ}れ」とばかりに、{私|わたし}たちのテーブルを{片付|かたづ}け{始|はじ}めた。', 'Người phục vụ bắt đầu dọn bàn của chúng tôi như muốn nói, ''Nhanh lên và rời đi.''')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('458', 'N1', '〜ともなく / 〜ともなしに', 'to mo naku / to mo nashi ni', 'Một cách vô thức, không định trước hướng nhìn hay suy nghĩ', 'Động từ thể từ điển (辞書形) + ともなく / Động từ thể từ điển (辞書形) + ともなしに / Từ để hỏi + (particle) + ともなく', 'Động từ tương tự thường được lặp lại (見るともなく見る). Với các từ để hỏi: どこからともなく ''đặc biệt là từ hư không'', 誰からともなく ''ai đó hay ai đó''.', '電車の中で、隣の二人の会話を聞くともなく聞いていた。', '{電車|でんしゃ}の{中|なか}で、{隣|となり}の{二人|ふたり}の{会話|かいわ}を{聞|き}くともなく{聞|き}いていた。', 'Trên tàu, tôi nửa nghe nửa chừng cuộc trò chuyện của hai người bên cạnh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('459', 'N1', '〜ないまでも', 'nai made mo', 'Dù không đạt đến mức lý tưởng nhưng chí ít cũng phải đạt mức tối thiểu', 'Động từ thể ない + までも', 'Phần đầu nêu tên một mức độ cao chưa đạt được; thứ hai là mức độ thấp hơn, có thể chấp nhận được, thường kèm theo せめて, 少なくとも hoặc lời khuyên.', '毎日とは言わないまでも、週に一度くらいは実家に電話しなさい。', '{毎日|まいにち}とは{言|い}わないまでも、{週|しゅう}に{一|いち}{度|ど}くらいは{実家|じっか}に{電話|でんわ}しなさい。', 'Tôi sẽ không nói mỗi ngày, nhưng ít nhất hãy gọi về nhà mỗi tuần một lần.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('460', 'N1', '〜ないものでもない', 'nai mono de mo nai', 'Không hẳn là hoàn toàn không thể làm được', 'Động từ thể ない + ものでもない', 'Một cách vòng vo, có phần miễn cưỡng để thừa nhận một khả năng. Tương tự như 〜ないことはない (N3) nhưng cứng hơn.', '条件次第では、その仕事を引き受けないものでもない。', '{条件|じょうけん}{次第|しだい}では、その{仕事|しごと}を{引|ひ}き{受|う}けないものでもない。', 'Tùy thuộc vào các điều khoản, tôi có thể sẵn sàng đảm nhận công việc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('461', 'N1', '〜ながらに / 〜ながらの', 'nagara ni / nagara no', 'Vẫn giữ nguyên trạng thái bẩm sinh từ xưa đến nay', 'Danh từ + ながらに / Danh từ + ながらの + Danh từ / Động từ thể ます (bỏ ます) + ながらに', 'Giới hạn trong việc thiết lập các biểu thức: 昔ながら, 涙ながら, 生まれながら, いつもながら, いながらにして (''không rời khỏi chỗ của mình'').', 'この町には、昔ながらの手作りの和菓子屋が今も残っている。', 'この{町|まち}には、{昔|むかし}ながらの{手作|てづく}りの{和菓子|わがし}{屋|や}が{今|いま}も{残|のこ}っている。', 'Thị trấn này vẫn còn một cửa hàng truyền thống làm đồ ngọt Nhật Bản bằng tay.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('462', 'N1', '〜なくして(は)', 'nakushite wa', 'Nếu không có được yếu tố then chốt đó thì mọi chuyện đều bất khả thi', 'Danh từ + なくして(は)', 'Tiếp theo là tiêu cực hoặc không thể; nhấn mạnh rằng danh từ là không thể thiếu. Nhấn mạnh và văn chương hơn 〜なしに(は).', '地域住民の理解なくして、この計画の成功はあり得ない。', '{地域|ちいき}{住民|じゅうみん}の{理解|りかい}なくして、この{計画|けいかく}の{成功|せいこう}はあり{得|え}ない。', 'Nếu không có sự hiểu biết của người dân địa phương, dự án này không thể thành công.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('463', 'N1', '〜なしに(は)', 'nashi ni wa', 'Không có... thì không thể nào tiến hành được', 'Danh từ + なしに(は)', 'なしに đơn giản có nghĩa là ''không có''; なしには được theo sau bởi âm (''không thể ... không có''). Hội thoại thường sử dụng 〜なしで.', '彼は何の連絡もなしに、三日間会社を休んだ。', '{彼|かれ}は{何|なん}の{連絡|れんらく}もなしに、{三日間|みっかかん}{会社|かいしゃ}を{休|やす}んだ。', 'Anh ấy đã nghỉ làm ba ngày mà không có bất kỳ thông báo nào.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('464', 'N1', '〜ならいざしらず / 〜はいざしらず', 'nara iza shirazu / wa iza shirazu', 'Nếu là người khác thì không bàn tới, đằng này lại là chính bạn!', 'Danh từ + ならいざしらず / Danh từ + はいざしらず / Động từ thể thông thường (V ngắn) + (の) + ならいざしらず', 'Trường hợp đầu tiên có thể được miễn hoặc không xác định được; trường hợp thực tế được xét xử nghiêm khắc hơn. Gần giống với 〜ならまだしも, nhưng mang tính văn chương hơn.', '子どもならいざしらず、大人がこんな簡単な漢字も読めないとは情けない。', '{子|こ}どもならいざしらず、{大人|おとな}がこんな{簡単|かんたん}な{漢字|かんじ}も{読|よ}めないとは{情|なさ}けない。', 'Đối với một đứa trẻ thì đó là một chuyện, nhưng thật đáng xấu hổ khi người lớn không thể đọc được những chữ kanji đơn giản như vậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('465', 'N1', '〜ならまだしも', 'nara mada shimo', 'Nếu ở mức độ nhẹ thì còn chấp nhận được, đằng này quá đáng rồi!', 'Danh từ + ならまだしも / Động từ thể thông thường (V ngắn) + (の) + ならまだしも', 'Trường hợp đầu tiên có thể chấp nhận được; tình hình thực tế còn tồi tệ hơn. Mang tính trò chuyện nhiều hơn 〜ならいざしらず. Biến thể: 〜はまだしも.', '一度ならまだしも、同じミスを三度も繰り返すのは問題だ。', '{一|いち}{度|ど}ならまだしも、{同|おな}じミスを{三|さん}{度|ど}も{繰|く}り{返|かえ}すのは{問題|もんだい}だ。', 'Một lần thì là một chuyện, nhưng lặp lại cùng một lỗi ba lần thì lại là một vấn đề.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('466', 'N1', '〜なり', 'nari', 'Ngay sau khi vừa mới làm việc gì đó thì lập tức có hành động bất ngờ', 'Động từ thể từ điển (辞書形) + なり', 'Cả hai hành động đều có cùng chủ đề (thường là ngôi thứ ba) và hành động thứ hai thường bất ngờ. Không dành cho hành động hoặc kế hoạch tương lai của chính người nói. Khác với 〜なり〜なり và 〜なりに.', '娘は学校から帰ってくるなり、自分の部屋に閉じこもってしまった。', '{娘|むすめ}は{学校|がっこう}から{帰|かえ}ってくるなり、{自分|じぶん}の{部屋|へや}に{閉|と}じこもってしまった。', 'Khi con gái tôi đi học về, nó nhốt mình trong phòng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('467', 'N1', '〜なり〜なり', 'nari ... nari', 'Làm cách này hay cách kia đi chăng nữa để giải quyết', 'Danh từ + なり + Danh từ + なり / Động từ thể từ điển (辞書形) + なり + Động từ thể từ điển (辞書形) + なり', 'Đưa ra các lựa chọn mẫu và thường được theo sau bởi một gợi ý, yêu cầu hoặc mệnh lệnh chứ không phải một sự kiện trong quá khứ. Một 〜なり có thể có nghĩa là ''hoặc cái gì đó'' (お茶なり).', '分からないことがあれば、先生に聞くなり辞書で調べるなりしなさい。', '{分|わ}からないことがあれば、{先生|せんせい}に{聞|き}くなり{辞書|じしょ}で{調|しら}べるなりしなさい。', 'Nếu có điều gì không hiểu, bạn có thể hỏi giáo viên hoặc tra từ điển.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('468', 'N1', '〜なりに / 〜なりの', 'nari ni / nari no', 'Đúng theo phong cách, tầm cỡ và năng lực vốn có của đối tượng', 'Danh từ + なりに / Danh từ + なりの + Danh từ / Tính từ đuôi い + なりに / Động từ thể thông thường (V ngắn) + なりに', 'Chấp nhận một giới hạn (tuổi, khả năng, kích thước) và nói rằng điều gì đó đã được thực hiện hoặc có giá trị trong giới hạn đó. Có thể nghe có vẻ trịch thượng nếu dùng về cấp trên.', '子どもは子どもなりに、親のことを心配しているものだ。', '{子|こ}どもは{子|こ}どもなりに、{親|おや}のことを{心配|しんぱい}しているものだ。', 'Con cái, theo cách riêng của chúng, lo lắng cho cha mẹ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('469', 'N1', '〜には当たらない', 'ni wa ataranai', 'Không đáng phải kinh ngạc, không đáng để bận tâm', 'Động từ thể từ điển (辞書形) + には当たらない / Danh từ + には当たらない', 'Dùng với các động từ phản ứng: 驚く, 責める, 非難する, 心配する, 悲しむ, 賞賛する. Tình hình không đảm bảo cho phản ứng đó.', '彼の実力を考えれば、今回の優勝は驚くには当たらない。', '{彼|かれ}の{実力|じつりょく}を{考|かんが}えれば、{今回|こんかい}の{優勝|ゆうしょう}は{驚|おどろ}くには{当|あ}たらない。', 'Xét về khả năng của anh thì chiến thắng lần này của anh không có gì đáng ngạc nhiên.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('470', 'N1', '〜にあって', 'ni atte', 'Chính vào thời khắc hoàn cảnh đặc biệt ngặt nghèo ấy', 'Danh từ + にあって(は / も)', 'Một 〜で / 〜において trang trọng nhấn mạnh việc đang ở trong một tình huống đặc biệt. にあっても = ''đồng đều''; にあっては = ''ở (vị trí như vậy)''.', '物価が上がり続ける状況にあって、家計の見直しは避けられない。', '{物価|ぶっか}が{上|あ}がり{続|つづ}ける{状況|じょうきょう}にあって、{家計|かけい}の{見直|みなお}しは{避|さ}けられない。', 'Với giá cả tiếp tục tăng, việc xem xét lại tài chính hộ gia đình là điều khó tránh khỏi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('471', 'N1', '〜に難くない', 'ni kataku nai', 'Không khó để hình dung, đồng cảm với nỗi lòng ấy', 'Động từ thể từ điển (辞書形) + に難くない / Danh từ + に難くない', 'Giới hạn trong một vài từ: 想像, 理解, 察する, 予想. Phong cách viết chính thức.', '突然家族を失った遺族の悲しみは、想像に難くない。', '{突然|とつぜん}{家族|かぞく}を{失|うしな}った{遺族|いぞく}の{悲|かな}しみは、{想像|そうぞう}に{難|かた}くない。', 'Không khó để hình dung nỗi đau buồn của những người đột ngột mất đi người thân.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('472', 'N1', '〜にして', 'ni shite', 'Chính vì có được tố chất kiệt xuất ấy mới làm nên chuyện', 'Danh từ + にして', 'Cách sử dụng chính: đã đạt đến một giai đoạn, thường là 〜にして初めて (''chỉ tại/với''); ''ngay cả đối với (ai đó đặc biệt)'' (名人にしてなお解けない); và AにしてB ''cả A và B''.', '彼女は四十歳にして、初めて小説を書き上げた。', '{彼女|かのじょ}は{四十歳|よんじゅっさい}にして、{初|はじ}めて{小説|しょうせつ}を{書|か}き{上|あ}げた。', 'Cô ấy viết xong cuốn tiểu thuyết đầu tiên của mình ở tuổi bốn mươi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('473', 'N1', '〜に即して', 'ni sokushite', 'Bám sát theo đúng tình hình thực tế, quy định, luật lệ', 'Danh từ + に即して / Danh từ + に即した + Danh từ', 'Danh từ điển hình: 事実, 実情, 現実, 実態, 状況. Với luật lệ, 〜に則して hoặc 〜に則って được sử dụng.', '規則を機械的に当てはめるのではなく、実情に即して判断すべきだ。', '{規則|きそく}を{機械|きかい}{的|てき}に{当|あ}てはめるのではなく、{実情|じつじょう}に{即|そく}して{判断|はんだん}すべきだ。', 'Thay vì áp dụng các quy tắc một cách máy móc, bạn nên phán đoán theo hoàn cảnh thực tế.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('474', 'N1', '〜に堪える / 〜に堪えない', 'ni taeru / ni taenai', 'Đáng để thưởng thức / Không thể nào chịu nổi khi phải chứng kiến', 'Động từ thể từ điển (辞書形) + に堪える / Danh từ + に堪える / Động từ thể từ điển (辞書形) + に堪えない / Danh từ + に堪えない', 'に堪える: ''đáng (đọc, nhìn thấy)''. Động từ + に堪えない: ''quá khủng khiếp để (nhìn, nghe)''. Danh từ cảm xúc + に堪えない (感謝, 遺憾): ''cảm nhận sâu sắc'', trong cách nói trang trọng.', 'この作品は、大人の鑑賞にも十分堪える完成度の高いアニメだ。', 'この{作品|さくひん}は、{大人|おとな}の{鑑賞|かんしょう}にも{十分|じゅうぶん}{堪|た}える{完成|かんせい}{度|ど}の{高|たか}いアニメだ。', 'Đây là một bộ anime bóng bẩy và được người lớn chấp nhận.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('475', 'N1', '〜に足る', 'ni taru', 'Đáng để tin cậy, đáng để tôn kính', 'Động từ thể từ điển (辞書形) + に足る + Danh từ / Danh từ + に足る + Danh từ', 'Hầu hết đứng trước danh từ (信頼に足る人物). Câu phủ định 取るに足らない (''tầm thường'') là một cụm từ phổ biến.', '彼は、部下からも取引先からも信頼に足る人物だと評価されている。', '{彼|かれ}は、{部下|ぶか}からも{取引|とりひき}{先|さき}からも{信頼|しんらい}に{足|た}る{人物|じんぶつ}だと{評価|ひょうか}されている。', 'Ông được cấp dưới cũng như khách hàng coi là người đáng tin cậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('476', 'N1', '〜にとどまらず', 'ni todomarazu', 'Không chỉ dừng lại ở phạm vi hẹp mà lan rộng ra toàn cầu', 'Danh từ / Động từ thể thông thường (V ngắn) + にとどまらず', 'Nhấn mạnh rằng điều gì đó lan rộng ra ngoài phạm vi hoặc khu vực dự kiến. Gần với 〜に限らず (N2) nhưng có cảm giác mở rộng mạnh mẽ hơn.', '感染症の影響は一国にとどまらず、世界経済全体に及んだ。', '{感染|かんせん}{症|しょう}の{影響|えいきょう}は{一国|いっこく}にとどまらず、{世界|せかい}{経済|けいざい}{全体|ぜんたい}に{及|およ}んだ。', 'Tác động của dịch bệnh không chỉ giới hạn ở một quốc gia; nó lan rộng ra toàn bộ nền kinh tế thế giới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('477', 'N1', '〜にひきかえ', 'ni hikikae', 'Trái ngược hoàn toàn một trời một vực so với đối tượng kia', 'Danh từ + にひきかえ / Động từ thể thông thường (V ngắn) + の + にひきかえ', 'Đối chiếu hai điều trái ngược nhau với sự đánh giá của người nói (một tốt, một xấu). Chủ quan hơn 〜に対して.', 'しっかり者の姉にひきかえ、妹はのんびり屋でいつも忘れ物ばかりしている。', 'しっかり{者|もの}の{姉|あね}にひきかえ、{妹|いもうと}はのんびり{屋|や}でいつも{忘|わす}れ{物|もの}ばかりしている。', 'Không giống như người chị đáng tin cậy của mình, cô em dễ tính và luôn quên mọi thứ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('478', 'N1', '〜にもまして', 'ni mo mashite', 'Vượt trội hơn cả quá khứ, vượt bậc hơn bình thường', 'Danh từ + にもまして / Từ để hỏi + にもまして', 'Thường là 以前にもまして, 例年にもまして. Với từ để hỏi (何にもまして, 誰にもまして) nó có nghĩa là ''hơn bất cứ thứ gì/bất cứ ai''.', '今年の夏は、例年にもまして暑さが厳しい。', '{今年|ことし}の{夏|なつ}は、{例年|れいねん}にもまして{暑|あつ}さが{厳|きび}しい。', 'Cái nóng mùa hè năm nay thậm chí còn gay gắt hơn thường lệ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('479', 'N1', '〜の至り', 'no itari', 'Tột cùng của niềm vinh dự, tột cùng của sự hổ thẹn', 'Danh từ + の至り', 'Chủ yếu là trong các cụm từ cố định: 光栄の至り, 恐縮の至り, 感激の至り, và 若気の至り (''tuổi trẻ điên rồ''). Được sử dụng trong các bài phát biểu và thư từ trang trọng.', 'このような席でご挨拶させていただけるとは、光栄の至りでございます。', 'このような{席|せき}でご{挨拶|あいさつ}させていただけるとは、{光栄|こうえい}の{至|いた}りでございます。', 'Thật là vinh dự lớn nhất khi được nói chuyện với bạn trong một dịp như vậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('480', 'N1', '〜のみならず', 'nomi narazu', 'Không chỉ riêng một đối tượng mà còn mở rộng ra nhiều khía cạnh', 'Danh từ / Động từ thể thông thường (V ngắn) / Tính từ đuôi い + のみならず / Tính từ đuôi な + である + のみならず / Danh từ + である + のみならず', 'Văn viết trang trọng tương đương với 〜だけでなく / 〜ばかりでなく. Cũng có thể mở câu: のみならず、…', 'この問題は日本のみならず、多くの先進国が抱える課題である。', 'この{問題|もんだい}は{日本|にほん}のみならず、{多|おお}くの{先進|せんしん}{国|こく}が{抱|かか}える{課題|かだい}である。', 'Đây là thách thức không chỉ của Nhật Bản mà của nhiều nước phát triển.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('481', 'N1', '〜はおろか', 'wa oroka', 'Đừng nói tới chuyện lớn, ngay cả việc nhỏ nhất còn chưa làm xong', 'Danh từ + はおろか', 'AはおろかBも / さえ / まで: B thậm chí còn là trường hợp cơ bản hơn; thường là về những sự thật tiêu cực hoặc đáng ngạc nhiên. Không giống như 〜どころか, không được dùng với mệnh lệnh hoặc lời chúc.', '足を骨折してしまい、走ることはおろか、歩くことさえままならない。', '{足|あし}を{骨折|こっせつ}してしまい、{走|はし}ることはおろか、{歩|ある}くことさえままならない。', 'Tôi bị gãy chân nên thậm chí còn không thể đi được chứ đừng nói đến việc chạy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('482', 'N1', '〜ばこそ', 'ba koso', 'Chính vì lý do đó nên mới càng phải nỗ lực thực hiện', 'Động từ thể điều kiện (ば) + こそ / Tính từ đuôi い stem + ければこそ / Tính từ đuôi な + であればこそ / Danh từ + であればこそ', 'Nhấn mạnh một lý do tích cực hoặc có ý tốt, thường là để giải thích điều gì đó có thể bị hiểu lầm. Văn chương hơn 〜からこそ.', '君の将来を思えばこそ、厳しいことを言うのだ。', '{君|きみ}の{将来|しょうらい}を{思|おも}えばこそ、{厳|きび}しいことを{言|い}うのだ。', 'Chính vì quan tâm đến tương lai của bạn nên tôi mới nghiêm khắc với bạn như vậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('483', 'N1', '〜ばそれまでだ', 'ba sore made da', 'Một khi đã xảy ra thì coi như chấm hết mọi công sức', 'Động từ thể điều kiện (ば) + それまでだ', 'Hai cách sử dụng: mọi nỗ lực sẽ bị lãng phí nếu X xảy ra; hoặc 〜と言ってしまえばそれまでだ, ''nói X kết thúc cuộc thảo luận (nhưng ...)''. Thường đi kèm với 〜てしまえば.', 'どんなに高価な機械でも、壊れてしまえばそれまでだ。', 'どんなに{高価|こうか}な{機械|きかい}でも、{壊|こわ}れてしまえばそれまでだ。', 'Máy móc dù có đắt tiền đến đâu, một khi nó hỏng thì coi như xong.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('484', 'N1', '〜べからず / 〜べからざる', 'bekarazu / bekarazaru', 'Tuyệt đối không được phép xâm phạm (luật lệ cấm chỉ tối cao)', 'Động từ thể từ điển (辞書形) + べからず / Động từ thể từ điển (辞書形) + べからざる + Danh từ (する → すべからず)', 'Cổ xưa; chủ yếu là về các dấu hiệu, các quy tắc và các cụm từ cố định. べからざる trước danh từ xuất hiện trong 欠くべからざる (''không thể thiếu'') và 許すべからざる (''không thể tha thứ'').', '境内の池のほとりには、「魚にえさを与えるべからず」と書かれた立て札がある。', '{境内|けいだい}の{池|いけ}のほとりには、「{魚|さかな}にえさを{与|あた}えるべからず」と{書|か}かれた{立|た}て{札|ふだ}がある。', 'Bên ao trong khuôn viên chùa có một tấm biển ghi ''Không cho cá ăn''.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('485', 'N1', '〜べくもない', 'beku mo nai', 'Tuyệt đối không có cách nào, không thể nào hy vọng đạt được', 'Động từ thể từ điển (辞書形) + べくもない (する → すべくもない)', 'Phổ biến với 望む, 知る, 比べる (cũng là 比ぶべくもない cổ điển). Văn học; sự không thể đến từ hoàn cảnh.', 'プロの演奏と私の演奏とでは、比べるべくもない。', 'プロの{演奏|えんそう}と{私|わたし}の{演奏|えんそう}とでは、{比|くら}べるべくもない。', 'Không có gì có thể so sánh hiệu suất của một chuyên gia với của tôi.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('486', 'N1', '〜まじき', 'majiki', 'Một hành vi không thể chấp nhận được ở vị trí xã hội ấy', 'Danh từ + に + あるまじき + Danh từ / Động từ thể từ điển (辞書形) + まじき + Danh từ', 'Chủ yếu là 〜にあるまじき / 〜としてあるまじき + 行為, 発言, 態度: hành vi không xứng đáng với vai trò. Mạnh mẽ quan trọng và chính thức.', '生徒に暴言を吐くなど、教育者にあるまじき行為だ。', '{生徒|せいと}に{暴言|ぼうげん}を{吐|は}くなど、{教育|きょういく}{者|しゃ}にあるまじき{行為|こうい}だ。', 'Quấy rối học sinh bằng lời nói là hành vi không xứng đáng của một nhà giáo dục.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('487', 'N1', '〜までもない', 'made mo nai', 'Không cần thiết phải nhọc công làm việc hiển nhiên đó', 'Động từ thể từ điển (辞書形) + までもない', 'Vấn đề quá rõ ràng hoặc quá nhỏ để cần hành động. 言うまでもなく (''không cần phải nói'') là một cụm từ phổ biến.', '言うまでもないが、試験中の私語は禁止だ。', '{言|い}うまでもないが、{試験|しけん}{中|ちゅう}の{私語|しご}は{禁止|きんし}だ。', 'Không cần phải nói, việc nói chuyện trong kỳ thi là bị cấm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('488', 'N1', '〜までだ / 〜までのことだ', 'made da / made no koto da', 'Cùng lắm thì chọn giải pháp đó; Chỉ đơn thuần là làm tròn bổn phận', 'Động từ thể từ điển (辞書形) + までだ / Động từ thể た (quá khứ) + までだ / Động từ thể từ điển (辞書形) + までのことだ', 'Với Verb-dict: bình tĩnh quyết tâm thực hiện lựa chọn duy nhất còn lại. Với Verb-ta: ''Tôi chỉ làm X, không làm gì hơn'', biện minh cho hành động của một người.', '電車が動かないなら、歩いて帰るまでだ。', '{電車|でんしゃ}が{動|うご}かないなら、{歩|ある}いて{帰|かえ}るまでだ。', 'Nếu tàu không chạy, tôi sẽ đi bộ về nhà.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('489', 'N1', '〜まみれ', 'mamire', 'Bao trùm toàn thân đầy những thứ dơ bẩn (bùn đất, máu me, nợ nần)', 'Danh từ + まみれ', 'Có cái gì đó (bùn, máu, mồ hôi, dầu, bụi, nợ nần) dính đầy khắp nơi. So sánh だらけ (nhiều thứ, thậm chí sai sót) và ずくめ (hoàn toàn có thể tích cực).', '子どもたちは泥まみれになって、日が暮れるまで遊んでいた。', '{子|こ}どもたちは{泥|どろ}まみれになって、{日|ひ}が{暮|く}れるまで{遊|あそ}んでいた。', 'Bọn trẻ chơi đùa cho đến khi mặt trời lặn, người đầy bùn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('490', 'N1', '〜めく', 'meku', 'Nhuốm màu sắc, đượm không khí (nhuốm sắc xuân, đượm vẻ thần bí)', 'Danh từ + めく / Danh từ + めいた + Danh từ', 'Giới hạn ở một số danh từ nhất định: 春めく, 秋めく, 謎めく, 皮肉めく, 冗談めく, 言い訳めく. Văn chương hơn 〜らしい / 〜っぽい.', '三月に入り、日差しもだいぶ春めいてきた。', '{三月|さんがつ}に{入|はい}り、{日差|ひざ}しもだいぶ{春|はる}めいてきた。', 'Bây giờ đã là tháng ba, ánh nắng đã trở nên như mùa xuân.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('491', 'N1', '〜もさることながら', 'mo saru koto nagara', 'Điều đó là hiển nhiên rồi, nhưng mặt này còn đáng chú ý hơn', 'Danh từ + もさることながら', 'Thừa nhận A, sau đó nhấn mạnh B là quan trọng hơn hoặc đáng chú ý hơn. Thông thường đánh giá tích cực.', 'この店は、料理の味もさることながら、器の美しさにも心を奪われる。', 'この{店|みせ}は、{料理|りょうり}の{味|あじ}もさることながら、{器|うつわ}の{美|うつく}しさにも{心|こころ}を{奪|うば}われる。', 'Tại nhà hàng này, không chỉ hương vị của món ăn mà vẻ đẹp của bộ đồ ăn cũng làm bạn say đắm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('492', 'N1', '〜ものを', 'mono o', 'Giá mà chịu lắng nghe thì đã không xảy ra hậu quả đáng tiếc!', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + な + ものを', 'Hối tiếc, không hài lòng hoặc trách móc vì mọi việc đã không diễn ra như lẽ ra phải có. Thường đi kèm với 〜ば / 〜たら; có thể kết thúc câu: 言ってくれればよかったものを.', '一言相談してくれれば手伝ったものを、どうして一人で抱え込んだのか。', '{一言|ひとこと}{相談|そうだん}してくれれば{手伝|てつだ}ったものを、どうして{一人|ひとり}で{抱|かか}え{込|こ}んだのか。', 'Giá như bạn hỏi tôi, tôi đã giúp - tại sao bạn lại giữ tất cả cho riêng mình?')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('493', 'N1', '〜や否や', 'ya ina ya', 'Vừa mới dứt hành động thì lập tức sự việc tiếp theo xảy ra tức khắc', 'Động từ thể từ điển (辞書形) + や否や / Động từ thể từ điển (辞書形) + や', 'Được viết nhiều nhất trong nhóm ''ngay sau''; chỉ sự thật trong quá khứ, không phải ý định hoặc mệnh lệnh. Cũng được rút gọn thành 〜や. So sánh 〜なり (cùng chủ đề) và 〜そばから (lặp lại).', '首相が会見場に姿を現すや否や、一斉にカメラのフラッシュがたかれた。', '{首相|しゅしょう}が{会見|かいけん}{場|じょう}に{姿|すがた}を{現|あらわ}すや{否|いな}や、{一斉|いっせい}にカメラのフラッシュがたかれた。', 'Khoảnh khắc thủ tướng xuất hiện trong phòng họp báo, các camera đồng loạt lóe sáng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('494', 'N1', '〜ゆえ(に)', 'yue ni', 'Bởi vì lý do cao cả, nguyên nhân trọng đại (văn phong cổ kính)', 'Danh từ + (の) + ゆえ(に) / Động từ thể thông thường (V ngắn) + (が) + ゆえ(に) / Tính từ đuôi い + (が) + ゆえ(に) / Tính từ đuôi な + である + がゆえに', 'Trang trọng viết ''bởi vì''; 〜がゆえに mang tính nhấn mạnh và 〜ゆえの + Danh từ bổ nghĩa cho danh từ. Khi bắt đầu câu, ゆえに có nghĩa là ''do đó'' (logic, toán học).', '若さゆえの無謀な挑戦だったが、今ではいい思い出だ。', '{若|わか}さゆえの{無謀|むぼう}な{挑戦|ちょうせん}だったが、{今|いま}ではいい{思|おも}い{出|で}だ。', 'Đó là một thử thách liều lĩnh sinh ra từ tuổi trẻ, nhưng giờ đây nó là một kỷ niệm đẹp.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('495', 'N1', '〜をおいて', 'o oite', 'Ngoài người này/vật này ra thì không còn ai xứng đáng hơn', 'Danh từ + をおいて(ほかに)〜ない', 'Ca ngợi điều gì đó là sự lựa chọn khả thi duy nhất; theo sau là âm (いない, ない). Không liên quan đến 置いて ''bỏ lại phía sau''.', 'この難しい手術を任せられるのは、彼をおいてほかにいない。', 'この{難|むずか}しい{手術|しゅじゅつ}を{任|まか}せられるのは、{彼|かれ}をおいてほかにいない。', 'Không có ai ngoài anh ấy mà chúng ta có thể tin tưởng thực hiện ca phẫu thuật khó khăn này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('496', 'N1', '〜を皮切りに', 'o kawakiri ni', 'Mở đầu bằng sự kiện đó rồi sau đó bùng nổ liên tiếp', 'Danh từ + を皮切りに / Động từ thể thông thường (V ngắn) + の + を皮切りに', 'Đánh dấu sự kiện đầu tiên trong một chuỗi các sự kiện tương tự được mở rộng hoặc lan rộng. Ngoài ra còn có 〜を皮切りとして.', 'バンドは来月の東京公演を皮切りに、全国二十か所を回るツアーを行う。', 'バンドは{来月|らいげつ}の{東京|とうきょう}{公演|こうえん}を{皮切|かわき}りに、{全国|ぜんこく}{二十|にじゅっ}か{所|しょ}を{回|まわ}るツアーを{行|おこな}う。', 'Bắt đầu với buổi hòa nhạc ở Tokyo vào tháng tới, ban nhạc sẽ lưu diễn 20 địa điểm trên toàn quốc.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('497', 'N1', '〜を機に', 'o ki ni', 'Lấy thời điểm này làm mốc kết thúc dứt điểm', 'Danh từ + を機に / Động từ thể thông thường (V ngắn) + の + を機に', 'Trang trọng hơn 〜をきっかけに (N2), thường dùng để chỉ một bước ngoặt quan trọng. Ngoài ra 〜を機として.', '還暦を機に、父は長年勤めた会社を退職した。', '{還暦|かんれき}を{機|き}に、{父|ちち}は{長年|ながねん}{勤|つと}めた{会社|かいしゃ}を{退職|たいしょく}した。', 'Khi bước sang tuổi sáu mươi, cha tôi nghỉ hưu ở công ty mà ông đã làm việc hàng chục năm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('498', 'N1', '〜を限りに', 'o kagiri ni', 'Kể từ thời điểm này trở đi sẽ chính thức ngừng hoạt động', 'Danh từ + を限りに', 'Đánh dấu điểm cuối cùng trước khi điều gì đó kết thúc (本日を限りに, 今回を限りに). 声を限りに ''to nhất có thể'' là một cụm từ cố định.', '本日を限りに、当店は四十年の歴史に幕を下ろします。', '{本日|ほんじつ}を{限|かぎ}りに、{当店|とうてん}は{四|よん}{十|じゅう}{年|ねん}の{歴史|れきし}に{幕|まく}を{下|お}ろします。', 'Tính đến hôm nay, cửa hàng của chúng tôi đóng cửa sau bốn mươi năm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('499', 'N1', '〜をもって', 'o motte', 'Bằng phương tiện chính thức; Lấy mốc thời gian này làm hạn định kết thúc', 'Danh từ + をもって', 'Hai cách sử dụng: phương tiện (書面をもって, 実力をもって) và một điểm kết thúc trang trọng (本日をもって). 〜をもちまして thường dùng trong các thông báo; 〜をもってすれば = ''với ..., (bất cứ điều gì đều có thể)''.', '以上をもちまして、本日の説明会を終了いたします。', '{以上|いじょう}をもちまして、{本日|ほんじつ}の{説明|せつめい}{会|かい}を{終了|しゅうりょう}いたします。', 'Với điều đó, chúng ta kết thúc buổi thông tin ngày hôm nay.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('500', 'N1', '〜をものともせず', 'o mono tomo sezu', 'Bất chấp mọi hiểm nguy, thời tiết khắc nghiệt vẫn kiên cường', 'Danh từ + をものともせず(に)', 'Mô tả ngưỡng mộ về người khác vượt qua khó khăn; không được sử dụng về bản thân. So sánh 〜をよそに (không quan tâm đến sự quan tâm của người khác, thường chỉ trích).', '選手たちは激しい雨をものともせず、最後まで全力で戦い抜いた。', '{選手|せんしゅ}たちは{激|はげ}しい{雨|あめ}をものともせず、{最後|さいご}まで{全力|ぜんりょく}で{戦|たたか}い{抜|ぬ}いた。', 'Không nản lòng trước cơn mưa lớn, các cầu thủ đã chiến đấu hết mình đến cùng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('501', 'N1', '〜をよそに', 'o yoso ni', 'Phớt lờ, để ngoài tai mọi lời can ngăn lo lắng của gia đình', 'Danh từ + をよそに', 'Danh từ thường là cảm xúc hoặc phản ứng của người khác (心配, 期待, 反対, 批判) mà chủ ngữ bỏ qua; thường phê bình nhẹ nhàng.', '親の心配をよそに、息子は連日夜遅くまで遊び歩いている。', '{親|おや}の{心配|しんぱい}をよそに、{息子|むすこ}は{連日|れんじつ}{夜|よる}{遅|おそ}くまで{遊|あそ}び{歩|ある}いている。', 'Bất chấp sự lo lắng của cha mẹ, con trai họ mỗi đêm đều ra ngoài đến khuya.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('502', 'N1', '〜んがため(に)', 'n ga tame ni', 'Với quyết tâm mãnh liệt nhằm đạt được mục đích cao cả', 'Động từ thể ない (bỏ ない) + んがため(に) (する → せんがため)', 'Cổ xưa và nhấn mạnh, thể hiện quyết tâm mạnh mẽ; không phải vì những mục tiêu tầm thường. 〜んがための + Danh từ bổ nghĩa cho danh từ.', '彼は夢をかなえんがため、全財産をなげうって留学した。', '{彼|かれ}は{夢|ゆめ}をかなえんがため、{全|ぜん}{財産|ざいさん}をなげうって{留学|りゅうがく}した。', 'Để thực hiện ước mơ của mình, anh đã từ bỏ tất cả những gì mình có để đi du học.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('503', 'N1', '〜んばかり', 'n bakari', 'Tưởng chừng như sắp sửa... đến nơi rồi', 'Động từ thể ない (bỏ ない) + んばかり (する → せんばかり)', 'Một trạng thái cực đoan đến mức dường như có điều gì đó sắp xảy ra nhưng thực tế không phải vậy. Các dạng: 〜んばかりに, 〜んばかりだ, 〜んばかりの + Danh từ. 言わんばかり trùng với 〜とばかりに.', '優勝が決まった瞬間、スタジアムは割れんばかりの拍手に包まれた。', '{優勝|ゆうしょう}が{決|き}まった{瞬間|しゅんかん}、スタジアムは{割|わ}れんばかりの{拍手|はくしゅ}に{包|つつ}まれた。', 'Thời điểm chức vô địch được quyết định, sân vận động tràn ngập tiếng vỗ tay chói tai.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('504', 'N1', '〜うえは', 'ue wa', 'Một khi đã đưa ra quyết định thì phải chịu trách nhiệm đến cùng', 'Động từ thể từ điển (辞書形) / Động từ thể た (quá khứ) + うえは', 'Giống như 〜以上(は) và 〜からには (N2) nhưng cứng hơn; nửa sau thể hiện quyết tâm, nghĩa vụ hoặc lời khuyên. Cũng được viết 上は.', '引き受けたうえは、最後まで責任を持ってやり遂げるつもりだ。', '{引|ひ}き{受|う}けたうえは、{最後|さいご}まで{責任|せきにん}を{持|も}ってやり{遂|と}げるつもりだ。', 'Bây giờ tôi đã thực hiện nó, tôi dự định sẽ thực hiện nó một cách có trách nhiệm đến cùng.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('505', 'N1', 'ただ〜のみ', 'tada ... nomi', 'Chỉ duy nhất một điều, không có gì khác ngoài...', 'ただ + Danh từ + のみ / ただ + Động từ thể từ điển (辞書形) + のみ', 'Phiên bản trang trọng của ただ〜だけ. Thường kết thúc câu bằng 〜のみだ (''tất cả những gì một người có thể làm là ...''). riêng のみ cũng có nghĩa là ''chỉ'' trong văn viết.', '今はただ、手術の成功を祈るのみだ。', '{今|いま}はただ、{手術|しゅじゅつ}の{成功|せいこう}を{祈|いの}るのみだ。', 'Tất cả những gì chúng tôi có thể làm bây giờ là cầu nguyện cho ca phẫu thuật thành công.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('506', 'N1', '〜だの〜だの', 'dano ... dano', 'Nào là chuyện này, nào là chuyện kia (liệt kê với thái độ ngán ngẩm)', 'Danh từ / Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な + だの + ... + だの', 'Liệt kê các ví dụ với sự khó chịu hoặc không tán thành, thường theo sau là と(言う). So sánh 〜やら〜やら trung tính (N2) và 〜とか〜とか.', '息子は、野菜は苦いだの肉は硬いだのと文句ばかり言って、なかなか食べない。', '{息子|むすこ}は、{野菜|やさい}は{苦|にが}いだの{肉|にく}は{硬|かた}いだのと{文句|もんく}ばかり{言|い}って、なかなか{食|た}べない。', 'Con trai tôi cứ phàn nàn rằng rau đắng, thịt dai, khó ăn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('507', 'N1', '〜てはばからない', 'te habakaranai', 'Nói năng thẳng thừng, công khai không chút e dè sợ hãi', 'Động từ thể て + はばからない', 'Với động từ nói (言う, 公言する, 断言する); thường ám chỉ sự táo bạo là quá mức. Thông thường là về người thứ ba.', '彼は自分こそが業界一の技術者だと公言してはばからない。', '{彼|かれ}は{自分|じぶん}こそが{業界|ぎょうかい}{一|いち}の{技術|ぎじゅつ}{者|しゃ}だと{公言|こうげん}してはばからない。', 'Anh ấy công khai tuyên bố rằng mình là kỹ sư giỏi nhất trong ngành.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('508', 'N1', '〜ともあろう', 'tomo arou', 'Một người có vị thế danh vọng tầm cỡ như vậy mà lại hành xử thế!', 'Danh từ + ともあろう + Danh từ', 'Danh từ là một vai trò hoặc tổ chức được tôn trọng; câu phê phán hành vi không xứng đáng với nó. Thông thường là 〜ともあろう者が / 人が.', '警察官ともあろう者が、飲酒運転で捕まるとは。', '{警察官|けいさつかん}ともあろう{者|もの}が、{飲酒|いんしゅ}{運転|うんてん}で{捕|つか}まるとは。', 'Nghĩ đến việc một sĩ quan cảnh sát, trong số tất cả mọi người, lại bị bắt vì lái xe khi say rượu!')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('509', 'N1', '〜にかこつけて', 'ni kakotsukete', 'Lấy cớ, viện lý do này để thực hiện mục đích khác', 'Danh từ + にかこつけて', 'Lý do đã nêu là cái cớ cho điều người đó thực sự muốn làm. Gần 〜を口実に.', '彼は出張にかこつけて、地方の温泉を巡っているらしい。', '{彼|かれ}は{出張|しゅっちょう}にかこつけて、{地方|ちほう}の{温泉|おんせん}を{巡|めぐ}っているらしい。', 'Có vẻ như anh ấy đang lợi dụng những chuyến công tác như một cái cớ để đi tham quan các suối nước nóng khắp đất nước.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('510', 'N1', '〜に言わせれば', 'ni iwasereba', 'Theo như nhận định, ý kiến riêng của người đó thì...', 'Danh từ + に言わせれば / Danh từ + に言わせると', 'Đưa ra quan điểm cá nhân, có quan điểm mạnh mẽ và có thể khác với quan điểm của người khác. Không giống như 〜によれば trung lập, nó nhấn mạnh quan điểm của cá nhân.', '祖母に言わせれば、最近の若者は我慢が足りないそうだ。', '{祖母|そぼ}に{言|い}わせれば、{最近|さいきん}の{若者|わかもの}は{我慢|がまん}が{足|た}りないそうだ。', 'Theo bà tôi, giới trẻ ngày nay thiếu kiên nhẫn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('511', 'N1', '〜にかまけて', 'ni kamakete', 'Quá mải mê bị cuốn vào việc này mà xao nhãng việc quan trọng khác', 'Danh từ + にかまけて', 'Kết quả là mệnh đề chính nêu tên một điều gì đó bị bỏ quên; thường tự phê bình hoặc viện cớ.', '仕事にかまけて、家族と過ごす時間をおろそかにしてしまった。', '{仕事|しごと}にかまけて、{家族|かぞく}と{過|す}ごす{時間|じかん}をおろそかにしてしまった。', 'Bận rộn với công việc, tôi bỏ bê thời gian đáng lẽ phải dành cho gia đình.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('512', 'N1', '〜かいもなく', 'kai mo naku', 'Mặc dù đã bỏ ra bao nhiêu công sức nhưng rốt cuộc uổng công vô ích', 'Động từ thể た (quá khứ) + かいもなく / Danh từ + の + かいもなく', 'Nỗ lực đã không được đền đáp. Từ tích cực 〜かいがあって có nghĩa là ''nỗ lực đó thật xứng đáng''. Cũng được viết 甲斐もなく.', '家族の懸命な看病のかいもなく、祖父は静かに息を引き取った。', '{家族|かぞく}の{懸命|けんめい}な{看病|かんびょう}のかいもなく、{祖父|そふ}は{静|しず}かに{息|いき}を{引|ひ}き{取|と}った。', 'Được sự quan tâm tận tình của gia đình, ông tôi đã ra đi thanh thản.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('513', 'N1', '〜きっての', 'kitte no', 'Bậc nhất, xuất sắc số một trong toàn bộ khu vực hoặc tập thể', 'Danh từ + きっての + Danh từ', 'Danh từ chỉ một nhóm hoặc một địa điểm (社内, 町内, 学校, 業界, 国内). Được sử dụng để khen ngợi, thường là của một người hoặc một tổ chức.', '彼女は社内きっての切れ者として知られている。', '{彼女|かのじょ}は{社内|しゃない}きっての{切|き}れ{者|もの}として{知|し}られている。', 'Cô được biết đến là người có đầu óc nhạy bén nhất công ty.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('514', 'N1', '〜ずとも', 'zu tomo', 'Cho dù không cần phải làm việc đó thì sự thật vẫn hiển hiện', 'Động từ thể ない (bỏ ない) + ずとも (する → せずとも)', 'Phiên bản văn học của 〜なくても. 〜ずともよい có nghĩa là ''không cần''.', 'わざわざ言わずとも、彼なら状況を察してくれるだろう。', 'わざわざ{言|い}わずとも、{彼|かれ}なら{状況|じょうきょう}を{察|さっ}してくれるだろう。', 'Ngay cả khi tôi không đánh vần nó, anh ấy có thể sẽ nắm bắt được tình hình.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('515', 'N1', '〜とはいえ', 'to wa ie', 'Mặc dù là nói như vậy, dẫu biết là thế nhưng thực tế thì...', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + とはいえ', 'Thừa nhận một sự thật, sau đó nói rằng kết quả mong đợi không hoàn toàn như vậy. Có thể mở một câu (''điều đó đã nói''). Trang trọng hơn 〜といっても.', '春とはいえ、朝晩はまだ冷え込む。', '{春|はる}とはいえ、{朝晩|あさばん}はまだ{冷|ひ}え{込|こ}む。', 'Dù đang là mùa xuân nhưng buổi sáng và buổi tối vẫn lạnh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('516', 'N1', '〜べくして', 'beku shite', 'Chuyện xảy ra là điều tất yếu, đúng như quy luật đã định', 'Động từ thể từ điển (辞書形) + べくして + Động từ thể た (quá khứ) (same verb)', 'Động từ tương tự được lặp lại (起こるべくして起こる); kết quả là hệ quả tự nhiên của hoàn cảnh.', '安全点検を怠っていたのだから、あの事故は起こるべくして起こったと言える。', '{安全|あんぜん}{点検|てんけん}を{怠|おこた}っていたのだから、あの{事故|じこ}は{起|お}こるべくして{起|お}こったと{言|い}える。', 'Vì việc kiểm tra an toàn đã bị bỏ qua nên có thể nói rằng tai nạn chắc chắn sẽ xảy ra.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('517', 'N1', '〜じみる / 〜じみた', 'jimiru / jimita', 'Đượm vẻ, mang hơi hướng tiêu cực như già nua, lấm lem', 'Danh từ + じみる / Danh từ + じみた + Danh từ', 'Sắc thái tiêu cực: 子どもじみた, 年寄りじみた, 説教じみた, 芝居じみた. So sánh 〜めく trung tính và 〜っぽい thông thường. Với 汗 hoặc 油 nó có nghĩa là ''vết bẩn''.', 'いい大人が、そんな子どもじみた言い訳をするものではない。', 'いい{大人|おとな}が、そんな{子|こ}どもじみた{言|い}い{訳|わけ}をするものではない。', 'Người lớn không nên đưa ra những lý do trẻ con như vậy.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('518', 'N1', '〜ぐるみ', 'gurumi', 'Toàn thể, bao gồm toàn bộ mọi thành viên trong gia đình/tập thể', 'Danh từ + ぐるみ', 'Hoạt động với một số danh từ hạn chế: 家族, 町, 地域, 会社, 組織, 身ぐるみ. 会社ぐるみ / 組織ぐるみ thường mô tả hành vi sai trái có tổ chức.', '隣の家とは、家族ぐるみで付き合っている。', '{隣|となり}の{家|いえ}とは、{家族|かぞく}ぐるみで{付|つ}き{合|あ}っている。', 'Cả gia đình chúng tôi đều thân thiết với gia đình bên cạnh.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('519', 'N1', '〜にもほどがある', 'ni mo hodo ga aru', 'Làm cái gì cũng phải có giới hạn chừng mực, quá đà rồi!', 'Danh từ / Tính từ đuôi な / Tính từ đuôi い / Động từ thể từ điển (辞書形) + にもほどがある', 'Chỉ trích mạnh mẽ hoặc phẫn nộ rằng điều gì đó đã đi quá xa. Trò chuyện và cảm xúc.', '約束の時間に二時間も遅れてくるなんて、非常識にもほどがある。', '{約束|やくそく}の{時間|じかん}に{二|に}{時間|じかん}も{遅|おく}れてくるなんて、{非常識|ひじょうしき}にもほどがある。', 'Đến trễ hai giờ trong cuộc hẹn - mức độ thiếu cân nhắc của bạn có thể có giới hạn.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('520', 'N1', '〜をいいことに', 'o ii koto ni', 'Lợi dụng tình thế thuận lợi để làm điều khuất tất có lợi cho mình', 'Danh từ + をいいことに / Động từ thể thông thường (V ngắn) + の + をいいことに', 'Quan trọng: ai đó lợi dụng tình huống để làm điều gì đó không đúng đắn. Với cách ''nắm bắt cơ hội'' một cách trung lập, hãy sử dụng 〜を機に hoặc 〜をきっかけに.', '親が留守なのをいいことに、息子は友達を呼んで夜通し騒いでいた。', '{親|おや}が{留守|るす}なのをいいことに、{息子|むすこ}は{友達|ともだち}を{呼|よ}んで{夜通|よどお}し{騒|さわ}いでいた。', 'Lợi dụng lúc bố mẹ đi vắng, cậu con trai mời bạn bè đến chơi và tiệc tùng thâu đêm.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('521', 'N1', '〜に則って', 'ni nottotte', 'Tuân theo đúng quy chuẩn, phong tục truyền thống hoặc pháp luật', 'Danh từ + に則って / Danh từ + に則った + Danh từ', 'Danh từ là chuẩn mực hoặc hình mẫu: 法律, 規則, 伝統, しきたり, 手順. So sánh 〜に即して, dùng với sự kiện và điều kiện thực tế.', '大会は、国際ルールに則って公正に運営される。', '{大会|たいかい}は、{国際|こくさい}ルールに{則|のっと}って{公正|こうせい}に{運営|うんえい}される。', 'Giải đấu sẽ được diễn ra công bằng theo đúng luật lệ quốc tế.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('522', 'N1', '〜かれ〜かれ', 'kare ... kare', 'Dù ít hay nhiều, dù sớm hay muộn (cặp tính từ sóng đôi)', 'Tính từ đuôi い stem + かれ + Tính từ đuôi い stem + かれ', 'Một dạng tính từ cổ điển chỉ tồn tại khi có các cặp đối lập cố định: 遅かれ早かれ, 多かれ少なかれ, 良かれあしかれ. Không hiệu quả.', '遅かれ早かれ、この秘密はみんなに知られることになるだろう。', '{遅|おそ}かれ{早|はや}かれ、この{秘密|ひみつ}はみんなに{知|し}られることになるだろう。', 'Sớm hay muộn, có lẽ mọi người cũng sẽ phát hiện ra bí mật này.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('523', 'N1', '〜ことこの上ない', 'koto kono ue nai', 'Vô cùng, không có gì có thể vượt qua mức độ tuyệt vời này', 'Tính từ đuôi い + ことこの上ない / Tính từ đuôi な + な + ことこの上ない', 'Một cách viết cứng nhắc để nói ''cực kỳ''. Ngoài ra この上ない + Danh từ (この上ない喜び ''niềm vui lớn nhất''). Gần 〜極まりない.', '深夜の山道を一人で運転するのは、心細いことこの上ない。', '{深夜|しんや}の{山道|やまみち}を{一人|ひとり}で{運転|うんてん}するのは、{心細|こころぼそ}いことこの{上|うえ}ない。', 'Lái xe một mình trên đường núi vào đêm khuya thật đáng sợ.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('524', 'N1', '〜とみえる / 〜とみえて', 'to mieru / to miete', 'Dường như là, nhìn bề ngoài có vẻ là (suy đoán dựa trên chứng cứ)', 'Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + とみえる / Động từ thể thông thường (V ngắn) / Tính từ đuôi い / Tính từ đuôi な / Danh từ + とみえて', 'Một suy luận từ những gì người ta quan sát được; với 〜とみえて, bằng chứng quan sát được sẽ theo sau. Văn chương hơn 〜らしい / 〜ようだ. Cũng được viết là と見える.', '息子はよほど疲れていたとみえて、夕食も食べずに眠ってしまった。', '{息子|むすこ}はよほど{疲|つか}れていたとみえて、{夕食|ゆうしょく}も{食|た}べずに{眠|ねむ}ってしまった。', 'Con trai tôi chắc hẳn đã rất mệt mỏi - nó ngủ quên ngay cả khi chưa ăn tối.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('525', 'N1', '〜を踏まえて', 'o fumaete', 'Dựa trên cơ sở, căn cứ vào dữ liệu hoặc kinh nghiệm thực tế', 'Danh từ + を踏まえて / Danh từ + を踏まえた + Danh từ', 'Phổ biến trong kinh doanh và văn bản chính thức. Không giống như 〜に基づいて (N2), theo sát cơ sở, nó có nghĩa là dựa trên hoặc phản ánh điều gì đó.', 'アンケートの結果を踏まえて、来年度のサービス内容を見直すことにした。', 'アンケートの{結果|けっか}を{踏|ふ}まえて、{来年度|らいねんど}のサービス{内容|ないよう}を{見直|みなお}すことにした。', 'Dựa trên kết quả khảo sát, chúng tôi quyết định xem xét các dịch vụ cung cấp trong năm tới.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, example_sentence, example_furigana, example_meaning)
VALUES ('526', 'N1', '〜ないとも限らない', 'nai to mo kagiranai', 'Không hẳn là hoàn toàn không có khả năng xảy ra', 'Động từ thể ない + とも限らない', 'Cảnh báo về một khả năng nhỏ nhưng có thật, thường là không mong muốn và thường kèm theo biện pháp phòng ngừa. Khác với 〜とは限らない (''không nhất thiết'').', '途中で渋滞に巻き込まれないとも限らないから、早めに出発しよう。', '{途中|とちゅう}で{渋滞|じゅうたい}に{巻|ま}き{込|こ}まれないとも{限|かぎ}らないから、{早|はや}めに{出発|しゅっぱつ}しよう。', 'Chúng ta có thể bị kẹt xe trên đường nên hãy về sớm nhé.')
ON CONFLICT (grammar_code) DO UPDATE SET
    jlpt_level = EXCLUDED.jlpt_level,
    pattern = EXCLUDED.pattern,
    romaji = EXCLUDED.romaji,
    meaning = EXCLUDED.meaning,
    formation = EXCLUDED.formation,
    notes = EXCLUDED.notes,
    example_sentence = EXCLUDED.example_sentence,
    example_furigana = EXCLUDED.example_furigana,
    example_meaning = EXCLUDED.example_meaning,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;
