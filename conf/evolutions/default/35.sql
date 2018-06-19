# --- Removing collectionCoownerLink

# --- !Ups

drop table if exists collectionCoownerLink;

# --- !Downs

create table collectionCoownerLink(
  id             bigint not null auto_increment,
  userId         bigint not null,
  collectionId   bigint not null,
  primary key(id)
);

