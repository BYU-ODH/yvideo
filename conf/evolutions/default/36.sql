# --- Removing collectionCoownerLink

# --- !Ups

alter table coursePermissions rename column courseId to collectionId;
alter table coursePermissions rename to collectionPermissions;

# --- !Downs

alter table collectionPermissions rename to coursePermissions;
alter table coursePermissions rename column collectionId to courseId;

