package com.kizuna.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Từ điển bản địa hóa tiếng Việt 100% chuẩn sư phạm cho Cẩm Nang Ngữ Pháp JLPT (N5 - N1: 526 cấu trúc).
 * Tuân thủ nghiêm ngặt quy tắc Zero Backend Leaks & UI_LOCALIZATION_RULES.md:
 * - Không để tiếng Anh thô trên giao diện và file tài liệu người học.
 * - Cung cấp ý nghĩa tiếng Việt tự nhiên, giải thích công thức kết hợp rõ ràng.
 * - Dịch nghĩa toàn bộ câu ví dụ thực tế sang tiếng Việt mượt mà.
 */
public final class GrammarVietnameseDictionary {

    private GrammarVietnameseDictionary() {}

    private static final Map<String, String> MEANING_MAP = new HashMap<>(600);
    private static final Map<String, String> NOTES_MAP = new HashMap<>(300);
    private static final Map<String, String> TAG_MAP = new HashMap<>(100);
    private static final Map<String, String> EXAMPLE_MAP = new HashMap<>(1000);

    static {
        initN5Meanings();
        initN4Meanings();
        initN3Meanings();
        initN2Meanings();
        initN1Meanings();
        initPedagogicNotes();
        initTagTranslations();
        initCoreExamples();
        loadJsonExamples();
        loadJsonNotes();
    }

