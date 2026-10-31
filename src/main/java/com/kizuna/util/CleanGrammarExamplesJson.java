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

public class CleanGrammarExamplesJson {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws Exception {
        File file = new File("src/main/resources/data/grammar-examples-vi.json");
        Map<String, String> map = mapper.readValue(new FileInputStream(file), new TypeReference<>() {});

        Map<String, String> enToVn = new HashMap<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!isViolating(e.getValue())) {
                enToVn.put(e.getKey().trim(), e.getValue().trim());
            }
        }

        Map<String, String> toTranslate = new ConcurrentHashMap<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            String val = e.getValue().trim();
            if (isViolating(val)) {
                if (enToVn.containsKey(val)) {
                    e.setValue(enToVn.get(val));
                } else {
                    toTranslate.put(e.getKey(), val);
                }
            }
        }

        System.out.println("Remaining English values to translate: " + toTranslate.size());

        if (!toTranslate.isEmpty()) {
            ExecutorService executor = Executors.newFixedThreadPool(20);
            AtomicInteger count = new AtomicInteger(0);
            List<Future<?>> futures = new ArrayList<>();

            for (Map.Entry<String, String> entry : toTranslate.entrySet()) {
                futures.add(executor.submit(() -> {
                    String translated = translateGoogle(entry.getValue());
                    if (translated != null && !translated.isBlank()) {
                        map.put(entry.getKey(), translated);
                        int c = count.incrementAndGet();
                        if (c % 100 == 0 || c == toTranslate.size()) {
                            System.out.println("Translated: " + c + "/" + toTranslate.size());
                        }
                    }
                }));
            }

            for (Future<?> f : futures) {
                f.get();
            }
            executor.shutdown();
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, map);
        File target = new File("target/classes/data/grammar-examples-vi.json");
        if (target.getParentFile().exists()) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(target, map);
        }
        System.out.println("Cleaned and saved grammar-examples-vi.json successfully!");
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
