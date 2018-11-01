# --- remove labels

# --- !Ups

alter table content drop column labels;

# --- !Downs

alter table content add column labels longtext NOT NULL;

