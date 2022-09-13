--liquibase formatted sql
--changeset vitaxa:create-initial-schema
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE monitor_state AS ENUM ('suspended','active');

CREATE TYPE update_state AS ENUM ('not_started', 'in_progress', 'finished', 'error');

CREATE TYPE subscription_plan AS ENUM ('default', 'pro', 'advanced');

CREATE TYPE payment_status AS ENUM ('pending', 'success', 'canceled', 'error');

CREATE TYPE initialize_state AS ENUM ('not_started', 'in_progress', 'finished', 'error');

CREATE TABLE payment
(
    id                uuid PRIMARY KEY,
    user_id           CHARACTER VARYING NOT NULL,
    external_id       CHARACTER VARYING NOT NULL,
    amount            BIGINT            NOT NULL,
    subscription_plan subscription_plan NOT NULL,
    status            payment_status    NOT NULL,
    multiply          SMALLINT          NOT NULL DEFAULT 1
);

CREATE TABLE subscription
(
    id    BIGSERIAL PRIMARY KEY,
    name  CHARACTER VARYING UNIQUE,
    plan  subscription_plan,
    price BIGINT NOT NULL
);

INSERT INTO subscription (name, plan, price)
VALUES ('Базовый', 'default', 600000),
       ('Расширенный', 'advanced', 1000000),
       ('Продвинутый', 'pro', 1400000);

