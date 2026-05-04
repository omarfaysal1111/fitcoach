package com.fitcoach.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaCompatibilityInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureTraineeStatusColumn();
        addColumnIfMissing("trainees", "current_streak", "INTEGER NOT NULL DEFAULT 0");
        ensureProgressPicturesColumns();
    }

    /**
     * Ensures {@code trainees.status} exists, defaults to ACTIVE, and has no NULL rows
     * (legacy DBs may have added the column nullable or pre-date the field).
     */
    private void ensureTraineeStatusColumn() {
        if (!columnExists("trainees", "status")) {
            log.warn("Missing column trainees.status detected. Applying compatibility migration.");
            jdbcTemplate.execute(
                    "ALTER TABLE trainees ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
            log.info("Applied compatibility migration: added trainees.status column.");
            return;
        }

        int backfilled = jdbcTemplate.update(
                "UPDATE trainees SET status = 'ACTIVE' WHERE status IS NULL OR TRIM(status) = ''");
        if (backfilled > 0) {
            log.info("Backfilled trainees.status to ACTIVE for {} row(s).", backfilled);
        }

        jdbcTemplate.execute("ALTER TABLE trainees ALTER COLUMN status SET DEFAULT 'ACTIVE'");
        jdbcTemplate.execute("ALTER TABLE trainees ALTER COLUMN status SET NOT NULL");
    }

    private void ensureProgressPicturesColumns() {
        if (!tableExists("progress_pictures")) {
            return;
        }

        addColumnIfMissing("progress_pictures", "date", "DATE NOT NULL DEFAULT CURRENT_DATE");
        addColumnIfMissing("progress_pictures", "front_picture_url", "TEXT");
        addColumnIfMissing("progress_pictures", "side_picture_url", "TEXT");
        addColumnIfMissing("progress_pictures", "back_picture_url", "TEXT");
        addColumnIfMissing("progress_pictures", "notes", "VARCHAR(500)");
        addColumnIfMissing("progress_pictures", "uploaded_at", "TIMESTAMP NOT NULL DEFAULT NOW()");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        log.warn("Missing column {}.{} detected. Applying compatibility migration.", tableName, columnName);
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                  AND table_name = ?
                """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }
}
