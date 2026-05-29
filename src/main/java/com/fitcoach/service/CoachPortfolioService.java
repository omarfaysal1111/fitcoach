package com.fitcoach.service;

import com.fitcoach.domain.entity.Coach;
import com.fitcoach.domain.entity.CoachCertificate;
import com.fitcoach.domain.entity.CoachPortfolioItem;
import com.fitcoach.domain.entity.CoachTransformation;
import com.fitcoach.domain.entity.Trainee;
import com.fitcoach.dto.response.CoachCertificateResponse;
import com.fitcoach.dto.response.CoachPortfolioItemResponse;
import com.fitcoach.dto.response.CoachPortfolioResponse;
import com.fitcoach.dto.response.CoachTransformationResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.CoachCertificateRepository;
import com.fitcoach.repository.CoachPortfolioItemRepository;
import com.fitcoach.repository.CoachRepository;
import com.fitcoach.repository.CoachTransformationRepository;
import com.fitcoach.repository.TraineeRepository;
import com.fitcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CoachPortfolioService {

    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final TraineeRepository traineeRepository;
    private final CoachCertificateRepository certificateRepository;
    private final CoachTransformationRepository transformationRepository;
    private final CoachPortfolioItemRepository portfolioItemRepository;
    private final FileStorageService fileStorageService;

    // ── Get portfolio ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CoachPortfolioResponse getMyPortfolio(String coachEmail) {
        Coach coach = findCoachByEmail(coachEmail);
        return buildPortfolioResponse(coach);
    }

    @Transactional(readOnly = true)
    public CoachPortfolioResponse getPortfolioForTrainee(String traineeEmail) {
        var user = userRepository.findByEmail(traineeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Trainee trainee = traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));
        Coach coach = trainee.getCoach();
        if (coach == null) {
            throw new ResourceNotFoundException("No coach assigned to this trainee");
        }
        return buildPortfolioResponse(coach);
    }

    // ── Certificates ──────────────────────────────────────────────────────────

    @Transactional
    public CoachCertificateResponse addCertificate(String coachEmail, String title,
            String issuingOrganization, Integer issueYear, MultipartFile image) {
        Coach coach = findCoachByEmail(coachEmail);

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.store(image, "portfolio/certificates/" + coach.getId());
        }

        CoachCertificate cert = CoachCertificate.builder()
                .coach(coach)
                .title(title)
                .issuingOrganization(issuingOrganization)
                .issueYear(issueYear)
                .imageUrl(imageUrl)
                .build();

        return toCertResponse(certificateRepository.save(cert));
    }

    @Transactional
    public void deleteCertificate(String coachEmail, Long certId) {
        Coach coach = findCoachByEmail(coachEmail);
        CoachCertificate cert = certificateRepository.findById(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));
        if (!cert.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this certificate");
        }
        if (cert.getImageUrl() != null) {
            fileStorageService.delete(cert.getImageUrl());
        }
        certificateRepository.delete(cert);
    }

    // ── Transformations ───────────────────────────────────────────────────────

    @Transactional
    public CoachTransformationResponse addTransformation(String coachEmail, String clientLabel,
            String description, String statsSummary,
            MultipartFile beforePhoto, MultipartFile afterPhoto) {
        Coach coach = findCoachByEmail(coachEmail);

        String beforeUrl = null;
        if (beforePhoto != null && !beforePhoto.isEmpty()) {
            beforeUrl = fileStorageService.store(beforePhoto, "portfolio/transformations/" + coach.getId());
        }
        String afterUrl = null;
        if (afterPhoto != null && !afterPhoto.isEmpty()) {
            afterUrl = fileStorageService.store(afterPhoto, "portfolio/transformations/" + coach.getId());
        }

        CoachTransformation transformation = CoachTransformation.builder()
                .coach(coach)
                .clientLabel(clientLabel)
                .description(description)
                .statsSummary(statsSummary)
                .beforePhotoUrl(beforeUrl)
                .afterPhotoUrl(afterUrl)
                .build();

        return toTransformationResponse(transformationRepository.save(transformation));
    }

    @Transactional
    public void deleteTransformation(String coachEmail, Long transformationId) {
        Coach coach = findCoachByEmail(coachEmail);
        CoachTransformation t = transformationRepository.findById(transformationId)
                .orElseThrow(() -> new ResourceNotFoundException("Transformation not found"));
        if (!t.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this transformation");
        }
        if (t.getBeforePhotoUrl() != null) fileStorageService.delete(t.getBeforePhotoUrl());
        if (t.getAfterPhotoUrl() != null) fileStorageService.delete(t.getAfterPhotoUrl());
        transformationRepository.delete(t);
    }

    // ── Portfolio Items ────────────────────────────────────────────────────────

    @Transactional
    public CoachPortfolioItemResponse addPortfolioItem(String coachEmail, String title,
            String description, MultipartFile image) {
        Coach coach = findCoachByEmail(coachEmail);

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.store(image, "portfolio/items/" + coach.getId());
        }

        CoachPortfolioItem item = CoachPortfolioItem.builder()
                .coach(coach)
                .title(title)
                .description(description)
                .imageUrl(imageUrl)
                .build();

        return toItemResponse(portfolioItemRepository.save(item));
    }

    @Transactional
    public void deletePortfolioItem(String coachEmail, Long itemId) {
        Coach coach = findCoachByEmail(coachEmail);
        CoachPortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found"));
        if (!item.getCoach().getId().equals(coach.getId())) {
            throw new IllegalArgumentException("Not authorized to delete this portfolio item");
        }
        if (item.getImageUrl() != null) fileStorageService.delete(item.getImageUrl());
        portfolioItemRepository.delete(item);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Coach findCoachByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach profile not found"));
    }

    private CoachPortfolioResponse buildPortfolioResponse(Coach coach) {
        return CoachPortfolioResponse.builder()
                .coachId(coach.getId().toString())
                .coachName(coach.getUser().getFullName())
                .specialisation(coach.getSpecialisation())
                .bio(coach.getBio())
                .certificates(certificateRepository.findByCoachIdOrderByCreatedAtDesc(coach.getId())
                        .stream().map(this::toCertResponse).toList())
                .transformations(transformationRepository.findByCoachIdOrderByCreatedAtDesc(coach.getId())
                        .stream().map(this::toTransformationResponse).toList())
                .items(portfolioItemRepository.findByCoachIdOrderByCreatedAtDesc(coach.getId())
                        .stream().map(this::toItemResponse).toList())
                .build();
    }

    private CoachCertificateResponse toCertResponse(CoachCertificate c) {
        return CoachCertificateResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .issuingOrganization(c.getIssuingOrganization())
                .issueYear(c.getIssueYear())
                .imageUrl(c.getImageUrl())
                .build();
    }

    private CoachTransformationResponse toTransformationResponse(CoachTransformation t) {
        return CoachTransformationResponse.builder()
                .id(t.getId())
                .clientLabel(t.getClientLabel())
                .description(t.getDescription())
                .statsSummary(t.getStatsSummary())
                .beforePhotoUrl(t.getBeforePhotoUrl())
                .afterPhotoUrl(t.getAfterPhotoUrl())
                .build();
    }

    private CoachPortfolioItemResponse toItemResponse(CoachPortfolioItem i) {
        return CoachPortfolioItemResponse.builder()
                .id(i.getId())
                .title(i.getTitle())
                .description(i.getDescription())
                .imageUrl(i.getImageUrl())
                .build();
    }
}
