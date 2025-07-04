package com.tenjiku.appointment_service.controller;

import com.tenjiku.appointment_service.dtos.DoctorAvailabilityDto;
import com.tenjiku.appointment_service.dtos.DoctorAvailabilityFullResponseDto;
import com.tenjiku.appointment_service.dtos.DoctorAvailabilityResponseDto;
import com.tenjiku.appointment_service.service.DoctorAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DoctorAvailabilityController {
    private final DoctorAvailabilityService doctorAvailabilityService;

    @PostMapping(value = "/setAvailability")
    public ResponseEntity<?> setAvailability(@Valid @RequestBody DoctorAvailabilityDto doctorAvailabilityDto){
        DoctorAvailabilityResponseDto response = doctorAvailabilityService.setAvailability(doctorAvailabilityDto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{doctorId}")
                .buildAndExpand(response.getDoctorId())
                .toUri();

        return ResponseEntity.created(location).body(response);// if there is no any other instance
    }


    @GetMapping(value = "/{doctorId}")
    public  ResponseEntity<List<DoctorAvailabilityFullResponseDto>> getAvailability(@PathVariable String doctorId) {
        return ResponseEntity.ok( doctorAvailabilityService.getAvailability(doctorId));
    }

    @PutMapping(value ="/updateAvailability" )
    public ResponseEntity<DoctorAvailabilityResponseDto> updateAvailability(@Valid @RequestBody DoctorAvailabilityDto doctorAvailabilityDto){
        return ResponseEntity.ok(doctorAvailabilityService.updateAvailability(doctorAvailabilityDto));
    }
    @DeleteMapping("/{doctorId}")
    public ResponseEntity<String> deleteAllAvailability(@PathVariable String doctorId) {
        doctorAvailabilityService.deleteAllAvailability(doctorId);
        return ResponseEntity.ok(STR."All availability deleted for doctor \{doctorId}");
    }
    @DeleteMapping("/{doctorId}/{dayOfWeek}")
    public ResponseEntity<String> deleteAvailabilityByDay(
            @PathVariable String doctorId,
            @PathVariable DayOfWeek dayOfWeek) {
        doctorAvailabilityService.deleteAvailabilityForDay(doctorId, dayOfWeek);
        return ResponseEntity.ok(STR."Availability deleted for \{dayOfWeek}");
    }

}