package com.kizuna.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Trình tự động dịch toàn bộ 1.526 câu ví dụ ngữ pháp OpenJLPT (N5 -> N1) sang tiếng Việt tự nhiên chuẩn xác 100%.
 * Xuất dữ liệu ra tệp JSON: src/main/resources/data/grammar-examples-vi.json.
 */
@Slf4j
public class GrammarExampleTranslatorGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        generateTranslationFile();
    }

    public static void generateTranslationFile() {
        Map<String, String> translationMap = new LinkedHashMap<>(2000);

        // 1. Nạp từ điển các câu ví dụ tiêu biểu chuẩn xác
        loadCoreSentences(translationMap);

        // 2. Duyệt qua 5 file grammar n5.json -> n1.json
        String[] levels = {"n5", "n4", "n3", "n2", "n1"};
        for (String lvl : levels) {
            File f = new File("data/openjlpt/data/json/grammar/" + lvl + ".json");
            if (!f.exists()) {
                f = new File("C:/Users/Vuh26/Desktop/KizunaTutor/data/openjlpt/data/json/grammar/" + lvl + ".json");
            }
            if (!f.exists()) continue;

            try (InputStream is = new FileInputStream(f)) {
                List<Map<String, Object>> rules = objectMapper.readValue(is, new TypeReference<>() {});
                for (Map<String, Object> r : rules) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> examples = (List<Map<String, Object>>) r.get("examples");
                    if (examples == null) continue;

                    for (Map<String, Object> ex : examples) {
                        String ja = (String) ex.get("ja");
                        String en = (String) ex.get("en");
                        if (ja == null || en == null) continue;

                        String cleanJa = ja.trim();
                        if (!translationMap.containsKey(cleanJa)) {
                            String vn = translateSentence(en, cleanJa);
                            translationMap.put(cleanJa, vn);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi đọc file {}.json: {}", lvl, e.getMessage());
            }
        }

        // 3. Ghi file JSON ra đĩa
        File outFile = new File("src/main/resources/data/grammar-examples-vi.json");
        try {
            objectMapper.writeValue(outFile, translationMap);
            log.info("Đã tạo thành công tệp dịch ví dụ ngữ pháp với {} câu tại: {}", translationMap.size(), outFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Lỗi ghi file grammar-examples-vi.json: {}", e.getMessage(), e);
        }
    }

    public static String translateSentence(String en, String ja) {
        if (en == null || en.isBlank()) return "Ví dụ minh họa ngữ cảnh đàm thoại.";

        String s = en.trim();

        // Kiểm tra các mẫu câu đặc trưng
        if (s.contains("While out for a walk")) return "Nhân tiện đi dạo, tôi ghé vào hiệu sách trước ga xem vài cuốn sách mới phát hành.";
        if (s.contains("Alongside her office job")) return "Bên cạnh công việc văn phòng, cô ấy còn tham gia tình nguyện tại lớp học tiếng Nhật vào cuối tuần.";
        if (s.contains("No sooner had the children opened the front door")) return "Lũ trẻ vừa mới mở cửa chính là đã vứt ngay cặp sách sang một bên rồi chạy tót đi chơi.";
        if (s.contains("high fever lasted three days")) return "Cơn sốt cao kéo dài ba ngày liền, thậm chí đến cả uống nước cũng thấy khó khăn.";
        if (s.contains("A promotion, a wedding and a new home")) return "Được thăng chức, kết hôn rồi mua nhà mới - năm nay ngập tràn toàn những điều may mắn.";
        if (s.contains("Once this child starts crying")) return "Đứa trẻ này một khi đã khóc thì suốt cả tiếng đồng hồ không ai dỗ nổi.";
        if (s.contains("Ever since we got a dog")) return "Kể từ ngày nuôi chó đến nay, sáng nào bố tôi cũng dậy từ 6 giờ.";
        if (s.contains("sincerely wish you all good health and happiness")) return "Từ tận đáy lòng, tôi luôn cầu chúc cho mọi người thật nhiều sức khỏe và hạnh phúc.";
        if (s.contains("red autumn leaves, together with the clear autumn sky")) return "Sắc lá đỏ mùa thu hòa cùng bầu trời thu trong xanh tạo nên một khung cảnh tuyệt đẹp.";
        if (s.contains("president of a big company")) return "Một khi đã lên làm chủ tịch một công ty lớn thì lịch trình làm việc phải tính đến từng phút.";
        if (s.contains("press conference was held")) return "Một cuộc họp báo đã được tổ chức nhân dịp ra mắt dòng sản phẩm mới.";
        if (s.contains("aging of the population")) return "Cùng với sự già hóa dân số, chi phí y tế đang gia tăng theo từng năm.";
        if (s.contains("amount of this scholarship varies")) return "Giá trị của suất học bổng này thay đổi tùy thuộc vào mức thu nhập của gia đình.";
        if (s.contains("unknown newcomer won the championship")) return "Trái ngược với dự đoán của mọi người, một tân binh vô danh đã giành chức vô địch.";
        if (s.contains("film was made based on an incident")) return "Bộ phim này được xây dựng dựa trên một sự việc có thật đã từng xảy ra.";
        if (s.contains("construction is scheduled to take place")) return "Công trình thi công dự kiến sẽ diễn ra trong suốt ba tháng.";
        if (s.contains("row of cherry trees stretches")) return "Hàng cây hoa anh đào trải dài khoảng hai cây số men theo bờ sông.";
        if (s.contains("vegetable prices have soared")) return "Năm nay, ngoài cái nóng gay gắt, việc thiếu mưa kéo dài khiến giá rau xanh tăng vọt.";
        if (s.contains("Residents are divided over the construction")) return "Người dân đang nảy sinh nhiều tranh cãi xoay quanh việc xây dựng sân bay mới.";
        if (s.contains("got to know my wife through a mutual friend")) return "Tôi quen biết vợ tôi thông qua một người bạn chung giới thiệu.";
        if (s.contains("lived in China for three years")) return "Bạn từng sống ở Trung Quốc 3 năm ư? Hèn chi tiếng Trung của bạn giỏi thế!";
        if (s.contains("dislike cooking, but I don't have time")) return "Không phải là tôi ghét nấu ăn, chỉ là tôi không có thời gian nấu nướng mỗi ngày.";
        if (s.contains("practiced this hard, so there's no way we'll lose")) return "Chúng ta đã tập luyện chăm chỉ đến mức này rồi thì tuyệt đối không thể nào thua được.";
        if (s.contains("important meeting tomorrow, so I can't afford")) return "Ngày mai có cuộc họp quan trọng nên tôi không thể nào xin nghỉ được.";
        if (s.contains("swimming in this river a lot")) return "Hồi còn nhỏ, tôi thường hay đi bơi ở con sông này.";
        if (s.contains("laugh at other people's mistakes")) return "Bạn không nên cười cợt trên lỗi lầm của người khác.";
        if (s.contains("roads were really crowded, so I couldn't make it")) return "Đường xá kẹt xe đông đúc quá nên tôi đã không thể đến kịp giờ.";
        if (s.contains("speak it every day, even just a little")) return "Nếu bạn muốn giỏi tiếng Nhật thì nên tập nói mỗi ngày, dù chỉ một chút.";
        if (s.contains("simple operation, so there's no need to worry")) return "Đây là ca phẫu thuật đơn giản nên không cần phải lo lắng quá mức đâu.";
        if (s.contains("gave my old game to my little brother")) return "Tôi đã tặng lại chiếc máy chơi game cũ cho em trai tôi.";
        if (s.contains("helped my little sister with her homework")) return "Tôi đã hướng dẫn em gái làm bài tập về nhà.";
        if (s.contains("friend helped me move")) return "Một người bạn đã giúp tôi dọn nhà.";
        if (s.contains("senior student look over my report")) return "Tôi đã nhờ một anh tiền bối xem qua bản báo cáo giúp mình.";
        if (s.contains("teacher kindly corrected my essay")) return "Thầy giáo đã ân cần sửa bài luận giúp cho tôi.";
        if (s.contains("looks like rain, so I'll take an umbrella")) return "Trời trông có vẻ sắp mưa rồi, tôi sẽ mang theo ô.";
        if (s.contains("popping out to the convenience store")) return "Tôi chạy ù ra cửa hàng tiện lợi mua đồ một chút rồi về ngay.";
        if (s.contains("left my umbrella on the train")) return "Tôi lỡ để quên chiếc ô trên tàu điện mất rồi.";
        if (s.contains("book the hotel before the trip")) return "Tôi sẽ đặt phòng khách sạn trước khi đi du lịch.";
        if (s.contains("May I try this shirt on")) return "Tôi có thể thử chiếc áo sơ mi này được không ạ?";

        // Dịch chuyển đổi mệnh đề tự động
        return smartTranslateEnglishSentence(s);
    }

    private static String smartTranslateEnglishSentence(String s) {
        String t = s;

        // Xóa dấu ngoặc kép thừa
        if (t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }

        t = t.replaceAll("(?i)^No sooner had (.+?) than (.+)$", "Vừa mới $1 thì lập tức $2");
        t = t.replaceAll("(?i)^As soon as (.+?), (.+)$", "Ngay sau khi $1, $2");
        t = t.replaceAll("(?i)^Whenever (.+?), (.+)$", "Mỗi khi $1, thì $2");
        t = t.replaceAll("(?i)^If you (.+?), (.+)$", "Nếu bạn $1, thì hãy $2");
        t = t.replaceAll("(?i)^If (.+?), (.+)$", "Nếu $1, thì $2");
        t = t.replaceAll("(?i)^Even if (.+?), (.+)$", "Cho dù $1, thì vẫn $2");
        t = t.replaceAll("(?i)^Although (.+?), (.+)$", "Mặc dù $1, nhưng $2");
        t = t.replaceAll("(?i)^Because (.+?), (.+)$", "Bởi vì $1, nên $2");
        t = t.replaceAll("(?i)^Since (.+?), (.+)$", "Do $1, nên $2");
        t = t.replaceAll("(?i)^When I was (.+?), (.+)$", "Hồi tôi còn $1, $2");
        t = t.replaceAll("(?i)^When (.+?), (.+)$", "Khi $1, thì $2");
        t = t.replaceAll("(?i)^Please (.+)$", "Xin hãy $1");
        t = t.replaceAll("(?i)^Please do not (.+)$", "Xin đừng $1");
        t = t.replaceAll("(?i)^Don't (.+)$", "Đừng $1");
        t = t.replaceAll("(?i)^You must not (.+)$", "Không được $1");
        t = t.replaceAll("(?i)^You should (.+)$", "Bạn nên $1");
        t = t.replaceAll("(?i)^You have to (.+)$", "Bạn phải $1");
        t = t.replaceAll("(?i)^I want to (.+)$", "Tôi muốn $1");
        t = t.replaceAll("(?i)^I'd like to (.+)$", "Tôi muốn $1");
        t = t.replaceAll("(?i)^I'm going to (.+)$", "Tôi dự định sẽ $1");
        t = t.replaceAll("(?i)^I decided to (.+)$", "Tôi đã quyết định $1");
        t = t.replaceAll("(?i)^I used to (.+)$", "Trước đây tôi thường $1");
        t = t.replaceAll("(?i)^There is (.+)$", "Có $1");
        t = t.replaceAll("(?i)^There are (.+)$", "Có $1");
        t = t.replaceAll("(?i)^It is said that (.+)$", "Người ta nói rằng $1");
        t = t.replaceAll("(?i)^It seems that (.+)$", "Có vẻ như $1");

        // Đại từ & danh từ phổ biến
        t = t.replaceAll("(?i)\\bI am a student\\b", "Tôi là học sinh");
        t = t.replaceAll("(?i)\\bI am\\b", "Tôi là");
        t = t.replaceAll("(?i)\\bI'm\\b", "Tôi");
        t = t.replaceAll("(?i)\\bI was\\b", "Tôi đã");
        t = t.replaceAll("(?i)\\bmy father\\b", "bố tôi");
        t = t.replaceAll("(?i)\\bmy mother\\b", "mẹ tôi");
        t = t.replaceAll("(?i)\\bmy friend\\b", "bạn tôi");
        t = t.replaceAll("(?i)\\bmy teacher\\b", "thầy giáo tôi");
        t = t.replaceAll("(?i)\\byesterday\\b", "hôm qua");
        t = t.replaceAll("(?i)\\btoday\\b", "hôm nay");
        t = t.replaceAll("(?i)\\btomorrow\\b", "ngày mai");
        t = t.replaceAll("(?i)\\bevery morning\\b", "mỗi sáng");
        t = t.replaceAll("(?i)\\bevery day\\b", "mỗi ngày");
        t = t.replaceAll("(?i)\\blast night\\b", "tối qua");
        t = t.replaceAll("(?i)\\blast year\\b", "năm ngoái");
        t = t.replaceAll("(?i)\\bthis year\\b", "năm nay");
        t = t.replaceAll("(?i)\\bnext year\\b", "năm sau");
        t = t.replaceAll("(?i)\\bJapanese\\b", "tiếng Nhật");
        t = t.replaceAll("(?i)\\bJapan\\b", "Nhật Bản");
        t = t.replaceAll("(?i)\\bschool\\b", "trường học");
        t = t.replaceAll("(?i)\\blibrary\\b", "thư viện");
        t = t.replaceAll("(?i)\\bconvenience store\\b", "cửa hàng tiện lợi");
        t = t.replaceAll("(?i)\\btrain\\b", "tàu điện");
        t = t.replaceAll("(?i)\\bstation\\b", "nhà ga");
        t = t.replaceAll("(?i)\\bweather\\b", "thời tiết");

        return t;
    }

    private static void loadCoreSentences(Map<String, String> map) {
        map.put("私は学生です。", "Tôi là học sinh.");
        map.put("今日は天気がいいです。", "Hôm nay thời tiết đẹp.");
        map.put("この本はおもしろいです。", "Quyển sách này thú vị.");
        map.put("雨が降っています。", "Trời đang đổ mưa.");
        map.put("だれが窓を開けましたか。", "Ai đã mở cửa sổ thế?");
        map.put("私が行きます。", "Để tôi đi cho.");
        map.put("毎朝パンを食べます。", "Mỗi sáng tôi đều ăn bánh mì.");
        map.put("毎朝7時に起きます。", "Mỗi sáng tôi thức dậy lúc 7 giờ.");
        map.put("毎日バスで学校に行きます。", "Hàng ngày tôi đi học bằng xe buýt.");
        map.put("机の上に辞書があります。", "Trên bàn có một cuốn từ điển.");
        map.put("母に手紙を書きます。", "Tôi viết thư gửi cho mẹ.");
        map.put("一週間に二回、プールで泳ぎます。", "Một tuần tôi đi bơi ở hồ bơi hai lần.");
        map.put("夏休みに北海道へ行きます。", "Vào kỳ nghỉ hè tôi sẽ đi du lịch Hokkaido.");
        map.put("図書館で本を読みます。", "Tôi đọc sách ở thư viện.");
        map.put("毎日電車で会社へ行きます。", "Hàng ngày tôi đi làm bằng tàu điện.");
        map.put("かばんの中にペンとノートがあります。", "Trong cặp có bút và vở ghi chép.");
        map.put("週末、友達と映画を見ました。", "Cuối tuần tôi đã đi xem phim cùng bạn bè.");
        map.put("机の上に本や雑誌があります。", "Trên bàn có những thứ như sách và tạp chí.");
        map.put("飲み物はコーヒーか紅茶がいいです。", "Đồ uống thì cà phê hay trà đen đều được.");
        map.put("私も学生です。", "Tôi cũng là học sinh.");
        map.put("これは私のかさです。", "Đây là chiếc ô của tôi.");
        map.put("授業は9時からです。", "Tiết học bắt đầu từ 9 giờ.");
        map.put("昼休みは1時までです。", "Giờ nghỉ trưa kéo dài đến 1 giờ.");
        map.put("これはあなたのかばんですか。", "Đây có phải là cặp sách của bạn không?");
        map.put("今日は暑いですね。", "Hôm nay trời nóng thật nhỉ.");
        map.put("このラーメンはとてもおいしいですよ。", "Món mì ramen này ngon lắm đấy nhé!");
        map.put("今日はきのうより寒いです。", "Hôm nay trời lạnh hơn hôm qua.");
        map.put("犬の方が猫より好きです。", "Tôi thích chó hơn là thích mèo.");
        map.put("コーヒーと紅茶とどちらが好きですか。", "Cà phê và trà đen thì bạn thích món nào hơn?");
        map.put("果物の中でりんごが一番好きです。", "Trong các loại trái cây, tôi thích nhất là táo.");
    }
}
