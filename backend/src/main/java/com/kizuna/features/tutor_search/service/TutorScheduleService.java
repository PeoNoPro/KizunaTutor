package com.kizuna.features.tutor_search.service;

import com.kizuna.features.tutor_search.dto.CreateAvailabilityRequestDTO;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;

import java.util.List;
import java.util.UUID;

public interface TutorScheduleService {

    List<TutorAvailabilityDTO> getMyAvailabilities(String userEmail);

    TutorAvailabilityDTO addAvailability(String userEmail, CreateAvailabilityRequestDTO request);

    void removeAvailability(String userEmail, UUID availabilityId);

    TutorAvailabilityDTO toggleAvailability(String userEmail, UUID availabilityId, boolean active);
}

