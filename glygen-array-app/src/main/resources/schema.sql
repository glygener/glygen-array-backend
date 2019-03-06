drop table IF EXISTS settings;
drop table IF EXISTS verification_token;
drop table IF EXISTS graphs;
drop table IF EXISTS user_roles;
drop table IF EXISTS roles;
drop table IF EXISTS users;

DROP TABLE IF EXISTS logging_event CASCADE;
DROP TABLE IF EXISTS logging_access CASCADE;
DROP TABLE IF EXISTS web_logging_event CASCADE;
DROP TABLE IF EXISTS web_logging_access CASCADE;
DROP TABLE IF EXISTS logging_event_exception;
DROP SEQUENCE IF EXISTS error_id_seq;
DROP SEQUENCE IF EXISTS access_id_seq;
DROP SEQUENCE IF EXISTS web_error_id_seq;
DROP SEQUENCE IF EXISTS web_access_id_seq;


drop sequence IF EXISTS user_seq;
drop sequence IF EXISTS role_seq;
drop sequence IF EXISTS TOKEN_SEQ;
drop sequence IF EXISTS GRAPH_SEQ;

drop type IF EXISTS login CASCADE;
create type login AS ENUM ('LOCAL', 'GOOGLE');
CREATE CAST (CHARACTER VARYING as login) WITH INOUT AS IMPLICIT;

create table IF NOT EXISTS users (
  userId bigint not null,
  username varchar(256),
  password varchar(256),
  enabled boolean,
  firstname varchar(256),
  lastname varchar(256),
  email varchar(256),
  affiliation varchar(256),
  affiliationWebsite varchar(256),
  publicflag boolean,
  logintype login,
  primary key (userId)
);

create table IF NOT EXISTS roles (
  roleId bigint not null,
  name varchar(256) not null,
  primary key (roleId)
);

create table IF NOT EXISTS user_roles (
    userId bigint not null,
    roleId bigint not null,
    primary key (userId, roleId)
);

create table IF NOT EXISTS settings (
  name varchar(256),
  value varchar(256)
);

create table IF NOT EXISTS verification_token (
  id bigint not null,
  userid bigint not null,
  token varchar(256) not null,
  expirydate date
);

create table IF NOT EXISTS graphs (
  id bigint not null,
  userid bigint not null,
  graphuri varchar(256) not null
);

alter table verification_token 
        add constraint FK_m1eg457wh2xxe878rx5y5limo 
        foreign key (userid) 
        references users;
        
alter table graphs 
        add constraint FK_m1eg457wh2xxe878rx5y5graph
        foreign key (userid) 
        references users;
        
alter table user_roles 
        add constraint FK_kt76tbwgvgf7m5rhnrkxxvgyc 
        foreign key (roleId) 
        references roles;

alter table user_roles 
        add constraint FK_oj50qpdthexxxmvur61ggy2fb 
        foreign key (userId) 
        references users;
        
create sequence IF NOT EXISTS ROLE_SEQ start 4 increment 50;
create sequence IF NOT EXISTS USER_SEQ start 2 increment 50;
create sequence IF NOT EXISTS TOKEN_SEQ start 1;
create sequence IF NOT EXISTS GRAPH_SEQ start 1;

CREATE SEQUENCE IF NOT EXISTS error_id_seq MINVALUE 1 START 1;
CREATE SEQUENCE IF NOT EXISTS access_id_seq MINVALUE 1 START 1;
CREATE SEQUENCE IF NOT EXISTS web_error_id_seq MINVALUE 1 START 1;
CREATE SEQUENCE IF NOT EXISTS web_access_id_seq MINVALUE 1 START 1;


CREATE TABLE IF NOT EXISTS logging_event
  (
    event_id          BIGINT DEFAULT nextval('error_id_seq') PRIMARY KEY,
    timestmp         timestamp without time zone NOT NULL,
    formatted_message  TEXT NOT NULL,
    level_string      VARCHAR(254) NOT NULL,
    parameters		  bytea,
    caller_filename   VARCHAR(254) NOT NULL,
    caller_class      VARCHAR(254) NOT NULL,
    caller_method     VARCHAR(254) NOT NULL,
    caller_line       VARCHAR(254) NOT NULL,
    caller_user		  VARCHAR(254) NOT NULL
  );

CREATE TABLE IF NOT EXISTS logging_access
  (
    event_id          	BIGINT DEFAULT nextval('access_id_seq') PRIMARY KEY,
    timestmp			timestamp without time zone NOT NULL,
    request_message  	TEXT NOT NULL,
    uri					VARCHAR(254) NOT NULL,
    request_payload		TEXT,
    response_payload	TEXT,
    caller_user       	VARCHAR(254) NOT NULL
  );
  
  CREATE TABLE IF NOT EXISTS logging_event_exception
  (
    event_id         BIGINT NOT NULL,
    i                SMALLINT NOT NULL,
    trace_line       VARCHAR(254) NOT NULL,
    PRIMARY KEY(event_id, i),
    FOREIGN KEY (event_id) REFERENCES logging_event(event_id)
  );
  
  CREATE TABLE IF NOT EXISTS web_logging_access
  (
    event_id          	BIGINT DEFAULT nextval('web_access_id_seq') PRIMARY KEY,
    dates				date,
    level_string      	VARCHAR(254) NOT NULL,
    page				VARCHAR(254) NOT NULL,
    message  			TEXT NOT NULL,
    comment				TEXT,
    caller_user       	VARCHAR(254) NOT NULL
  );
  
  CREATE TABLE IF NOT EXISTS web_logging_event
  (
    event_id          	BIGINT DEFAULT nextval('web_error_id_seq') PRIMARY KEY,
    dates				date,
    level_string      	VARCHAR(254) NOT NULL,
    page				VARCHAR(254) NOT NULL,
    message  			TEXT NOT NULL,
    comment				TEXT,
    caller_user      	VARCHAR(254) NOT NULL
  );
  