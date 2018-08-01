DO $$ BEGIN
    CREATE TYPE login AS ENUM ('LOCAL', 'GOOGLE');
    CREATE CAST (CHARACTER VARYING as login) WITH INOUT AS IMPLICIT;
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

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

alter table verification_token 
        add constraint FK_m1eg457wh2xxe878rx5y5limo 
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

CREATE SEQUENCE IF NOT EXISTS error_id_seq MINVALUE 1 START 1;
CREATE SEQUENCE IF NOT EXISTS access_id_seq MINVALUE 1 START 1;

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
    access_id          	BIGINT DEFAULT nextval('access_id_seq') PRIMARY KEY,
    timestmp			timestamp without time zone NOT NULL,
    request_message  	TEXT NOT NULL,
    uri					VARCHAR(254) NOT NULL,
    request_payload		bytea,
    response_payload	bytea,
    caller_user       	VARCHAR(254) NOT NULL
  );
  
  CREATE TABLE logging_event_exception
  (
    event_id         BIGINT NOT NULL,
    i                SMALLINT NOT NULL,
    trace_line       VARCHAR(254) NOT NULL,
    PRIMARY KEY(event_id, i),
    FOREIGN KEY (event_id) REFERENCES logging_event(event_id)
  );