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

public class GrammarNotesTranslator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws Exception {
        File cacheFile = new File("src/main/resources/data/grammar-notes-vi.json");
        Map<String, String> cache = new ConcurrentHashMap<>();
        if (cacheFile.exists()) {
            Map<String, String> existing = mapper.readValue(cacheFile, new TypeReference<>() {});
            cache.putAll(existing);
            System.out.println("Loaded " + cache.size() + " existing translated notes from cache.");
        }

        // Collect all grammar notes from 5 levels
        String[] levels = {"n5", "n4", "n3", "n2", "n1"};
        Set<String> toTranslate = new LinkedHashSet<>();

        for (String lvl : levels) {
            File f = new File("data/openjlpt/data/json/grammar/" + lvl + ".json");
            if (!f.exists()) continue;
            List<Map<String, Object>> list = mapper.readValue(new FileInputStream(f), new TypeReference<>() {});
            for (Map<String, Object> map : list) {
                String notes = (String) map.get("notes");
                if (notes != null && !notes.isBlank()) {
                    String clean = notes.trim();
                    if (!cache.containsKey(clean)) {
                        toTranslate.add(clean);
                    }
                }
            }
        }

        System.out.println("Need to translate " + toTranslate.size() + " new grammar notes...");

        ExecutorService executor = Executors.newFixedThreadPool(15);
        AtomicInteger count = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (String text : toTranslate) {
            futures.add(executor.submit(() -> {
                String translated = translateGoogle(text);
                if (translated != null && !translated.isBlank()) {
                    // Tinh chỉnh một số thuật ngữ tiếng Nhật/ngữ pháp
                    translated = polishGrammarNote(translated);
                    cache.put(text, translated);
                    int c = count.incrementAndGet();
                    if (c % 50 == 0 || c == toTranslate.size()) {
                        System.out.println("Translated grammar notes: " + c + "/" + toTranslate.size());
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Save cache
        mapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, cache);
        // Also copy to target/classes if exists
        File targetCache = new File("target/classes/data/grammar-notes-vi.json");
        if (targetCache.getParentFile().exists()) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(targetCache, cache);
        }

        System.out.println("Successfully saved " + cache.size() + " translated grammar notes!");
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
                Thread.sleep(200);
            } catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        return null;
    }

    private static String polishGrammarNote(String text) {
        return text.replace("Kinh nguyệt đến trước", "Khoảng thời gian đứng trước")
                .replace("kinh nguyệt đến trước", "khoảng thời gian đứng trước")
                .replace("giai đoạn đến trước", "khoảng thời gian đứng trước");
    }
}
