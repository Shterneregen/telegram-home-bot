create table if not exists feature_switcher
(
    id      varchar(36) not null,
    enabled boolean     not null,
    name    varchar(255),
    primary key (id)
);

create table if not exists host_time_logs
(
    id           varchar(36) not null,
    created_date timestamp,
    state        integer,
    host_id      varchar(36),
    primary key (id)
);

create table if not exists hosts
(
    id                  varchar(36)  not null,
    device_name         varchar(255),
    host_interface      varchar(255),
    ip                  varchar(255),
    mac                 varchar(255) not null,
    notes               varchar(255),
    state               integer,
    wake_on_lan_enabled boolean,
    primary key (id)
);

create table if not exists privilege
(
    id   bigint not null,
    name varchar(255),
    primary key (id)
);

create table if not exists role
(
    id   bigint not null,
    name varchar(255),
    primary key (id)
);

create table if not exists roles_privileges
(
    role_id      bigint not null,
    privilege_id bigint not null
);

create table if not exists telegram_commands
(
    id            varchar(36) not null,
    command       varchar(255),
    command_alias varchar(255),
    enabled       boolean,
    primary key (id)
);

create table if not exists user
(
    user_id    bigint generated by default as identity,
    email      varchar(255),
    enabled    boolean,
    first_name varchar(255),
    last_name  varchar(255),
    password   varchar(255),
    username   varchar(255) not null,
    primary key (user_id)
);

create table if not exists users_roles
(
    user_id bigint not null,
    role_id bigint not null
);

create table if not exists weather_item
(
    id        varchar(36) not null,
    city_id   varchar(255),
    city_name varchar(255),
    lat       varchar(255),
    lon       varchar(255),
    primary key (id)
);

alter table hosts
    drop constraint if exists ct_unique_hosts_mac;
alter table hosts
    add constraint if not exists ct_unique_hosts_mac unique (mac);
alter table telegram_commands
    drop constraint if exists ct_unique_telegram_commands_command_alias;
alter table telegram_commands
    add constraint if not exists ct_unique_telegram_commands_command_alias unique (command_alias);
alter table user
    drop constraint if exists ct_unique_user_username;
alter table user
    add constraint if not exists ct_unique_user_username unique (username);

create sequence if not exists hibernate_sequence start with 1 increment by 1;

alter table host_time_logs
    add constraint if not exists fk_host_time_logs_hosts foreign key (host_id) references hosts;
alter table roles_privileges
    add constraint if not exists fk_roles_privileges_privilege foreign key (privilege_id) references privilege;
alter table roles_privileges
    add constraint if not exists fk_roles_privileges_role foreign key (role_id) references role;
alter table users_roles
    add constraint if not exists fk_users_roles_role foreign key (role_id) references role;
alter table users_roles
    add constraint if not exists fk_users_roles_user foreign key (user_id) references user;