package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress_photos")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    /** Optional label supplied by the uploader (e.g. "Front", "Back", "Side", or free notes). */
    @Column(length = 200)
    private String label;

    /** Relative URL path returned by {@link com.fitcoach.service.FileStorageService#store}. */
    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    /** Date the photo was taken; defaults to today when not supplied. */
    @Column(name = "photo_date")
    private LocalDate photoDate;

    @CreatedDate
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
