# --- Removing collectionCoownerLink

# --- !Ups

alter table content add column collectionId bigint NOT NULL;

# --- !Downs

alter table content drop column collectionId;

