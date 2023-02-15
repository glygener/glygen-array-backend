insert into roles (roleid, name) values (2, 'ROLE_ADMIN') ON CONFLICT DO NOTHING;
insert into roles (roleid, name) values (3, 'ROLE_MODERATOR') ON CONFLICT DO NOTHING;
insert into roles (roleid, name) values (4, 'ROLE_USER') ON CONFLICT DO NOTHING;
insert into roles (roleid, name) values (5, 'ROLE_DATA') ON CONFLICT DO NOTHING;

insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag, logintype) values (2, 'admin', '{bcrypt}$2a$10$z9fy7u1V1XhGV27DMEqp.O4BZT8.H5.MduqOJmaKENWdlJbn/qdN.', true, 'Glygen', 'Administrator', 'sena@uga.edu', 'UGA', 'uga.edu', true, 'LOCAL') ON CONFLICT DO NOTHING;
insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag, logintype) values (3, 'cfgdata', '{bcrypt}$2a$10$mvRzMQuJ3Chv5knhZBQY6.e7pYuLXZSl2UQOW4/Tk0DsV.zXPrn8S', true, 'Glygen', 'Data', 'sena@uga.edu', 'UGA', 'uga.edu', true, 'LOCAL') ON CONFLICT DO NOTHING;
insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag, logintype) values (4, 'ncfgdata', '{bcrypt}$2a$10$.oIPruTO7ikYGUSnez5Dk.ngQDkWk3j8O5USMsEWzqEfBKj7udGWC', true, 'Harvard', 'Data', 'aymehta@bidmc.harvard.edu', 'Harvard', 'https://ncfg.hms.harvard.edu', true, 'LOCAL') ON CONFLICT DO NOTHING;
insert into users (userid, username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag, logintype) values (5, 'imperialdata', '{bcrypt}$2a$10$xWjpcUlt.akz4f0DLeyXTusQhHyJ0iRGdteZhFiFTYnbZKkecwtW6', true, 'Imperial', 'Data', 'y.akune@imperial.ac.uk', 'Imperial', 'https://www.imperial.ac.uk', true, 'LOCAL') ON CONFLICT DO NOTHING;
insert into user_roles values (2, 2) ON CONFLICT DO NOTHING;
insert into user_roles values (3, 5) ON CONFLICT DO NOTHING;
insert into user_roles values (4, 5) ON CONFLICT DO NOTHING;
insert into user_roles values (5, 5) ON CONFLICT DO NOTHING;

INSERT INTO settings (name, value) SELECT 'token.expiration', '8640000' WHERE NOT EXISTS (SELECT value FROM settings WHERE name = 'token.expiration'); 
INSERT INTO settings (name, value) SELECT 'timeDelay', '3600' WHERE NOT EXISTS (SELECT value FROM settings WHERE name = 'timeDelay'); 
INSERT INTO settings (name, value) SELECT 'apiVersion', '1.0.0' WHERE NOT EXISTS (SELECT value FROM settings WHERE name = 'apiVersion'); 
INSERT INTO settings (name, value) SELECT 'portalVersion', '1.0.0' WHERE NOT EXISTS (SELECT value FROM settings WHERE name = 'portalVersion');
INSERT INTO settings (name, value) SELECT 'apiReleaseDate', 'Feb 1, 2023' WHERE NOT EXISTS (SELECT value FROM settings WHERE name = 'apiReleaseDate'); 
INSERT INTO settings (name, value) SELECT 'portalReleaseDate', 'Feb 1, 2023' WHERE NOT EXISTS (SELECT value FROM settings WHERE name = 'portalReleaseDate');
