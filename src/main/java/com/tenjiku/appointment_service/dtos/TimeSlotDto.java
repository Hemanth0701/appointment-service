package com.tenjiku.appointment_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeSlotDto {
    private String partOfDay; // "MORNING" or "EVENING"
    private LocalTime startTime;
    private LocalTime endTime;
}
