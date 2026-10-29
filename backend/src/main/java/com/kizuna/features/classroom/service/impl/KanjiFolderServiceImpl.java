package com.kizuna.features.classroom.service.impl;


import com.kizuna.core.entity.*;
import com.kizuna.features.learning_support.dto.KanjiCardDto;
import com.kizuna.features.classroom.dto.*;
import com.kizuna.features.learning_support.dto.*;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import com.kizuna.features.classroom.entity.KanjiFolder;
import com.kizuna.features.classroom.entity.KanjiFolderItem;
import com.kizuna.core.entity.User;
import com.kizuna.features.learning_support.repository.KanjiDictionaryRepository;
import com.kizuna.features.classroom.repository.KanjiFolderItemRepository;
import com.kizuna.features.classroom.repository.KanjiFolderRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.classroom.service.IKanjiFolderService;
import com.kizuna.features.learning_support.util.KanjiReadingNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KanjiFolderServiceImpl implements IKanjiFolderService {

    private final KanjiFolderRepository folderRepository;
    private final KanjiFolderItemRepository folderItemRepository;
    private final KanjiDictionaryRepository kanjiRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<KanjiFolderSummaryDto> getUserFolders(UUID userId) {
        User user = resolveOrCreateUser(userId);
        List<KanjiFolder> folders = folderRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());

        // Bỏ cơ chế gán cứng thư mục mặc định; nếu chưa có thư mục nào thì trả về danh sách rỗng để người dùng tự tạo
        return folders.stream().map(f -> {
            List<KanjiFolderItem> folderItems = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(f.getFolderId());
            List<KanjiCardDto> cardDtos = folderItems.stream().map(item -> {
                KanjiDictionary k = item.getKanji();
                return KanjiCardDto.builder()
                        .id(k.getKanjiId())
                        .unicodeHex(k.getUnicodeHex())
                        .kanjiChar(k.getKanjiCharacter())
                        .strokeCount(k.getStrokeCount())
                        .radical(k.getRadical())
                        .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                        .onyomi(k.getOnyomi() != null ? k.getOnyomi() : "-")
                        .kunyomi(k.getKunyomi() != null ? k.getKunyomi() : "-")
                        .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                        .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                        .build();
            }).toList();

            List<String> sampleChars = cardDtos.stream().map(KanjiCardDto::getKanjiChar).toList();

            return KanjiFolderSummaryDto.builder()
                    .folderId(f.getFolderId())
                    .folderName(f.getFolderName())
                    .description(f.getDescription())
                    .color(f.getColor() != null ? f.getColor() : "#58CC02")
                    .isDefault(Boolean.TRUE.equals(f.getIsDefault()))
                    .kanjiCount((long) cardDtos.size())
                    .createdAt(f.getCreatedAt())
                    .updatedAt(f.getUpdatedAt())
                    .items(cardDtos)
                    .sampleChars(sampleChars)
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KanjiFolderDetailDto getFolderDetail(UUID folderId) {
        KanjiFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thư mục với ID: " + folderId));

        List<KanjiFolderItem> items = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(folderId);
        List<KanjiCardDto> cardDtos = items.stream().map(item -> {
            KanjiDictionary k = item.getKanji();
            return KanjiCardDto.builder()
                    .id(k.getKanjiId())
                    .unicodeHex(k.getUnicodeHex())
                    .kanjiChar(k.getKanjiCharacter())
                    .strokeCount(k.getStrokeCount())
                    .radical(k.getRadical())
                    .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                    .onyomi(k.getOnyomi() != null ? k.getOnyomi() : "-")
                    .kunyomi(k.getKunyomi() != null ? k.getKunyomi() : "-")
                    .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                    .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                    .build();
        }).toList();

        return KanjiFolderDetailDto.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .description(folder.getDescription())
                .color(folder.getColor())
                .isDefault(Boolean.TRUE.equals(folder.getIsDefault()))
                .kanjiCount((long) cardDtos.size())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .items(cardDtos)
                .build();
    }

    @Override
    @Transactional
    public KanjiFolderSummaryDto createFolder(UUID userId, CreateKanjiFolderRequest request) {
        User user = resolveOrCreateUser(userId);

        KanjiFolder folder = KanjiFolder.builder()
                .user(user)
                .folderName(request.getFolderName().trim())
                .description(request.getDescription())
                .color(request.getColor() != null ? request.getColor() : "#58CC02")
                .isDefault(false)
                .build();

        folder = folderRepository.save(folder);

        int count = 0;
        if (request.getKanjiIds() != null && !request.getKanjiIds().isEmpty()) {
            int order = 0;
            for (UUID kanjiId : request.getKanjiIds()) {
                KanjiDictionary kanji = kanjiRepository.findById(kanjiId).orElse(null);
                if (kanji != null) {
                    KanjiFolderItem item = KanjiFolderItem.builder()
                            .folder(folder)
                            .kanji(kanji)
                            .orderIndex(order++)
                            .build();
                    folderItemRepository.save(item);
                    count++;
                }
            }
        }

        return KanjiFolderSummaryDto.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .description(folder.getDescription())
                .color(folder.getColor())
                .isDefault(false)
                .kanjiCount(count)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public KanjiFolderSummaryDto updateFolder(UUID folderId, UpdateKanjiFolderRequest request) {
        KanjiFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thư mục với ID: " + folderId));

        if (request.getFolderName() != null && !request.getFolderName().isBlank()) {
            folder.setFolderName(request.getFolderName().trim());
        }
        if (request.getDescription() != null) {
            folder.setDescription(request.getDescription());
        }
        if (request.getColor() != null) {
            folder.setColor(request.getColor());
        }

        // Thêm danh sách Kanji mới (bỏ qua nếu đã tồn tại)
        if (request.getAddKanjiIds() != null && !request.getAddKanjiIds().isEmpty()) {
            int currentMaxOrder = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(folderId)
                    .stream().mapToInt(KanjiFolderItem::getOrderIndex).max().orElse(-1);

            for (UUID addId : request.getAddKanjiIds()) {
                if (!folderItemRepository.existsByFolderFolderIdAndKanjiKanjiId(folderId, addId)) {
                    KanjiDictionary k = kanjiRepository.findById(addId).orElse(null);
                    if (k != null) {
                        KanjiFolderItem item = KanjiFolderItem.builder()
                                .folder(folder)
                                .kanji(k)
                                .orderIndex(++currentMaxOrder)
                                .build();
                        folderItemRepository.save(item);
                    }
                }
            }
        }

        // Xóa danh sách Kanji
        if (request.getRemoveKanjiIds() != null && !request.getRemoveKanjiIds().isEmpty()) {
            for (UUID removeId : request.getRemoveKanjiIds()) {
                folderItemRepository.deleteByFolderIdAndKanjiId(folderId, removeId);
            }
        }

        folderRepository.save(folder);
        long count = folderRepository.countItemsByFolderId(folderId);

        return KanjiFolderSummaryDto.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .description(folder.getDescription())
                .color(folder.getColor())
                .isDefault(Boolean.TRUE.equals(folder.getIsDefault()))
                .kanjiCount(count)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteFolder(UUID folderId) {
        KanjiFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thư mục với ID: " + folderId));
        // Xóa toàn bộ các item bên trong trước để tránh lỗi khóa ngoại hoặc kẹt dữ liệu
        folderItemRepository.deleteAllByFolderId(folderId);
        folderRepository.delete(folder);
    }

    @Override
    @Transactional
    public void addKanjiToFolder(UUID folderId, UUID kanjiId) {
        KanjiFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thư mục: " + folderId));
        KanjiDictionary kanji = kanjiRepository.findById(kanjiId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chữ Kanji: " + kanjiId));

        if (!folderItemRepository.existsByFolderFolderIdAndKanjiKanjiId(folderId, kanjiId)) {
            int currentMaxOrder = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(folderId)
                    .stream().mapToInt(KanjiFolderItem::getOrderIndex).max().orElse(-1);

            KanjiFolderItem item = KanjiFolderItem.builder()
                    .folder(folder)
                    .kanji(kanji)
                    .orderIndex(currentMaxOrder + 1)
                    .build();
            folderItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void removeKanjiFromFolder(UUID folderId, UUID kanjiId) {
        folderItemRepository.deleteByFolderIdAndKanjiId(folderId, kanjiId);
    }

    @Override
    @Transactional
    public KanjiFolderSummaryDto duplicateFolder(UUID folderId) {
        KanjiFolder source = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ thẻ với ID: " + folderId));

        KanjiFolder clone = KanjiFolder.builder()
                .user(source.getUser())
                .folderName(source.getFolderName() + " (Bản sao)")
                .description(source.getDescription())
                .color(source.getColor())
                .isDefault(false)
                .build();
        clone = folderRepository.save(clone);

        List<KanjiFolderItem> sourceItems = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(folderId);
        List<KanjiCardDto> cardDtos = new ArrayList<>();
        int order = 0;
        for (KanjiFolderItem item : sourceItems) {
            KanjiFolderItem clonedItem = KanjiFolderItem.builder()
                    .folder(clone)
                    .kanji(item.getKanji())
                    .orderIndex(order++)
                    .notes(item.getNotes())
                    .build();
            folderItemRepository.save(clonedItem);
            KanjiDictionary k = item.getKanji();
            cardDtos.add(KanjiCardDto.builder()
                    .id(k.getKanjiId())
                    .unicodeHex(k.getUnicodeHex())
                    .kanjiChar(k.getKanjiCharacter())
                    .strokeCount(k.getStrokeCount())
                    .radical(k.getRadical())
                    .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                    .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                    .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                    .build());
        }

        return KanjiFolderSummaryDto.builder()
                .folderId(clone.getFolderId())
                .folderName(clone.getFolderName())
                .description(clone.getDescription())
                .color(clone.getColor())
                .isDefault(false)
                .kanjiCount((long) cardDtos.size())
                .createdAt(clone.getCreatedAt())
                .updatedAt(clone.getUpdatedAt())
                .items(cardDtos)
                .sampleChars(cardDtos.stream().map(KanjiCardDto::getKanjiChar).toList())
                .build();
    }

    @Override
    @Transactional
    public KanjiFolderSummaryDto mergeFolders(UUID sourceFolderId, UUID targetFolderId) {
        if (sourceFolderId.equals(targetFolderId)) {
            throw new IllegalArgumentException("Không thể gộp bộ thẻ vào chính nó");
        }
        KanjiFolder target = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ thẻ đích: " + targetFolderId));

        List<KanjiFolderItem> sourceItems = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(sourceFolderId);
        int currentMaxOrder = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(targetFolderId)
                .stream().mapToInt(KanjiFolderItem::getOrderIndex).max().orElse(-1);
        int nextOrder = currentMaxOrder + 1;

        for (KanjiFolderItem sItem : sourceItems) {
            UUID kanjiId = sItem.getKanji().getKanjiId();
            if (!folderItemRepository.existsByFolderFolderIdAndKanjiKanjiId(targetFolderId, kanjiId)) {
                KanjiFolderItem newItem = KanjiFolderItem.builder()
                        .folder(target)
                        .kanji(sItem.getKanji())
                        .orderIndex(nextOrder++)
                        .notes(sItem.getNotes())
                        .build();
                folderItemRepository.save(newItem);
            }
        }

        List<KanjiFolderItem> updatedItems = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(targetFolderId);
        List<KanjiCardDto> cardDtos = updatedItems.stream().map(item -> {
            KanjiDictionary k = item.getKanji();
            return KanjiCardDto.builder()
                    .id(k.getKanjiId())
                    .unicodeHex(k.getUnicodeHex())
                    .kanjiChar(k.getKanjiCharacter())
                    .strokeCount(k.getStrokeCount())
                    .radical(k.getRadical())
                    .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                    .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                    .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                    .build();
        }).toList();

        return KanjiFolderSummaryDto.builder()
                .folderId(target.getFolderId())
                .folderName(target.getFolderName())
                .description(target.getDescription())
                .color(target.getColor())
                .isDefault(Boolean.TRUE.equals(target.getIsDefault()))
                .kanjiCount((long) cardDtos.size())
                .createdAt(target.getCreatedAt())
                .updatedAt(target.getUpdatedAt())
                .items(cardDtos)
                .sampleChars(cardDtos.stream().map(KanjiCardDto::getKanjiChar).toList())
                .build();
    }

    @Override
    @Transactional
    public AddRandomKanjiResponse addRandomKanjis(UUID folderId, AddRandomKanjiRequest request) {
        KanjiFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thư mục với ID: " + folderId));

        String jlpt = (request != null && request.getJlpt() != null && !request.getJlpt().isBlank())
                ? request.getJlpt().trim().toUpperCase() : "N5";
        int quantity = (request != null && request.getQuantity() != null && request.getQuantity() > 0)
                ? Math.min(request.getQuantity(), 50) : 10;

        // Truy vấn ngẫu nhiên các chữ Kanji thuộc cấp độ JLPT đã chọn và CHƯA có trong folder này
        List<KanjiDictionary> randomKanjis = kanjiRepository.findRandomKanjisByJlptExcludingFolder(jlpt, folderId, quantity);

        if (randomKanjis.isEmpty()) {
            long currentCount = folderRepository.countItemsByFolderId(folderId);
            return AddRandomKanjiResponse.builder()
                    .success(true)
                    .addedCount(0)
                    .totalFolderCount(currentCount)
                    .message("Bạn đã thêm toàn bộ chữ cấp độ " + jlpt + " vào bộ thẻ!")
                    .addedItems(List.of())
                    .build();
        }

        int currentMaxOrder = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(folderId)
                .stream().mapToInt(KanjiFolderItem::getOrderIndex).max().orElse(-1);

        List<KanjiFolderItem> newItems = new ArrayList<>();
        List<KanjiCardDto> addedCardDtos = new ArrayList<>();

        for (KanjiDictionary k : randomKanjis) {
            KanjiFolderItem item = KanjiFolderItem.builder()
                    .folder(folder)
                    .kanji(k)
                    .orderIndex(++currentMaxOrder)
                    .build();
            newItems.add(item);

            addedCardDtos.add(KanjiCardDto.builder()
                    .id(k.getKanjiId())
                    .unicodeHex(k.getUnicodeHex())
                    .kanjiChar(k.getKanjiCharacter())
                    .strokeCount(k.getStrokeCount())
                    .radical(k.getRadical())
                    .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                    .onyomi(k.getOnyomi() != null ? k.getOnyomi() : "-")
                    .kunyomi(k.getKunyomi() != null ? k.getKunyomi() : "-")
                    .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                    .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : jlpt)
                    .build());
        }

        folderItemRepository.saveAll(newItems);
        long newTotalCount = folderRepository.countItemsByFolderId(folderId);

        return AddRandomKanjiResponse.builder()
                .success(true)
                .addedCount(newItems.size())
                .totalFolderCount(newTotalCount)
                .message("Đã nạp thêm " + newItems.size() + " chữ " + jlpt + " ngẫu nhiên vào bộ thẻ!")
                .addedItems(addedCardDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KanjiPlayDataDto> getFolderPlayData(UUID folderId) {
        List<KanjiDictionary> kanjiList;
        if (folderId != null) {
            List<KanjiFolderItem> items = folderItemRepository.findByFolderFolderIdOrderByOrderIndexAsc(folderId);
            kanjiList = items.stream().map(KanjiFolderItem::getKanji).toList();
        } else {
            kanjiList = kanjiRepository.findPlayableSampleKanjis(20);
        }

        if (kanjiList.isEmpty()) {
            kanjiList = kanjiRepository.findPlayableSampleKanjis(20);
        }

        return kanjiList.stream().map(this::mapToPlayDataDto).toList();
    }

    private KanjiPlayDataDto mapToPlayDataDto(KanjiDictionary k) {
        String kun = k.getKunyomi();
        String on = k.getOnyomi();

        List<String> readings = KanjiReadingNormalizer.extractNormalizedReadings(kun, on);
        String primary = KanjiReadingNormalizer.determinePrimaryReading(kun, on, readings);

        if (readings.isEmpty() && primary.isEmpty()) {
            readings = List.of("あ");
            primary = "あ";
        }

        return KanjiPlayDataDto.builder()
                .kanjiId(k.getKanjiId())
                .character(k.getKanjiCharacter())
                .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                .jlptLevel(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                .strokeCount(k.getStrokeCount())
                .onyomi(k.getOnyomi())
                .kunyomi(k.getKunyomi())
                .primaryReading(primary)
                .readings(readings)
                .build();
    }

    private User resolveOrCreateUser(UUID userId) {
        if (userId != null) {
            return userRepository.findById(userId).orElseGet(this::getOrCreateFallbackUser);
        }
        return getOrCreateFallbackUser();
    }

    private User getOrCreateFallbackUser() {
        return userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User guest = User.builder().build();
            return userRepository.save(guest);
        });
    }
}
