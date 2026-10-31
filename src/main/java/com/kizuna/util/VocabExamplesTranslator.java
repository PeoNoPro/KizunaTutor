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

public class VocabExamplesTranslator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws Exception {
        File cacheFile = new File("src/main/resources/data/vocab-examples-vi.json");
        Map<String, String> cache = new ConcurrentHashMap<>();
        if (cacheFile.exists()) {
            Map<String, String> existing = mapper.readValue(cacheFile, new TypeReference<>() {});
            cache.putAll(existing);
            System.out.println("Loaded " + cache.size() + " existing translated examples from cache.");
        }

        // Collect all vocab examples from 5 levels
        String[] levels = {"n5", "n4", "n3", "n2", "n1"};
        Set<String> toTranslate = new LinkedHashSet<>();

        for (String lvl : levels) {
            File f = new File("data/openjlpt/data/json/vocab/" + lvl + ".json");
            if (!f.exists()) continue;
            List<Map<String, Object>> list = mapper.readValue(new FileInputStream(f), new TypeReference<>() {});
            for (Map<String, Object> map : list) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exRaw = (List<Map<String, Object>>) map.get("examples");
                if (exRaw != null && !exRaw.isEmpty()) {
                    String en = (String) exRaw.get(0).get("en");
                    if (en != null && !en.isBlank()) {
                        String clean = en.trim();
                        if (!cache.containsKey(clean)) {
                            toTranslate.add(clean);
                        }
                    }
                }
            }
        }

        System.out.println("Need to translate " + toTranslate.size() + " new vocab examples...");

        int threadCount = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger count = new AtomicInteger(0);
        int total = toTranslate.size();
        List<Future<?>> futures = new ArrayList<>();

        for (String text : toTranslate) {
            futures.add(executor.submit(() -> {
                String translated = translateGoogle(text);
                if (translated != null && !translated.isBlank()) {
                    cache.put(text, translated);
                    int c = count.incrementAndGet();
                    if (c % 500 == 0 || c == total) {
                        System.out.println("Translated vocab examples: " + c + "/" + total);
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Save cache to src and target
        mapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, cache);
        File targetCache = new File("target/classes/data/vocab-examples-vi.json");
        if (targetCache.getParentFile().exists()) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(targetCache, cache);
        }

        System.out.println("Successfully saved " + cache.size() + " translated vocab examples!");
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
                Thread.sleep(150);
            } catch (Exception e) {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
        }
        return null;
    }
}
