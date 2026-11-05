package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class VerificationProfileDto {
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String bio;
    private Integer experienceYears;
    private String highestJlptLevel;
}
