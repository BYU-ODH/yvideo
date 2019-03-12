# --- Remove view field in content objects

# --- !Ups

alter table userView add column time varchar(255) not null;

# --- !Downs

alter table content drop column time;

