# --- Swapping out course fields for collection fields

# --- !Ups

alter table course change name department varchar(255) not null;
alter table course add column catalogNumber varchar(255);
alter table course add column sectionNumber varchar(255);
alter table collectionMembership add column exception boolean default false;

# --- !Downs

alter table course change department name varchar(255) not null;
alter table course drop column catalogNumber;
alter table course drop column sectionNumber;
alter table collectionMembership drop column exception;
