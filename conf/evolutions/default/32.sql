# --- Swapping out course fields for collection fields

# --- !Ups

ALTER TABLE courseMembership CHANGE courseId collectionId bigint not null;
ALTER TABLE courseMembership RENAME collectionMembership;

ALTER TABLE contentListing CHANGE courseId collectionId bigint not null;

# --- !Downs

ALTER TABLE collectionMembership RENAME courseMembership;
ALTER TABLE courseMembership CHANGE collectionId courseId bigint not null;

ALTER TABLE contentListing CHANGE collectionId courseId bigint not null;
