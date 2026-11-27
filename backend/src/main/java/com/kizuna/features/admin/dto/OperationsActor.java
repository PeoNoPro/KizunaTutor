package com.kizuna.features.admin.dto;
import java.util.UUID;
public record OperationsActor(UUID userId, String role, String fullName, String email, boolean mustChangePassword) { }
