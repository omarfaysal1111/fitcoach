package com.fitcoach.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateProgressPictureRequest {

    @NotNull
    @PastOrPresent
    private LocalDate date;

    private String frontPictureUrl;
    private String sidePictureUrl;
    private String backPictureUrl;

    private String notes;
}
