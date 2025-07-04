package com.tenjiku.appointment_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoctorAvailabilityResponseDto {
    private String doctorId;
    private String message;
    private int totalDays;
    private Map<DayOfWeek, List<String>> partOfDaysPerDay;
}
