# API Contract: FE-15 — Gợi ý chấm của AI

Tiền tố chung với FE-07: `/api/classrooms/{spaceId}/exercises/{exerciseId}/attempts/{attemptId}`. Header `Authorization: Bearer <token>`. Phản hồi bọc `ClassroomApiResponse` như FE-07: `{ "success": true, "message": "...", "data": ... }`; lỗi theo `ClassroomExceptionHandler`.

## GET `.../ai-reviews`

Chỉ gia sư của lớp (lớp chỉ xem vẫn xem được).

**200**

```json
{
  "success": true,
  "message": "Tải gợi ý chấm của AI thành công.",
  "data": [
    {
      "questionId": "uuid",
      "status": "DONE",
      "statusLabel": "AI đã gợi ý",
      "suggestedScore": 2.5,
      "feedback": "Câu trả lời đúng ý chính nhưng dùng sai trợ từ に.",
      "updatedAt": "2026-11-19T09:00:00Z"
    }
  ]
}
```

- `status`: `PENDING` | `DONE` | `FAILED` (giao diện dùng `statusLabel`, không hiện mã). `PENDING` quá 10 phút trả về `FAILED`.
- `suggestedScore`, `feedback` chỉ có khi `DONE`.
- Câu chưa có dòng (chưa tới lượt xử lý) không có trong danh sách.

**Lỗi**: 401 chưa đăng nhập; 403 "Chỉ gia sư của lớp mới xem được gợi ý chấm của AI."; 404 "Không tìm thấy lượt làm bài." (lượt không thuộc bài / bài không thuộc lớp); 503 "Chức năng AI chấm bài chưa sẵn sàng. Vui lòng thử lại sau." (bảng chưa được tạo).

## POST `.../ai-reviews/retry`

Gia sư được chấm điểm lớp này (`ClassroomService.loadGradableSpace`). Lượt phải đã nộp.

**200**: `{ "success": true, "message": "Đã gửi lại cho AI chấm. Kết quả sẽ có trong ít phút.", "data": [ ...như GET... ] }`

**Lỗi**: 401; 403 / 409 theo điều kiện chấm điểm FE-07; 404; 409 "Học viên chưa nộp lượt làm bài này."; 503 như trên.

## Không đổi

`PUT .../grade` (FE-07) vẫn là cách duy nhất để lưu điểm chính thức. API của học viên không trả gợi ý AI.

## Frontend (`frontend/js/api.js`)

```js
API.exerciseAiReviews = {
  list(spaceId, exerciseId, attemptId),   // GET .../ai-reviews
  retry(spaceId, exerciseId, attemptId)   // POST .../ai-reviews/retry
};
```