CREATE TABLE account
(
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  CHARACTER VARYING UNIQUE,
    subscription_id          BIGINT REFERENCES subscription,
    subscription_valid_until TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX account_user_id_idx ON account (user_id);

CREATE TABLE ke_account
(
    id                  uuid PRIMARY KEY,
    account_id          BIGINT            NOT NULL REFERENCES account ON DELETE CASCADE,
    external_account_id BIGINT,
    name                CHARACTER VARYING,
    email               CHARACTER VARYING,
    login               CHARACTER VARYING NOT NULL,
    password            CHARACTER VARYING NOT NULL,
    last_update         TIMESTAMP WITHOUT TIME ZONE,
    monitor_state       monitor_state     NOT NULL DEFAULT 'suspended',
    update_state        update_state      NOT NULL DEFAULT 'not_started',
    initialize_state    initialize_state  NOT NULL DEFAULT 'not_started'
);

CREATE UNIQUE INDEX ke_account_account_id_idx ON ke_account (account_id, external_account_id);

CREATE TABLE ke_account_shop
(
    id               uuid PRIMARY KEY,
    ke_account_id    uuid              NOT NULL,
    external_shop_id BIGINT            NOT NULL,
    name             CHARACTER VARYING NOT NULL,

    CONSTRAINT fk_ke_account_shop_ke_account FOREIGN KEY (ke_account_id) REFERENCES ke_account (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ke_account_shop_ke_account_id_external_shop_id_idx ON ke_account_shop (ke_account_id, external_shop_id);

CREATE TABLE ke_account_shop_item
(
    id                 uuid PRIMARY KEY,
    ke_account_id      uuid                        NOT NULL,
    ke_account_shop_id uuid                        NOT NULL,
    category_id        BIGINT                      NOT NULL,
    product_id         BIGINT                      NOT NULL,
    sku_id             BIGINT                      NOT NULL,
    name               CHARACTER VARYING           NOT NULL,
    photo_key          CHARACTER VARYING           NOT NULL,
    full_price         BIGINT,
    sell_price         BIGINT                      NOT NULL,
    barcode            BIGINT                      NOT NULL,
    product_sku        CHARACTER VARYING           NOT NULL,
    sku_title          CHARACTER VARYING           NOT NULL,
    available_amount   BIGINT                      NOT NULL,
    minimum_threshold  BIGINT,
    maximum_threshold  BIGINT,
    step               INT,
    last_update        TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT fk_ke_account_shop_item_ke_account FOREIGN KEY (ke_account_id) REFERENCES ke_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_ke_account_shop_ke_account FOREIGN KEY (ke_account_shop_id) REFERENCES ke_account_shop (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ke_account_shop_item_account_id_account_shop_id_idx ON
    ke_account_shop_item (ke_account_id, ke_account_shop_id, product_id, sku_id);

CREATE TABLE ke_account_shop_item_competitor
(
    id                      uuid PRIMARY KEY,
    ke_account_shop_item_id uuid,
    product_id              BIGINT NOT NULL,
    sku_id                  BIGINT NOT NULL,

    CONSTRAINT fk_ke_account_shop_item_competitor_ke_account_shop_item
        FOREIGN KEY (ke_account_shop_item_id) REFERENCES ke_account_shop_item (id)
);

CREATE UNIQUE INDEX ke_account_shop_item_id_product_id_sku_id_idx ON
    ke_account_shop_item_competitor (ke_account_shop_item_id, product_id, sku_id);

CREATE TABLE ke_account_shop_item_pool
(
    ke_account_shop_item_id uuid PRIMARY KEY REFERENCES ke_account_shop_item ON DELETE CASCADE,
    last_check              TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE ke_account_shop_item_price_history
(
    ke_account_shop_item_id            uuid REFERENCES ke_account_shop_item ON DELETE CASCADE,
    ke_account_shop_item_competitor_id uuid REFERENCES ke_account_shop_item_competitor ON DELETE CASCADE,
    change_time                        TIMESTAMP WITHOUT TIME ZONE,
    old_price                          BIGINT NOT NULL,
    price                              BIGINT NOT NULL,

    CONSTRAINT fk_ke_account_shop_item_price_history_item_competitor FOREIGN KEY
        (ke_account_shop_item_competitor_id) REFERENCES ke_account_shop_item_competitor (id)
);

CREATE TABLE ke_shop_item
(
    product_id           BIGINT            NOT NULL,
    sku_id               BIGINT            NOT NULL,
    category_id          BIGINT            NOT NULL,
    name                 CHARACTER VARYING NOT NULL,
    photo_key            CHARACTER VARYING NOT NULL,
    avg_hash_fingerprint CHARACTER VARYING NOT NULL,
    p_hash_fingerprint   CHARACTER VARYING NOT NULL,
    price                BIGINT            NOT NULL,
    last_update          TIMESTAMP WITHOUT TIME ZONE,
    available_amount     BIGINT            NOT NULL,
    PRIMARY KEY (product_id, sku_id)
);

CREATE INDEX ke_shop_item_avg_hash_index ON ke_shop_item (avg_hash_fingerprint);
CREATE INDEX ke_shop_item_name_idx ON ke_shop_item USING gist (name gist_trgm_ops);

--changeset vitaxa:create-qrtz-schema
DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250) NULL,
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        BOOL         NOT NULL,
    IS_NONCONCURRENT  BOOL         NOT NULL,
    IS_UPDATE_DATA    BOOL         NOT NULL,
    REQUESTS_RECOVERY BOOL         NOT NULL,
    JOB_DATA          BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS
(
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT       NULL,
    PREV_FIRE_TIME BIGINT       NULL,
    PRIORITY       INTEGER      NULL,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT       NOT NULL,
    END_TIME       BIGINT       NULL,
    CALENDAR_NAME  VARCHAR(200) NULL,
    MISFIRE_INSTR  SMALLINT     NULL,
    JOB_DATA       BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS
(
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT       NOT NULL,
    REPEAT_INTERVAL BIGINT       NOT NULL,
    TIMES_TRIGGERED BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CRON_TRIGGERS
(
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
(
    SCHED_NAME    VARCHAR(120)   NOT NULL,
    TRIGGER_NAME  VARCHAR(200)   NOT NULL,
    TRIGGER_GROUP VARCHAR(200)   NOT NULL,
    STR_PROP_1    VARCHAR(512)   NULL,
    STR_PROP_2    VARCHAR(512)   NULL,
    STR_PROP_3    VARCHAR(512)   NULL,
    INT_PROP_1    INT            NULL,
    INT_PROP_2    INT            NULL,
    LONG_PROP_1   BIGINT         NULL,
    LONG_PROP_2   BIGINT         NULL,
    DEC_PROP_1    NUMERIC(13, 4) NULL,
    DEC_PROP_2    NUMERIC(13, 4) NULL,
    BOOL_PROP_1   BOOL           NULL,
    BOOL_PROP_2   BOOL           NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_BLOB_TRIGGERS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA     BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CALENDARS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR      BYTEA        NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);


CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_FIRED_TRIGGERS
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT       NOT NULL,
    SCHED_TIME        BIGINT       NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(200) NULL,
    JOB_GROUP         VARCHAR(200) NULL,
    IS_NONCONCURRENT  BOOL         NULL,
    REQUESTS_RECOVERY BOOL         NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE QRTZ_SCHEDULER_STATE
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT       NOT NULL,
    CHECKIN_INTERVAL  BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS
(
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY
    ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP
    ON QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J
    ON QRTZ_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG
    ON QRTZ_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C
    ON QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME
    ON QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE
    ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE
    ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP
    ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);


COMMIT;


