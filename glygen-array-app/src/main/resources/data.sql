insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag, logintype) values (1, 'user', '{noop}user', true, 'Glygen', 'User', 'user@uga.edu', 'UGA', 'uga.edu', true, 'LOCAL');

insert into roles (roleid, name) values (2, 'ROLE_ADMIN');
insert into roles (roleid, name) values (3, 'ROLE_MODERATOR');
insert into roles (roleid, name) values (4, 'ROLE_USER');
insert into user_roles values (1, 4);

insert into settings (name, value) values ('server.email', 'glygenarray.api@gmail.com');
insert into settings (name, value) values ('server.email.password', 'UbYYJ8JeSNddcu+bz12Ajefnce6+Hpti');
insert into settings (name, value) values ('token.expiration', '8640000');  

