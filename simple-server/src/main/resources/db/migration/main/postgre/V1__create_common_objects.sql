create table requests
(
    id           bigserial not null primary key,
    message_id   UUID,
    service_id   varchar(50),
    message_date TIMESTAMP WITH TIME ZONE,
    data         varchar(32000)
);


create table responses
(
    id             bigserial not null primary key,
    request_id     bigint    not null,
    message_id     UUID,
    service_id     varchar(50),
    is_success     boolean,
    response_date  TIMESTAMP WITH TIME ZONE,
    status_code    varchar(50),
    status_message varchar(300),
    data           varchar(32000)
);

