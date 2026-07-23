-- liquibase formatted sql

-- changeset Wiktor:1784772000000-1
ALTER TABLE user_emails
    RENAME COLUMN "user" TO user_id;
