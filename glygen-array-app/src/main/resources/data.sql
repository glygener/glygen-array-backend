insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag, logintype) values (1, 'user', '{noop}user', true, 'Glygen', 'User', 'user@uga.edu', 'UGA', 'uga.edu', true, 'LOCAL');
insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag, logintype) values (2, 'admin', '{bcrypt}$2a$10$z9fy7u1V1XhGV27DMEqp.O4BZT8.H5.MduqOJmaKENWdlJbn/qdN.', true, 'Glygen', 'Administrator', 'sena@uga.edu', 'UGA', 'uga.edu', true, 'LOCAL');

insert into roles (roleid, name) values (2, 'ROLE_ADMIN');
insert into roles (roleid, name) values (3, 'ROLE_MODERATOR');
insert into roles (roleid, name) values (4, 'ROLE_USER');
insert into user_roles values (1, 4);
insert into user_roles values (2, 2);

insert into settings (name, value) values ('token.expiration', '8640000');  
insert into settings (name, value) values ('timeDelay', '3600');