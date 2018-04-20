insert into users (username, password, enabled, firstname, lastname, email, affiliation, affiliationwebsite, publicflag) values ('user', '{noop}user', true, 'Glygen', 'User', 'user@uga.edu', 'UGA', 'uga.edu', true);

insert into authorities (username, authority) values ('user', 'ROLE_ADMIN');