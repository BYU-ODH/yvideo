# --- adding asset tracking and content lockdown

# --- !Ups

alter table content add column physicalCopyExists boolean NOT NULL;
alter table content add column isCopyrighted boolean NOT NULL;
alter table content add column submitter varchar(255) NOT NULL;
alter table content add column enabled boolean NOT NULL;
alter table content add column dateEnabled varchar(255);

# --- !Downs

alter table content drop column physicalCopyExists;
alter table content drop column isCopyrighted;
alter table content drop column submitter;
alter table content drop column enabled;
alter table content drop column dateEnabled;

