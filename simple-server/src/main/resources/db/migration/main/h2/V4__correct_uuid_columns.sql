alter table requests alter column  message_id type varchar(255);
alter table responses alter column  message_id type varchar(255);
alter table open_close_requests alter column  message_id type varchar(255);
alter table open_close_responses alter column  message_id type varchar(255);