update hosts
set wake_on_lan_enabled = false
where wake_on_lan_enabled is null;

update telegram_commands
set ENABLED = false
where ENABLED is null;

ALTER TABLE hosts
    ALTER COLUMN wake_on_lan_enabled boolean NOT NULL;
ALTER TABLE telegram_commands
    ALTER COLUMN ENABLED boolean NOT NULL;