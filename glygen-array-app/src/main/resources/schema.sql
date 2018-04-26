drop table IF EXISTS settings;
drop table IF EXISTS verification_token;
drop table IF EXISTS user_roles;
drop table IF EXISTS roles;
drop table IF EXISTS users;
drop sequence IF EXISTS user_seq;
drop sequence IF EXISTS role_seq;
drop sequence IF EXISTS TOKEN_SEQ;

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