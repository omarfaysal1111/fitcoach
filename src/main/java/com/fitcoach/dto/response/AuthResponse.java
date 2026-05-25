package com.fitcoach.dto.response;

import com.fitcoach.domain.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    /** Coach entity ID — only populated when role == COACH. Null for trainees. */
    private Long coachId;
    private String fullName;
    private String email;
    private Role role;
}
