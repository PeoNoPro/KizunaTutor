package com.kizuna.controller;

import com.kizuna.repository.KanjiDictionaryRepository;
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

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("pageTitle", "KizunaTutor - Nền Tảng Kết Nối Gia Sư Tiếng Nhật & Luyện Chữ Kanji");
        model.addAttribute("totalKanji", kanjiRepository.count());
        return "index";
    }

    @GetMapping("/kanji")
    public String kanjiPractice(@RequestParam(required = false, defaultValue = "愛") String charQuery, Model model) {
        model.addAttribute("pageTitle", "Luyện Viết Chữ Hán Kanji - KizunaTutor");
        model.addAttribute("currentChar", charQuery);
        kanjiRepository.findByKanjiCharacter(charQuery).ifPresent(kanji -> {
            if (kanji.getSvgData() != null) {
                kanji.setSvgData(SvgUtils.cleanKanjiVgSvg(kanji.getSvgData()));
            }
            model.addAttribute("kanjiData", kanji);
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
