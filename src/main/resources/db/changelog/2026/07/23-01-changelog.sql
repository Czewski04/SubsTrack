-- liquibase formatted sql

-- changeset Wiktor:1784769873135-1
CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

-- changeset Wiktor:1784769873135-2
CREATE TABLE notification_durations
(
    notification_id UUID NOT NULL,
    duration_before BIGINT
);

-- changeset Wiktor:1784769873135-3
CREATE TABLE notification_sent
(
    id              UUID   NOT NULL,
    notification_id UUID   NOT NULL,
    billing_date    date   NOT NULL,
    days_before     BIGINT NOT NULL,
    sent_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_notification_sent PRIMARY KEY (id)
);

-- changeset Wiktor:1784769873135-4
CREATE TABLE notifications
(
    id              UUID NOT NULL,
    subscription_id UUID NOT NULL,
    is_active       BOOLEAN,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

-- changeset Wiktor:1784769873135-5
CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

-- changeset Wiktor:1784769873135-6
CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

-- changeset Wiktor:1784769873135-7
CREATE TABLE subscriptions
(
    id            UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    user_id       UUID         NOT NULL,
    email_id      UUID         NOT NULL,
    price         DECIMAL      NOT NULL,
    currency      VARCHAR(255) NOT NULL,
    start_date    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date      TIMESTAMP WITHOUT TIME ZONE,
    period INTEGER,
    period_type   SMALLINT,
    is_active     BOOLEAN      NOT NULL,
    include_trail BOOLEAN      NOT NULL,
    trail_length  INTEGER,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id)
);

-- changeset Wiktor:1784769873135-8
CREATE TABLE user_emails
(
    id         UUID         NOT NULL,
    email      VARCHAR(255) NOT NULL,
    "user"     UUID         NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_user_emails PRIMARY KEY (id)
);

-- changeset Wiktor:1784769873135-9
CREATE TABLE users
(
    id            UUID         NOT NULL,
    username      VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- changeset Wiktor:1784769873135-10
ALTER TABLE notification_sent
    ADD CONSTRAINT uc_d3f21dc5108113a7b276e8f1f UNIQUE (notification_id, billing_date, days_before);

-- changeset Wiktor:1784769873135-11
ALTER TABLE user_emails
    ADD CONSTRAINT uc_user_emails_email UNIQUE (email);

-- changeset Wiktor:1784769873135-12
ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

-- changeset Wiktor:1784769873135-13
ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);

-- changeset Wiktor:1784769873135-14
ALTER TABLE user_emails
    ADD CONSTRAINT FK_USER_EMAILS_ON_USER FOREIGN KEY ("user") REFERENCES users (id);

-- changeset Wiktor:1784769873135-15
ALTER TABLE notification_durations
    ADD CONSTRAINT fk_notification_durations_on_notification FOREIGN KEY (notification_id) REFERENCES notifications (id);

-- changeset Wiktor:1784769873135-16
ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);

