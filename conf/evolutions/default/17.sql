# --- Adding help page categories

# --- !Ups

alter table helpPage add column category varchar(255) not null default "Uncategorized";

# --- !Downs

alter table helpPage drop column category;
