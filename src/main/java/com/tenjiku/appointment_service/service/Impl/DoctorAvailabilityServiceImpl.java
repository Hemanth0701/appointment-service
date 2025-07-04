package com.tenjiku.appointment_service.service.Impl;

import com.tenjiku.appointment_service.dtos.*;
import com.tenjiku.appointment_service.model.DoctorAvailability;
import com.tenjiku.appointment_service.model.TimeSlot;
import com.tenjiku.appointment_service.repos.DoctorAvailabilityRepository;
import com.tenjiku.appointment_service.service.DoctorAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {

    private final DoctorAvailabilityRepository repository;

    @Override
    public DoctorAvailabilityResponseDto setAvailability(DoctorAvailabilityDto doctorAvailabilityDto) {

        if (doctorAvailabilityDto.getAvailabilityList().size() > 7) {
            throw new IllegalArgumentException("You cannot set availability for more than 7 days.");
        }

        Set<DayOfWeek> seenDays = new HashSet<>();
        for (SimpleAvailability req : doctorAvailabilityDto.getAvailabilityList()) {
            if (!seenDays.add(req.getDayOfWeek())) {
                throw new IllegalArgumentException(STR."Duplicate day entry: \{req.getDayOfWeek()}");
            }
        }

        List<DoctorAvailability> existing = repository.findByDoctorId(doctorAvailabilityDto.getDoctorId());
        repository.deleteAll(existing);

        List<DoctorAvailability> newAvailabilities = new ArrayList<>();

        for (SimpleAvailability req : doctorAvailabilityDto.getAvailabilityList() ) {
            List<TimeSlot> slots = new ArrayList<>();

            if (req.isMorning()) {
                slots.addAll(generateSlots("MORNING"));
            }
            if (req.isEvening()) {
                slots.addAll(generateSlots("EVENING"));
            }

            validateNoOverlap(slots);

            DoctorAvailability availability = DoctorAvailability.builder()
                    .doctorId(doctorAvailabilityDto.getDoctorId())
                    .dayOfWeek(req.getDayOfWeek())
                    .build();

            for (TimeSlot slot : slots) {
                slot.setAvailability(availability);
            }

            availability.setTimeSlots(slots);
            newAvailabilities.add(availability);
        }

        repository.saveAll(newAvailabilities);
        Map<DayOfWeek, List<String>> partOfDaysMap = new LinkedHashMap<>();
        for (SimpleAvailability req : doctorAvailabilityDto.getAvailabilityList()) {
            List<String> parts = new ArrayList<>();
            if (req.isMorning()) parts.add("MORNING");
            if (req.isEvening()) parts.add("EVENING");
            partOfDaysMap.put(req.getDayOfWeek(), parts);
        }

        return DoctorAvailabilityResponseDto.builder()
                .doctorId(doctorAvailabilityDto.getDoctorId())
                .message("Availability set successfully")
                .totalDays(partOfDaysMap.size())
                .partOfDaysPerDay(partOfDaysMap)
                .build();
    }

    @Override
    public List<DoctorAvailabilityFullResponseDto> getAvailability(String doctorId) {
        List<DoctorAvailability> entities = repository.findByDoctorId(doctorId);

        return entities.stream().map(availability ->
                DoctorAvailabilityFullResponseDto.builder()
                        .dayOfWeek(availability.getDayOfWeek())
                        .timeSlots(
                                availability.getTimeSlots().stream().map(slot ->
                                        TimeSlotDto.builder()
                                                .partOfDay(slot.getPartOfDay())
                                                .startTime(slot.getStartTime())
                                                .endTime(slot.getEndTime())
                                                .build()
                                ).toList()
                        )
                        .build()
        ).toList();
    }

    @Override
    public DoctorAvailabilityResponseDto updateAvailability(DoctorAvailabilityDto doctorAvailabilityDto) {
        if (doctorAvailabilityDto.getAvailabilityList().isEmpty()) {
            throw new IllegalArgumentException("At least one availability entry must be provided.");
        }

        String doctorId = doctorAvailabilityDto.getDoctorId();
        Set<DayOfWeek> updatedDays = new HashSet<>();
        Map<DayOfWeek, List<String>> partOfDaysMap = new LinkedHashMap<>();

        // Remove existing availability for provided days only
        for (SimpleAvailability req : doctorAvailabilityDto.getAvailabilityList()) {
            updatedDays.add(req.getDayOfWeek());
        }

        List<DoctorAvailability> existing = repository.findByDoctorId(doctorId);
        List<DoctorAvailability> toDelete = existing.stream()
                .filter(a -> updatedDays.contains(a.getDayOfWeek()))
                .toList();

        repository.deleteAll(toDelete);

        List<DoctorAvailability> newAvailabilities = new ArrayList<>();

        for (SimpleAvailability req : doctorAvailabilityDto.getAvailabilityList()) {
            List<TimeSlot> slots = new ArrayList<>();
            List<String> parts = new ArrayList<>();

            if (req.isMorning()) {
                slots.addAll(generateSlots("MORNING"));
                parts.add("MORNING");
            }
            if (req.isEvening()) {
                slots.addAll(generateSlots("EVENING"));
                parts.add("EVENING");
            }

            validateNoOverlap(slots);

            DoctorAvailability availability = DoctorAvailability.builder()
                    .doctorId(doctorId)
                    .dayOfWeek(req.getDayOfWeek())
                    .build();

            for (TimeSlot slot : slots) {
                slot.setAvailability(availability);
            }

            availability.setTimeSlots(slots);
            newAvailabilities.add(availability);
            partOfDaysMap.put(req.getDayOfWeek(), parts);
        }

        repository.saveAll(newAvailabilities);

        return DoctorAvailabilityResponseDto.builder()
                .doctorId(doctorId)
                .message(STR."Updated \{newAvailabilities.size()} day(s) availability.")
                .totalDays(partOfDaysMap.size())
                .partOfDaysPerDay(partOfDaysMap)
                .build();
    }

    @Override
    public void deleteAllAvailability(String doctorId) {
        List<DoctorAvailability> existing = repository.findByDoctorId(doctorId);
        if (existing.isEmpty()) {
            throw new NoSuchElementException(STR."No availability found for doctor \{doctorId}");
        }
        repository.deleteAll(existing);
    }

    @Override
    public void deleteAvailabilityForDay(String doctorId, DayOfWeek dayOfWeek) {
        List<DoctorAvailability> existing = repository.findByDoctorId(doctorId);
        Optional<DoctorAvailability> toDelete = existing.stream()
                .filter(a -> a.getDayOfWeek() == dayOfWeek)
                .findFirst();

        if (toDelete.isEmpty()) {
            throw new NoSuchElementException(STR."No availability found for \{dayOfWeek}");
        }

        repository.delete(toDelete.get());
    }

    private void validateNoOverlap(List<TimeSlot> slots) {
        slots.sort(Comparator.comparing(TimeSlot::getStartTime));
        for (int i = 0; i < slots.size() - 1; i++) {
            TimeSlot current = slots.get(i);
            TimeSlot next = slots.get(i + 1);
            if (!current.getEndTime().isBefore(next.getStartTime())) {
                throw new IllegalArgumentException(STR."Overlapping time slots detected: \{current.getStartTime()}-\{current.getEndTime()} overlaps with \{next.getStartTime()}-\{next.getEndTime()}");
            }
        }
    }

    private Collection<? extends TimeSlot> generateSlots(String partOfDay) {
        List<TimeSlot> slots = new ArrayList<>();
        if (partOfDay.equals("MORNING")) {
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(8, 30)).endTime(LocalTime.of(8, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(8, 46)).endTime(LocalTime.of(9, 0)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(9, 1)).endTime(LocalTime.of(9, 15)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(9, 16)).endTime(LocalTime.of(9, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(9, 31)).endTime(LocalTime.of(9, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 15)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(10, 16)).endTime(LocalTime.of(10, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(10, 31)).endTime(LocalTime.of(10, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(10, 45)).endTime(LocalTime.of(10, 59)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 15)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(11, 16)).endTime(LocalTime.of(11, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(11, 31)).endTime(LocalTime.of(11, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(12, 0)).endTime(LocalTime.of(12, 15)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(12, 16)).endTime(LocalTime.of(12, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(12, 31)).endTime(LocalTime.of(12, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("MORNING").startTime(LocalTime.of(12, 46)).endTime(LocalTime.of(13, 0)).build());

        } else if (partOfDay.equals("EVENING")) {
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(16, 30)).endTime(LocalTime.of(16, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(16, 46)).endTime(LocalTime.of(16, 59)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(17, 0)).endTime(LocalTime.of(17, 15)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(17, 16)).endTime(LocalTime.of(17, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(17, 31)).endTime(LocalTime.of(17, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(18, 15)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(18, 16)).endTime(LocalTime.of(18, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(18, 31)).endTime(LocalTime.of(18, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(18, 46)).endTime(LocalTime.of(18, 59)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(19, 15)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(19, 16)).endTime(LocalTime.of(19, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(19, 31)).endTime(LocalTime.of(19, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(19, 46)).endTime(LocalTime.of(19, 59)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(20, 15)).endTime(LocalTime.of(20, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(20, 31)).endTime(LocalTime.of(20, 45)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(20, 46)).endTime(LocalTime.of(20, 59)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(21, 0)).endTime(LocalTime.of(21, 15)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(21, 16)).endTime(LocalTime.of(21, 30)).build());
            slots.add(TimeSlot.builder().partOfDay("EVENING").startTime(LocalTime.of(21, 31)).endTime(LocalTime.of(21, 45)).build());
        } else {
            throw new IllegalArgumentException(STR."Invalid part of day: \{partOfDay}");
        }
        return slots;
    }

}
