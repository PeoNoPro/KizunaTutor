package com.kizuna.features.progress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.progress.entity.ExerciseQuestionType;
import com.kizuna.features.progress.util.ExerciseQuestionEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.kizuna.features.progress.entity.ExerciseQuestionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Kiểm tra dữ liệu câu hỏi, đề cho học viên (không lộ đáp án) và chấm từng dạng câu. */
class ExerciseQuestionEngineTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExerciseQuestionEngine engine = new ExerciseQuestionEngine(mapper);
    private static final BigDecimal TWO = new BigDecimal("2");

    private JsonNode json(String s) {
        try {
            return mapper.readTree(s.replace('\'', '"'));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String valid(ExerciseQuestionType type, String data) {
        return engine.validate(type, "Đề", TWO, json(data), true, true, "Câu 1");
    }

    private ExerciseQuestionEngine.Grade grade(ExerciseQuestionType type, String data, String response) {
        return engine.grade(type, valid(type, data), TWO, json(response));
    }

    @Test
    void choiceQuestions() {
        String single = "{'options':['あ','い','う'],'correct':1}";
        assertThat(grade(SINGLE_CHOICE, single, "{'choice':1}").score()).isEqualByComparingTo("2");
        assertThat(grade(SINGLE_CHOICE, single, "{'choice':0}").correct()).isFalse();
        assertThat(grade(SINGLE_CHOICE, single, "{}").score()).isEqualByComparingTo("0");

        String multi = "{'options':['a','b','c','d'],'correct':[0,2,3]}";
        assertThat(grade(MULTI_CHOICE, multi, "{'choices':[3,0,2]}").correct()).isTrue();
        // Thiếu một đáp án đúng → sai cả câu
        assertThat(grade(MULTI_CHOICE, multi, "{'choices':[0,2]}").score()).isEqualByComparingTo("0");
        assertThat(grade(MULTI_CHOICE, multi, "{'choices':[0,1,2,3]}").correct()).isFalse();

        assertThat(grade(TRUE_FALSE, "{'correct':false}", "{'value':false}").correct()).isTrue();
        assertThat(grade(TRUE_FALSE, "{'correct':false}", "{'value':true}").correct()).isFalse();
    }

    @Test
    void fillBlankMatchesListedAnswersFirstAndSendsTheRestToReview() {
        String data = "{'text':'私は りんごを ___ 。 毎日 ___ 。','answers':[['たべます','食べます'],['Go']]}";
        // Khớp đáp án (bỏ qua độ rộng ký tự, hoa thường, khoảng trắng) → máy chấm đúng ngay
        assertThat(grade(FILL_BLANK, data, "{'blanks':['  食べます ','ｇｏ']}").correct()).isTrue();
        // Điền khác đáp án → chờ chấm (AI gợi ý, gia sư duyệt), chưa cho điểm
        ExerciseQuestionEngine.Grade other = grade(FILL_BLANK, data, "{'blanks':['食べます','went']}");
        assertThat(other.correct()).isNull();
        assertThat(other.score()).isEqualByComparingTo("0");
        // Bỏ trống hết → sai luôn, không cần chấm
        assertThat(grade(FILL_BLANK, data, "{'blanks':['','  ']}").correct()).isFalse();
        // Đáp án đầy đủ thì câu không bắt buộc chấm tay (chỉ chờ chấm khi học viên điền khác)
        assertThat(engine.needsManualGrading(FILL_BLANK, valid(FILL_BLANK, data))).isFalse();
        assertThat(engine.mayNeedReview(FILL_BLANK, valid(FILL_BLANK, data))).isTrue();
    }

    @Test
    void fillBlankAnswersAreOptional() {
        String noAnswers = valid(FILL_BLANK, "{'text':'昨日、友達 ___ 映画を見ました。 ___ 。'}");
        assertThat(engine.read(noAnswers).get("answers")).hasSize(2);
        assertThat(engine.needsManualGrading(FILL_BLANK, noAnswers)).isTrue();
        assertThat(engine.grade(FILL_BLANK, noAnswers, TWO, json("{'blanks':['と','楽しかった']}")).correct()).isNull();
        // Chỉ có đáp án cho ô 1: ô 2 vẫn chờ chấm
        String partial = valid(FILL_BLANK, "{'text':'A ___ B ___','answers':[['x'],[]]}");
        assertThat(engine.needsManualGrading(FILL_BLANK, partial)).isTrue();
        assertThat(engine.grade(FILL_BLANK, partial, TWO, json("{'blanks':['x','y']}")).correct()).isNull();
    }

    @Test
    void oldQuestionsWithoutReviewMarkKeepTheOldGrading() {
        // Câu soạn trước khi có AI (không có khóa "review"): điền khuyết sai một ô → sai cả câu; tự luận có đáp án → máy chấm
        String oldFill = "{\"text\":\"A ___\",\"answers\":[[\"x\"]]}";
        assertThat(engine.grade(FILL_BLANK, oldFill, TWO, json("{'blanks':['y']}")).correct()).isFalse();
        assertThat(engine.mayNeedReview(FILL_BLANK, oldFill)).isFalse();
        String oldWritten = "{\"grader\":\"AUTO\",\"accepted\":[\"ありがとう\"]}";
        assertThat(engine.grade(WRITTEN, oldWritten, TWO, json("{'text':' ありがとう '}")).correct()).isTrue();
        assertThat(engine.needsManualGrading(WRITTEN, oldWritten)).isFalse();
    }

    @Test
    void wordBankMatchingAndOrdering() {
        assertThat(grade(WORD_BANK, "{'text':'___ は ___ です','answers':['これ','ペン'],'distractors':['あれ']}",
                "{'blanks':['これ','ペン']}").correct()).isTrue();
        assertThat(grade(WORD_BANK, "{'text':'___ は ___ です','answers':['これ','ペン'],'distractors':['あれ']}",
                "{'blanks':['あれ','ペン']}").correct()).isFalse();

        String pairs = "{'pairs':[{'left':'犬','right':'dog'},{'left':'猫','right':'cat'},{'left':'鳥','right':'bird'}]}";
        assertThat(grade(MATCHING, pairs, "{'matches':['dog','cat','bird']}").correct()).isTrue();
        assertThat(grade(MATCHING, pairs, "{'matches':['dog','bird','cat']}").correct()).isFalse();

        String items = "{'items':['私は','学生','です']}";
        assertThat(grade(ORDERING, items, "{'order':['私は','学生','です']}").correct()).isTrue();
        assertThat(grade(ORDERING, items, "{'order':['学生','私は','です']}").correct()).isFalse();
    }

    @Test
    void writtenHasNoAnswersAndAlwaysWaitsForReview() {
        // Gia sư chỉ ra đề: đáp án / bài mẫu / người chấm gửi lên đều bị bỏ
        String w = engine.validate(WRITTEN, "Viết đoạn văn", TWO,
                json("{'accepted':['x'],'sample':'...','grader':'AUTO'}"), true, true, "Câu 1");
        assertThat(engine.read(w).has("accepted")).isFalse();
        assertThat(engine.read(w).has("sample")).isFalse();
        assertThat(engine.needsManualGrading(WRITTEN, w)).isTrue();
        assertThat(engine.grade(WRITTEN, w, TWO, json("{'text':'x'}")).correct()).isNull();
        assertThat(engine.correctAnswer(WRITTEN, w).get("accepted")).isEmpty();
    }

    @Test
    void wordBankChoosesOneSetOfWordsForAllBlanks() {
        // Câu 3 chỗ trống: mỗi phương án là bộ 3 từ cách nhau bằng /
        String data = "{'text':'私は ___ 学校 ___ 日本語 ___ 勉強します。','options':['は / で / を','が/に/を','は/を/で'],'correct':0}";
        String v = valid(WORD_BANK, data);
        assertThat(engine.read(v).get("options").get(0).asText()).isEqualTo("は/で/を");
        assertThat(grade(WORD_BANK, data, "{'choice':0}").correct()).isTrue();
        assertThat(grade(WORD_BANK, data, "{'choice':2}").correct()).isFalse();
        // Đề cho học viên có các phương án nhưng không lộ phương án đúng
        JsonNode view = engine.learnerView(WORD_BANK, v, 1L);
        assertThat(view.get("options")).hasSize(3);
        assertThat(view.has("correct")).isFalse();
        assertThat(view.get("segments")).hasSize(4);
        assertThat(engine.correctAnswer(WORD_BANK, v).get("correct").asInt()).isZero();
        assertThat(engine.mayNeedReview(WORD_BANK, v)).isFalse();
    }

    @Test
    void wordBankRules() {
        assertThatThrownBy(() -> valid(WORD_BANK, "{'text':'___ は ___','options':['a/b'],'correct':0}"))
                .hasMessageContaining("ít nhất 2 phương án");
        assertThatThrownBy(() -> valid(WORD_BANK, "{'text':'___ は ___','options':['a/b','c'],'correct':0}"))
                .hasMessageContaining("phương án B phải có đủ 2 từ, cách nhau bằng dấu /");
        assertThatThrownBy(() -> valid(WORD_BANK, "{'text':'___ は ___','options':['a/b','a / b'],'correct':0}"))
                .hasMessageContaining("trùng");
        assertThatThrownBy(() -> valid(WORD_BANK, "{'text':'___ は ___','options':['a/b','c/d']}"))
                .hasMessageContaining("chưa chọn phương án đúng");
        // Lưu nháp: chưa đủ vẫn lưu được
        assertThat(engine.validate(WORD_BANK, null, TWO, json("{'text':'___','options':['a']}"), false, true, "Câu 1")).contains("options");
        // Dạng cũ (bộ từ để kéo) vẫn chấm như trước
        assertThat(engine.grade(WORD_BANK, "{\"text\":\"___ です\",\"answers\":[\"これ\"],\"distractors\":[\"あれ\"]}",
                TWO, json("{'blanks':['これ']}")).correct()).isTrue();
    }

    @Test
    void learnerViewNeverContainsAnswers() {
        String single = valid(SINGLE_CHOICE, "{'options':['a','b'],'correct':1}");
        assertThat(engine.learnerView(SINGLE_CHOICE, single, 1L).has("correct")).isFalse();

        String fill = valid(FILL_BLANK, "{'text':'A ___ B ___ C','answers':[['x'],['y']]}");
        JsonNode v = engine.learnerView(FILL_BLANK, fill, 1L);
        assertThat(v.has("answers")).isFalse();
        assertThat(v.get("segments")).hasSize(3);

        String items = valid(ORDERING, "{'items':['1','2','3','4']}");
        JsonNode o = engine.learnerView(ORDERING, items, 42L);
        assertThat(o.get("items").toString()).isNotEqualTo("[\"1\",\"2\",\"3\",\"4\"]");
        // Cùng seed → cùng thứ tự xáo (tải lại trang vẫn giống)
        assertThat(engine.learnerView(ORDERING, items, 42L)).isEqualTo(o);

        String fillOld = "{\"text\":\"A ___\",\"answers\":[[\"bí mật\"]]}";
        assertThat(engine.learnerView(FILL_BLANK, fillOld, 1L).toString()).doesNotContain("bí mật");
    }

    @Test
    void strictValidationNamesTheQuestionAndDraftIsLenient() {
        assertThatThrownBy(() -> engine.validate(SINGLE_CHOICE, "Đề", TWO, json("{'options':['a','b']}"), true, true, "Câu 2"))
                .hasMessage("Câu 2: chưa chọn đáp án đúng.");
        assertThatThrownBy(() -> engine.validate(FILL_BLANK, null, TWO, json("{'text':'không có chỗ trống','answers':[]}"), true, true, "Câu 3"))
                .hasMessageContaining("___");
        assertThatThrownBy(() -> engine.validate(MATCHING, "Nối", TWO, json("{'pairs':[{'left':'a','right':'b'}]}"), true, true, "Câu 5"))
                .hasMessage("Câu 5: cần ít nhất 2 cặp nối.");
        assertThatThrownBy(() -> engine.validate(TRUE_FALSE, "Đề", BigDecimal.ZERO, json("{'correct':true}"), true, true, "Câu 6"))
                .hasMessageContaining("điểm");
        // Lưu nháp: chưa chọn đáp án vẫn được lưu
        assertThat(engine.validate(SINGLE_CHOICE, "", TWO, json("{'options':['a']}"), false, true, "Câu 1")).contains("options");
    }
}
