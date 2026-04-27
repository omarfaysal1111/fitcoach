package com.fitcoach.service;

import com.fitcoach.domain.entity.*;
import com.fitcoach.domain.enums.SetLogOutcome;
import com.fitcoach.dto.request.CompleteWorkoutRequest;
import com.fitcoach.dto.request.ExerciseLogItemRequest;
import com.fitcoach.dto.request.ExerciseSetLogRequest;
import com.fitcoach.dto.request.ReviewExerciseLogRequest;
import com.fitcoach.dto.response.WorkoutLogResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseLogService {

    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;
    private final PlanSessionRepository planSessionRepository;
    private final PlanAssignmentRepository planAssignmentRepository;
    private final PlanSessionExerciseRepository planSessionExerciseRepository;
    private final TraineeWorkoutCompletionRepository workoutCompletionRepository;
    private final TraineeExerciseLogRepository exerciseLogRepository;
    private final TraineeExerciseSetLogRepository exerciseSetLogRepository;
    private final CoachRepository coachRepository;

    /**
     * Trainee completes a plan session with per-exercise logging.
     * Preferred payload: {@link ExerciseLogItemRequest#setOutcomes} with one entry per prescribed set
     * (COMPLETED, SKIPPED, or MISSED). SKIPPED and MISSED require a per-set reason.
     * Legacy: {@code actualSetsCompleted} + {@code skippedSets} with a single {@code excuse} when anything was skipped.
     * Submitting again the same day replaces exercise logs for that completion.
     */
    @Transactional
    public void completeWorkoutWithLogs(String email, UUID planSessionId, CompleteWorkoutRequest request) {
        if (request == null || request.getExerciseLogs() == null || request.getExerciseLogs().isEmpty()) {
            throw new IllegalArgumentException("exerciseLogs is required and must not be empty");
        }

        Trainee trainee = getTraineeByEmail(email);
        PlanSession session = planSessionRepository.findById(planSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan session not found"));

        if (!planAssignmentRepository.existsByPlan_IdAndTrainee_Id(session.getPlan().getId(), trainee.getId())) {
            throw new ResourceNotFoundException("Plan session not part of your assigned plans");
        }

        Set<UUID> seenExerciseIds = new HashSet<>();
        for (ExerciseLogItemRequest item : request.getExerciseLogs()) {
            if (item == null) {
                throw new IllegalArgumentException("exerciseLogs contains a null item");
            }
            if (!seenExerciseIds.add(item.getPlanSessionExerciseId())) {
                throw new IllegalArgumentException(
                        "Duplicate planSessionExerciseId in request: " + item.getPlanSessionExerciseId());
            }
            validateExerciseLogItem(item);
        }

        var today = LocalDate.now();
        var now = LocalDateTime.now();

        Optional<TraineeWorkoutCompletion> existingCompletion = workoutCompletionRepository
                .findByTrainee_IdAndPlanSession_IdAndCompletionDate(trainee.getId(), planSessionId, today);

        TraineeWorkoutCompletion completion;
        if (existingCompletion.isPresent()) {
            completion = existingCompletion.get();
            // Delete children first — bulk delete on logs alone fails if FK has no ON DELETE CASCADE.
            exerciseSetLogRepository.deleteAllByWorkoutCompletionId(completion.getId());
            exerciseLogRepository.deleteAllByWorkoutCompletionId(completion.getId());
        } else {
            completion = workoutCompletionRepository.save(
                    TraineeWorkoutCompletion.builder()
                            .trainee(trainee)
                            .planSession(session)
                            .completionDate(today)
                            .completedAt(now)
                            .build());
        }

        List<TraineeExerciseLog> logs = new ArrayList<>();
        for (ExerciseLogItemRequest item : request.getExerciseLogs()) {
            PlanSessionExercise pse = planSessionExerciseRepository.findById(item.getPlanSessionExerciseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Plan session exercise not found: " + item.getPlanSessionExerciseId()));
            if (!pse.getPlanSession().getId().equals(planSessionId)) {
                throw new IllegalArgumentException(
                        "Exercise log item does not belong to plan session " + planSessionId);
            }
            logs.add(buildExerciseLog(completion, pse, item, now));
        }

        exerciseLogRepository.saveAll(logs);
    }

    private void validateExerciseLogItem(ExerciseLogItemRequest item) {
        List<ExerciseSetLogRequest> outcomes = item.getSetOutcomes();
        if (outcomes != null && !outcomes.isEmpty()) {
            for (int i = 0; i < outcomes.size(); i++) {
                ExerciseSetLogRequest s = outcomes.get(i);
                if (s.getOutcome() == SetLogOutcome.SKIPPED || s.getOutcome() == SetLogOutcome.MISSED) {
                    if (s.getReason() == null || s.getReason().isBlank()) {
                        throw new IllegalArgumentException(
                                "A reason is required for set " + (i + 1) + " on planSessionExercise "
                                        + item.getPlanSessionExerciseId() + " when outcome is " + s.getOutcome());
                    }
                }
            }
            return;
        }
        if (item.getSkippedSets() > 0
                && (item.getExcuse() == null || item.getExcuse().isBlank())) {
            throw new IllegalArgumentException(
                    "An excuse is required for planSessionExercise " + item.getPlanSessionExerciseId()
                            + " because sets were skipped.");
        }
    }

    private TraineeExerciseLog buildExerciseLog(
            TraineeWorkoutCompletion completion,
            PlanSessionExercise pse,
            ExerciseLogItemRequest item,
            LocalDateTime now) {

        List<ExerciseSetLogRequest> outcomes = item.getSetOutcomes();
        if (outcomes != null && !outcomes.isEmpty()) {
            return buildFromSetOutcomes(completion, pse, outcomes, now);
        }
        return buildFromLegacyAggregates(completion, pse, item, now);
    }

    private TraineeExerciseLog buildFromSetOutcomes(
            TraineeWorkoutCompletion completion,
            PlanSessionExercise pse,
            List<ExerciseSetLogRequest> outcomes,
            LocalDateTime now) {

        int planned = pse.getSets();
        if (outcomes.size() != planned) {
            throw new IllegalArgumentException(
                    "planSessionExercise " + pse.getId() + " has " + planned
                            + " prescribed sets but " + outcomes.size() + " set outcomes were sent.");
        }

        int completed = 0;
        int skipped = 0;
        int missed = 0;
        for (ExerciseSetLogRequest o : outcomes) {
            switch (o.getOutcome()) {
                case COMPLETED -> completed++;
                case SKIPPED -> skipped++;
                case MISSED -> missed++;
            }
        }

        TraineeExerciseLog exerciseLog = TraineeExerciseLog.builder()
                .workoutCompletion(completion)
                .planSessionExercise(pse)
                .actualSetsCompleted(completed)
                .skippedSets(skipped)
                .missedSets(missed)
                .excuse(null)
                .isReviewedByCoach(false)
                .loggedAt(now)
                .build();

        for (int i = 0; i < outcomes.size(); i++) {
            ExerciseSetLogRequest req = outcomes.get(i);
            String reason = req.getReason() != null && !req.getReason().isBlank() ? req.getReason().trim() : null;
            TraineeExerciseSetLog row = TraineeExerciseSetLog.builder()
                    .exerciseLog(exerciseLog)
                    .setNumber(i + 1)
                    .outcome(req.getOutcome())
                    .reason(reason)
                    .weightKg(req.getWeightKg())
                    .reps(req.getReps())
                    .build();
            exerciseLog.getSetLogs().add(row);
        }
        return exerciseLog;
    }

    private TraineeExerciseLog buildFromLegacyAggregates(
            TraineeWorkoutCompletion completion,
            PlanSessionExercise pse,
            ExerciseLogItemRequest item,
            LocalDateTime now) {

        int planned = pse.getSets();
        int actual = item.getActualSetsCompleted();
        int skipped = item.getSkippedSets();
        if (actual + skipped != planned) {
            throw new IllegalArgumentException(
                    "planSessionExercise " + pse.getId() + " has " + planned
                            + " prescribed sets; actualSetsCompleted + skippedSets must equal that (got "
                            + actual + " + " + skipped + ").");
        }

        TraineeExerciseLog exerciseLog = TraineeExerciseLog.builder()
                .workoutCompletion(completion)
                .planSessionExercise(pse)
                .actualSetsCompleted(actual)
                .skippedSets(skipped)
                .missedSets(0)
                .excuse(item.getExcuse() != null ? item.getExcuse().trim() : null)
                .isReviewedByCoach(false)
                .loggedAt(now)
                .build();

        String excuse = item.getExcuse() != null ? item.getExcuse().trim() : null;
        int setNum = 1;
        for (int i = 0; i < actual; i++) {
            exerciseLog.getSetLogs().add(TraineeExerciseSetLog.builder()
                    .exerciseLog(exerciseLog)
                    .setNumber(setNum++)
                    .outcome(SetLogOutcome.COMPLETED)
                    .reason(null)
                    .build());
        }
        for (int i = 0; i < skipped; i++) {
            exerciseLog.getSetLogs().add(TraineeExerciseSetLog.builder()
                    .exerciseLog(exerciseLog)
                    .setNumber(setNum++)
                    .outcome(SetLogOutcome.SKIPPED)
                    .reason(excuse)
                    .build());
        }
        return exerciseLog;
    }

    @Transactional(readOnly = true)
    public List<WorkoutLogResponse> getWorkoutLogsForTrainee(String coachEmail, Long traineeId) {
        verifyCoachOwnsTrainee(coachEmail, traineeId);

        List<TraineeExerciseLog> allLogs =
                exerciseLogRepository.findAllByTraineeIdWithAssociations(traineeId);

        var grouped = allLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getWorkoutCompletion().getId()));

        List<WorkoutLogResponse> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            TraineeWorkoutCompletion completion = entry.getValue().get(0).getWorkoutCompletion();
            result.add(buildWorkoutLogResponse(completion, entry.getValue()));
        }
        return result;
    }

    /**
     * Full completion history for a trainee: every calendar completion of a plan session, with
     * per-exercise / per-set detail when present. Newest first.
     */
    @Transactional(readOnly = true)
    public List<WorkoutLogResponse> getWorkoutCompletionHistoryForTrainee(String coachEmail, Long traineeId) {
        verifyCoachOwnsTrainee(coachEmail, traineeId);
        List<TraineeWorkoutCompletion> completions =
                workoutCompletionRepository.findByTrainee_IdOrderByCompletionDateDescCompletedAtDesc(traineeId);
        List<TraineeExerciseLog> allLogs = exerciseLogRepository.findAllByTraineeIdWithAssociations(traineeId);
        Map<Long, List<TraineeExerciseLog>> byCompletionId = allLogs.stream()
                .collect(Collectors.groupingBy(l -> l.getWorkoutCompletion().getId()));

        List<WorkoutLogResponse> result = new ArrayList<>();
        for (TraineeWorkoutCompletion c : completions) {
            List<TraineeExerciseLog> logs = byCompletionId.getOrDefault(c.getId(), List.of());
            result.add(buildWorkoutLogResponse(c, logs));
        }
        return result;
    }

    private WorkoutLogResponse buildWorkoutLogResponse(
            TraineeWorkoutCompletion completion, List<TraineeExerciseLog> logsUnsorted) {

        PlanSession ps = completion.getPlanSession();
        WorkoutPlan plan = ps.getPlan();

        List<TraineeExerciseLog> logs = logsUnsorted.stream()
                .sorted(Comparator.comparingInt(l -> l.getPlanSessionExercise().getOrderIndex()))
                .toList();

        List<WorkoutLogResponse.ExerciseLogItem> items =
                logs.stream().map(this::toLogItem).collect(Collectors.toList());

        return WorkoutLogResponse.builder()
                .completionId(completion.getId())
                .planSessionId(ps.getId())
                .planSessionTitle(ps.getTitle())
                .workoutPlanId(plan != null ? plan.getId() : null)
                .workoutPlanTitle(plan != null ? plan.getTitle() : null)
                .dayOrder(ps.getDayOrder())
                .completionDate(completion.getCompletionDate().toString())
                .completedAt(completion.getCompletedAt())
                .hasDetailedLogs(!items.isEmpty())
                .exerciseLogs(items)
                .build();
    }

    @Transactional
    public WorkoutLogResponse.ExerciseLogItem reviewExerciseLog(
            String coachEmail, Long logId, ReviewExerciseLogRequest request) {

        TraineeExerciseLog log = exerciseLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise log not found"));

        Long traineeId = log.getWorkoutCompletion().getTrainee().getId();
        verifyCoachOwnsTrainee(coachEmail, traineeId);

        log.setCoachNotes(request.getCoachNotes());
        log.setIsReviewedByCoach(true);
        exerciseLogRepository.save(log);

        return toLogItem(log);
    }

    private WorkoutLogResponse.ExerciseLogItem toLogItem(TraineeExerciseLog log) {
        int planned = log.getPlanSessionExercise().getSets();
        List<TraineeExerciseSetLog> sortedSets = log.getSetLogs().stream()
                .sorted(Comparator.comparingInt(TraineeExerciseSetLog::getSetNumber))
                .toList();

        List<WorkoutLogResponse.SetLogDetail> details = sortedSets.stream()
                .map(sl -> WorkoutLogResponse.SetLogDetail.builder()
                        .setNumber(sl.getSetNumber())
                        .outcome(sl.getOutcome())
                        .reason(sl.getReason())
                        .weightKg(sl.getWeightKg())
                        .reps(sl.getReps())
                        .build())
                .collect(Collectors.toList());

        boolean allDone = planned > 0
                && log.getActualSetsCompleted() == planned
                && log.getSkippedSets() == 0
                && log.getMissedSets() == 0;

        return WorkoutLogResponse.ExerciseLogItem.builder()
                .logId(log.getId())
                .planSessionExerciseId(log.getPlanSessionExercise().getId())
                .exerciseName(log.getPlanSessionExercise().getExercise().getName())
                .plannedSets(planned)
                .actualSetsCompleted(log.getActualSetsCompleted())
                .skippedSets(log.getSkippedSets())
                .missedSets(log.getMissedSets())
                .allSetsCompleted(allDone)
                .excuse(log.getExcuse())
                .coachNotes(log.getCoachNotes())
                .isReviewedByCoach(log.getIsReviewedByCoach())
                .loggedAt(log.getLoggedAt())
                .setDetails(details)
                .build();
    }

    private Trainee getTraineeByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return traineeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainee profile not found"));
    }

    private void verifyCoachOwnsTrainee(String coachEmail, Long traineeId) {
        var user = userRepository.findByEmail(coachEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var coach = coachRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));
        boolean owns = coach.getTrainees().stream().anyMatch(t -> t.getId().equals(traineeId));
        if (!owns) {
            throw new ResourceNotFoundException("Trainee not found for this coach");
        }
    }
}
