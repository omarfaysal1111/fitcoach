package com.fitcoach.dto.request;

import lombok.Data;

@Data
public class UpdateTraineeNotesRequest {
    private String coachFeedback;
    private String cautionNotes;
}
