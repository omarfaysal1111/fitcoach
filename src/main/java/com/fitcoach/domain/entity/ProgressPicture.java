package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress_pictures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trainee_id")
    private Trainee trainee;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "front_picture_url")
    private String frontPictureUrl;

    @Column(name = "side_picture_url")
    private String sidePictureUrl;

    @Column(name = "back_picture_url")
    private String backPictureUrl;

    /** Optional note from trainee */
    @Column(length = 500)
    private String notes;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
