/*drop legacy stuff*/
DROP TABLE IF EXISTS web_logging_event CASCADE;
DROP TABLE IF EXISTS web_logging_access CASCADE;
DROP SEQUENCE IF EXISTS web_access_id_seq;
DROP SEQUENCE IF EXISTS web_error_id_seq;

CREATE OR REPLACE FUNCTION create_login_type() RETURNS integer AS $$
DECLARE v_exists INTEGER;

BEGIN
    SELECT into v_exists (SELECT 1 FROM pg_type WHERE typname = 'login');
    IF v_exists IS NULL THEN
        CREATE TYPE login AS ENUM ('LOCAL', 'GOOGLE');
    	CREATE CAST (CHARACTER VARYING as login) WITH INOUT AS IMPLICIT;
    END IF;
    RETURN v_exists;
END;
$$ LANGUAGE plpgsql;

-- Call the function you just created
SELECT create_login_type();

-- Remove the function you just created
DROP function create_login_type();

CREATE OR REPLACE FUNCTION create_constraint_if_not_exists (
    t_name text, c_name text, constraint_sql text
) 
returns void AS
$$
DECLARE v_exists INTEGER;
BEGIN
    -- Look for our constraint
    SELECT into v_exists (select 1 
                   from information_schema.table_constraints 
                   where table_name = t_name  and constraint_name = c_name);
    IF v_exists IS NULL then
        execute constraint_sql;
    END IF;
END;
$$ language plpgsql;

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

alter table users drop constraint if exists unq_username cascade;
alter table users add constraint unq_username unique(username);

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

create table IF NOT EXISTS slidelayout (
  uri varchar(256) not null PRIMARY KEY,
  jsonvalue text
);

create table IF NOT EXISTS verification_token (
  id bigint not null PRIMARY KEY,
  userid bigint not null,
  token varchar(256) not null,
  expirydate date
);

create table IF NOT EXISTS graphs (
  id bigint not null PRIMARY KEY,
  userid bigint not null,
  graphuri varchar(256) not null
);

create table IF NOT EXISTS email (
  id bigint not null PRIMARY KEY,
  userid bigint not null,
  oldemail varchar(256) not null,
  newemail varchar(256) not null
);

create table IF NOT EXISTS permissions (
  id bigint not null PRIMARY KEY,
  userid bigint not null,
  graphuri varchar(256),
  resourceuri varchar(256),
  permissiontype bigint not null,
  additiondate date
);

create table IF NOT EXISTS glycansearchresult (
  sequence text not null,
  idlist text not null
);

ALTER TABLE permissions ADD COLUMN IF NOT EXISTS additiondate date;
ALTER TABLE users ADD COLUMN IF NOT EXISTS groupname varchar(256);
ALTER TABLE users ADD COLUMN IF NOT EXISTS department varchar(256);
       
select create_constraint_if_not_exists(
        'graphs',
        'fk_m1eg457wh2xxe878rx5y5graph',
        'alter table graphs add constraint fk_m1eg457wh2xxe878rx5y5graph foreign key (userid) references users;');
        
select create_constraint_if_not_exists(
        'permissions',
        'fk_m1eg457wh2xxe878rx5y5permission',
        'alter table permissions add constraint fk_m1eg457wh2xxe878rx5y5permission foreign key (userid) references users;');
 
select create_constraint_if_not_exists(
        'verification_token',
        'fk_m1eg457wh2xxe878rx5y5limo', 'alter table verification_token add constraint fk_m1eg457wh2xxe878rx5y5limo foreign key (userid) references users;');
        
select create_constraint_if_not_exists(
        'user_roles',
        'fk_kt76tbwgvgf7m5rhnrkxxvgyc', 'alter table user_roles add constraint fk_kt76tbwgvgf7m5rhnrkxxvgyc foreign key (roleId) references roles;');

select create_constraint_if_not_exists(
        'user_roles',
        'fk_oj50qpdthexxxmvur61ggy2fb', 'alter table user_roles add constraint fk_oj50qpdthexxxmvur61ggy2fb foreign key (userId) references users;');
        
select create_constraint_if_not_exists(
        'email',
        'fk_emailuser', 'alter table email add constraint fk_emailuser foreign key (userId) references users;');
        
create sequence IF NOT EXISTS ROLE_SEQ start 10 increment 50;
create sequence IF NOT EXISTS USER_SEQ start 10 increment 50;
create sequence IF NOT EXISTS TOKEN_SEQ minvalue 1 start 1 increment 1;
create sequence IF NOT EXISTS GRAPH_SEQ minvalue 1 start 1 increment 1;
create sequence IF NOT EXISTS EMAIL_SEQ minvalue 1 start 1 increment 1;

CREATE SEQUENCE IF NOT EXISTS error_id_seq MINVALUE 1 START 1;
CREATE SEQUENCE IF NOT EXISTS access_id_seq MINVALUE 1 START 1;
CREATE SEQUENCE IF NOT EXISTS web_event_id_seq MINVALUE 1 START 1;
CREATE SEQUENCE IF NOT EXISTS web_acc_id_seq MINVALUE 1 START 1;


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
  
  CREATE TABLE IF NOT EXISTS web_log_access
  (
    log_id          	BIGINT DEFAULT nextval('web_acc_id_seq') PRIMARY KEY,
    loggedin_user		varchar(256) NOT NULL,
    page				VARCHAR(254) NOT NULL,
    session_id		    VARCHAR(254) NOT NULL,
    log_timestamp		TIMESTAMP NOT NULL
  );
  
  ALTER TABLE web_log_access DROP CONSTRAINT IF EXISTS web_log_access_loggedin_user_fkey;
  ALTER TABLE web_log_access ADD CONSTRAINT web_log_access_user_fkey FOREIGN KEY(loggedin_user) REFERENCES users(username);
  
  CREATE TABLE IF NOT EXISTS web_log_event
  (
    log_id          	BIGINT DEFAULT nextval('web_event_id_seq') PRIMARY KEY,
    loggedin_user		varchar(256) NOT NULL,
    page				VARCHAR(254) NOT NULL,
    session_id		    VARCHAR(254) NOT NULL,
    event_type			VARCHAR(15) NOT NULL,
    params				VARCHAR(254),
    info				TEXT,
    comments			TEXT,
    log_timestamp		TIMESTAMP NOT NULL    
  );
  
  ALTER TABLE web_log_event DROP CONSTRAINT IF EXISTS web_log_event_loggedin_user_fkey;
  ALTER TABLE web_log_event ADD CONSTRAINT web_log_event_user_fkey FOREIGN KEY(loggedin_user) REFERENCES users(username);
  
  