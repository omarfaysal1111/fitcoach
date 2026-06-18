package com.fitcoach.dto.request;

import lombok.Data;

@Data
public class TraineeNoteRequest {
    private String note;
    /** Optional: NUTRITION | WORKOUT | GENERAL. Defaults to GENERAL when absent/invalid. */
    private String category;
}
