# --- Adding Collections

# --- !Ups

create table collection (
  id             bigint not null auto_increment,
  owner    	     bigint not null,
  name           varchar(255) not null,
  primary key(id)
);

create table collectionCourseLink(
  id             bigint not null auto_increment,
  courseId       bigint not null,
  collectionId   bigint not null,
  primary key(id)
);

create table collectionCoownerLink(
  id             bigint not null auto_increment,
  userId         bigint not null,
  collectionId   bigint not null,
  primary key(id)
);

# --- !Downs

drop table if exists collection;
drop table if exists collectionCourseLink;
drop table if exists collectionCoownerLink;
