package com.kizuna.features.classroom.dto;


import com.kizuna.core.entity.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKanjiFolderRequest {

    @Size(max = 100, message = "Tên thư mục tối đa 100 ký tự")
    private String folderName;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    private String color;

    /**
     * Danh sách kanjiIds cần thêm vào thư mục
     */
    @Builder.Default
    private List<UUID> addKanjiIds = new ArrayList<>();

    /**
     * Danh sách kanjiIds cần gỡ khỏi thư mục
     */
    @Builder.Default
    private List<UUID> removeKanjiIds = new ArrayList<>();
}
