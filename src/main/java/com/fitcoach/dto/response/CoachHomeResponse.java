package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CoachHomeResponse {

    private CoachProfileResponse coach;

    /**
     * Flat list of the coach's trainees. The mobile app can derive
     * "needs attention", "today's sessions" and "top performers"
     * groupings from this list for now.
     */
    private List<TraineeProfileResponse> trainees;

    /**
     * All invitations created by this coach. The mobile app can
     * filter by status == PENDING to build the "Pending Invitations"
     * section.
     */
    private List<InvitationResponse> invitations;
}

