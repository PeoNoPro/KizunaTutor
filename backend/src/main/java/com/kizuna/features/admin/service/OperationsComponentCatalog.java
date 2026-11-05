package com.kizuna.features.admin.service;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class OperationsComponentCatalog {
    public static final Set<String> CODES = Set.of("APPLICATION", "DATABASE", "AGENT_MEMORY");
    public static final Map<String,String> LABELS = Map.of("APPLICATION","Tiến trình ứng dụng","DATABASE","Cơ sở dữ liệu","AGENT_MEMORY","Bộ nhớ hỗ trợ học tập");
    public List<Map<String,String>> options() {
        return CODES.stream().sorted().map(code -> Map.of("value", code, "label", LABELS.get(code))).toList();
    }
}
