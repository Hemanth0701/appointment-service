package com.tenjiku.appointment_service.dtos;

import lombok.Getter;

import java.time.DayOfWeek;
@Getter
public class SimpleAvailability {
    private DayOfWeek dayOfWeek;
    private boolean morning;
    private boolean evening;
}
