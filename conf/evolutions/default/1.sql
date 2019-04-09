# --- Initilaize DB 27-Mar-2019

# --- !Ups
CREATE TABLE `collection` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `owner` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `published` boolean NOT NULL DEFAULT '0',
  `archived` boolean NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
);


CREATE TABLE `collectionCourseLink` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `courseId` bigint(20) NOT NULL,
  `collectionId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `collectionMembership` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `collectionId` bigint(20) NOT NULL,
  `teacher` boolean NOT NULL,
  `exception` boolean DEFAULT '0',
  PRIMARY KEY (`id`)
);


CREATE TABLE `collectionPermissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `collectionId` varchar(255) DEFAULT NULL,
  `userId` bigint(20) NOT NULL,
  `permission` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `content` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `contentType` varchar(255) NOT NULL,
  `thumbnail` varchar(255) NOT NULL,
  `resourceId` varchar(255) NOT NULL,
  `authKey` varchar(255) NOT NULL,
  `views` bigint(20) NOT NULL DEFAULT '0',
  `collectionId` bigint(20) NOT NULL,
  `physicalCopyExists` boolean NOT NULL,
  `isCopyrighted` boolean NOT NULL,
  `requester` varchar(255) NOT NULL,
  `enabled` boolean NOT NULL,
  `published` boolean NOT NULL,
  `dateValidated` varchar(255) DEFAULT NULL,
  `fullVideo` boolean NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `contentSetting` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `contentId` bigint(20) NOT NULL,
  `setting` varchar(255) NOT NULL,
  `argument` text NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `course` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `department` varchar(255) NOT NULL,
  `catalogNumber` varchar(255) DEFAULT NULL,
  `sectionNumber` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `feedback` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `category` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `submitted` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `helpPage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `contents` longtext NOT NULL,
  `category` varchar(255) NOT NULL DEFAULT 'Uncategorized',
  PRIMARY KEY (`id`)
);


CREATE TABLE `homePageContent` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `text` varchar(255) NOT NULL,
  `link` varchar(255) NOT NULL,
  `linkText` varchar(255) NOT NULL,
  `background` varchar(255) NOT NULL,
  `active` boolean NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `setting` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `settingValue` longtext NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `sitePermissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `permission` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `userAccount` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `authId` varchar(255) NOT NULL,
  `authScheme` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `role` int(11) NOT NULL DEFAULT '1',
  `picture` varchar(511) DEFAULT NULL,
  `accountLinkId` bigint(20) NOT NULL DEFAULT '-1',
  `created` varchar(255) NOT NULL DEFAULT '2013-01-01T12:00:00.000-06:00',
  `lastLogin` varchar(255) NOT NULL DEFAULT '2013-01-01T12:00:00.000-06:00',
  PRIMARY KEY (`id`)
);


CREATE TABLE `userView` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `contentId` bigint(20) NOT NULL,
  `time` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
);


CREATE TABLE `wordList` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `word` text NOT NULL,
  `userId` bigint(20) NOT NULL,
  `srcLang` varchar(8) NOT NULL DEFAULT 'eng',
  `destLang` varchar(8) NOT NULL DEFAULT 'eng',
  PRIMARY KEY (`id`)
);


# --- !Downs
DROP TABLE IF EXISTS `collection`;
DROP TABLE IF EXISTS `collectionCourseLink`;
DROP TABLE IF EXISTS `collectionMembership`;
DROP TABLE IF EXISTS `collectionPermissions`;
DROP TABLE IF EXISTS `content`;
DROP TABLE IF EXISTS `contentSetting`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `helpPage`;
DROP TABLE IF EXISTS `homePageContent`;
DROP TABLE IF EXISTS `setting`;
DROP TABLE IF EXISTS `sitePermissions`;
DROP TABLE IF EXISTS `userAccount`;
DROP TABLE IF EXISTS `userView`;
DROP TABLE IF EXISTS `wordList`;

