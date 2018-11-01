# --- adding asset tracking and content lockdown

# --- !Ups

alter table content add column collectionId bigint NOT NULL;
alter table content add column physicalCopyExists boolean NOT NULL;
alter table content add column isCopyrighted boolean NOT NULL;
alter table content add column requester varchar(255) NOT NULL;
alter table content add column enabled boolean NOT NULL;
alter table content add column published boolean NOT NULL;
alter table content add column dateValidated varchar(255);
alter table content drop column dateAdded;
drop table addCourseRequest;

# --- !Downs

alter table content drop column collectionId;
alter table content drop column physicalCopyExists;
alter table content drop column isCopyrighted;
alter table content drop column requester;
alter table content drop column enabled;
alter table content drop column published;
alter table content drop column dateValidated;
alter table content add column dateAdded varchar(255);
create table addCourseRequest;
