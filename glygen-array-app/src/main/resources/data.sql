insert into users (username, password, enabled, firstname, lastname, affiliation, affiliationwebsite, publicflag) values ('user', '{noop}user', true, 'Glygen', 'User', 'UGA', 'uga.edu', true);

insert into authorities (username, authority) values ('user', 'ROLE_ADMIN');