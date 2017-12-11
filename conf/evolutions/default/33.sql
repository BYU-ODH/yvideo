# --- Swapping out course fields for collection fields

# --- !Ups

DROP TABLE course;
CREATE TABLE course (
  id        bigint not null auto_increment,
  name      varchar(255) not null,
  primary key(id)
);

# --- !Downs

DROP TABLE course;
CREATE TABLE course (
  id             bigint not null auto_increment,
  name           varchar(255) not null,
  startDate      varchar(255) not null,
  endDate        varchar(255) not null,
  lmsKey         varchar(255) not null,
  primary key(id)
);
