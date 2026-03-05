package com.fitcoach.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCoachRequest {

    @Size(max = 80, message = "Full name must be at most 80 characters")
    private String fullName;

    @Size(max = 120, message = "Specialisation must be at most 120 characters")
    private String specialisation;

    private String bio;
}
