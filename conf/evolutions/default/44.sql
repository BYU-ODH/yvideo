# --- add published field to collections

# --- !Ups

alter table collection add column published boolean not null default false;
alter table collection add column archived boolean not null default false;

# --- !Downs

alter table collection drop column published;
alter table collection drop column archived;

