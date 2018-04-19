drop table users;
drop table authorities;

create table users (
  username varchar(256),
  password varchar(256),
  enabled boolean,
  firstname varchar(256),
  lastname varchar(256),
  affiliation varchar(256),
  affiliationWebsite varchar(256),
  publicflag boolean
);

create table authorities (
  username varchar(256),
  authority varchar(256)
);