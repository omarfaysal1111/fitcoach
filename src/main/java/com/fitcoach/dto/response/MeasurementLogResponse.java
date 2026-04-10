package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MeasurementLogResponse {

    private Long id;
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
    private LocalDateTime recordedAt;
}
