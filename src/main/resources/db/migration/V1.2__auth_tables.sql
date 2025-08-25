CREATE SEQUENCE IF NOT EXISTS privilege_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS privilege
(
    id   BIGINT       NOT NULL,
    name VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS role_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS role
(
    id   BIGINT       NOT NULL,
    name VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS roles_privileges
(
    role_id      BIGINT NOT NULL,
    privilege_id BIGINT NOT NULL,
    CONSTRAINT fk_roles_privileges_role FOREIGN KEY (role_id) REFERENCES role (id),
    CONSTRAINT fk_roles_privileges_privilege FOREIGN KEY (privilege_id) REFERENCES privilege (id)
);

CREATE SEQUENCE IF NOT EXISTS user_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS thb_user
(
    user_id    BIGINT       NOT NULL,
    email      VARCHAR(255),
    enabled    BOOLEAN,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    password   VARCHAR(255),
    username   VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT ct_unique_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS users_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES thb_user (user_id),
    CONSTRAINT fk_users_privileges_role FOREIGN KEY (role_id) REFERENCES role (id)
);
