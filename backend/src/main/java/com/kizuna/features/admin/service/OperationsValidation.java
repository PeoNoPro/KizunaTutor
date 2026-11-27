package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import java.util.Set;
public final class OperationsValidation {
    private OperationsValidation() { }
    public static String text(String value, int max) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > max) throw BusinessException.invalid();
        return value;
    }
    public static int page(int page, int size) {
        if (page < 0 || page > 1000000 || size < 1 || size > 50) throw BusinessException.invalid();
        return Math.multiplyExact(page, size);
    }
    public static void version(Long expected, long actual) {
        if (expected == null || expected < 0) throw BusinessException.invalid();
        if (expected != actual) throw BusinessException.conflict();
    }
    public static String member(String value, Set<String> allowed) {
        if (!allowed.contains(value == null ? "" : value)) throw BusinessException.invalid();
        return value;
    }
}
