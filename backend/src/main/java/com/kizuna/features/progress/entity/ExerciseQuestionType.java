package com.kizuna.features.progress.entity;

/**
 * Dạng câu hỏi của bài làm trực tiếp (FE-07).
 * Mọi dạng trừ WRITTEN không có đáp án sẵn đều được hệ thống chấm tự động; câu nhiều phần chỉ có điểm khi đúng hết.
 */
public enum ExerciseQuestionType {
    /** Trắc nghiệm 1 đáp án. data: {options:[..], correct:int} */
    SINGLE_CHOICE,
    /** Trắc nghiệm nhiều đáp án. data: {options:[..], correct:[int..]} */
    MULTI_CHOICE,
    /** Đúng / Sai. data: {correct:bool} */
    TRUE_FALSE,
    /** Điền khuyết (gõ chữ). data: {text:"..___..", answers:[[đáp án được chấp nhận..] mỗi chỗ trống]} */
    FILL_BLANK,
    /** Điền từ có sẵn (kéo thả / bấm chọn). data: {text:"..___..", answers:[từ đúng mỗi chỗ trống], distractors:[..]} */
    WORD_BANK,
    /** Nối cột. data: {pairs:[{left, right}..]} */
    MATCHING,
    /** Sắp xếp thứ tự. data: {items:[.. theo thứ tự đúng]} */
    ORDERING,
    /** Tự luận / trả lời tự viết. data: {accepted:[..] (rỗng = gia sư chấm), sample:"đáp án mẫu"} */
    WRITTEN
}
