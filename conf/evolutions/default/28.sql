# --- Removing LMS interoperablility

# --- !Ups

alter table course drop column lmsKey;

# --- !Downs

alter table course add column lmsKey varchar(255) not null;
