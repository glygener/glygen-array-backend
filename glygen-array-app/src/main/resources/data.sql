insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag) values (1, 'user', '{noop}user', true, 'Glygen', 'User', 'user@uga.edu', 'UGA', 'uga.edu', true);

insert into roles (roleid, name) values (2, 'ROLE_ADMIN');
insert into roles (roleid, name) values (3, 'ROLE_MODERATOR');
insert into roles (roleid, name) values (4, 'ROLE_USER');
insert into user_roles values (1, 4);

insert into settings (name, value) values ('server.email', 'glycomic@uga.edu');
insert into settings (name, value) values ('server.email.password', 'miOeR$5q1Y');

