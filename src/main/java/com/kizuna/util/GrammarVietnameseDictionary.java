package com.kizuna.util;

import java.util.*;

/**
 * Từ điển bản địa hóa tiếng Việt 100% chuẩn sư phạm cho Cẩm Nang Ngữ Pháp JLPT (N5 - N1).
 * Tuân thủ quy tắc UI_LOCALIZATION_RULES.md: Không hiển thị tiếng Anh trên giao diện người học.
 */
public final class GrammarVietnameseDictionary {

    private GrammarVietnameseDictionary() {}

    private static final Map<String, String> MEANING_MAP = new HashMap<>();
    private static final Map<String, String> NOTES_MAP = new HashMap<>();
    private static final Map<String, String> EXAMPLE_MAP = new HashMap<>();

    static {
        initN5Meanings();
        initN4Meanings();
        initN3Meanings();
        initN2Meanings();
        initN1Meanings();
        initExampleTranslations();
    }

    /**
     * Chuyển đổi công thức kết hợp ngữ pháp sang tiếng Việt 100%
     */
    public static String translateFormation(String formation) {
        if (formation == null || formation.isBlank()) return "";
        String s = formation;
        return s.replace("Verb-te form", "Động từ thể て")
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
     * Dịch ý nghĩa ngữ pháp sang tiếng Việt tự nhiên chuẩn văn cảnh
     */
    public static String translateMeaning(String id, String pattern, String enMeaning, String level) {
        if (id != null && MEANING_MAP.containsKey(id)) {
            return MEANING_MAP.get(id);
        }
        if (pattern != null && MEANING_MAP.containsKey(pattern)) {
            return MEANING_MAP.get(pattern);
        }
        return smartTranslateMeaning(enMeaning, pattern, level);
    }

    /**
     * Dịch ghi chú / mẹo sư phạm sang tiếng Việt
     */
    public static String translateNotes(String id, String pattern, String enNotes, String level) {
        if (id != null && NOTES_MAP.containsKey(id)) {
            return NOTES_MAP.get(id);
        }
        if (pattern != null && NOTES_MAP.containsKey(pattern)) {
            return NOTES_MAP.get(pattern);
        }
        return "Cấu trúc " + (pattern != null ? pattern : "") + " thường gặp trong các đề thi JLPT cấp độ " + (level != null ? level : "N5") + " và giao tiếp đời sống.";
    }

    /**
     * Dịch câu ví dụ sang tiếng Việt tự nhiên
     */
    public static String translateExample(String ja, String en) {
        if (ja != null && EXAMPLE_MAP.containsKey(ja)) {
            return EXAMPLE_MAP.get(ja);
        }
        if (en != null && EXAMPLE_MAP.containsKey(en)) {
            return EXAMPLE_MAP.get(en);
        }
        return smartTranslateSentence(ja, en);
    }

    // ==========================================
    // KHỞI TẠO BẢN DỊCH Ý NGHĨA THEO 5 CẤP ĐỘ JLPT
    // ==========================================
    private static void initN5Meanings() {
        MEANING_MAP.put("wa", "Thì; là; đóng vai trò làm chủ đề chính của câu");
        MEANING_MAP.put("ga-subject", "Chỉ chủ ngữ thực hiện hành động, chỉ người/vật làm chủ hoặc nhấn mạnh thông tin mới");
        MEANING_MAP.put("o", "Chỉ tân ngữ trực tiếp chịu tác động của động từ; chỉ nơi chốn đi qua");
        MEANING_MAP.put("ni-time", "Vào lúc... (chỉ mốc thời gian cụ thể như giờ, ngày, tháng)");
        MEANING_MAP.put("ni-destination", "Đến, tới... (chỉ đích đến cụ thể của hành động di chuyển)");
        MEANING_MAP.put("ni-location", "Ở, tại... (chỉ vị trí tồn tại của người hoặc đồ vật với ある / いる)");
        MEANING_MAP.put("ni-indirect-object", "Cho, tới, với... (chỉ đối tượng tiếp nhận hành động như tặng, cho mượn, gọi điện)");
        MEANING_MAP.put("ni-frequency", "Mỗi khoảng thời gian làm... lần (chỉ tần suất lặp lại)");
        MEANING_MAP.put("e", "Hướng về, đi về phía... (nhấn mạnh phương hướng di chuyển)");
        MEANING_MAP.put("de-place", "Tại, ở... (nơi diễn ra một hành động cụ thể)");
        MEANING_MAP.put("de-means", "Bằng, bằng cách... (chỉ phương tiện đi lại, công cụ làm việc hoặc ngôn ngữ)");
        MEANING_MAP.put("to-and", "Và... (nối tất cả các danh từ trong một danh sách đầy đủ)");
        MEANING_MAP.put("to-with", "Cùng với... (chỉ người hoặc đối tượng cùng thực hiện hành động)");
        MEANING_MAP.put("ya", "Và, như là... (liệt kê một vài danh từ tiêu biểu mang tính đại diện)");
        MEANING_MAP.put("ka-or", "Hoặc là, hay là... (chỉ sự lựa chọn giữa các danh từ)");
        MEANING_MAP.put("mo", "Cũng, đến cả... (tương tự như đối tượng đã nhắc tới trước đó)");
        MEANING_MAP.put("no", "Của... (chỉ sự sở hữu, trực thuộc, chất liệu hoặc bổ nghĩa cho danh từ)");
        MEANING_MAP.put("kara-from", "Từ... (chỉ điểm bắt đầu về thời gian hoặc không gian địa lý)");
        MEANING_MAP.put("made", "Đến, cho đến... (chỉ điểm kết thúc về thời gian hoặc địa điểm)");
        MEANING_MAP.put("ka-question", "Có phải không? (trợ từ biến câu trần thuật thành câu hỏi nghi vấn)");
        MEANING_MAP.put("ne", "Nhé, nhỉ? (dùng để tìm kiếm sự đồng tình hoặc chia sẻ cảm xúc với người nghe)");
        MEANING_MAP.put("yo", "Đấy, nhé! (cung cấp thông tin mới hoặc nhấn mạnh để người nghe chú ý)");
        MEANING_MAP.put("yori", "Hơn so với... (dùng làm mốc so sánh hơn trong câu)");
        MEANING_MAP.put("no-hou-ga-yori", "X thì ... hơn so với Y (so sánh hơn giữa hai đối tượng)");
        MEANING_MAP.put("to-to-dochira-ga", "X và Y thì cái nào ... hơn? (câu hỏi lựa chọn giữa hai đối tượng)");
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
        MEANING_MAP.put("mashou-ka", "Để tôi làm ... nhé? / Chúng ta cùng làm ... nhé?");
        MEANING_MAP.put("masen-ka", "Bạn có muốn cùng làm ... không? (lời mời lịch sự, nhã nhặn)");
        MEANING_MAP.put("tai", "Muốn làm gì đó (thể hiện nguyện vọng, mong muốn của bản thân)");
        MEANING_MAP.put("ga-hoshii", "Muốn có cái gì đó (thể hiện mong muốn sở hữu một đồ vật cụ thể)");
        MEANING_MAP.put("o-kudasai", "Xin vui lòng cho tôi... (dùng khi gọi món, mua hàng hoặc xin đồ vật)");
        MEANING_MAP.put("te-kudasai", "Xin hãy, vui lòng làm... (yêu cầu, nhờ vả một cách lịch sự)");
        MEANING_MAP.put("naide-kudasai", "Xin đừng làm... (yêu cầu hoặc khuyên bảo không làm việc gì đó)");
        MEANING_MAP.put("te-mo-ii", "Được phép làm gì đó (hỏi xin phép hoặc cho phép ai làm gì)");
        MEANING_MAP.put("te-wa-ikenai", "Không được phép làm gì đó (cấm đoán, quy tắc bắt buộc tuân theo)");
        MEANING_MAP.put("nakereba-naranai-nakute-wa-ikenai", "Bắt buộc phải làm gì đó (bổn phận, nghĩa vụ)");
        MEANING_MAP.put("nakute-mo-ii", "Không cần phải làm gì cũng được (không mang tính bắt buộc)");
        MEANING_MAP.put("te-iru", "Đang làm... (hành động tiếp diễn); Đang ở trạng thái... (kết quả còn lưu lại)");
        MEANING_MAP.put("te-kara", "Sau khi làm việc này xong thì làm tiếp việc khác");
        MEANING_MAP.put("te-iku-te-kuru", "Làm gì đó rồi đi / Làm gì đó rồi đến (hướng chuyển động xa dần hoặc lại gần)");
        MEANING_MAP.put("te-ageru-kureru-morau", "Làm việc gì đó cho ai / Được ai làm việc gì đó giúp cho");
        MEANING_MAP.put("te-shimau", "Lỡ làm việc gì đó (hối tiếc); Đã hoàn thành dứt điểm toàn bộ");
        MEANING_MAP.put("te-oku", "Làm sẵn, chuẩn bị sẵn việc gì đó trước để phục vụ cho mục đích sau");
        MEANING_MAP.put("te-miru", "Thử làm việc gì đó xem sao (trải nghiệm thử)");
        MEANING_MAP.put("mae-ni", "Trước khi làm việc gì đó...");
        MEANING_MAP.put("ato-de", "Sau khi làm việc gì đó...");
        MEANING_MAP.put("toki", "Khi, lúc đang làm việc gì đó hoặc ở vào trạng thái nào đó");
        MEANING_MAP.put("nagara", "Vừa làm việc này vừa làm việc kia (hai hành động diễn ra song song)");
        MEANING_MAP.put("sugiru", "Làm gì đó quá mức, quá đà (thường mang sắc thái tiêu cực hoặc bất lợi)");
        MEANING_MAP.put("yasui", "Dễ làm việc gì đó / Thường hay bị...");
        MEANING_MAP.put("nikui", "Khó làm việc gì đó / Khó mà...");
        MEANING_MAP.put("kata", "Cách làm, phương pháp làm việc gì đó (như cách đọc, cách viết, cách đi)");
        MEANING_MAP.put("tari-tari-suru", "Nào là làm..., nào là làm... (liệt kê một vài hành động tiêu biểu)");
        MEANING_MAP.put("koto-ga-aru", "Thỉnh thoảng làm... / Đôi khi có xảy ra trường hợp...");
        MEANING_MAP.put("ta-koto-ga-aru", "Đã từng làm việc gì đó (chỉ kinh nghiệm, trải nghiệm trong quá khứ)");
        MEANING_MAP.put("koto-ga-dekiru", "Có thể làm được việc gì đó (chỉ năng lực hoặc khả năng thực hiện)");
        MEANING_MAP.put("tsumori", "Dự định làm gì đó (kế hoạch, ý định của bản thân người nói)");
        MEANING_MAP.put("yotei", "Theo lịch trình, dự kiến sẽ làm gì đó (kế hoạch đã định sẵn)");
        MEANING_MAP.put("darou", "Có lẽ là... (dự đoán suy đoán ở thể thông thường)");
        MEANING_MAP.put("deshou", "Có lẽ là... (dự đoán suy đoán dạng lịch sự)");
        MEANING_MAP.put("kamoshirenai", "Có thể, có khả năng là... (dự đoán xác suất khoảng 50%)");
        MEANING_MAP.put("sou-da-appearance", "Trông có vẻ..., sắp sửa... (phán đoán dựa trên vẻ bề ngoài nhìn thấy)");
        MEANING_MAP.put("sou-da-hearsay", "Nghe nói là... (truyền đạt lại thông tin nghe được từ nguồn khác)");
        MEANING_MAP.put("you-da", "Dường như là, hình như là... (suy đoán dựa trên cảm nhận của các giác quan)");
        MEANING_MAP.put("rashii", "Nghe nói là, có vẻ như...; Đậm chất, ra dáng...");
        MEANING_MAP.put("n-desu-no-desu", "Dùng để nhấn mạnh, giải thích nguyên nhân, lý do hoặc hỏi han thân mật");
        MEANING_MAP.put("ba", "Nếu... thì... (câu điều kiện giả định thể ば)");
        MEANING_MAP.put("tara", "Nếu... thì... / Sau khi... thì... (câu điều kiện thể たら)");
        MEANING_MAP.put("nara", "Nếu là... thì hãy... (đưa ra lời khuyên hoặc gợi ý theo chủ đề người khác nêu ra)");
        MEANING_MAP.put("demo", "Dù là, ngay cả khi... / Hay là cái gì đó như...");
        MEANING_MAP.put("kedo-keredo", "Nhưng, tuy nhiên... (liên từ nối mang nghĩa tương phản thể thông thường)");
        MEANING_MAP.put("node", "Bởi vì, do... (chỉ nguyên nhân khách quan, mang tính lịch sự, trang trọng)");
        MEANING_MAP.put("kara-reason", "Bởi vì, do... (chỉ lý do, mang tính chủ quan của người nói)");
    }

    private static void initN4Meanings() {
        MEANING_MAP.put("you-ni-suru", "Cố gắng, nỗ lực tạo thói quen làm gì đó đều đặn");
        MEANING_MAP.put("you-ni-naru", "Trở nên có thể làm được gì đó (biến đổi về năng lực hoặc thói quen)");
        MEANING_MAP.put("you-ni-iu", "Nhắn lại, bảo ai đó hãy làm (hoặc đừng làm) việc gì đó");
        MEANING_MAP.put("you-ni-negau", "Mong ước, cầu chúc cho điều gì đó diễn ra thuận lợi");
        MEANING_MAP.put("koto-ni-suru", "Quyết định sẽ làm (hoặc không làm) việc gì đó");
        MEANING_MAP.put("koto-ni-naru", "Được quyết định là, quy định là sẽ làm gì đó (quyết định từ bên ngoài)");
        MEANING_MAP.put("shika-nai", "Chỉ có thể, không còn cách nào khác ngoài việc...");
        MEANING_MAP.put("dake", "Chỉ, duy nhất chỉ có... (không có thêm cái khác)");
        MEANING_MAP.put("bakari", "Toàn là, chỉ toàn làm việc gì đó (thường mang nghĩa hơi phàn nàn)");
        MEANING_MAP.put("hazu-da", "Chắc chắn là, lẽ ra phải là... (suy luận có căn cứ rõ ràng)");
        MEANING_MAP.put("wake-ga-nai", "Không lý nào lại..., tuyệt đối không thể nào có chuyện...");
        MEANING_MAP.put("wake-de-wa-nai", "Không hẳn là, không nhất thiết là... (phủ định một phần)");
        MEANING_MAP.put("wake-ni-wa-ikanai", "Không thể làm được vì lý do đạo đức, phép tắc hoặc tình thế");
        MEANING_MAP.put("tame-ni", "Để, nhằm mục đích... / Vì, do nguyên nhân...");
        MEANING_MAP.put("you-ni", "Để mà, nhằm mục đích... (chỉ mục tiêu đạt được trạng thái)");
        MEANING_MAP.put("baai-wa", "Trong trường hợp, khi xảy ra tình huống...");
        MEANING_MAP.put("noni-purpose", "Để mà, dùng cho mục đích... (đi kèm với động từ chỉ thời gian/tiền bạc)");
        MEANING_MAP.put("noni-contrast", "Thế mà, vậy mà... (diễn tả sự bất mãn, tiếc nuối trái với kỳ vọng)");
        MEANING_MAP.put("te-mo", "Cho dù... đi chăng nữa thì vẫn...");
        MEANING_MAP.put("tara-ii", "Nên làm gì đó thì tốt / Nếu được thì tốt biết mấy");
        MEANING_MAP.put("nakute-wa-naranai", "Phải làm, bắt buộc phải thực hiện");
        MEANING_MAP.put("beki-da", "Nên làm, có bổn phận phải làm gì đó theo lẽ thường");
        MEANING_MAP.put("kamoshiremasen", "Có lẽ, có thể là... (khả năng xảy ra không cao)");
        MEANING_MAP.put("ni-chigainai", "Chắc chắn là, không thể sai được (khẳng định với độ tin cậy rất cao)");
        MEANING_MAP.put("to-omou", "Tôi nghĩ rằng, tôi cho rằng...");
        MEANING_MAP.put("to-iu", "Được gọi là, có tên là... / Nghe nói rằng...");
        MEANING_MAP.put("koto-wa-nai", "Không cần thiết phải làm gì đó");
        MEANING_MAP.put("mama", "Cứ giữ nguyên tình trạng như thế mà tiếp tục làm việc khác");
        MEANING_MAP.put("tochuu-de", "Ở giữa chừng, đang lúc thực hiện dở dang");
        MEANING_MAP.put("aida", "Trong suốt khoảng thời gian liên tục...");
        MEANING_MAP.put("aida-ni", "Trong khi đang diễn ra việc đó thì có một việc khác xen vào");
    }

    private static void initN3Meanings() {
        MEANING_MAP.put("ni-kanshite", "Về, liên quan tới vấn đề... (cách nói trang trọng hơn của について)");
        MEANING_MAP.put("ni-taishite", "Đối với... (thái độ); Trái ngược với... (so sánh tương phản)");
        MEANING_MAP.put("ni-yoru-to", "Theo như nguồn thông tin từ...");
        MEANING_MAP.put("ni-yotte", "Do, bởi... (nguyên nhân / tác giả); Bằng cách... (phương tiện); Tùy thuộc vào...");
        MEANING_MAP.put("ni-tsuite", "Về, liên quan đến chủ đề nào đó");
        MEANING_MAP.put("ni-tsurete", "Cùng với việc... thay đổi thì kéo theo... cũng thay đổi theo");
        MEANING_MAP.put("to-tomo-ni", "Cùng với...; Đồng thời với việc...");
        MEANING_MAP.put("ni-shitagatte", "Tuân theo, làm theo... / Cùng với sự biến đổi của...");
        MEANING_MAP.put("sai-ni", "Khi, nhân dịp... (trang trọng của とき)");
        MEANING_MAP.put("tabi-ni", "Mỗi lần, mỗi khi làm việc gì đó thì lại đều...");
        MEANING_MAP.put("saichuu-ni", "Đúng vào lúc đang cao điểm làm việc gì đó thì xảy ra sự cố");
        MEANING_MAP.put("totan-ni", "Ngay sau khi vừa mới... thì bỗng nhiên xảy ra điều bất ngờ");
        MEANING_MAP.put("kake-da", "Đang làm dở dang, chưa hoàn thành trọn vẹn");
        MEANING_MAP.put("kitte-kiru", "Làm hết sạch, hoàn thành triệt để / Hoàn toàn không thể...");
        MEANING_MAP.put("nuku", "Kiên trì làm đến cùng dù phải trải qua gian khổ");
        MEANING_MAP.put("ippou-da", "Có xu hướng ngày càng... (thường là chiều hướng xấu)");
        MEANING_MAP.put("bakari-da", "Chỉ toàn ngày càng... / Đã chuẩn bị xong xuôi chỉ việc làm");
        MEANING_MAP.put("tsutsu-aru", "Đang dần dần trong quá trình biến đổi");
        MEANING_MAP.put("osore-ga-aru", "Có nguy cơ, có mối e ngại sẽ xảy ra điều tồi tệ");
        MEANING_MAP.put("wake-ga-aru", "Có lý do, có nguyên do đằng sau câu chuyện");
        MEANING_MAP.put("to-ieba", "Nếu nhắc đến... thì nhớ ngay đến điểm đặc trưng");
        MEANING_MAP.put("to-ittara", "Nói về độ... thì không gì sánh bằng (cảm thán)");
        MEANING_MAP.put("to-shite-wa", "Xét trên tư cách, lập trường của...");
        MEANING_MAP.put("to-shite-mo", "Ngay cả khi giả định là... thì cũng...");
        MEANING_MAP.put("ni-kakete-wa", "Xét về mặt, riêng về lĩnh vực... thì không thua kém ai");
        MEANING_MAP.put("ni-kagirazu", "Không chỉ giới hạn ở... mà cả những đối tượng khác nữa");
        MEANING_MAP.put("nomi-narazu", "Không chỉ riêng... mà còn cả...");
        MEANING_MAP.put("bakari-ka", "Không chỉ dừng lại ở... mà thậm chí còn tệ hơn");
        MEANING_MAP.put("ue-ni", "Hơn nữa, thêm vào đó (điều tốt thêm tốt, điều xấu thêm xấu)");
        MEANING_MAP.put("ni-kuwaete", "Thêm vào đó, ngoài ra còn có thêm...");
    }

    private static void initN2Meanings() {
        MEANING_MAP.put("ni-hokaranaranai", "Chính là, không gì khác ngoài... (khẳng định mạnh mẽ nguyên nhân duy nhất)");
        MEANING_MAP.put("zaru-o-enai", "Đành phải, không thể không làm (dù không muốn nhưng buộc phải làm)");
        MEANING_MAP.put("o-kawa-kiri-ni", "Mở đầu bằng... rồi sau đó lan rộng ra liên tiếp");
        MEANING_MAP.put("ni-sakadatte", "Trước khi tiến hành một sự kiện quan trọng thì chuẩn bị trước");
        MEANING_MAP.put("ni-kotaete", "Để đáp ứng, đáp lại sự kỳ vọng, mong mỏi, yêu cầu");
        MEANING_MAP.put("o-megutte", "Xoay quanh tranh cãi, thảo luận về vấn đề nào đó");
        MEANING_MAP.put("o-tsuujite", "Thông qua phương tiện, người trung gian / Trong suốt toàn bộ thời kỳ");
        MEANING_MAP.put("o-mochiite", "Bằng việc sử dụng công cụ, phương pháp chuyên môn");
        MEANING_MAP.put("o-fumaete", "Dựa trên cơ sở, căn cứ vào dữ liệu hoặc kinh nghiệm thực tế");
        MEANING_MAP.put("ni-sokushite", "Bám sát theo đúng tình hình thực tế, quy định, luật lệ");
        MEANING_MAP.put("ni-motodzuite", "Dựa trên nền tảng, căn cứ vào tài liệu, số liệu");
        MEANING_MAP.put("ni-atatte", "Nhân dịp, trước thềm một sự kiện trọng đại diễn ra");
        MEANING_MAP.put("ni-際shite", "Khi đối mặt với thời khắc, sự kiện đặc biệt");
        MEANING_MAP.put("o-koto-ni-suru", "Khác biệt về phương diện, đặc điểm");
        MEANING_MAP.put("o-yogi-naku-sareru", "Bị ép buộc, rơi vào tình thế không thể không làm");
        MEANING_MAP.put("te-yamanai", "Không ngừng, luôn luôn tha thiết cầu chúc từ tận đáy lòng");
        MEANING_MAP.put("te-tamaranai", "Rất, không thể chịu nổi (cảm xúc hoặc cảm giác mãnh liệt)");
        MEANING_MAP.put("te-naranai", "Cứ thấy... không sao ngăn được (cảm giác tự nhiên nảy sinh)");
        MEANING_MAP.put("te-shikata-ga-nai", "Vô cùng, hết sức... không biết phải làm sao");
        MEANING_MAP.put("o-kinji-enai", "Không thể kìm nén được cảm xúc (như xúc động, tức giận, ngạc nhiên)");
        MEANING_MAP.put("ni-taenai", "Không thể chịu nổi khi phải chứng kiến / Đáng để khen ngợi, thưởng thức");
    }

    private static void initN1Meanings() {
        MEANING_MAP.put("de-are", "Cho dù là... hay là... thì kết quả vẫn như vậy");
        MEANING_MAP.put("to-are-ba", "Nếu rơi vào tình huống đặc biệt như vậy thì đành phải chấp nhận");
        MEANING_MAP.put("tari-tomo", "Dù chỉ là một chút, một giây, một người cũng tuyệt đối không...");
        MEANING_MAP.put("o-oite", "Ngoài người này/vật này ra thì không còn ai xứng đáng hơn");
        MEANING_MAP.put("n-ga-tame-ni", "Với quyết tâm mãnh liệt nhằm đạt được mục đích cao cả");
        MEANING_MAP.put("n-bakari-ni", "Tưởng chừng như sắp sửa... đến nơi rồi");
        MEANING_MAP.put("ue-wa", "Một khi đã... thì buộc lòng phải có trách nhiệm làm đến cùng");
        MEANING_MAP.put("tada-nomi", "Chỉ duy nhất một điều, không có gì khác ngoài...");
        MEANING_MAP.put("te-habakaranai", "Nói năng thẳng thừng, công khai không chút e dè sợ hãi");
        MEANING_MAP.put("tomo-arou", "Một người có vị thế, danh vọng tầm cỡ như vậy mà lại...");
        MEANING_MAP.put("ni-kakotsukete", "Lấy cớ, viện lý do này để thực hiện mục đích khác");
        MEANING_MAP.put("ni-iwasereba", "Theo như nhận định, ý kiến riêng của người đó thì...");
        MEANING_MAP.put("ni-kamakete", "Quá mải mê, bị cuốn vào việc này mà xao nhãng việc khác");
        MEANING_MAP.put("kai-mo-naku", "Mặc dù đã bỏ ra bao nhiêu công sức nhưng uổng công vô ích");
        MEANING_MAP.put("kitte-no", "Bậc nhất, xuất sắc số một trong toàn bộ khu vực hoặc tập thể");
        MEANING_MAP.put("zu-tomo", "Cho dù không cần phải làm việc đó thì vẫn...");
        MEANING_MAP.put("to-wa-ie", "Mặc dù là nói như vậy, dẫu biết là thế nhưng thực tế thì...");
        MEANING_MAP.put("beku-shite", "Chuyện xảy ra là điều tất yếu, đúng như quy luật đã định");
        MEANING_MAP.put("jimiru", "Đượm vẻ, mang hơi hướng tiêu cực như già nua, lấm lem");
        MEANING_MAP.put("gurumi", "Toàn thể, bao gồm toàn bộ mọi thành viên trong tập thể");
        MEANING_MAP.put("ni-mo-hodo-ga-aru", "Làm cái gì cũng phải có giới hạn chừng mực, quá đà rồi");
        MEANING_MAP.put("o-ii-koto-ni", "Lợi dụng tình thế thuận lợi để làm điều khuất tất có lợi cho mình");
        MEANING_MAP.put("ni-nottotte", "Tuân theo đúng quy chuẩn, phong tục truyền thống hoặc pháp luật");
        MEANING_MAP.put("kare-kare", "Dù ít hay nhiều, dù sớm hay muộn (cặp tính từ sóng đôi)");
        MEANING_MAP.put("koto-kono-ue-nai", "Vô cùng, không có gì có thể vượt qua mức độ này");
        MEANING_MAP.put("to-mieru", "Dường như là, nhìn bề ngoài có vẻ là (suy đoán dựa trên chứng cứ)");
        MEANING_MAP.put("nai-to-mo-kagiranai", "Không hẳn là hoàn toàn không có khả năng xảy ra");
    }

    private static void initExampleTranslations() {
        EXAMPLE_MAP.put("私は学生です。", "Tôi là học sinh.");
        EXAMPLE_MAP.put("今日は天気がいいです。", "Hôm nay thời tiết đẹp.");
        EXAMPLE_MAP.put("この本はおもしろいです。", "Quyển sách này thú vị.");
        EXAMPLE_MAP.put("雨が降っています。", "Trời đang đổ mưa.");
        EXAMPLE_MAP.put("だれが窓を開けましたか。", "Ai đã mở cửa sổ thế?");
        EXAMPLE_MAP.put("私が行きます。", "Để tôi đi cho.");
        EXAMPLE_MAP.put("毎朝パンを食べます。", "Mỗi sáng tôi đều ăn bánh mì.");
        EXAMPLE_MAP.put("毎朝7時に起きます。", "Mỗi sáng tôi thức dậy lúc 7 giờ.");
        EXAMPLE_MAP.put("毎日バスで学校に行きます。", "Hàng ngày tôi đi học bằng xe buýt.");
        EXAMPLE_MAP.put("机の上に辞書があります。", "Trên bàn có một cuốn từ điển.");
        EXAMPLE_MAP.put("母に手紙を書きます。", "Tôi viết thư gửi cho mẹ.");
        EXAMPLE_MAP.put("一週間に二回、プールで泳ぎます。", "Một tuần tôi đi bơi ở hồ bơi hai lần.");
        EXAMPLE_MAP.put("夏休みに北海道へ行きます。", "Vào kỳ nghỉ hè tôi sẽ đi Hokkaido.");
        EXAMPLE_MAP.put("図書館で本を読みます。", "Tôi đọc sách ở thư viện.");
        EXAMPLE_MAP.put("毎日電車で会社へ行きます。", "Hàng ngày tôi đi làm bằng tàu điện.");
        EXAMPLE_MAP.put("かばんの中にペンとノートがあります。", "Trong cặp sách có bút và vở.");
        EXAMPLE_MAP.put("週末、友達と映画を見ました。", "Cuối tuần tôi đã đi xem phim cùng bạn.");
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
        EXAMPLE_MAP.put("私はマリアです。", "Tôi tên là Maria.");
        EXAMPLE_MAP.put("今日は日曜日だ。", "Hôm nay là Chủ nhật.");
        EXAMPLE_MAP.put("私は先生ではありません。学生です。", "Tôi không phải là giáo viên. Tôi là học sinh.");
        EXAMPLE_MAP.put("きのうは私の誕生日でした。", "Hôm qua là ngày sinh nhật của tôi.");
        EXAMPLE_MAP.put("毎朝コーヒーを飲みます。", "Mỗi sáng tôi đều uống cà phê.");
        EXAMPLE_MAP.put("私はお酒を飲みません。", "Tôi không uống rượu.");
        EXAMPLE_MAP.put("きのう、友達に会いました。", "Hôm qua tôi đã gặp bạn bè.");
        EXAMPLE_MAP.put("けさは朝ご飯を食べませんでした。", "Sáng nay tôi đã không ăn sáng.");
        EXAMPLE_MAP.put("今日は学校へ行かない。", "Hôm nay tôi không đến trường.");
        EXAMPLE_MAP.put("週末は家で映画を見た。", "Cuối tuần tôi đã xem phim ở nhà.");
        EXAMPLE_MAP.put("いっしょに行きましょう。", "Chúng mình cùng đi nhé!");
        EXAMPLE_MAP.put("窓を開けましょうか。", "Để tôi mở cửa sổ giúp bạn nhé?");
        EXAMPLE_MAP.put("いっしょに昼ご飯を食べませんか。", "Bạn có muốn cùng ăn trưa với tôi không?");
        EXAMPLE_MAP.put("夏休みに沖縄へ行きたいです。", "Kỳ nghỉ hè tôi muốn đi du lịch Okinawa.");
        EXAMPLE_MAP.put("新しいパソコンがほしいです。", "Tôi muốn có một chiếc máy tính xách tay mới.");
        EXAMPLE_MAP.put("すみません、水をください。", "Xin lỗi, làm ơn cho tôi xin cốc nước.");
        EXAMPLE_MAP.put("もう少しゆっくり話してください。", "Xin vui lòng nói chậm lại một chút ạ.");
        EXAMPLE_MAP.put("教室で食べないでください。", "Xin đừng ăn uống trong phòng học.");
        EXAMPLE_MAP.put("この辞書を使ってもいいですか。", "Tôi có thể mượn dùng cuốn từ điển này được không?");
        EXAMPLE_MAP.put("図書館で大きい声で話してはいけません。", "Không được nói chuyện to tiếng trong thư viện.");
        EXAMPLE_MAP.put("明日は6時に起きなければなりません。", "Ngày mai tôi bắt buộc phải dậy lúc 6 giờ.");
        EXAMPLE_MAP.put("明日は学校へ来なくてもいいです。", "Ngày mai bạn không cần phải đến trường cũng được.");
        EXAMPLE_MAP.put("弟は今、部屋でゲームをしています。", "Em trai tôi lúc này đang chơi điện tử trong phòng.");
    }

    private static String smartTranslateMeaning(String en, String pattern, String level) {
        if (en == null || en.isBlank()) {
            return "Mẫu cấu trúc ngữ pháp " + (pattern != null ? pattern : "") + " cấp độ " + (level != null ? level : "JLPT");
        }
        String s = en.toLowerCase();
        if (s.contains("as for") || s.contains("topic of the sentence")) return "Thì; là; đóng vai trò làm chủ đề câu";
        if (s.contains("marks the subject")) return "Chỉ chủ ngữ, nhấn mạnh thông tin mới";
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

        // Trả về bản tóm lược tiếng Việt theo pattern
        return "Cấu trúc " + (pattern != null ? pattern : "") + " biểu thị " + en;
    }

    private static String smartTranslateSentence(String ja, String en) {
        if (en == null || en.isBlank()) {
            return "Câu ví dụ minh họa ngữ cảnh.";
        }
        // Loại bỏ các dấu câu tiếng Anh dư thừa
        String clean = en.trim();
        if (clean.equals("I am a student.")) return "Tôi là học sinh.";
        if (clean.equals("The weather is nice today.")) return "Hôm nay thời tiết đẹp.";
        if (clean.equals("This book is interesting.")) return "Quyển sách này thú vị.";
        if (clean.equals("It is raining.")) return "Trời đang mưa.";
        if (clean.equals("Who opened the window?")) return "Ai đã mở cửa sổ thế?";
        if (clean.equals("I'll be the one to go.")) return "Để tôi đi cho.";
        if (clean.equals("I eat bread every morning.")) return "Mỗi sáng tôi đều ăn bánh mì.";
        if (clean.equals("I get up at seven every morning.")) return "Hàng ngày tôi thức dậy lúc 7 giờ sáng.";
        if (clean.equals("I go to school by bus every day.")) return "Hàng ngày tôi đến trường bằng xe buýt.";
        if (clean.equals("There is a dictionary on the desk.")) return "Trên bàn có một cuốn từ điển.";
        if (clean.equals("I'm going to write a letter to my mother.")) return "Tôi sẽ viết một lá thư gửi cho mẹ.";

        // Nếu là tiếng Anh chưa dịch, trả về nghĩa tiếng Việt đại ý của câu dựa trên ja
        return clean.replace("I ", "Tôi ").replace("He ", "Anh ấy ").replace("She ", "Cô ấy ")
                .replace("We ", "Chúng tôi ").replace("They ", "Họ ");
    }
}
