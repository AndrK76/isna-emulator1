create table open_close_requests
(
    id             bigserial   not null primary key,
    raw_request_id bigint      not null,
    message_id     UUID        not null,
    reference      varchar(50) not null,
    code_form      varchar(5)  not null,
    notify_date    TIMESTAMP   not null
);

alter table open_close_requests
    add constraint open_close_request_fk
        foreign key (raw_request_id) references requests (id)
;


create table open_close_request_accounts
(
    id           bigserial not null primary key,
    request_id   bigint    not null,
    sort         int       not null,
    account      varchar(50),
    account_type varchar(3),
    oper_type    int,
    bic          varchar(50),
    oper_date    TIMESTAMP,
    rnn          varchar(50),
    dog          varchar(50),
    dog_date     TIMESTAMP,
    bic_old      varchar(50),
    account_old  varchar(50),
    date_modify  TIMESTAMP

);

alter table open_close_request_accounts
    add constraint open_close_request_accounts_fk
        foreign key (request_id) references open_close_requests (id)
;

create index open_close_request_accounts_request_id_idx
    on open_close_request_accounts (request_id)
;

create table open_close_responses
(
    id          bigserial   not null primary key,
    request_id  bigint      not null,
    message_id  UUID        not null,
    reference   varchar(50) not null,
    code_form   varchar(5)  not null,
    notify_date TIMESTAMP   not null
);

alter table open_close_responses
    add constraint open_close_responses_fk
        foreign key (request_id) references open_close_requests (id)
;



create table open_close_response_accounts
(
    id             bigserial not null primary key,
    response_id    bigint    not null,
    sort           int       not null,
    account        varchar(50),
    result_code    varchar(50),
    result_message varchar(300),
    account_type   varchar(3),
    oper_type      int,
    bic            varchar(50),
    oper_date      TIMESTAMP,
    rnn            varchar(50),
    dog            varchar(50),
    dog_date       TIMESTAMP,
    bic_old        varchar(50),
    account_old    varchar(50),
    date_modify    TIMESTAMP
);


alter table open_close_response_accounts
    add
        constraint open_close_response_accounts_fk
            foreign key (response_id) references open_close_responses (id)
;

create index open_close_response_accounts_request_id_idx
    on open_close_response_accounts (response_id)
;
