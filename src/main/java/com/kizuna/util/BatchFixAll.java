package com.kizuna.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchFixAll {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws Exception {
        translateGrammarExamples();
        translateVocabMeanings();
    }

    private static void translateGrammarExamples() throws Exception {
        System.out.println("=== 1. CHECKING GRAMMAR EXAMPLES ===");
        File file = new File("src/main/resources/data/grammar-examples-vi.json");
        Map<String, String> cache = new ConcurrentHashMap<>();
        if (file.exists()) {
            cache.putAll(mapper.readValue(new FileInputStream(file), new TypeReference<>() {}));
        }

        Set<String> toTranslate = new LinkedHashSet<>();
        String[] levels = {"n5", "n4", "n3", "n2", "n1"};
        for (String lvl : levels) {
            File f = new File("data/openjlpt/data/json/grammar/" + lvl + ".json");
            if (!f.exists()) continue;
            List<Map<String, Object>> list = mapper.readValue(new FileInputStream(f), new TypeReference<>() {});
            for (Map<String, Object> map : list) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exRaw = (List<Map<String, Object>>) map.get("examples");
                if (exRaw != null) {
                    for (Map<String, Object> ex : exRaw) {
                        String en = (String) ex.get("en");
                        if (en != null && !en.isBlank()) {
                            String clean = en.trim();
                            if (!cache.containsKey(clean)) {
                                toTranslate.add(clean);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Need to translate " + toTranslate.size() + " grammar examples...");
        translateInParallel(toTranslate, cache);

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, cache);
        File target = new File("target/classes/data/grammar-examples-vi.json");
        if (target.getParentFile().exists()) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(target, cache);
        }
        System.out.println("Saved " + cache.size() + " grammar examples!");
    }

    private static void translateVocabMeanings() throws Exception {
        System.out.println("=== 2. CHECKING VOCAB MEANINGS ===");
        File file = new File("src/main/resources/data/vocab-meanings-vi.json");
        Map<String, String> cache = new ConcurrentHashMap<>();
        if (file.exists()) {
            cache.putAll(mapper.readValue(new FileInputStream(file), new TypeReference<>() {}));
        }

        Set<String> toTranslate = new LinkedHashSet<>();
        String[] levels = {"n5", "n4", "n3", "n2", "n1"};
        for (String lvl : levels) {
            File f = new File("data/openjlpt/data/json/vocab/" + lvl + ".json");
            if (!f.exists()) continue;
            List<Map<String, Object>> list = mapper.readValue(new FileInputStream(f), new TypeReference<>() {});
            for (Map<String, Object> map : list) {
                String word = (String) map.get("word");
                String reading = (String) map.get("reading");
                @SuppressWarnings("unchecked")
                List<String> meanings = (List<String>) map.get("meanings");
                String enMeaning = (meanings != null && !meanings.isEmpty()) ? String.join("; ", meanings) : "";

                // Check if already translated by dictionary
                String currentVn = VocabularyVietnameseDictionary.translateMeaning(word, reading, enMeaning, lvl.toUpperCase(), "");
                if (isViolating(currentVn) && !enMeaning.isBlank()) {
                    if (!cache.containsKey(enMeaning.trim())) {
                        toTranslate.add(enMeaning.trim());
                    }
                }
            }
        }

        System.out.println("Need to translate " + toTranslate.size() + " untranslated vocab meanings...");
        translateInParallel(toTranslate, cache);

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, cache);
        File target = new File("target/classes/data/vocab-meanings-vi.json");
        if (target.getParentFile().exists()) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(target, cache);
        }
        System.out.println("Saved " + cache.size() + " vocab meanings!");
    }

    private static void translateInParallel(Set<String> toTranslate, Map<String, String> cache) throws Exception {
        if (toTranslate.isEmpty()) return;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger count = new AtomicInteger(0);
        int total = toTranslate.size();
        List<Future<?>> futures = new ArrayList<>();

        for (String text : toTranslate) {
            futures.add(executor.submit(() -> {
                String res = translateGoogle(text);
                if (res != null && !res.isBlank()) {
                    cache.put(text, res);
                    int c = count.incrementAndGet();
                    if (c % 200 == 0 || c == total) {
                        System.out.println("Translated: " + c + "/" + total);
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();
    }

    private static boolean isViolating(String text) {
        if (text == null || text.isBlank()) return false;
        boolean hasVietnameseDiacritic = text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ].*");
        boolean hasEnglishWord = text.matches(".*[a-zA-Z]{3,}.*");
        return !hasVietnameseDiacritic && hasEnglishWord;
    }

    private static String translateGoogle(String text) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String url = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=en&tl=vi&q="
                        + URLEncoder.encode(text, StandardCharsets.UTF_8);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() == 200) {
                    String body = resp.body().trim();
                    List<?> list = mapper.readValue(body, List.class);
                    if (!list.isEmpty()) {
                        Object first = list.get(0);
                        if (first instanceof String) {
                            return (String) first;
                        } else if (first instanceof List && !((List<?>) first).isEmpty()) {
                            return String.valueOf(((List<?>) first).get(0));
                        }
                    }
                }
                Thread.sleep(100);
            } catch (Exception e) {
                try { Thread.sleep(250); } catch (InterruptedException ignored) {}
            }
        }
        return null;
    }
}
