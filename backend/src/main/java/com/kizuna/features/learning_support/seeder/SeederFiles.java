package com.kizuna.features.learning_support.seeder;

import java.io.File;

/** Tìm tệp dữ liệu nạp sẵn trong thư mục database/ (chạy từ thư mục gốc dự án hoặc từ backend/). */
final class SeederFiles {

    private SeederFiles() {
    }

    static File find(String fileName) {
        for (String folder : new String[]{"database", "../database"}) {
            File file = new File(folder, fileName);
            if (file.isFile()) return file;
        }
        return null;
    }
}
