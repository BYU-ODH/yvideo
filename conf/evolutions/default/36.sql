# --- Removing collectionCoownerLink

# --- !Ups

alter table coursePermissions change column courseId collectionId varchar(255);
alter table coursePermissions rename to collectionPermissions;

# --- !Downs

alter table collectionPermissions rename to coursePermissions;
alter table coursePermissions change column collectionId courseId varchar(255);

