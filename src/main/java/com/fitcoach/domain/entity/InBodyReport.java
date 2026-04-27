package com.fitcoach.domain.entity;

import com.fitcoach.domain.enums.FileType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbody_reports")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InBodyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    /** Human-readable label set by the coach at upload time (e.g. "Week 4 scan"). */
    @Column(length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private FileType fileType;

    /** Relative URL path returned by {@link com.fitcoach.service.FileStorageService#store}. */
    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    /** The date the InBody measurement was taken (supplied by the uploader). */
    @Column(name = "report_date")
    private LocalDate reportDate;

    @CreatedDate
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
