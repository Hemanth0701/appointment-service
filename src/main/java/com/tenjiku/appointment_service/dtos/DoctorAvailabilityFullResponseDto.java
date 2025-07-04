package com.tenjiku.appointment_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoctorAvailabilityFullResponseDto {
    private DayOfWeek dayOfWeek;
    private List<TimeSlotDto> timeSlots;
}
