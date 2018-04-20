create table IF NOT EXISTS users (
  username varchar(256),
  password varchar(256),
  enabled boolean,
  firstname varchar(256),
  lastname varchar(256),
  email varchar(256),
  affiliation varchar(256),
  affiliationWebsite varchar(256),
  publicflag boolean
);

create table IF NOT EXISTS authorities (
  username varchar(256),
  authority varchar(256)
);