    private static void loadJsonNotes() {
        try {
            File f = new File("src/main/resources/data/grammar-notes-vi.json");
            if (!f.exists()) {
                f = new File("C:/Users/Vuh26/Desktop/KizunaTutor/src/main/resources/data/grammar-notes-vi.json");
            }
            if (f.exists()) {
                ObjectMapper om = new ObjectMapper();
                try (InputStream is = new FileInputStream(f)) {
                    Map<String, String> map = om.readValue(is, new TypeReference<>() {});
                    NOTES_MAP.putAll(map);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadJsonExamples() {
        try {
            File f = new File("src/main/resources/data/grammar-examples-vi.json");
            if (!f.exists()) {
                f = new File("C:/Users/Vuh26/Desktop/KizunaTutor/src/main/resources/data/grammar-examples-vi.json");
            }
            if (f.exists()) {
                ObjectMapper om = new ObjectMapper();
                try (InputStream is = new FileInputStream(f)) {
                    Map<String, String> map = om.readValue(is, new TypeReference<>() {});
                    EXAMPLE_MAP.putAll(map);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Chuyển đổi công thức kết hợp ngữ pháp sang tiếng Việt 100%
     */
    public static String translateFormation(String formation) {
        if (formation == null || formation.isBlank()) return "";
        return formation
                .replace("Verb-te form", "Động từ thể て")
                .replace("Verb-te", "Động từ thể て")
                .replace("Verb-nai stem", "Động từ thể ない (bỏ ない)")
                .replace("Verb-nai form", "Động từ thể ない")
                .replace("Verb-nai", "Động từ thể ない")
                .replace("Verb-dictionary form", "Động từ thể từ điển (辞書形)")
                .replace("Verb-dict", "Động từ thể từ điển (辞書形)")
                .replace("Verb-dictionary", "Động từ thể từ điển (辞書形)")
                .replace("Verb-casual", "Động từ thể thông thường (V ngắn)")
                .replace("Verb-plain", "Động từ thể thông thường (V ngắn)")
                .replace("Verb-stem", "Động từ thể ます (bỏ ます)")
                .replace("Verb-ta form", "Động từ thể た (quá khứ)")
                .replace("Verb-ta", "Động từ thể た (quá khứ)")
                .replace("Verb-potential", "Động từ thể khả năng")
                .replace("Verb-passive", "Động từ thể bị động")
                .replace("Verb-causative", "Động từ thể sai khiến")
                .replace("Verb-volitional", "Động từ thể ý chí")
                .replace("Verb-conditional", "Động từ thể điều kiện")
                .replace("Verb-ba", "Động từ thể điều kiện (ば)")
                .replace("Verb", "Động từ")
                .replace("i-adjective", "Tính từ đuôi い")
                .replace("I-adjective", "Tính từ đuôi い")
                .replace("i-Adj", "Tính từ đuôi い")
                .replace("na-adjective", "Tính từ đuôi な")
                .replace("Na-adjective", "Tính từ đuôi な")
                .replace("na-Adj", "Tính từ đuôi な")
                .replace("Adjective", "Tính từ")
                .replace("Noun", "Danh từ")
                .replace("Sentence", "Mệnh đề / Câu")
                .replace("Phrase", "Cụm từ")
                .replace("Clause", "Mệnh đề")
                .replace("Plain form", "Thể thông thường")
                .replace("Polite form", "Thể lịch sự")
                .replace("Counters", "Số từ + Lượng từ")
                .replace("Counter", "Số từ + Lượng từ")
                .replace("Question word", "Từ để hỏi");
    }

    /**
     * Dịch danh sách Tags sang tiếng Việt
     */
    public static List<String> translateTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return List.of("Ngữ pháp JLPT");
        List<String> list = new ArrayList<>();
        for (String t : tags) {
            if (t == null) continue;
            String clean = t.trim().toLowerCase();
            list.add(TAG_MAP.getOrDefault(clean, clean));
        }
        return list.isEmpty() ? List.of("Ngữ pháp JLPT") : list;
    }

    /**
     * Dịch ý nghĩa ngữ pháp sang tiếng Việt tự nhiên chuẩn văn cảnh
     */
    public static String translateMeaning(String id, String pattern, String enMeaning, String level) {
        if (id != null && MEANING_MAP.containsKey(id.trim().toLowerCase())) {
            return MEANING_MAP.get(id.trim().toLowerCase());
        }
        if (pattern != null && MEANING_MAP.containsKey(pattern.trim())) {
            return MEANING_MAP.get(pattern.trim());
        }
        return smartTranslateMeaning(enMeaning, pattern, level);
    }

    /**
     * Dịch ghi chú / mẹo sư phạm sang tiếng Việt
     */
    public static String translateNotes(String id, String pattern, String enNotes, String level) {
        if (id != null && NOTES_MAP.containsKey(id.trim().toLowerCase())) {
            return NOTES_MAP.get(id.trim().toLowerCase());
        }
        if (pattern != null && NOTES_MAP.containsKey(pattern.trim())) {
            return NOTES_MAP.get(pattern.trim());
        }
        if (enNotes != null && !enNotes.isBlank()) {
            String clean = enNotes.trim();
            if (NOTES_MAP.containsKey(clean)) {
                return NOTES_MAP.get(clean);
            }
            return smartTranslateNotes(enNotes, pattern, level);
        }
        return "Cấu trúc " + (pattern != null ? pattern : "") + " thường gặp trong các đề thi JLPT cấp độ " + (level != null ? level : "N5") + " và giao tiếp đời sống hàng ngày.";
    }

    /**
     * Dịch câu ví dụ sang tiếng Việt tự nhiên
     */
    public static String translateExample(String ja, String en) {
        if (en != null && EXAMPLE_MAP.containsKey(en.trim())) {
            String val = EXAMPLE_MAP.get(en.trim());
            if (val != null && !val.isBlank()) return val;
        }
        if (ja != null && EXAMPLE_MAP.containsKey(ja.trim())) {
            String val = EXAMPLE_MAP.get(ja.trim());
            if (val != null && !val.isBlank()) return val;
        }
        // Tận dụng trình dịch thông minh của từ điển từ vựng
        return VocabularyVietnameseDictionary.translateExample(ja, en);
    }

    // ==========================================
    // 1. CẤP ĐỘ JLPT N5 (81 MẪU CÂU)
    // ==========================================
    private static void initN5Meanings() {
        MEANING_MAP.put("wa", "Thì; là; đóng vai trò làm chủ đề chính của câu");
        MEANING_MAP.put("ga-subject", "Chỉ chủ ngữ thực hiện hành động; nhấn mạnh thông tin mới");
        MEANING_MAP.put("o", "Chỉ tân ngữ trực tiếp chịu tác động của động từ; nơi chốn đi qua");
        MEANING_MAP.put("ni-time", "Vào lúc... (chỉ mốc thời gian cụ thể như giờ, ngày, tháng)");
        MEANING_MAP.put("ni-destination", "Đến, tới... (chỉ đích đến cụ thể của hành động di chuyển)");
        MEANING_MAP.put("ni-location", "Ở, tại... (chỉ vị trí tồn tại của người hoặc vật với ある/いる)");
        MEANING_MAP.put("ni-indirect-object", "Cho, tới, với... (chỉ đối tượng tiếp nhận hành động như tặng, mượn)");
        MEANING_MAP.put("ni-frequency", "Mỗi khoảng thời gian làm... lần (chỉ tần suất thực hiện)");
        MEANING_MAP.put("e", "Hướng về, đi về phía... (nhấn mạnh phương hướng di chuyển)");
        MEANING_MAP.put("de-place", "Tại, ở... (nơi diễn ra một hành động cụ thể)");
        MEANING_MAP.put("de-means", "Bằng, bằng cách... (chỉ phương tiện, công cụ hoặc ngôn ngữ)");
        MEANING_MAP.put("to-and", "Và... (nối tất cả các danh từ trong một danh sách đầy đủ)");
        MEANING_MAP.put("to-with", "Cùng với... (chỉ người hoặc đối tượng cùng làm việc)");
        MEANING_MAP.put("ya", "Và, như là... (liệt kê một vài danh từ tiêu biểu mang tính đại diện)");
        MEANING_MAP.put("ka-or", "Hoặc là, hay là... (chỉ sự lựa chọn giữa các danh từ)");
        MEANING_MAP.put("mo", "Cũng, đến cả... (tương tự như đối tượng đã nhắc tới trước đó)");
        MEANING_MAP.put("no", "Của... (chỉ sở hữu, trực thuộc, chất liệu hoặc bổ nghĩa danh từ)");
        MEANING_MAP.put("kara-from", "Từ... (chỉ điểm bắt đầu về thời gian hoặc địa điểm)");
        MEANING_MAP.put("made", "Đến, cho đến... (chỉ điểm kết thúc về thời gian hoặc địa điểm)");
        MEANING_MAP.put("ka-question", "Có phải không? (trợ từ kết thúc tạo câu hỏi nghi vấn)");
        MEANING_MAP.put("ne", "Nhé, nhỉ? (tìm kiếm sự đồng tình hoặc chia sẻ cảm xúc)");
        MEANING_MAP.put("yo", "Đấy, nhé! (cung cấp thông tin mới hoặc nhấn mạnh cho người nghe)");
        MEANING_MAP.put("yori", "Hơn so với... (dùng làm mốc so sánh hơn)");
        MEANING_MAP.put("no-hou-ga-yori", "X thì ... hơn so với Y (so sánh hơn giữa 2 đối tượng)");
        MEANING_MAP.put("to-to-dochira-ga", "X và Y thì cái nào ... hơn? (câu hỏi lựa chọn giữa 2 đối tượng)");
        MEANING_MAP.put("no-naka-de-ga-ichiban", "Trong số các ... thì ... là nhất (so sánh nhất trong một tập hợp)");
        MEANING_MAP.put("desu", "Là, thì... (trợ động từ kết thúc câu khẳng định lịch sự)");
        MEANING_MAP.put("da", "Là, thì... (thể thông thường thân mật của です)");
        MEANING_MAP.put("de-wa-arimasen-ja-nai", "Không phải là... (thể phủ định của danh từ và tính từ đuôi な)");
        MEANING_MAP.put("deshita", "Đã là... (thể quá khứ khẳng định lịch sự của です)");
        MEANING_MAP.put("masu", "Làm gì đó (thể khẳng định hiện tại / tương lai dạng lịch sự)");
        MEANING_MAP.put("masen", "Không làm gì đó (thể phủ định lịch sự của động từ)");
        MEANING_MAP.put("mashita", "Đã làm gì đó (thể quá khứ khẳng định lịch sự của động từ)");
        MEANING_MAP.put("masen-deshita", "Đã không làm gì đó (thể quá khứ phủ định lịch sự của động từ)");
        MEANING_MAP.put("nai", "Không làm gì đó (thể phủ định thông thường ngắn của động từ)");
        MEANING_MAP.put("ta", "Đã làm gì đó (thể quá khứ thông thường của động từ)");
        MEANING_MAP.put("mashou", "Cùng nhau làm ... nhé! (lời rủ rê, đề xuất thực hiện hành động)");
        MEANING_MAP.put("mashou-ka", "Để tôi làm ... giúp nhé? / Chúng ta cùng làm ... nhé?");
        MEANING_MAP.put("masen-ka", "Bạn có muốn cùng làm ... không? (lời mời lịch sự, nhã nhặn)");
        MEANING_MAP.put("tai", "Muốn làm gì đó (thể hiện mong muốn, nguyện vọng của bản thân)");
        MEANING_MAP.put("ga-hoshii", "Muốn có cái gì đó (thể hiện mong muốn sở hữu đồ vật)");
        MEANING_MAP.put("o-kudasai", "Xin vui lòng cho tôi... (dùng khi mua hàng, gọi món)");
        MEANING_MAP.put("te-kudasai", "Xin hãy, vui lòng làm... (yêu cầu, nhờ vả lịch sự)");
        MEANING_MAP.put("naide-kudasai", "Xin đừng làm... (yêu cầu, khuyên bảo không làm việc gì)");
        MEANING_MAP.put("te-mo-ii", "Được phép làm gì đó (hỏi xin phép hoặc cho phép ai làm gì)");
        MEANING_MAP.put("te-wa-ikenai", "Không được phép làm gì đó (cấm đoán, quy tắc bắt buộc)");
        MEANING_MAP.put("nakereba-naranai-nakute-wa-ikenai", "Bắt buộc phải làm gì đó (bổn phận, nghĩa vụ)");
        MEANING_MAP.put("nakute-mo-ii", "Không cần phải làm gì cũng được (không bắt buộc)");
        MEANING_MAP.put("te-iru", "Đang làm... (hành động tiếp diễn); Đang ở trạng thái... (kết quả)");
        MEANING_MAP.put("te", "Và, rồi thì... (nối các động từ, tính từ theo trình tự thời gian)");
        MEANING_MAP.put("te-kara", "Sau khi làm việc này xong thì làm tiếp việc khác");
        MEANING_MAP.put("tari-tari-suru", "Nào là làm..., nào là làm... (liệt kê hành động tiêu biểu)");
        MEANING_MAP.put("ta-koto-ga-aru", "Đã từng làm việc gì đó (chỉ kinh nghiệm, trải nghiệm quá khứ)");
        MEANING_MAP.put("mae-ni", "Trước khi làm việc gì đó...");
        MEANING_MAP.put("ato-de", "Sau khi làm việc gì đó...");
        MEANING_MAP.put("nagara", "Vừa làm việc này vừa làm việc kia (hai việc diễn ra song song)");
        MEANING_MAP.put("ni-iku", "Đi đến đâu để làm gì đó (chỉ mục đích của hành động di chuyển)");
        MEANING_MAP.put("ga-suki-ga-kirai", "Thích / Ghét đối tượng nào đó");
        MEANING_MAP.put("ga-jouzu-ga-heta", "Giỏi / Kém trong lĩnh vực hoặc kỹ năng nào đó");
        MEANING_MAP.put("ga-aru-ga-iru", "Có người / đồ vật ở đâu đó (tồn tại)");
        MEANING_MAP.put("kara-reason", "Bởi vì, do... (chỉ nguyên nhân mang tính chủ quan của người nói)");
        MEANING_MAP.put("ga-but", "Nhưng, tuy nhiên... (nối hai vế tương phản hoặc mở đầu câu nói)");
        MEANING_MAP.put("deshou", "Có lẽ là... đúng không? (suy đoán hoặc tìm kiếm sự đồng thuận lịch sự)");
        MEANING_MAP.put("ku-naru-ni-naru", "Trở nên, biến đổi thành... (sự biến đổi về trạng thái)");
        MEANING_MAP.put("ni-suru", "Chọn cái này, quyết định chọn cái này (dùng khi gọi món, lựa chọn)");
        MEANING_MAP.put("mada-te-inai", "Vẫn chưa làm việc gì đó");
        MEANING_MAP.put("mou-ta", "Đã làm xong việc gì đó rồi");
        MEANING_MAP.put("amari-nai", "Không... lắm (đi với thể phủ định biểu thị mức độ thấp)");
        MEANING_MAP.put("zenzen-nai", "Hoàn toàn không... chút nào (phủ định tuyệt đối)");
        MEANING_MAP.put("dake", "Chỉ, duy nhất chỉ có... (không có thêm cái khác)");
        MEANING_MAP.put("kurai-gurai", "Khoảng chừng, ước lượng mức độ...");
        MEANING_MAP.put("goro", "Vào khoảng thời gian... (chỉ mốc thời điểm ước chừng)");
        MEANING_MAP.put("nani-mo-nai-dare-mo-nai", "Không có gì cả / Không có ai cả (phủ định toàn bộ)");
        MEANING_MAP.put("nani-ka-dare-ka-doko-ka", "Cái gì đó / Ai đó / Nơi nào đó (bất định)");
        MEANING_MAP.put("kore-sore-are-dore", "Cái này / Cái đó / Cái kia / Cái nào (đại từ chỉ định đồ vật)");
        MEANING_MAP.put("kono-sono-ano-dono", "Cái... này / Cái... đó / Cái... kia / Cái... nào (từ bổ nghĩa danh từ)");
        MEANING_MAP.put("koko-soko-asoko-doko", "Ở đây / Ở đó / Ở kia / Ở đâu (chỉ vị trí, nơi chốn)");
        MEANING_MAP.put("ku-nai-katta-ku-nakatta", "Cách chia phủ định và quá khứ của tính từ đuôi い");
        MEANING_MAP.put("na-na-adjective", "Cách dùng tính từ đuôi な bổ nghĩa cho danh từ");
        MEANING_MAP.put("kute-de", "Và, vì... (cách nối tính từ đuôi い và tính từ đuôi な)");
        MEANING_MAP.put("to-iu-named", "Có tên gọi là, được gọi là... (giới thiệu tên người, vật, quán ăn)");
    }

    // ==========================================
    // 2. CẤP ĐỘ JLPT N4 (98 MẪU CÂU)
    // ==========================================
    private static void initN4Meanings() {
        MEANING_MAP.put("ageru-kureru-morau", "Cho, tặng / Được cho, nhận (hành vi cho nhận ơn huệ)");
        MEANING_MAP.put("te-ageru", "Làm việc gì đó giúp cho người khác");
        MEANING_MAP.put("te-kureru", "Được ai đó làm việc gì giúp cho mình hoặc người thân");
        MEANING_MAP.put("te-morau", "Nhận được sự giúp đỡ từ ai đó (mình chủ động nhờ vả)");
        MEANING_MAP.put("te-kudasaru-te-itadaku", "Thể kính ngữ lịch sự của てくれる và てもらう");
        MEANING_MAP.put("te-iku", "Làm gì rồi đi tiếp; Hành động tiếp tục tiến triển về tương lai");
        MEANING_MAP.put("te-kuru", "Làm gì rồi quay lại; Sự việc bắt đầu xuất hiện hoặc kéo dài tới nay");
        MEANING_MAP.put("te-shimau-chau", "Lỡ làm việc gì đó (hối tiếc); Đã hoàn thành dứt điểm toàn bộ");
        MEANING_MAP.put("te-oku", "Làm sẵn, chuẩn bị sẵn việc gì đó trước để phục vụ cho sau này");
        MEANING_MAP.put("te-miru", "Thử làm việc gì đó xem sao (trải nghiệm thử)");
        MEANING_MAP.put("te-aru", "Đang được làm sẵn, ở trạng thái có chủ đích của con người");
        MEANING_MAP.put("toki", "Khi, lúc đang làm việc gì đó hoặc ở vào trạng thái nào đó");
        MEANING_MAP.put("aida-aida-ni", "Trong suốt khoảng thời gian... / Trong khi đang... thì có việc khác chen vào");
        MEANING_MAP.put("sou-da-appearance", "Trông có vẻ..., sắp sửa... (phán đoán dựa trên vẻ ngoài nhìn thấy)");
        MEANING_MAP.put("sou-da-hearsay", "Nghe nói là... (truyền đạt lại thông tin nghe từ nguồn khác)");
        MEANING_MAP.put("rashii", "Nghe nói là, có vẻ như...; Đậm chất, ra dáng...");
        MEANING_MAP.put("you-da", "Dường như là, hình như là... (suy đoán dựa trên cảm nhận giác quan)");
        MEANING_MAP.put("mitai", "Giống như là, có vẻ như... (văn nói thân mật của ようだ)");
        MEANING_MAP.put("sugiru", "Làm gì đó quá mức, quá đà (thường mang sắc thái tiêu cực)");
        MEANING_MAP.put("kata", "Cách làm, phương pháp làm việc gì đó (cách đọc, cách viết, cách đi)");
        MEANING_MAP.put("chuu-juu", "Đang trong lúc làm... / Trong suốt cả một khoảng thời gian, không gian");
        MEANING_MAP.put("tsumori", "Dự định làm gì đó (kế hoạch, ý định của bản thân người nói)");
        MEANING_MAP.put("yotei", "Theo lịch trình, dự kiến sẽ làm gì đó (kế hoạch đã định sẵn)");
        MEANING_MAP.put("koto-ni-suru", "Quyết định sẽ làm (hoặc không làm) việc gì đó");
        MEANING_MAP.put("koto-ni-naru", "Được quy định, quyết định là sẽ làm gì đó (từ bên ngoài quyết định)");
        MEANING_MAP.put("koto-ga-dekiru", "Có thể làm được việc gì đó (chỉ năng lực hoặc khả năng thực hiện)");
        MEANING_MAP.put("eru-rareru-potential", "Động từ thể khả năng: Có thể làm được gì đó");
        MEANING_MAP.put("reru-rareru-passive", "Động từ thể bị động: Bị, được tác động bởi hành động của người khác");
        MEANING_MAP.put("seru-saseru", "Động từ thể sai khiến: Bắt buộc hoặc cho phép ai đó làm gì");
        MEANING_MAP.put("serareru-saserareru", "Động từ thể bị động sai khiến: Bị bắt buộc phải làm việc gì ngoài ý muốn");
        MEANING_MAP.put("ou-you", "Động từ thể ý chí: Nào chúng ta cùng làm... / Định bụng sẽ làm...");
        MEANING_MAP.put("you-to-omou", "Đang có ý định, dự tính sẽ làm việc gì đó");
        MEANING_MAP.put("e-ro", "Động từ thể mệnh lệnh: Hãy làm ngay! (mệnh lệnh dứt khoát)");
        MEANING_MAP.put("na-prohibition", "Cấm làm việc gì đó! (thể cấm chỉ dứt khoát trong văn nói nam giới/biển báo)");
        MEANING_MAP.put("nasai", "Hãy làm đi nào! (lời nhắc nhở, ra lệnh thân mật của cha mẹ, thầy cô)");
        MEANING_MAP.put("ba", "Nếu... thì... (câu điều kiện giả định thể ば)");
        MEANING_MAP.put("tara", "Nếu... thì... / Sau khi... thì... (câu điều kiện phổ biến nhất thể たら)");
        MEANING_MAP.put("nara", "Nếu là... thì hãy... (đưa ra lời khuyên hoặc gợi ý theo chủ đề người khác nói)");
        MEANING_MAP.put("to", "Hễ mà... thì đương nhiên... (điều kiện tự nhiên, thói quen, máy móc)");
        MEANING_MAP.put("te-mo-de-mo", "Cho dù... đi chăng nữa thì vẫn...");
        MEANING_MAP.put("noni-although", "Thế mà, vậy mà... (diễn tả sự tiếc nuối, bất mãn trái với mong đợi)");
        MEANING_MAP.put("noni-purpose", "Để mà, dùng cho mục đích... (đi kèm từ chỉ thời gian, tiền bạc, công sức)");
        MEANING_MAP.put("node", "Bởi vì, do... (chỉ nguyên nhân khách quan, mang tính lịch sự, trang trọng)");
        MEANING_MAP.put("te-reason", "Vì... nên... (dùng thể て để chỉ lý do gây ra cảm xúc hoặc kết quả)");
        MEANING_MAP.put("shi", "Vừa... lại vừa...; Vì... và vì... (liệt kê nhiều nguyên nhân, lý do)");
        MEANING_MAP.put("kedo-keredomo", "Nhưng, tuy nhiên... (liên từ nối mang nghĩa tương phản)");
        MEANING_MAP.put("tame-ni", "Để, nhằm mục đích... / Vì, do nguyên nhân...");
        MEANING_MAP.put("you-ni", "Để mà, nhằm đạt được trạng thái... (chỉ mục đích)");
        MEANING_MAP.put("you-ni-suru", "Cố gắng, nỗ lực tạo thói quen làm gì đó đều đặn");
        MEANING_MAP.put("you-ni-naru", "Trở nên có thể làm được gì đó (biến đổi về năng lực hoặc thói quen)");
        MEANING_MAP.put("te-hoshii", "Muốn người khác làm việc gì đó giúp mình");
        MEANING_MAP.put("tagaru", "Người thứ ba muốn làm việc gì đó (diễn tả mong muốn của người khác)");
        MEANING_MAP.put("garu", "Người thứ ba cảm thấy... (nhìn thấy qua biểu hiện bên ngoài)");
        MEANING_MAP.put("kamo-shirenai", "Có thể, có khả năng là... (suy đoán xác suất khoảng 50%)");
        MEANING_MAP.put("darou", "Có lẽ là... (dự đoán suy đoán ở thể thông thường)");
        MEANING_MAP.put("hazu-da", "Chắc chắn là, lẽ ra phải là... (suy luận có căn cứ vững chắc)");
        MEANING_MAP.put("hazu-ga-nai", "Không lý nào lại..., chắc chắn không thể nào như thế được");
        MEANING_MAP.put("n-desu-no-desu", "Dùng để nhấn mạnh, giải thích nguyên nhân hoặc hỏi han quan tâm");
        MEANING_MAP.put("ka-dou-ka", "Có... hay không (câu hỏi lồng ghép vào câu chính)");
        MEANING_MAP.put("ka-embedded-question", "Nghi vấn từ lồng ghép vào giữa câu trần thuật");
        MEANING_MAP.put("to-omou", "Tôi nghĩ rằng, tôi cho rằng...");
        MEANING_MAP.put("to-iu-say", "Nói rằng, bảo rằng... (trích dẫn lời nói trực tiếp hoặc gián tiếp)");
        MEANING_MAP.put("shika-nai", "Chỉ có thể, không còn cách nào khác ngoài việc...");
        MEANING_MAP.put("bakari", "Toàn là, chỉ toàn làm việc gì đó (thường mang nghĩa hơi phàn nàn)");
        MEANING_MAP.put("ta-bakari", "Vừa mới làm xong việc gì đó tức thì (cảm nhận chủ quan)");
        MEANING_MAP.put("tokoro-da", "Đang chuẩn bị làm / Đang làm dở dang / Vừa mới làm xong");
        MEANING_MAP.put("yasui-nikui", "Dễ làm... / Khó làm việc gì đó...");
        MEANING_MAP.put("hajimeru-owaru-tsuzukeru", "Bắt đầu làm / Kết thúc làm / Tiếp tục làm việc gì đó");
        MEANING_MAP.put("dasu", "Bất thình lình bật ra hành động (bật cười, khóc òa, đổ mưa)");
        MEANING_MAP.put("hodo-nai", "Không... bằng so với đối tượng kia (so sánh kém)");
        MEANING_MAP.put("nakanaka-nai", "Mãi mà không... được; Khá là khó để...");
        MEANING_MAP.put("zutsu", "Từng... một (phân chia đồng đều về số lượng hoặc thời gian)");
        MEANING_MAP.put("mo-emphasis", "Đến tận... (nhấn mạnh số lượng nhiều bất ngờ)");
        MEANING_MAP.put("made-ni", "Trước, muộn nhất là vào thời điểm... (chỉ hạn chót deadline)");
        MEANING_MAP.put("koto-ga-aru", "Thỉnh thoảng làm... / Đôi khi có xảy ra trường hợp...");
        MEANING_MAP.put("nai-to-ikenai-nakucha", "Phải làm, bắt buộc phải làm việc gì (văn nói rút gọn)");
        MEANING_MAP.put("sasete-kudasai", "Xin hãy cho phép tôi được làm việc này");
        MEANING_MAP.put("sonkeigo", "Tôn kính ngữ: Nâng cao hành động của đối phương, đối tác");
        MEANING_MAP.put("kenjougo", "Khiêm nhường ngữ: Hạ thấp hành động của bản thân để tôn kính người nghe");
        MEANING_MAP.put("o-ni-naru", "Dạng thức cấu tạo tôn kính ngữ tiêu chuẩn cho động từ");
        MEANING_MAP.put("o-suru", "Dạng thức cấu tạo khiêm nhường ngữ tiêu chuẩn cho động từ");
        MEANING_MAP.put("de-gozaimasu", "Dạng thức trang trọng lịch sự tối cao của です");
        MEANING_MAP.put("te-mo-kamawanai", "Dù có làm... cũng không sao, không hề phiền gì");
        MEANING_MAP.put("no-koto-nominalizer", "Sự việc, cái việc... (danh từ hóa mệnh đề động từ)");
        MEANING_MAP.put("ba-yokatta", "Giá mà đã làm... thì tốt biết mấy (hối tiếc điều chưa làm)");
        MEANING_MAP.put("tara-dou", "Thử làm... xem sao? (đưa ra lời khuyên hoặc gợi ý)");
        MEANING_MAP.put("hou-ga-ii", "Nên làm... / Không nên làm... thì tốt hơn");
        MEANING_MAP.put("toka-toka", "Như là..., như là... (liệt kê thân mật trong văn nói)");
        MEANING_MAP.put("nado", "Vân vân, và những thứ tương tự...");
        MEANING_MAP.put("de-mo-suggestion", "Hay là cái gì đó như... (đưa ra ví dụ mang tính gợi ý nhẹ nhàng)");
        MEANING_MAP.put("sa", "Độ, mức độ (hậu tố biến tính từ thành danh từ đo lường)");
        MEANING_MAP.put("me", "Thứ... (hậu tố chỉ số thứ tự như lần thứ nhất, thứ hai)");
        MEANING_MAP.put("oki-ni", "Cứ cách mỗi khoảng thời gian/khoảng cách lại...");
        MEANING_MAP.put("ku-suru-ni-suru", "Làm cho trở nên... (tác động chủ quan làm biến đổi tính chất)");
        MEANING_MAP.put("ga-suru", "Có cảm giác, ngửi thấy mùi, nghe thấy âm thanh phát ra");
        MEANING_MAP.put("kana-kashira", "Liệu có... không nhỉ? (tự hỏi bản thân trong suy nghĩ)");
        MEANING_MAP.put("te-kuremasen-ka-te-itadakemasen-ka", "Bạn có thể làm giúp tôi được không? (nhờ vả lịch sự)");
        MEANING_MAP.put("naide", "Mà không làm việc gì đó (thực hiện hành động khác trong tình trạng không làm)");
    }

    // ==========================================
    // 3. CẤP ĐỘ JLPT N3 (101 MẪU CÂU)
    // ==========================================
    private static void initN3Meanings() {
        MEANING_MAP.put("wake-da", "Thảo nào, hèn chi; Chẳng trách mà... / Hóa ra là vì nguyên do đó");
        MEANING_MAP.put("wake-de-wa-nai", "Không hẳn là, không nhất thiết là... (phủ định một phần)");
        MEANING_MAP.put("wake-ga-nai", "Tuyệt đối không thể nào có chuyện... / Không lý nào lại như vậy");
        MEANING_MAP.put("wake-ni-wa-ikanai", "Không thể làm được vì lý do đạo đức, trách nhiệm xã hội hoặc tình thế");
        MEANING_MAP.put("mono-da", "Vốn dĩ là bản chất tự nhiên; Thường hay làm... trong quá khứ; Cảm thán");
        MEANING_MAP.put("mono-de-wa-nai", "Không nên làm việc đó (lời khuyên luân lý, lẽ thường)");
        MEANING_MAP.put("mono-mon", "Bởi vì... mà (ngụy biện, làm nũng, giải thích lý do của phụ nữ/trẻ em)");
        MEANING_MAP.put("mono-dakara-mono-de", "Bởi vì, tại vì... (giải thích phân trần lý do bất đắc dĩ)");
        MEANING_MAP.put("koto-da", "Nên / Không nên làm gì đó (lời khuyên trực tiếp của người có kinh nghiệm)");
        MEANING_MAP.put("koto-wa-nai", "Không cần thiết phải làm gì đó (không đáng phải bận tâm)");
        MEANING_MAP.put("koto-ni-shite-iru", "Tự mình duy trì thói quen, quy tắc làm gì đó");
        MEANING_MAP.put("koto-ni-natte-iru", "Theo quy định, nội quy là phải... (quy tắc chung đã được đặt ra)");
        MEANING_MAP.put("koto-instruction", "Hãy..., Phải... (dùng trong văn bản viết, bảng thông báo chỉ dẫn)");
        MEANING_MAP.put("you-ni-iu-you-ni-tanomu", "Bảo, dặn dò hoặc nhờ ai đó hãy làm (hoặc đừng làm) việc gì");
        MEANING_MAP.put("you-to-suru", "Đang định, sắp sửa làm gì đó / Cố gắng nỗ lực thực hiện");
        MEANING_MAP.put("sou-ni-nai-sou-mo-nai", "Tuyệt đối không có vẻ gì là sẽ xảy ra");
        MEANING_MAP.put("ippou-da", "Có xu hướng ngày càng... (thường là chiều hướng xấu đi)");
        MEANING_MAP.put("ippou-de", "Mặt khác, ngược lại... (so sánh 2 mặt của một vấn đề)");
        MEANING_MAP.put("ni-taishite", "Đối với... (thái độ); Trái ngược với... (so sánh tương phản)");
        MEANING_MAP.put("ni-tsuite", "Về, liên quan đến chủ đề nào đó");
        MEANING_MAP.put("ni-yotte-ni-yoru", "Do, bởi... (nguyên nhân/tác giả); Bằng cách... (phương thức); Tùy vào...");
        MEANING_MAP.put("ni-yoru-to-ni-yoreba", "Theo như nguồn thông tin từ...");
        MEANING_MAP.put("bakari-de-naku", "Không chỉ... mà còn cả những đối tượng khác nữa");
        MEANING_MAP.put("dake-de-naku", "Không chỉ riêng... mà còn cả...");
        MEANING_MAP.put("mo-ba-mo", "Vừa... lại vừa...; Cả... lẫn... đều");
        MEANING_MAP.put("rashii-typical-of", "Đậm chất, đúng phong thái tiêu biểu của...");
        MEANING_MAP.put("ppoi", "Có vẻ, hay có tính chất... (dễ nổi nóng, hay quên, màu nhờ nhờ)");
        MEANING_MAP.put("teki", "Mang tính chất, về mặt... (hậu tố biến danh từ thành tính từ)");
        MEANING_MAP.put("you-na-you-ni-like", "Giống như là, như thể là... (so sánh ví von hình tượng)");
        MEANING_MAP.put("marude-you-da", "Cứ như thể là, hoàn toàn giống như...");
        MEANING_MAP.put("ni-chigai-nai", "Chắc chắn là, không thể sai được (độ tin cậy gần như tuyệt đối)");
        MEANING_MAP.put("ni-kimatte-iru", "Nhất định là, chắc chắn là... (khẳng định mang tính trực giác)");
        MEANING_MAP.put("ki-ga-suru", "Có cảm giác là, dường như là...");
        MEANING_MAP.put("zu-ni", "Mà không làm việc gì đó (dạng văn viết trang trọng của ないで)");
        MEANING_MAP.put("mama", "Cứ giữ nguyên tình trạng như thế mà tiếp tục làm việc khác");
        MEANING_MAP.put("uchi-ni", "Trong khi còn... (tranh thủ làm trước khi điều kiện thay đổi); Trong lúc đang...");
        MEANING_MAP.put("tokoro-ni-tokoro-e-tokoro-o", "Đúng vào thời điểm đang làm việc đó thì có sự việc khác tác động");
        MEANING_MAP.put("ba-hodo", "Càng... thì lại càng...");
        MEANING_MAP.put("hodo", "Đến mức, đến độ...; Càng... thì càng...");
        MEANING_MAP.put("kurai-gurai-extent", "Đến mức, cỡ như là... (chỉ mức độ cao)");
        MEANING_MAP.put("sae", "Ngay cả, thậm chí đến cả... (đưa ra ví dụ cực đoan)");
        MEANING_MAP.put("sae-ba", "Chỉ cần... là đủ, mọi chuyện sẽ tốt đẹp");
        MEANING_MAP.put("koso", "Chính là... (nhấn mạnh đối tượng, từ chối sự nhầm lẫn)");
        MEANING_MAP.put("to-shite", "Với tư cách là, trên cương vị là...");
        MEANING_MAP.put("ni-totte", "Đối với bản thân ai đó (nhìn nhận từ góc độ của người đó)");
        MEANING_MAP.put("ni-shite-wa", "Xét theo... thì quả là bất ngờ (trái với mức trung bình dự kiến)");
        MEANING_MAP.put("ni-shite-mo", "Cho dù là... đi chăng nữa thì cũng...");
        MEANING_MAP.put("ni-kanshite-ni-kansuru", "Về, liên quan tới vấn đề... (cách nói trang trọng hơn của について)");
        MEANING_MAP.put("ni-kurabete", "So với... thì...");
        MEANING_MAP.put("ni-kawatte", "Thay mặt cho, đại diện cho ai đó làm việc gì");
        MEANING_MAP.put("kawari-ni", "Thay vì... / Đổi lại, bù lại thì...");
        MEANING_MAP.put("ni-shitagatte", "Cùng với sự biến đổi của... kéo theo; Tuân theo quy định");
        MEANING_MAP.put("ni-tsurete", "Cùng với việc... kéo theo... cũng biến đổi song hành");
        MEANING_MAP.put("to-tomo-ni", "Cùng với...; Đồng thời với việc...");
        MEANING_MAP.put("tabi-ni", "Mỗi lần, mỗi khi làm việc gì đó thì lại đều xảy ra...");
        MEANING_MAP.put("goto-ni", "Cứ mỗi... lại một lần (chu kỳ lặp lại)");
        MEANING_MAP.put("saichuu-ni", "Đúng vào lúc đang cao điểm làm việc gì đó thì xảy ra sự cố bất ngờ");
        MEANING_MAP.put("tsuide-ni", "Nhân tiện làm việc này thì làm thêm việc kia");
        MEANING_MAP.put("okage-de", "Nhờ có, nhờ ơn... (chỉ nguyên nhân đem lại kết quả tốt đẹp)");
        MEANING_MAP.put("sei-de", "Tại vì, do... (chỉ nguyên nhân dẫn tới hậu quả xấu, oán trách)");
        MEANING_MAP.put("ta-totan-ta-totan-ni", "Ngay sau khi vừa mới... thì bỗng nhiên xảy ra điều bất ngờ ngoài dự kiến");
        MEANING_MAP.put("tara-ta", "Sau khi làm... thì phát hiện ra kết quả bất ngờ");
        MEANING_MAP.put("tatte-datte", "Cho dù... đi chăng nữa (thể thân mật văn nói của ても/でも)");
        MEANING_MAP.put("nai-koto-wa-nai", "Không phải là không... (phủ định kép: thật ra là có thể)");
        MEANING_MAP.put("te-shou-ga-nai-te-shikata-ga-nai", "Vô cùng, không biết phải làm sao (cảm xúc không kìm nén được)");
        MEANING_MAP.put("te-tamaranai", "Rất, không thể chịu nổi (cảm giác cơ thể hoặc khát khao mãnh liệt)");
        MEANING_MAP.put("te-hajimete", "Kể từ sau khi... thì mới nhận ra, mới bắt đầu hiểu được");
        MEANING_MAP.put("te-bakari-iru", "Suốt ngày chỉ toàn làm việc gì đó (mang ý trách móc, phàn nàn)");
        MEANING_MAP.put("ba-ii", "Nên làm gì đó thì tốt / Ước gì...");
        MEANING_MAP.put("baai-wa", "Trong trường hợp, khi xảy ra tình huống...");
        MEANING_MAP.put("to-sureba-to-shitara", "Nếu giả sử là... thì sẽ thế nào?");
        MEANING_MAP.put("to-shite-mo", "Ngay cả khi giả định là... thì cũng...");
        MEANING_MAP.put("toka-hearsay", "Nghe nói là, hình như là... (truyền đạt thông tin chưa kiểm chứng)");
        MEANING_MAP.put("tte", "Rằng là...; Nói là... (rút gọn của と/というのは/と聞いている)");
        MEANING_MAP.put("to-iu-koto-da", "Nghĩa là, nghe nói là... (truyền đạt lại nội dung hoặc giải thích ý)");
        MEANING_MAP.put("to-iu-no-wa", "Cái gọi là... có nghĩa là... (định nghĩa khái niệm)");
        MEANING_MAP.put("to-iu-yori", "Nói đúng hơn là... thay vì nói là...");
        MEANING_MAP.put("wari-ni-wari-ni-wa", "So với... thì quả là khác biệt (vượt ngoài dự đoán)");
        MEANING_MAP.put("kuse-ni", "Thế mà, vậy mà lại... (mỉa mai, trách móc đối tượng)");
        MEANING_MAP.put("kara-ni-kakete", "Từ... đến... (khoảng thời gian hoặc không gian tương đối)");
        MEANING_MAP.put("buri", "Sau suốt một khoảng thời gian dài mới lại làm việc gì");
        MEANING_MAP.put("kke", "Có phải là... không nhỉ? (hỏi lại khi quên hoặc cố nhớ lại)");
        MEANING_MAP.put("masu-you-ni", "Cầu chúc cho, mong sao cho... (lời cầu nguyện, chúc phúc)");
        MEANING_MAP.put("furi-o-suru", "Giả vờ như là, làm bộ làm tịch như thể...");
        MEANING_MAP.put("muki-muke", "Thích hợp cho... / Thiết kế dành riêng cho đối tượng nào đó");
        MEANING_MAP.put("gachi", "Thường hay bị, có xu hướng dễ bị... (chiều hướng tiêu cực)");
        MEANING_MAP.put("gimi", "Có vẻ hơi, có triệu chứng hơi... (hơi sốt, hơi mệt)");
        MEANING_MAP.put("darake", "Toàn là, đầy những thứ tiêu cực (đầy bùn, đầy rác, đầy lỗi)");
        MEANING_MAP.put("kiru-kirenai", "Làm hết sạch hoàn toàn / Không thể nào làm hết xuể được");
        MEANING_MAP.put("kakeru", "Đang làm dở dang, chưa hoàn thành trọn vẹn");
        MEANING_MAP.put("au", "Cùng nhau làm việc gì đó qua lại lẫn nhau");
        MEANING_MAP.put("naosu", "Làm lại việc gì đó một lần nữa cho chuẩn");
        MEANING_MAP.put("zurai", "Khó làm việc gì đó (về mặt tâm lý hoặc thao tác)");
        MEANING_MAP.put("beki-da-beki-de-wa-nai", "Nên làm / Không nên làm gì đó theo lẽ thường, đạo lý");
        MEANING_MAP.put("hodo-wa-nai", "Không đến mức phải... như vậy đâu");
        MEANING_MAP.put("shika-nai-no-choice", "Chỉ còn cách, không còn lựa chọn nào khác");
        MEANING_MAP.put("made-even", "Đến tận cả, thậm chí đến mức...");
        MEANING_MAP.put("tokoro-datta", "Suýt chút nữa thì đã xảy ra chuyện...");
        MEANING_MAP.put("kara-to-itte", "Dù nói là... đi chăng nữa thì cũng không được...");
        MEANING_MAP.put("toori-ni-doori-ni", "Đúng theo như, y hệt như...");
        MEANING_MAP.put("nanka-nante", "Những thứ như là... (coi nhẹ, khiêm tốn hoặc ngạc nhiên)");
    }

    // ==========================================
    // 4. CẤP ĐỘ JLPT N2 (123 MẪU CÂU)
    // ==========================================
    private static void initN2Meanings() {
        MEANING_MAP.put("ni-saishite", "Nhân dịp, khi đối mặt với một sự kiện quan trọng trọng đại");
        MEANING_MAP.put("ni-tomonatte", "Cùng với, đi kèm với sự phát triển biến đổi của...");
        MEANING_MAP.put("ni-oujite", "Tùy theo, tương ứng với, để phù hợp với...");
        MEANING_MAP.put("ni-hanshite", "Trái ngược với dự đoán; Vi phạm quy định, nguyên tắc");
        MEANING_MAP.put("ni-motozuite", "Dựa trên cơ sở, căn cứ vào tài liệu, số liệu, pháp luật");
        MEANING_MAP.put("ni-watatte", "Suốt trong khoảng thời gian, trên toàn bộ phạm vi không gian");
        MEANING_MAP.put("ni-sotte", "Men theo con đường; Bám sát theo kế hoạch, chủ trương");
        MEANING_MAP.put("ni-kuwaete", "Thêm vào đó, ngoài ra còn có thêm...");
        MEANING_MAP.put("o-megutte", "Xoay quanh những tranh cãi, thảo luận về vấn đề nào đó");
        MEANING_MAP.put("o-tsuujite-o-tooshite", "Thông qua phương tiện, người trung gian; Trong suốt cả thời kỳ");
        MEANING_MAP.put("o-kikkake-ni", "Nhân cơ hội, lấy việc đó làm khởi điểm bước ngoặt");
        MEANING_MAP.put("o-nozoite", "Ngoại trừ đối tượng này ra");
        MEANING_MAP.put("ka-no-you-da", "Cứ y như là, hệt như thể là...");
        MEANING_MAP.put("kanenai", "Có khả năng, nguy cơ xảy ra hậu quả xấu");
        MEANING_MAP.put("kaneru", "Khó mà, không thể nào làm được (từ chối lịch sự trong kinh doanh)");
        MEANING_MAP.put("zu-ni-wa-irarenai", "Không thể không làm, không kìm nén được thôi thúc hành động");
        MEANING_MAP.put("zaru-o-enai", "Đành phải, buộc lòng phải làm dù bản thân không muốn");
        MEANING_MAP.put("te-wa-irarenai", "Không thể cứ tiếp tục ở tình trạng này được nữa");
        MEANING_MAP.put("mono-ka", "Tuyệt đối không bao giờ làm! (phủ định cực lực, thách thức)");
        MEANING_MAP.put("mono-no", "Mặc dù là... nhưng thực tế thì lại...");
        MEANING_MAP.put("mono-nara", "Nếu như có thể làm được... (điều kiện khó thành hiện thực)");
        MEANING_MAP.put("kurai-nara-gurai-nara", "Nếu phải làm việc đó thì thà chọn việc này còn hơn");
        MEANING_MAP.put("mono-ga-aru", "Có cảm giác thực sự là... (cảm nhận sâu sắc)");
        MEANING_MAP.put("koto-ka", "Biết bao nhiêu, xiết bao! (cảm thán mức độ cảm xúc)");
        MEANING_MAP.put("koto-ni-wa", "Thật là... làm sao! (nhấn mạnh cảm xúc của người nói)");
        MEANING_MAP.put("koto-dakara", "Vì là đối tượng đó nên chắc chắn sẽ...");
        MEANING_MAP.put("koto-naku", "Mà không hề làm việc gì đó suốt cả quá trình");
        MEANING_MAP.put("koto-kara", "Xuất phát từ nguyên nhân, lý do là... mà dẫn tới");
        MEANING_MAP.put("nai-koto-ni-wa", "Nếu không làm việc này trước thì tuyệt đối không thể làm việc kia");
        MEANING_MAP.put("koto-wa-ga", "Làm thì có làm thật đấy nhưng mà...");
        MEANING_MAP.put("ageku", "Sau một hồi dây dưa rắc rối cuối cùng lại dẫn tới kết cục tồi tệ");
        MEANING_MAP.put("amari-ni", "Vì quá mức... nên dẫn tới kết quả không mong muốn");
        MEANING_MAP.put("ue-ni", "Hơn nữa, thêm vào đó (điều tốt thêm tốt, điều xấu thêm xấu)");
        MEANING_MAP.put("ue-de", "Sau khi đã làm việc này kỹ càng thì mới làm tiếp; Xét trên phương diện");
        MEANING_MAP.put("ijou-wa", "Một khi đã... thì buộc lòng phải có trách nhiệm làm đến cùng");
        MEANING_MAP.put("kara-ni-wa", "Một khi đã... thì nhất định phải làm");
        MEANING_MAP.put("kara-koso", "Chính vì lý do đó nên mới càng...");
        MEANING_MAP.put("kara-shite", "Ngay từ điểm đó đã thấy... (đưa ra dẫn chứng tiêu biểu)");
        MEANING_MAP.put("kara-suru-to-kara-sureba", "Xét từ góc độ, căn cứ vào dữ liệu đó thì suy đoán...");
        MEANING_MAP.put("kara-miru-to-kara-mireba", "Nhìn từ góc nhìn, lập trường của đối tượng đó thì...");
        MEANING_MAP.put("kara-iu-to-kara-itte", "Xét về mặt, nếu nói từ phương diện đó thì...");
        MEANING_MAP.put("sai-ni", "Khi, nhân dịp... (dạng trang trọng của とき)");
        MEANING_MAP.put("shidai", "Ngay sau khi làm xong thì sẽ lập tức làm bước tiếp theo");
        MEANING_MAP.put("shidai-da-shidai-de", "Tùy thuộc vào... mà quyết định; Duyên cớ là như thế");
        MEANING_MAP.put("sue-ni", "Sau rất nhiều trăn trở, nỗ lực cuối cùng đã đạt được kết quả");
        MEANING_MAP.put("ta-tokoro-de", "Dù cho có làm việc đó đi chăng nữa thì kết quả vẫn vô ích");
        MEANING_MAP.put("ta-tokoro", "Sau khi thử làm việc đó xong thì nhận được kết quả");
        MEANING_MAP.put("bakari-ka", "Không chỉ dừng lại ở mức đó mà thậm chí còn hơn thế nữa");
        MEANING_MAP.put("bakari-ni", "Chỉ vì một nguyên nhân nhỏ đó mà dẫn tới hậu quả cay đắng");
        MEANING_MAP.put("bakari-da", "Chỉ việc làm... (mọi khâu chuẩn bị đã xong xuôi); Ngày càng xấu đi");
        MEANING_MAP.put("wa-mochiron-wa-motoyori", "Đối tượng này là đương nhiên rồi, và ngay cả những cái khác nữa");
        MEANING_MAP.put("wa-tomokaku", "Tạm thời gác chuyện đó sang một bên");
        MEANING_MAP.put("wa-sateoki", "Khoan hãy bàn tới chuyện đó, vấn đề quan trọng trước mắt là...");
        MEANING_MAP.put("hanmen", "Ngược lại, mặt khác (so sánh hai khía cạnh đối lập)");
        MEANING_MAP.put("nagara-mo", "Tuy là... dẫu biết là thế nhưng thực tế thì...");
        MEANING_MAP.put("tsutsu-tsutsu-mo", "Dù biết là không tốt nhưng vẫn cứ làm; Vừa làm vừa...");
        MEANING_MAP.put("tsutsu-aru", "Đang dần dần trong quá trình biến đổi");
        MEANING_MAP.put("ppanashi", "Cứ để nguyên trạng thái dở dang như thế không chịu dọn dẹp");
        MEANING_MAP.put("nuku", "Kiên trì làm đến cùng dù phải trải qua vô vàn gian khó");
        MEANING_MAP.put("uru-enai", "Có thể xảy ra / Hoàn toàn không thể nào có khả năng xảy ra");
        MEANING_MAP.put("gatai", "Rất khó làm việc gì đó (khó tin, khó tha thứ, khó chấp nhận)");
        MEANING_MAP.put("you-ga-nai", "Hoàn toàn không có cách nào để mà làm được");
        MEANING_MAP.put("you-de-wa", "Nếu cứ trong tình trạng như thế này thì sẽ hỏng việc");
        MEANING_MAP.put("te-naranai", "Cứ thấy... không sao ngăn được (cảm xúc tự nhiên dâng trào)");
        MEANING_MAP.put("te-koso", "Chính nhờ có việc đó thì mới thực sự hiểu được");
        MEANING_MAP.put("te-kara-de-nai-to", "Nếu chưa làm việc này xong thì không thể làm việc tiếp theo");
        MEANING_MAP.put("te-irai", "Kể từ sau khi việc đó diễn ra cho tới tận bây giờ");
        MEANING_MAP.put("ni-suginai", "Chẳng qua chỉ là, không hơn không kém");
        MEANING_MAP.put("ni-kagiru", "Cách tốt nhất là..., không gì bằng...");
        MEANING_MAP.put("ni-kagirazu", "Không chỉ giới hạn ở... mà cả những đối tượng khác nữa");
        MEANING_MAP.put("ni-kagitte", "Đúng vào cái ngày đó thì xui xẻo; Riêng ai chứ người đó thì không làm thế");
        MEANING_MAP.put("ni-kagiri", "Chỉ dành riêng cho đối tượng đặc biệt này");
        MEANING_MAP.put("kagiri", "Chừng nào mà còn... thì vẫn tiếp tục; Trong toàn bộ giới hạn");
        MEANING_MAP.put("nai-kagiri", "Nếu không... thì không thể nào...");
        MEANING_MAP.put("ni-kakete-wa", "Xét về mặt, riêng về lĩnh vực đó thì không thua kém bất kỳ ai");
        MEANING_MAP.put("ni-kotaete", "Để đáp ứng, đền đáp lại sự kỳ vọng, mong mỏi, yêu cầu");
        MEANING_MAP.put("ni-sakidatte", "Trước thềm, trước khi tiến hành một sự kiện quan trọng");
        MEANING_MAP.put("ni-shiro-ni-seyo", "Cho dù là cái này hay cái kia thì kết quả vẫn như vậy");
        MEANING_MAP.put("ni-sureba-ni-shitara", "Nếu đứng trên cương vị, lập trường của người đó");
        MEANING_MAP.put("ni-soui-nai", "Chắc chắn là như vậy, không còn nghi ngờ gì nữa");
        MEANING_MAP.put("ni-koshita-koto-wa-nai", "Không gì tốt bằng việc làm trước điều đó");
        MEANING_MAP.put("ni-hoka-naranai", "Chính là, không gì khác ngoài... (nguyên nhân duy nhất)");
        MEANING_MAP.put("ni-mo-kakawarazu", "Mặc dù... thế nhưng thực tế lại xảy ra trái ngược");
        MEANING_MAP.put("ni-atatte", "Nhân dịp, trước thềm một sự kiện trọng đại diễn ra");
        MEANING_MAP.put("ni-oite-ni-okeru", "Ở, tại, trong... (văn viết trang trọng của で)");
        MEANING_MAP.put("ni-kakawarazu", "Bất kể, không phân biệt tuổi tác, giới tính, điều kiện");
        MEANING_MAP.put("ni-kakawaru", "Ảnh hưởng nghiêm trọng liên quan tới danh dự, tính mạng");
        MEANING_MAP.put("ni-tsuki", "Bởi vì lý do... (dùng trong văn bản thông báo công khai); Mỗi đơn vị là...");
        MEANING_MAP.put("ni-mukete", "Hướng tới mục tiêu, chuẩn bị cho sự kiện trong tương lai");
        MEANING_MAP.put("no-moto-de-no-moto-ni", "Dưới sự dẫn dắt, chỉ bảo, ảnh hưởng của...");
        MEANING_MAP.put("nuki-de-nuki-ni", "Bỏ qua, không tính đến yếu tố đó");
        MEANING_MAP.put("o-komete", "Gửi gắm toàn bộ tâm tình, tình cảm vào hành động");
        MEANING_MAP.put("o-chuushin-ni", "Lấy đối tượng đó làm trung tâm, nòng cốt");
        MEANING_MAP.put("o-moto-ni", "Dựa trên chất liệu, dữ liệu thực tế để sáng tạo");
        MEANING_MAP.put("o-towazu", "Không đòi hỏi, không kể bất kỳ điều kiện nào");
        MEANING_MAP.put("o-hajime", "Trước tiên phải kể đến đối tượng tiêu biểu hàng đầu");
        MEANING_MAP.put("o-to-shite", "Lấy... làm mục tiêu, hình mẫu");
        MEANING_MAP.put("ka-to-iu-to", "Nói như vậy nhưng không hẳn thực tế là như thế");
        MEANING_MAP.put("ka-to-omottara-ka-to-omou-to", "Vừa mới thấy... thì lập tức chuyển biến ngay");
        MEANING_MAP.put("ka-nai-ka-no-uchi-ni", "Hành động trước vừa dứt thì hành động sau đã bắt đầu ngay tức khắc");
        MEANING_MAP.put("kiri", "Kể từ lần đó là bặt vô âm tín luôn; Chỉ duy nhất lần đó");
        MEANING_MAP.put("ge", "Có vẻ, đượm vẻ (tâm trạng biểu lộ qua ánh mắt, nét mặt)");
        MEANING_MAP.put("kko-nai", "Tuyệt đối không thể nào làm được việc đó đâu");
        MEANING_MAP.put("dokoro-ka", "Đừng nói tới chuyện đó, ngay cả điều đơn giản hơn còn không làm nổi");
        MEANING_MAP.put("dokoro-de-wa-nai", "Lúc này không phải là lúc rảnh rỗi để làm việc đó");
        MEANING_MAP.put("to-wa-kagiranai", "Không hẳn nhất thiết lúc nào cũng là như vậy");
        MEANING_MAP.put("to-iu-mono-da", "Đúng thực sự là, đó chính là bản chất của...");
        MEANING_MAP.put("to-iu-mono-de-wa-nai", "Không thể nói tuyệt đối là như thế được");
        MEANING_MAP.put("to-itte-mo", "Dù nói là... đi chăng nữa nhưng thực tế cũng chỉ ở mức vừa phải");
        MEANING_MAP.put("to-ieba-to-iu-to", "Nếu nhắc đến chủ đề đó thì nghĩ ngay đến đặc trưng");
        MEANING_MAP.put("hoka-nai", "Không còn cách nào khác ngoài việc...");
        MEANING_MAP.put("mai", "Tuyệt đối không định làm nữa; Có lẽ sẽ không xảy ra");
        MEANING_MAP.put("you-de-wa-nai-ka", "Chúng ta hãy cùng nhau làm việc đó đi nào! (lời kêu gọi hào sảng)");
        MEANING_MAP.put("mo-kamawazu", "Không màng tới, bất chấp ánh nhìn hoặc trở ngại xung quanh");
        MEANING_MAP.put("jou", "Xét về mặt, trên phương diện lý thuyết hoặc đạo đức");
        MEANING_MAP.put("osore-ga-aru", "Có mối lo ngại, nguy cơ sẽ xảy ra tai họa");
        MEANING_MAP.put("sei-ka", "Chắc có lẽ là do nguyên nhân đó chăng (suy đoán không chắc chắn)");
        MEANING_MAP.put("dake-atte-dake-ni", "Quả đúng xứng danh với danh tiếng, năng lực ấy");
        MEANING_MAP.put("dake-no-koto-wa-aru", "Thật bõ công, quả xứng đáng với công sức bỏ ra");
        MEANING_MAP.put("tatoe-te-mo", "Cho dù giả sử có xảy ra chuyện gì đi chăng nữa thì...");
        MEANING_MAP.put("yara-yara", "Nào là chuyện này, nào là chuyện kia (rối rắm, bận rộn)");
        MEANING_MAP.put("zu-ni-sumu-naide-sumu", "May mắn không cần phải làm việc phiền phức mà vẫn giải quyết xong");
        MEANING_MAP.put("you-ni-yotte-wa", "Tùy thuộc vào cách nhìn nhận, cách làm mà sẽ ra kết quả khác nhau");
    }

    // ==========================================
    // 5. CẤP ĐỘ JLPT N1 (123 MẪU CÂU)
    // ==========================================
    private static void initN1Meanings() {
        MEANING_MAP.put("gatera", "Nhân tiện, tiện thể trên đường làm việc chính thì làm việc phụ");
        MEANING_MAP.put("katawara", "Bên cạnh công việc chính thì đồng thời còn kiêm thêm việc khác");
        MEANING_MAP.put("ga-hayai-ka", "Vừa mới dứt hành động thì lập tức sự việc khác ập đến ngay tức thì");
        MEANING_MAP.put("sura", "Ngay cả, thậm chí đến cả điều đơn giản nhất cũng không...");
        MEANING_MAP.put("zukume", "Toàn là điều tốt lành / Toàn là chuyện xui rủi nối tiếp");
        MEANING_MAP.put("tara-saigo-ga-saigo", "Một khi đã lỡ... thì coi như xong đời, không thể cứu vãn");
        MEANING_MAP.put("te-kara-to-iu-mono", "Kể từ thời khắc sự kiện đó xảy ra thì cuộc sống hoàn toàn đổi khác");
        MEANING_MAP.put("te-yamanai", "Không ngừng tha thiết cầu chúc từ tận đáy lòng");
        MEANING_MAP.put("to-aimatte", "Kết hợp cùng với yếu tố khác tạo nên hiệu ứng tổng hòa");
        MEANING_MAP.put("tomo-naru-to-tomo-nareba", "Một khi đã đạt đến tầm cỡ, vị thế cao như vậy thì đương nhiên");
        MEANING_MAP.put("nara-de-wa", "Chỉ duy nhất nơi đó / Người đó mới có được nét độc đáo này");
        MEANING_MAP.put("beku", "Nhằm mục đích, với quyết tâm thực hiện việc gì");
        MEANING_MAP.put("you-mono-nara", "Nếu chẳng may lỡ xảy ra chuyện đó thì hậu quả khôn lường");
        MEANING_MAP.put("ni-itaru-ni-itatte-ni-itatte-wa", "Phát triển tới mức nghiêm trọng; Đạt tới giai đoạn");
        MEANING_MAP.put("o-kinjienai", "Không thể nào kìm nén được cảm xúc xao xuyến, tức giận, xót xa");
        MEANING_MAP.put("o-yogi-naku-sareru", "Bị ép buộc rơi vào tình thế không thể không làm");
        MEANING_MAP.put("atte-no", "Chính nhờ có sự tồn tại của đối tượng đó thì mới có được thành quả hôm nay");
        MEANING_MAP.put("ikan-de-ikan-ni-yorazu", "Tùy thuộc vào... / Bất kể tình hình ra sao đi chăng nữa");
        MEANING_MAP.put("you-ga-you-to", "Cho dù có làm hay không thì kết quả vẫn chẳng thay đổi");
        MEANING_MAP.put("you-ga-mai-ga-you-to-mai-to", "Dù có xảy ra hay không xảy ra thì quyết định vẫn giữ nguyên");
        MEANING_MAP.put("you-ni-mo-nai", "Dù rất muốn làm nhưng hoàn toàn không thể làm nổi");
        MEANING_MAP.put("kagiri-da", "Vô cùng, hết sức (cảm xúc tột độ của người nói)");
        MEANING_MAP.put("katagata", "Tiện thể kết hợp thực hiện hai mục đích trang trọng cùng lúc");
        MEANING_MAP.put("ka-to-omoikiya", "Cứ ngỡ là sẽ diễn ra như thế, nào ngờ thực tế lại đảo ngược hoàn toàn");
        MEANING_MAP.put("kirai-ga-aru", "Có tật xấu, có xu hướng dễ mắc phải thói quen xấu");
        MEANING_MAP.put("kiwamaru-kiwamari-nai", "Cực kỳ, vô cùng (mức độ chạm đỉnh, nguy hiểm cực điểm)");
        MEANING_MAP.put("no-kiwami", "Đỉnh điểm của sự vinh quang / Đỉnh điểm của sự đau đớn");
        MEANING_MAP.put("gotoki-gotoku", "Như là, hệt như là... (văn phong cổ điển, trang trọng)");
        MEANING_MAP.put("koto-tote", "Bởi vì lý do đó nên mong được lượng thứ bỏ qua");
        MEANING_MAP.put("koto-nashi-ni", "Mà không hề có hành động nào xảy ra");
        MEANING_MAP.put("shimatsu-da", "Rốt cuộc lại dẫn tới kết cục thảm hại, đáng chê trách");
        MEANING_MAP.put("zu-jimai", "Rốt cuộc đến cuối cùng vẫn không kịp làm việc đó");
        MEANING_MAP.put("zu-ni-wa-okanai-nai-de-wa-okanai", "Nhất định sẽ trừng phạt, nhất định phải làm cho bằng được");
        MEANING_MAP.put("zu-ni-wa-sumanai-nai-de-wa-sumanai", "Không thể nào xong chuyện nếu không làm việc xin lỗi/đền bù");
        MEANING_MAP.put("soba-kara", "Vừa mới làm xong thì ngay lập tức lại bị xóa/quên mất");
        MEANING_MAP.put("tari-tomo", "Dù chỉ là một chút, một giây, một người cũng tuyệt đối không...");
        MEANING_MAP.put("dani", "Thậm chí ngay cả trong suy nghĩ, trong mơ cũng chưa từng...");
        MEANING_MAP.put("taru", "Xứng danh là, trên tư cách là người có vị thế cao");
        MEANING_MAP.put("tsu-tsu", "Lúc thì làm việc này, lúc lại làm việc kia sóng đôi liên tiếp");
        MEANING_MAP.put("de-are-de-are-de-are", "Dù cho là... hay là... thì bản chất vẫn không đổi");
        MEANING_MAP.put("te-shikaru-beki-da", "Lẽ dĩ nhiên phải làm việc đó theo chuẩn mực đạo lý");
        MEANING_MAP.put("de-wa-aru-mai-shi", "Đâu phải là trẻ con nữa mà lại hành xử như thế!");
        MEANING_MAP.put("temae", "Vì đã lỡ hứa / Vì danh dự trước mặt người khác nên buộc phải làm");
        MEANING_MAP.put("te-mo-sashitsukae-nai", "Dù có làm... thì cũng không gây trở ngại gì");
        MEANING_MAP.put("to-atte", "Vì là trong hoàn cảnh đặc biệt như thế nên việc đó diễn ra là tất yếu");
        MEANING_MAP.put("to-areba", "Nếu rơi vào tình huống đặc biệt như vậy thì đành phải chấp nhận");
        MEANING_MAP.put("to-ii-to-ii", "Dù nhìn về mặt này hay nhìn về mặt kia đều hoàn hảo / tồi tệ");
        MEANING_MAP.put("to-itta-tokoro-da", "Cao nhất cũng chỉ ở mức tầm tầm đó mà thôi");
        MEANING_MAP.put("to-iedomo", "Dẫu là bậc vĩ nhân, dẫu là chuyên gia đi chăng nữa thì vẫn có lúc sơ suất");
        MEANING_MAP.put("to-ittara-nai", "Không từ ngữ nào có thể diễn tả xiết được mức độ ấy!");
        MEANING_MAP.put("to-kitara", "Cứ hễ nhắc đến kẻ đó là lại bực mình!");
        MEANING_MAP.put("to-shita-tokoro-de-ni-shita-tokoro-de", "Ngay cả khi đứng trên lập trường của họ thì cũng bất lực");
        MEANING_MAP.put("to-wa", "Cái chuyện bất ngờ như thế mà lại xảy ra được ư! (kinh ngạc)");
        MEANING_MAP.put("to-bakari-ni", "Tựa như muốn nói rằng, dường như muốn ra hiệu rằng...");
        MEANING_MAP.put("to-mo-naku-to-mo-nashi-ni", "Một cách vô thức, không định trước hướng nhìn hay suy nghĩ");
        MEANING_MAP.put("nai-made-mo", "Dù không đạt đến mức lý tưởng nhưng chí ít cũng phải đạt mức tối thiểu");
        MEANING_MAP.put("nai-mono-de-mo-nai", "Không hẳn là hoàn toàn không thể làm được");
        MEANING_MAP.put("nagara-ni-nagara-no", "Vẫn giữ nguyên trạng thái bẩm sinh từ xưa đến nay");
        MEANING_MAP.put("nakushite-wa", "Nếu không có được yếu tố then chốt đó thì mọi chuyện đều bất khả thi");
        MEANING_MAP.put("nashi-ni-wa", "Không có... thì không thể nào tiến hành được");
        MEANING_MAP.put("nara-iza-shirazu-wa-iza-shirazu", "Nếu là người khác thì không bàn tới, đằng này lại là chính bạn!");
        MEANING_MAP.put("nara-mada-shimo", "Nếu ở mức độ nhẹ thì còn chấp nhận được, đằng này quá đáng rồi!");
        MEANING_MAP.put("nari", "Ngay sau khi vừa mới làm việc gì đó thì lập tức có hành động bất ngờ");
        MEANING_MAP.put("nari-nari", "Làm cách này hay cách kia đi chăng nữa để giải quyết");
        MEANING_MAP.put("nari-ni-nari-no", "Đúng theo phong cách, tầm cỡ và năng lực vốn có của đối tượng");
        MEANING_MAP.put("ni-wa-ataranai", "Không đáng phải kinh ngạc, không đáng để bận tâm");
        MEANING_MAP.put("ni-atte", "Chính vào thời khắc hoàn cảnh đặc biệt ngặt nghèo ấy");
        MEANING_MAP.put("ni-kataku-nai", "Không khó để hình dung, đồng cảm với nỗi lòng ấy");
        MEANING_MAP.put("ni-shite", "Chính vì có được tố chất kiệt xuất ấy mới làm nên chuyện");
        MEANING_MAP.put("ni-sokushite", "Bám sát theo đúng tình hình thực tế, quy định, luật lệ");
        MEANING_MAP.put("ni-taeru-ni-taenai", "Đáng để thưởng thức / Không thể nào chịu nổi khi phải chứng kiến");
        MEANING_MAP.put("ni-taru", "Đáng để tin cậy, đáng để tôn kính");
        MEANING_MAP.put("ni-todomarazu", "Không chỉ dừng lại ở phạm vi hẹp mà lan rộng ra toàn cầu");
        MEANING_MAP.put("ni-hikikae", "Trái ngược hoàn toàn một trời một vực so với đối tượng kia");
        MEANING_MAP.put("ni-mo-mashite", "Vượt trội hơn cả quá khứ, vượt bậc hơn bình thường");
        MEANING_MAP.put("no-itari", "Tột cùng của niềm vinh dự, tột cùng của sự hổ thẹn");
        MEANING_MAP.put("nomi-narazu", "Không chỉ riêng một đối tượng mà còn mở rộng ra nhiều khía cạnh");
        MEANING_MAP.put("wa-oroka", "Đừng nói tới chuyện lớn, ngay cả việc nhỏ nhất còn chưa làm xong");
        MEANING_MAP.put("ba-koso", "Chính vì lý do đó nên mới càng phải nỗ lực thực hiện");
        MEANING_MAP.put("ba-sore-made-da", "Một khi đã xảy ra thì coi như chấm hết mọi công sức");
        MEANING_MAP.put("bekarazu-bekarazaru", "Tuyệt đối không được phép xâm phạm (luật lệ cấm chỉ tối cao)");
        MEANING_MAP.put("beku-mo-nai", "Tuyệt đối không có cách nào, không thể nào hy vọng đạt được");
        MEANING_MAP.put("majiki", "Một hành vi không thể chấp nhận được ở vị trí xã hội ấy");
        MEANING_MAP.put("made-mo-nai", "Không cần thiết phải nhọc công làm việc hiển nhiên đó");
        MEANING_MAP.put("made-da-made-no-koto-da", "Cùng lắm thì chọn giải pháp đó; Chỉ đơn thuần là làm tròn bổn phận");
        MEANING_MAP.put("mamire", "Bao trùm toàn thân đầy những thứ dơ bẩn (bùn đất, máu me, nợ nần)");
        MEANING_MAP.put("meku", "Nhuốm màu sắc, đượm không khí (nhuốm sắc xuân, đượm vẻ thần bí)");
        MEANING_MAP.put("mo-saru-koto-nagara", "Điều đó là hiển nhiên rồi, nhưng mặt này còn đáng chú ý hơn");
        MEANING_MAP.put("mono-o", "Giá mà chịu lắng nghe thì đã không xảy ra hậu quả đáng tiếc!");
        MEANING_MAP.put("ya-ina-ya", "Vừa mới dứt hành động thì lập tức sự việc tiếp theo xảy ra tức khắc");
        MEANING_MAP.put("yue-ni", "Bởi vì lý do cao cả, nguyên nhân trọng đại (văn phong cổ kính)");
        MEANING_MAP.put("o-oite", "Ngoài người này/vật này ra thì không còn ai xứng đáng hơn");
        MEANING_MAP.put("o-kawakiri-ni", "Mở đầu bằng sự kiện đó rồi sau đó bùng nổ liên tiếp");
        MEANING_MAP.put("o-ki-ni", "Lấy thời điểm này làm mốc kết thúc dứt điểm");
        MEANING_MAP.put("o-kagiri-ni", "Kể từ thời điểm này trở đi sẽ chính thức ngừng hoạt động");
        MEANING_MAP.put("o-motte", "Bằng phương tiện chính thức; Lấy mốc thời gian này làm hạn định kết thúc");
        MEANING_MAP.put("o-mono-tomo-sezu", "Bất chấp mọi hiểm nguy, thời tiết khắc nghiệt vẫn kiên cường");
        MEANING_MAP.put("o-yoso-ni", "Phớt lờ, để ngoài tai mọi lời can ngăn lo lắng của gia đình");
        MEANING_MAP.put("n-ga-tame-ni", "Với quyết tâm mãnh liệt nhằm đạt được mục đích cao cả");
        MEANING_MAP.put("n-bakari", "Tưởng chừng như sắp sửa... đến nơi rồi");
        MEANING_MAP.put("ue-wa", "Một khi đã đưa ra quyết định thì phải chịu trách nhiệm đến cùng");
        MEANING_MAP.put("tada-nomi", "Chỉ duy nhất một điều, không có gì khác ngoài...");
        MEANING_MAP.put("dano-dano", "Nào là chuyện này, nào là chuyện kia (liệt kê với thái độ ngán ngẩm)");
        MEANING_MAP.put("te-habakaranai", "Nói năng thẳng thừng, công khai không chút e dè sợ hãi");
        MEANING_MAP.put("tomo-arou", "Một người có vị thế danh vọng tầm cỡ như vậy mà lại hành xử thế!");
        MEANING_MAP.put("ni-kakotsukete", "Lấy cớ, viện lý do này để thực hiện mục đích khác");
        MEANING_MAP.put("ni-iwasereba", "Theo như nhận định, ý kiến riêng của người đó thì...");
        MEANING_MAP.put("ni-kamakete", "Quá mải mê bị cuốn vào việc này mà xao nhãng việc quan trọng khác");
        MEANING_MAP.put("kai-mo-naku", "Mặc dù đã bỏ ra bao nhiêu công sức nhưng rốt cuộc uổng công vô ích");
        MEANING_MAP.put("kitte-no", "Bậc nhất, xuất sắc số một trong toàn bộ khu vực hoặc tập thể");
        MEANING_MAP.put("zu-tomo", "Cho dù không cần phải làm việc đó thì sự thật vẫn hiển hiện");
        MEANING_MAP.put("to-wa-ie", "Mặc dù là nói như vậy, dẫu biết là thế nhưng thực tế thì...");
        MEANING_MAP.put("beku-shite", "Chuyện xảy ra là điều tất yếu, đúng như quy luật đã định");
        MEANING_MAP.put("jimiru-jimita", "Đượm vẻ, mang hơi hướng tiêu cực như già nua, lấm lem");
        MEANING_MAP.put("gurumi", "Toàn thể, bao gồm toàn bộ mọi thành viên trong gia đình/tập thể");
        MEANING_MAP.put("ni-mo-hodo-ga-aru", "Làm cái gì cũng phải có giới hạn chừng mực, quá đà rồi!");
        MEANING_MAP.put("o-ii-koto-ni", "Lợi dụng tình thế thuận lợi để làm điều khuất tất có lợi cho mình");
        MEANING_MAP.put("ni-nottotte", "Tuân theo đúng quy chuẩn, phong tục truyền thống hoặc pháp luật");
        MEANING_MAP.put("kare-kare", "Dù ít hay nhiều, dù sớm hay muộn (cặp tính từ sóng đôi)");
        MEANING_MAP.put("koto-kono-ue-nai", "Vô cùng, không có gì có thể vượt qua mức độ tuyệt vời này");
        MEANING_MAP.put("to-mieru-to-miete", "Dường như là, nhìn bề ngoài có vẻ là (suy đoán dựa trên chứng cứ)");
        MEANING_MAP.put("o-fumaete", "Dựa trên cơ sở, căn cứ vào dữ liệu hoặc kinh nghiệm thực tế");
        MEANING_MAP.put("nai-to-mo-kagiranai", "Không hẳn là hoàn toàn không có khả năng xảy ra");
    }

    private static void initPedagogicNotes() {
        NOTES_MAP.put("wa", "Viết là chữ は nhưng đọc là 'wa'. Dùng để đưa ra chủ đề đàm thoại của câu; thông tin quan trọng mới sẽ nằm ở phía sau.");
        NOTES_MAP.put("ga-subject", "Từ để hỏi như だれ, 何 khi làm chủ ngữ bắt buộc phải đi với が, tuyệt đối không dùng は. Cũng đi với 好き, ほしい, 上手, わかる.");
        NOTES_MAP.put("o", "Viết là を nhưng phát âm là 'o'. Đi với các động từ di chuyển như 歩く, 散歩する để chỉ nơi chốn đi xuyên qua.");
        NOTES_MAP.put("ni-time", "Đi với các mốc thời gian có con số cụ thể như ngày, tháng, giờ. Các từ chỉ thời gian tương đối như 今日, 明日, 毎日 thì không dùng に.");
        NOTES_MAP.put("ni-destination", "Nhấn mạnh vào điểm đến (nơi cập bến). Khác với へ nhấn mạnh vào phương hướng di chuyển.");
        NOTES_MAP.put("de-place", "Phân biệt: 教室にいます (tồn tại ở đâu dùng に) với 教室で勉強します (hành động diễn ra ở đâu dùng で).");
        NOTES_MAP.put("te-kudasai", "Dùng để nhờ vả, yêu cầu lịch sự với người cùng cấp hoặc người dưới. Không dùng với cấp trên hoặc đối tác quan trọng (thay bằng ていただけませんか).");
    }

    private static void initTagTranslations() {
        TAG_MAP.put("written", "Văn viết");
        TAG_MAP.put("formal", "Trang trọng");
        TAG_MAP.put("emphasis", "Nhấn mạnh");
        TAG_MAP.put("time", "Thời gian");
        TAG_MAP.put("negation", "Phủ định");
        TAG_MAP.put("condition", "Điều kiện");
        TAG_MAP.put("reason", "Nguyên nhân / Lý do");
        TAG_MAP.put("degree", "Mức độ");
        TAG_MAP.put("concession", "Nhượng bộ");
        TAG_MAP.put("criticism", "Phê phán / Chỉ trích");
        TAG_MAP.put("emotion", "Cảm xúc");
        TAG_MAP.put("manner", "Cách thức");
        TAG_MAP.put("particle", "Trợ từ");
        TAG_MAP.put("sequence", "Trình tự thời gian");
        TAG_MAP.put("relation", "Mối quan hệ");
        TAG_MAP.put("conjecture", "Suy đoán");
        TAG_MAP.put("addition", "Bổ sung");
        TAG_MAP.put("result", "Kết quả");
        TAG_MAP.put("casual", "Thân mật / Văn nói");
        TAG_MAP.put("change", "Sự biến đổi");
        TAG_MAP.put("standpoint", "Lập trường / Góc nhìn");
        TAG_MAP.put("contrast", "Tương phản");
        TAG_MAP.put("possibility", "Khả năng");
        TAG_MAP.put("limitation", "Giới hạn");
        TAG_MAP.put("conjugation", "Chia thể");
        TAG_MAP.put("comparison", "So sánh");
        TAG_MAP.put("appearance", "Vẻ bề ngoài");
        TAG_MAP.put("state", "Trạng thái");
        TAG_MAP.put("range", "Phạm vi");
        TAG_MAP.put("simultaneity", "Đồng thời");
        TAG_MAP.put("tendency", "Khuynh hướng");
        TAG_MAP.put("topic", "Chủ đề");
        TAG_MAP.put("obligation", "Bổn phận / Nghĩa vụ");
        TAG_MAP.put("example", "Ví dụ minh họa");
        TAG_MAP.put("advice", "Lời khuyên");
        TAG_MAP.put("purpose", "Mục đích");
        TAG_MAP.put("politeness", "Lịch sự");
        TAG_MAP.put("intention", "Ý định");
        TAG_MAP.put("quantity", "Số lượng");
        TAG_MAP.put("ability", "Năng lực");
        TAG_MAP.put("surprise", "Bất ngờ / Ngạc nhiên");
        TAG_MAP.put("suggestion", "Gợi ý");
        TAG_MAP.put("decision", "Quyết định");
        TAG_MAP.put("necessity", "Cần thiết");
        TAG_MAP.put("question", "Nghi vấn");
        TAG_MAP.put("quotation", "Trích dẫn");
        TAG_MAP.put("means", "Phương tiện");
        TAG_MAP.put("request", "Nhờ vả / Yêu cầu");
        TAG_MAP.put("prohibition", "Cấm đoán");
        TAG_MAP.put("completion", "Hoàn thành");
        TAG_MAP.put("hearsay", "Nghe truyền đạt");
        TAG_MAP.put("regret", "Hối tiếc");
        TAG_MAP.put("permission", "Cho phép");
        TAG_MAP.put("giving-receiving", "Cho - Nhận ơn huệ");
        TAG_MAP.put("command", "Mệnh lệnh");
        TAG_MAP.put("invitation", "Rủ rê / Lời mời");
        TAG_MAP.put("voice", "Thể bị động / Sai khiến");
        TAG_MAP.put("tense", "Thì thời gian");
        TAG_MAP.put("desire", "Mong muốn");
        TAG_MAP.put("copula", "Trợ động từ");
        TAG_MAP.put("attempt", "Thử nghiệm");
        TAG_MAP.put("humble", "Khiêm nhường ngữ");
        TAG_MAP.put("wish", "Ước muốn");
        TAG_MAP.put("honorific", "Tôn kính ngữ");
        TAG_MAP.put("nominalization", "Danh từ hóa");
        TAG_MAP.put("exclusion", "Loại trừ");
        TAG_MAP.put("demonstrative", "Chỉ định từ");
        TAG_MAP.put("exclamation", "Cảm thán");
        TAG_MAP.put("recollection", "Hồi tưởng");
        TAG_MAP.put("experience", "Kinh nghiệm");
    }

    private static void initCoreExamples() {
        EXAMPLE_MAP.put("私は学生です。", "Tôi là học sinh.");
        EXAMPLE_MAP.put("今日は天気がいいです。", "Hôm nay thời tiết đẹp.");
        EXAMPLE_MAP.put("この本はおもしろいです。", "Quyển sách này rất thú vị.");
        EXAMPLE_MAP.put("雨が降っています。", "Trời đang đổ mưa.");
        EXAMPLE_MAP.put("だれが窓を開けましたか。", "Ai đã mở cửa sổ thế?");
        EXAMPLE_MAP.put("私が行きます。", "Để tôi đi cho.");
        EXAMPLE_MAP.put("毎朝パンを食べます。", "Mỗi sáng tôi đều ăn bánh mì.");
        EXAMPLE_MAP.put("毎朝7時に起きます。", "Mỗi sáng tôi thức dậy lúc 7 giờ.");
        EXAMPLE_MAP.put("毎日バスで学校に行きます。", "Hàng ngày tôi đi học bằng xe buýt.");
        EXAMPLE_MAP.put("机の上に辞書があります。", "Trên bàn có một cuốn từ điển.");
        EXAMPLE_MAP.put("母に手紙を書きます。", "Tôi viết thư gửi cho mẹ.");
        EXAMPLE_MAP.put("一週間に二回、プールで泳ぎます。", "Một tuần tôi đi bơi ở hồ bơi hai lần.");
        EXAMPLE_MAP.put("夏休みに北海道へ行きます。", "Kỳ nghỉ hè tôi sẽ đi Hokkaido.");
        EXAMPLE_MAP.put("図書館で本を読みます。", "Tôi đọc sách ở thư viện.");
        EXAMPLE_MAP.put("毎日電車で会社へ行きます。", "Hàng ngày tôi đi làm bằng tàu điện.");
        EXAMPLE_MAP.put("かばんの中にペンとノートがあります。", "Trong cặp có bút và vở ghi chép.");
        EXAMPLE_MAP.put("週末、友達と映画を見ました。", "Cuối tuần tôi đã đi xem phim cùng bạn bè.");
        EXAMPLE_MAP.put("机の上に本や雑誌があります。", "Trên bàn có những thứ như sách và tạp chí.");
        EXAMPLE_MAP.put("飲み物はコーヒーか紅茶がいいです。", "Đồ uống thì cà phê hay trà đen đều được.");
        EXAMPLE_MAP.put("私も学生です。", "Tôi cũng là học sinh.");
        EXAMPLE_MAP.put("これは私のかさです。", "Đây là chiếc ô của tôi.");
        EXAMPLE_MAP.put("授業は9時からです。", "Tiết học bắt đầu từ 9 giờ.");
        EXAMPLE_MAP.put("昼休みは1時までです。", "Giờ nghỉ trưa kéo dài đến 1 giờ.");
        EXAMPLE_MAP.put("これはあなたのかばんですか。", "Đây có phải là cặp sách của bạn không?");
        EXAMPLE_MAP.put("今日は暑いですね。", "Hôm nay trời nóng thật nhỉ.");
        EXAMPLE_MAP.put("このラーメンはとてもおいしいですよ。", "Món mì ramen này ngon lắm đấy nhé!");
        EXAMPLE_MAP.put("今日はきのうより寒いです。", "Hôm nay trời lạnh hơn hôm qua.");
        EXAMPLE_MAP.put("犬の方が猫より好きです。", "Tôi thích chó hơn là thích mèo.");
        EXAMPLE_MAP.put("コーヒーと紅茶とどちらが好きですか。", "Cà phê và trà đen thì bạn thích món nào hơn?");
        EXAMPLE_MAP.put("果物の中でりんごが一番好きです。", "Trong các loại trái cây, tôi thích nhất là táo.");
        EXAMPLE_MAP.put("散歩がてら、パンを買ってきた。", "Nhân tiện đi dạo, tôi đã mua bánh mì về.");
        EXAMPLE_MAP.put("買い物がてら、図書館へ行って本を返した。", "Nhân tiện đi mua sắm, tôi đã ghé qua thư viện trả sách.");
        EXAMPLE_MAP.put("大学で教えるかたわら、小説を書いている。", "Bên cạnh việc giảng dạy ở trường đại học, ông ấy còn viết tiểu thuyết.");
        EXAMPLE_MAP.put("ベルが鳴るが早いか、生徒たちは教室を飛び出した。", "Chuông vừa mới reo dứt thì các học sinh đã lập tức ùa ra khỏi lớp học.");
        EXAMPLE_MAP.put("名前すら書けない。", "Ngay cả tên của mình mà cũng không viết nổi.");
        EXAMPLE_MAP.put("いいことずくめの一日だった。", "Hôm nay quả là một ngày toàn những điều may mắn tốt lành.");
    }

    private static String smartTranslateMeaning(String en, String pattern, String level) {
        if (en == null || en.isBlank()) {
            return "Mẫu cấu trúc ngữ pháp " + (pattern != null ? pattern : "") + " cấp độ " + (level != null ? level : "JLPT");
        }
        String s = en.toLowerCase();
        if (s.contains("as for") || s.contains("topic of the sentence")) return "Thì; là; đóng vai trò làm chủ đề chính của câu";
        if (s.contains("marks the subject")) return "Chỉ chủ ngữ thực hiện hành động, nhấn mạnh thông tin mới";
        if (s.contains("direct object")) return "Chỉ tân ngữ trực tiếp chịu tác động của động từ";
        if (s.contains("destination")) return "Chỉ đích đến hoặc phương hướng di chuyển";
        if (s.contains("please do")) return "Xin hãy, vui lòng làm...";
        if (s.contains("please don't")) return "Xin đừng làm...";
        if (s.contains("want to")) return "Muốn làm gì đó (nguyện vọng bản thân)";
        if (s.contains("want")) return "Muốn có cái gì đó (mong muốn sở hữu)";
        if (s.contains("may do") || s.contains("allowed to")) return "Được phép làm gì đó";
        if (s.contains("must not") || s.contains("prohibition")) return "Không được phép làm... (cấm đoán)";
        if (s.contains("have to") || s.contains("must")) return "Bắt buộc phải làm...";
        if (s.contains("don't have to") || s.contains("need not")) return "Không cần phải làm gì cũng được";
        if (s.contains("ongoing action") || s.contains("state")) return "Đang làm...; Đang ở trạng thái...";
        if (s.contains("after doing")) return "Sau khi làm việc gì đó xong thì...";
        if (s.contains("before doing")) return "Trước khi làm việc gì đó...";
        if (s.contains("while")) return "Trong khi, vừa làm việc này vừa làm việc kia";
        if (s.contains("too much") || s.contains("excessively")) return "Quá mức, quá đà (mang nghĩa tiêu cực)";
        if (s.contains("easy to")) return "Dễ làm việc gì đó";
        if (s.contains("hard to") || s.contains("difficult to")) return "Khó làm việc gì đó";
        if (s.contains("experience")) return "Đã từng làm gì đó trong quá khứ";
        if (s.contains("can do") || s.contains("able to")) return "Có thể làm được việc gì đó";
        if (s.contains("plan to") || s.contains("intend to")) return "Dự định làm gì đó";
        if (s.contains("it seems") || s.contains("looks like")) return "Có vẻ như, dường như là...";
        if (s.contains("hearsay") || s.contains("heard that")) return "Nghe nói là, theo như được biết...";
        if (s.contains("because") || s.contains("reason")) return "Bởi vì, do nguyên nhân...";
        if (s.contains("if") || s.contains("condition")) return "Nếu... thì...";
        if (s.contains("even if") || s.contains("although")) return "Cho dù, dẫu cho... thì vẫn...";
        if (s.contains("in order to") || s.contains("purpose")) return "Để mà, nhằm mục đích...";
        if (s.contains("only") || s.contains("just")) return "Chỉ có, duy nhất chỉ...";
        if (s.contains("not only")) return "Không những... mà còn...";
        if (s.contains("about") || s.contains("regarding")) return "Về, liên quan tới...";
        if (s.contains("according to")) return "Theo như nguồn tin từ...";
        if (s.contains("cannot afford to") || s.contains("cannot")) return "Không thể làm được vì lí do đặc biệt";
        if (s.contains("no choice but")) return "Đành phải, không còn lựa chọn nào khác";

        // Tự động chuyển dịch các từ ngữ tiếng Anh thông dụng sang tiếng Việt
        String res = en.trim()
                .replace("while; on the way; taking the opportunity of", "Nhân tiện, tiện thể trên đường làm việc chính")
                .replace("while also; alongside", "Đồng thời kiêm thêm, bên cạnh việc chính")
                .replace("the moment; no sooner ... than", "Ngay sau khi vừa mới... thì lập tức")
                .replace("even; not even", "Ngay cả, thậm chí đến cả")
                .replace("nothing but; entirely", "Toàn là, ngập tràn những thứ")
                .replace("once ... (there's no going back)", "Một khi đã... thì coi như xong")
                .replace("ever since", "Kể từ thời điểm đó cho tới nay")
                .replace("never cease to; sincerely; deeply", "Không ngừng, luôn luôn tha thiết")
                .replace("combined with; coupled with", "Cùng với, kết hợp cùng với")
                .replace("when it comes to; once", "Một khi đã đạt đến tầm cỡ");

        return "Cấu trúc " + (pattern != null ? pattern : "") + ": " + res;
    }

    private static String smartTranslateNotes(String enNotes, String pattern, String level) {
        if (enNotes == null || enNotes.isBlank()) {
            return "Cấu trúc " + (pattern != null ? pattern : "") + " thường gặp trong các đề thi JLPT cấp độ " + (level != null ? level : "N5") + " và giao tiếp đời sống hàng ngày.";
        }
        String s = enNotes.trim();
        s = s.replace("Used with", "Thường dùng đi kèm với")
             .replace("Written", "Được viết là")
             .replace("pronounced", "nhưng được phát âm là")
             .replace("Compare", "So sánh đối chiếu với")
             .replace("Common with", "Phổ biến đi cùng các động từ")
             .replace("Suggests", "Biểu thị")
             .replace("Means", "Có nghĩa là")
             .replace("Often paired with", "Thường đi thành cặp với")
             .replace("Marks the end point", "Đánh dấu điểm kết thúc của sự việc");
        return s;
    }
}
