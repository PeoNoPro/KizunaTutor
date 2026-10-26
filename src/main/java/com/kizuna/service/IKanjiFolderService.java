package com.kizuna.service;

import com.kizuna.dto.minigame.*;

import java.util.List;
import java.util.UUID;

/**
 * Dịch vụ Quản lý Thư mục & Bộ thẻ học tập Chữ Hán Kanji.
 * 
 * QUY TẮC HIỂN THỊ (UI LOCALIZATION - XEM TẠI UI_LOCALIZATION_RULES.md):
 * Mọi thông điệp, tên thư mục mặc định và dữ liệu trả về phục vụ người dùng
 * BẮT BUỘC sử dụng 100% Tiếng Việt tự nhiên, thân thiện, chuẩn sư phạm;
 * Tuyệt đối không để lộ mã code kỹ thuật hay thuật ngữ ngoại ngữ không cần thiết.
 */
public interface IKanjiFolderService {

    /**
     * Lấy danh sách toàn bộ thư mục bộ thẻ Kanji của người dùng kèm số lượng chữ
     */
    List<KanjiFolderSummaryDto> getUserFolders(UUID userId);

    /**
     * Lấy thông tin chi tiết thư mục kèm danh sách thẻ Kanji
     */
    KanjiFolderDetailDto getFolderDetail(UUID folderId);

    /**
     * Tạo thư mục học tập mới kèm danh sách chữ Kanji ban đầu
     */
    KanjiFolderSummaryDto createFolder(UUID userId, CreateKanjiFolderRequest request);

    /**
     * Cập nhật thông tin thư mục: đổi tên, mô tả, thêm/bớt chữ Kanji
     */
    KanjiFolderSummaryDto updateFolder(UUID folderId, UpdateKanjiFolderRequest request);

    /**
     * Xóa thư mục bộ thẻ
     */
    void deleteFolder(UUID folderId);

    /**
     * Thêm 1 chữ Kanji vào bộ thẻ
     */
    void addKanjiToFolder(UUID folderId, UUID kanjiId);

    /**
     * Nạp ngẫu nhiên các chữ Kanji theo cấp độ JLPT vào bộ thẻ (tự động loại trừ các chữ đã có)
     */
    AddRandomKanjiResponse addRandomKanjis(UUID folderId, AddRandomKanjiRequest request);

    /**
     * Xóa 1 chữ Kanji khỏi bộ thẻ
     */
    void removeKanjiFromFolder(UUID folderId, UUID kanjiId);

    /**
     * Lấy dữ liệu chữ Kanji kèm danh sách cách đọc Hiragana chuẩn hóa để nạp vào Minigame
     */
    List<KanjiPlayDataDto> getFolderPlayData(UUID folderId);
}
