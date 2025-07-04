package com.tenjiku.appointment_service.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;
@Getter
public class DoctorAvailabilityDto {
    @NotBlank(message = " Doctor Id is required ")
    private String doctorId;
    @Min(1)
    private List<SimpleAvailability> availabilityList;
}
