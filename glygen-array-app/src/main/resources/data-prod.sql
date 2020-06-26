insert into roles (roleid, name) values (2, 'ROLE_ADMIN') ON CONFLICT DO NOTHING;
insert into roles (roleid, name) values (3, 'ROLE_MODERATOR') ON CONFLICT DO NOTHING;
insert into roles (roleid, name) values (4, 'ROLE_USER') ON CONFLICT DO NOTHING;

insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag, logintype) values (2, 'admin', '{bcrypt}$2a$10$z9fy7u1V1XhGV27DMEqp.O4BZT8.H5.MduqOJmaKENWdlJbn/qdN.', true, 'Glygen', 'Administrator', 'sena@uga.edu', 'UGA', 'uga.edu', true, 'LOCAL') ON CONFLICT DO NOTHING;
insert into user_roles values (2, 2) ON CONFLICT DO NOTHING;

INSERT INTO settings (name, value) SELECT 'token.expiration', '8640000' WHERE NOT EXISTS (SELECT value FROM settings WHERE name = 'token.expiration'); 