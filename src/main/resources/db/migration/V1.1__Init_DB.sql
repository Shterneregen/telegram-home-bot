CREATE SEQUENCE IF NOT EXISTS feature_switcher_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS feature_switcher
(
    id      BIGINT       NOT NULL,
    enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    name    VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS host_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS host
(
    id                  BIGINT       NOT NULL,
    device_name         VARCHAR(255),
    host_interface      VARCHAR(255),
    ip                  VARCHAR(255),
    mac                 VARCHAR(255) NOT NULL,
    notes               VARCHAR(255),
    state               INTEGER,
    wake_on_lan_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT ct_unique_host_mac UNIQUE (mac)
);

CREATE SEQUENCE IF NOT EXISTS host_time_log_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS host_time_log
(
    id           BIGINT      NOT NULL,
    created_date TIMESTAMP,
    state        INTEGER,
    host_id      BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_host_time_log_host FOREIGN KEY (host_id) REFERENCES host (id)
);

CREATE SEQUENCE IF NOT EXISTS telegram_command_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS telegram_command
(
    id            BIGINT       NOT NULL,
    command       VARCHAR(255),
    command_alias VARCHAR(255),
    enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT ct_unique_telegram_command_command_alias UNIQUE (command_alias)
);

CREATE SEQUENCE IF NOT EXISTS weather_item_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS weather_item
(
    id        BIGINT       NOT NULL,
    city_id   VARCHAR(255),
    city_name VARCHAR(255),
    lat       VARCHAR(255),
    lon       VARCHAR(255),
    PRIMARY KEY (id)
);
