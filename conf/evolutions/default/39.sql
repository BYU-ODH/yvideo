# --- Adding the userView table

# --- !Ups

create table userView (
    id          bigint not null auto_increment,
    userId      bigint not null,
    contentId   bigint not null,
    primary key(id)
);

# --- !Downs

drop table userView;
