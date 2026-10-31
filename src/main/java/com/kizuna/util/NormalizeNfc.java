package com.kizuna.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.*;

public class NormalizeNfc {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String[] files = {
                "src/main/resources/data/vocab-meanings-vi.json",
                "src/main/resources/data/vocab-examples-vi.json",
                "src/main/resources/data/grammar-notes-vi.json",
                "src/main/resources/data/grammar-examples-vi.json",
                "src/main/resources/data/kanji-dictionary-data.json"
        };

        for (String path : files) {
            File f = new File(path);
            if (!f.exists()) continue;
            if (path.contains("kanji")) {
                List<Map<String, Object>> list = mapper.readValue(new FileInputStream(f), new TypeReference<>() {});
                for (Map<String, Object> map : list) {
                    for (Map.Entry<String, Object> e : map.entrySet()) {
                        if (e.getValue() instanceof String) {
                            e.setValue(Normalizer.normalize((String) e.getValue(), Normalizer.Form.NFC));
                        }
                    }
                }
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, list);
            } else {
                Map<String, String> map = mapper.readValue(new FileInputStream(f), new TypeReference<>() {});
                Map<String, String> norm = new LinkedHashMap<>();
                for (Map.Entry<String, String> e : map.entrySet()) {
                    String k = Normalizer.normalize(e.getKey(), Normalizer.Form.NFC);
                    String v = Normalizer.normalize(e.getValue(), Normalizer.Form.NFC);
                    norm.put(k, v);
                }
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, norm);
            }
            System.out.println("Normalized NFC: " + f.getName());
        }
    }
}
