# --- !Ups

alter table userAccount drop column authId;
alter table userAccount drop column authScheme;

# --- !Downs

alter table userAccount add column authId varchar(255) NOT NULL;
alter table userAccount add column authScheme varchar(255) NOT NULL;

