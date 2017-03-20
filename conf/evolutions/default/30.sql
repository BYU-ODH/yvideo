# --- Removing Sharing functionality

# --- !Ups

alter table content drop column shareability;

# --- !Downs

alter table content add column shareability int(11) not null;
