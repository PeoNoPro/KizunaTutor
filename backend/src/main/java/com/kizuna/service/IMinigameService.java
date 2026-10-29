package com.kizuna.service;

import com.kizuna.dto.minigame.MinigameSessionRequest;
import com.kizuna.dto.minigame.MinigameSessionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Dịch vụ Quản lý & Phân tích Kết quả Trò chơi Mưa Chữ Kanji (Kanji Rain).
 * 
 * QUY TẮC HIỂN THỊ (UI LOCALIZATION - XEM TẠI UI_LOCALIZATION_RULES.md):
 * Danh hiệu xếp hạng, thông điệp phản hồi và số liệu phân tích hiển thị cho người học
 * BẮT BUỘC sử dụng 100% Tiếng Việt tự nhiên, thân thiện, chuẩn sư phạm;
 * Tuyệt đối không để lộ mã code kỹ thuật hay thuật ngữ ngoại ngữ không cần thiết.
 */
public interface IMinigameService {

    /**
     * Lưu kết quả phân tích phiên chơi Trò chơi Mưa Chữ Kanji
     */
    MinigameSessionResponse saveGameSession(UUID userId, MinigameSessionRequest request);

    /**
     * Lấy lịch sử các phiên luyện tập gần nhất của người dùng
     */
    List<MinigameSessionResponse> getUserSessions(UUID userId);
}
