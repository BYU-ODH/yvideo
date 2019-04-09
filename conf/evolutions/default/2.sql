# --- !Ups

alter table content change enabled expired boolean not null;

# --- !Downs

alter table content change expired enabled boolean not null;

