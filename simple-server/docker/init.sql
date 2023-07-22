grant all ON SCHEMA public to usr;
REVOKE ALL ON SCHEMA public FROM public;
create user test with encrypted password 'test';
create schema test;
alter role test SET search_path TO test;
grant all on schema test to test;
