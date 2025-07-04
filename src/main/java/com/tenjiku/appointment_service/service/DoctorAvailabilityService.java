package com.tenjiku.appointment_service.service;

import com.tenjiku.appointment_service.dtos.DoctorAvailabilityDto;
import com.tenjiku.appointment_service.dtos.DoctorAvailabilityFullResponseDto;
import com.tenjiku.appointment_service.dtos.DoctorAvailabilityResponseDto;
import jakarta.validation.Valid;

import java.time.DayOfWeek;
import java.util.List;

public interface DoctorAvailabilityService {
    
    DoctorAvailabilityResponseDto setAvailability(@Valid DoctorAvailabilityDto doctorAvailabilityDto);

    List<DoctorAvailabilityFullResponseDto> getAvailability(String doctorId);

    DoctorAvailabilityResponseDto updateAvailability(@Valid DoctorAvailabilityDto doctorAvailabilityDto);

    void deleteAllAvailability(String doctorId);

    void deleteAvailabilityForDay(String doctorId, DayOfWeek dayOfWeek);
}
