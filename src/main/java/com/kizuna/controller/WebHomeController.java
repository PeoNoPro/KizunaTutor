package com.kizuna.controller;

import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.service.kanji.HandwritingSearchService;
import com.kizuna.util.SvgUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WebHomeController {

    private final KanjiDictionaryRepository kanjiRepository;
    private final HandwritingSearchService handwritingSearchService;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("pageTitle", "KizunaTutor - Nền Tảng Kết Nối Gia Sư Tiếng Nhật & Luyện Chữ Kanji");
        model.addAttribute("totalKanji", kanjiRepository.count());
        return "index";
    }

    @GetMapping("/kanji")
    public String kanjiPractice(
            @RequestParam(required = false, defaultValue = "愛") String charQuery,
            @RequestParam(required = false, defaultValue = "folders") String tab,
            @RequestParam(required = false) String folderId,
            Model model) {
        model.addAttribute("pageTitle", "Không Gian Học Tập Kanji - KizunaTutor");
        model.addAttribute("currentChar", charQuery);
        model.addAttribute("activeTab", tab);
        model.addAttribute("folderId", folderId);
        kanjiRepository.findByKanjiCharacter(charQuery).ifPresentOrElse(kanji -> {
            if (kanji.getSvgData() != null) {
                kanji.setSvgData(SvgUtils.cleanKanjiVgSvg(kanji.getSvgData()));
            }
            handwritingSearchService.enrichKanjiMetadata(kanji);
            model.addAttribute("kanjiData", kanji);
        }, () -> {
            handwritingSearchService.getOfflineKanji(charQuery).ifPresent(off -> {
                KanjiDictionary fallback = KanjiDictionary.builder()
                        .kanjiCharacter(charQuery)
                        .hanViet(off.han_viet())
                        .onyomi(off.onyomi())
                        .kunyomi(off.kunyomi())
                        .meaning(off.meaning())
                        .strokeCount((short) 1)
                        .build();
                model.addAttribute("kanjiData", fallback);
            });
        });
        return "kanji/practice";
    }

    @GetMapping("/tutors")
    public String tutorsSearch(Model model) {
        model.addAttribute("pageTitle", "Tìm Kiếm Gia Sư Tiếng Nhật - KizunaTutor");
        return "tutors/search";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Đăng Nhập - KizunaTutor");
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("pageTitle", "Đăng Ký Tài Khoản - KizunaTutor");
        return "auth/register";
    }
}
