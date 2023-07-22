alter table open_close_requests	alter column message_id drop not null;
alter table open_close_responses alter column message_id drop not null;
alter table requests add column correlation_id varchar(255);
alter table responses add column correlation_id varchar(255);

