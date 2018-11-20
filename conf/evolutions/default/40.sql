# -- adding fullVideo option to content

# --- !Ups

alter table content add column fullVideo boolean NOT NULL;

# --- !Downs

alter table content drop column fullVideo boolean NOT NULL;