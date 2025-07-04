package com.tenjiku.appointment_service.repos;

import com.tenjiku.appointment_service.model.DoctorAvailability;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability,String> {
    List<DoctorAvailability> findByDoctorId(@NotBlank(message = " Doctor Id is required ") String doctorId);
}
