# --- Remove notifications table


# --- !Ups

drop table if exists notification;

# --- !Downs

create table notification (
  id             bigint not null auto_increment,
  userId         bigint not null,
  message        varchar(512) not null,
  dateSent       varchar(255) not null,
  messageRead    boolean not null,
  primary key(id)
);
