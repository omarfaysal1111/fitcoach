package com.fitcoach.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMeasurementLogRequest {

    @NotNull
    @PastOrPresent
    private LocalDate date;

    private Double weight;
    private Double bodyFatPercentage;
    private Double muscleMass;
    private Double waterPercentage;
    private Double chest;
    private Double waist;
    private Double arms;
    private Double hips;
    private Double thighs;
}
