package com.kizuna.features.progress.dto;

public record SubmissionFileContent(byte[] data, String contentType, String fileName) {
}
