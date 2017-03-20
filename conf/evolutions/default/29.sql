# --- Removing Sharing functionality

# --- !Ups

drop table is exists announcement;

# --- !Downs

create table announcement (
  id             bigint not null auto_increment,
  courseId       bigint not null,
  userId         bigint not null,
  timeMade       varchar(255) not null,
  content        text not null,
  primary key(id)
);

