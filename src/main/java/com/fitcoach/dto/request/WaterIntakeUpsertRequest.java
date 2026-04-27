package com.fitcoach.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WaterIntakeUpsertRequest {

    /**
     * Total liters recorded for the day (replaces any previous value for that date).
     */
    @NotNull(message = "liters is required")
    @DecimalMin(value = "0", inclusive = true, message = "liters must be >= 0")
    @DecimalMax(value = "50", inclusive = true, message = "liters must be <= 50")
    private Double liters;

    /**
     * Defaults to today on the server if omitted.
     */
    private LocalDate date;
}
