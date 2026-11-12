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
    void fillBlankAcceptsAnyListedAnswerIgnoringWidthCaseAndSpaces() {
        String data = "{'text':'私は りんごを ___ 。 毎日 ___ 。','answers':[['たべます','食べます'],['Go']]}";
        assertThat(grade(FILL_BLANK, data, "{'blanks':['  食べます ','ｇｏ']}").correct()).isTrue();
        // Một chỗ trống sai → sai cả câu
        assertThat(grade(FILL_BLANK, data, "{'blanks':['食べます','went']}").correct()).isFalse();
        assertThat(grade(FILL_BLANK, data, "{'blanks':['食べます']}").correct()).isFalse();
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
    void writtenAutoGradedOnlyWhenAcceptedAnswersExist() {
        assertThat(grade(WRITTEN, "{'accepted':['ありがとう']}", "{'text':' ありがとう '}").correct()).isTrue();
        String manual = engine.validate(WRITTEN, "Viết đoạn văn", TWO, json("{'sample':'...'}"), true, false, "Câu 1");
        assertThat(engine.needsManualGrading(WRITTEN, manual)).isTrue();
        assertThat(engine.grade(WRITTEN, manual, TWO, json("{'text':'abc'}")).correct()).isNull();
        // Bài chấm tự động: câu tự luận bắt buộc có đáp án
        assertThatThrownBy(() -> engine.validate(WRITTEN, "Viết", TWO, json("{}"), true, true, "Câu 4"))
                .isInstanceOf(ClassroomException.class).hasMessageStartingWith("Câu 4: bài chấm tự động");
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

        String written = valid(WRITTEN, "{'accepted':['bí mật']}");
        assertThat(engine.learnerView(WRITTEN, written, 1L).toString()).doesNotContain("bí mật");
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
