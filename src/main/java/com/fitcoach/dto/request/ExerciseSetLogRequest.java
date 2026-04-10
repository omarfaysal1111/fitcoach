package com.fitcoach.dto.request;

import com.fitcoach.domain.enums.SetLogOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExerciseSetLogRequest {

    @NotNull
    private SetLogOutcome outcome;

    /**
     * Required when outcome is SKIPPED or MISSED (what happened on that set).
     * Optional when COMPLETED.
     */
    private String reason;
}
