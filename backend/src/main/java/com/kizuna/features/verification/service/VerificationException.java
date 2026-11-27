package com.kizuna.features.verification.service;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * Lỗi nghiệp vụ của FE-03. Mỗi lỗi mang mã ổn định cho frontend, HTTP status
 * và thông điệp tiếng Việt hiển thị trực tiếp cho người dùng.
 */
@Getter
public class VerificationException extends RuntimeException {

    public static final String MSG_LOCKED =
            "Hồ sơ của bạn đang được thẩm định nên chưa thể chỉnh sửa. Vui lòng liên hệ bộ phận hỗ trợ nếu cần thay đổi gấp.";
    public static final String MSG_REJECTED_FINAL =
            "Hồ sơ của bạn đã bị từ chối nên không thể chỉnh sửa hoặc nộp lại. Vui lòng liên hệ bộ phận hỗ trợ nếu bạn cần trao đổi thêm.";
    public static final String MSG_ALREADY_PROCESSED =
            "Hồ sơ này vừa được xử lý bởi cán bộ khác. Vui lòng tải lại trang.";
    public static final String MSG_INCOMPLETE =
            "Không thể nộp hồ sơ. Vui lòng hoàn thiện các trường bắt buộc và tải lên đủ giấy tờ trước khi nộp.";
    public static final String MSG_INVALID_FILE =
            "Tệp không hợp lệ. Vui lòng chọn tệp PDF, PNG hoặc JPG có dung lượng tối đa 10MB.";
    public static final String MSG_VALIDATION =
            "Thông tin chưa hợp lệ. Vui lòng kiểm tra các trường được đánh dấu.";

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fields;
    private final List<String> missingDocuments;

    public VerificationException(HttpStatus status, String code, String message,
                                 Map<String, String> fields, List<String> missingDocuments) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = fields == null ? Map.of() : fields;
        this.missingDocuments = missingDocuments == null ? List.of() : missingDocuments;
    }

    private static VerificationException of(HttpStatus status, String code, String message) {
        return new VerificationException(status, code, message, null, null);
    }

    public static VerificationException unauthenticated() {
        return of(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    }

    public static VerificationException notFound() {
        return of(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy hồ sơ hoặc giấy tờ được yêu cầu.");
    }

    public static VerificationException locked() {
        return of(HttpStatus.CONFLICT, "VERIFICATION_LOCKED", MSG_LOCKED);
    }

    /**
     * Hồ sơ đã bị từ chối là quyết định cuối cùng: không chỉnh sửa, không nộp lại.
     */
    public static VerificationException rejectedFinal() {
        return of(HttpStatus.CONFLICT, "VERIFICATION_REJECTED", MSG_REJECTED_FINAL);
    }

    /** Hồ sơ đã phê duyệt đang có yêu cầu cập nhật chờ thẩm định: chưa đổi được giấy tờ / JLPT. */
    public static VerificationException updatePending() {
        return of(HttpStatus.CONFLICT, "PROFILE_UPDATE_PENDING",
                "Bạn đang có yêu cầu cập nhật hồ sơ chờ thẩm định nên chưa thể thay đổi giấy tờ hoặc trình độ JLPT.");
    }

    /** Giấy tờ đã được xác minh của hồ sơ đã phê duyệt không sửa / gỡ được; chỉ tải thêm giấy tờ mới. */
    public static VerificationException verifiedDocumentLocked() {
        return of(HttpStatus.CONFLICT, "DOCUMENT_VERIFIED",
                "Giấy tờ đã được xác minh nên không thể sửa hoặc gỡ. Bạn có thể tải lên giấy tờ mới để cập nhật.");
    }

    public static VerificationException alreadyProcessed() {
        return of(HttpStatus.CONFLICT, "ALREADY_PROCESSED", MSG_ALREADY_PROCESSED);
    }

    public static VerificationException invalidFile() {
        return of(HttpStatus.BAD_REQUEST, "INVALID_FILE", MSG_INVALID_FILE);
    }

    public static VerificationException commitmentRequired() {
        return of(HttpStatus.BAD_REQUEST, "COMMITMENT_REQUIRED", "Vui lòng xác nhận cam kết trước khi nộp hồ sơ.");
    }

    public static VerificationException incomplete(Map<String, String> fields, List<String> missingDocuments) {
        return new VerificationException(HttpStatus.BAD_REQUEST, "SUBMISSION_INCOMPLETE", MSG_INCOMPLETE,
                fields, missingDocuments);
    }

    public static VerificationException validation(String message, Map<String, String> fields) {
        return new VerificationException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                message != null ? message : MSG_VALIDATION, fields, null);
    }
}
