CREATE SCHEMA IF NOT EXISTS core;
CREATE TABLE IF NOT EXISTS core.structure
(
    structure_id serial NOT NULL,
    glyco_ct character varying(15000) NOT NULL,
    sequence_length integer,
    CONSTRAINT structure_pkey PRIMARY KEY (structure_id),
    CONSTRAINT structure_glyco_ct_key UNIQUE (glyco_ct)
);

DROP TABLE IF EXISTS core.glytoucan;
CREATE TABLE core.glytoucan
(
    id integer not null,
    glytoucan_id varchar(255),
    CONSTRAINT id_pkey PRIMARY KEY (id)
);