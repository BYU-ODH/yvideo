# --- Removing activity streams

# --- !Ups

drop table if exists contentListing;
drop table if exists accountLink;
drop table if exists contentOwnership;
drop table if exists scoring;
drop table if exists sitePermissionRequest;

# --- !Downs

create table contentListing (
  id                bigint not null auto_increment,
  collectionId      bigint not null,
  contentId         bigint not null,
  primary key(id)
);

create table accountLink (
  id                bigint not null auto_increment,
  userIds           longtext not null,
  primaryAccount    bigint not null,
  primary key(id)
);

create table contentOwnership (
  id                bigint not null auto_increment,
  userId            bigint not null,
  contentId         bigint not null,
  primary key(id)
);

create table scoring (
  id                bigint not null auto_increment,
  score             double not null,
  possible          double not null,
  results           text not null,
  userId            bigint not null,
  contentId         bigint not null,
  graded            varchar(255) not null,
  primary key(id)
);

create table sitePermissionRequest (
  id                bigint not null auto_increment,
  userId            bigint not null,
  permission        varchar(255) not null,
  reason            varchar(2048) not null,
  primary key(id)
);

