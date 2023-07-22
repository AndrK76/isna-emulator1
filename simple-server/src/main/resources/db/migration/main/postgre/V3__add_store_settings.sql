create table stored_settings
(
    grp_name      varchar(255) not null,
    setting_name  varchar(255) not null,
    setting_value varchar(2000),
    value_type    varchar(255)
);

alter table stored_settings
    add constraint
        stored_settings_pk primary key (grp_name, setting_name);

insert into stored_settings (grp_name, setting_name, setting_value, value_type)
values ('ISNA_BVU_BA_OPEN_CLOSE', 'CheckUniqueMessageId', 'false', 'java.lang.Boolean');
insert into stored_settings (grp_name, setting_name, setting_value, value_type)
values ('ISNA_BVU_BA_OPEN_CLOSE', 'CheckUniqueReference', 'false', 'java.lang.Boolean');
insert into stored_settings (grp_name, setting_name, setting_value, value_type)
values ('ISNA_BVU_BA_OPEN_CLOSE', 'ValidateAccountState', 'false', 'java.lang.Boolean');
insert into stored_settings (grp_name, setting_name, setting_value, value_type)
values ('ISNA_BVU_BA_OPEN_CLOSE', 'ValidateOperationDate', 'false', 'java.lang.Boolean');
insert into stored_settings (grp_name, setting_name, setting_value, value_type)
values ('ISNA_BVU_BA_OPEN_CLOSE', 'RaiseTestError', 'false', 'java.lang.Boolean');

/*
INSERT INTO stored_settings(grp_name, setting_name, setting_value, value_type)
VALUES ('ISNA_BVU_BA_OPEN_CLOSE', 'TestString', '"Строка 1"', 'java.lang.String');
INSERT INTO stored_settings(grp_name, setting_name, setting_value, value_type)
VALUES ('ISNA_BVU_BA_OPEN_CLOSE', 'TestLong', '10', 'java.lang.Long');
INSERT INTO stored_settings(grp_name, setting_name, setting_value, value_type)
VALUES ('ISNA_BVU_BA_OPEN_CLOSE', 'TestDouble', '15.2', 'java.lang.Double');
INSERT INTO stored_settings(grp_name, setting_name, setting_value, value_type)
VALUES ('ISNA_BVU_BA_OPEN_CLOSE', 'TestLocalDateTime', '[2023,6,27,19,27,35]', 'java.time.LocalDateTime');
INSERT INTO stored_settings(grp_name, setting_name, setting_value, value_type)
VALUES ('ISNA_BVU_BA_OPEN_CLOSE', 'TestLocalDate', '[2023,6,27]', 'java.time.LocalDate');
*/