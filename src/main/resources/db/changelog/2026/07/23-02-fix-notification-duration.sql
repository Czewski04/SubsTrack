-- liquibase formatted sql

-- changeset Wiktor:1784771000000-1
ALTER TABLE notification_durations
    ALTER COLUMN duration_before TYPE NUMERIC(21, 0)
    USING duration_before::NUMERIC(21, 0);
