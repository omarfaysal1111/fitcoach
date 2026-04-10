package com.fitcoach.domain.enums;

/**
 * Per-set result when logging a prescribed exercise during session completion.
 */
public enum SetLogOutcome {
    /** Set was performed as intended (or to the trainee's best completion). */
    COMPLETED,
    /** Trainee chose to skip the set (e.g. time, equipment); a reason should be recorded. */
    SKIPPED,
    /** Set was attempted but not finished (e.g. fatigue, failure); a reason should be recorded. */
    MISSED
}
