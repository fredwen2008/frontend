/*
SQLyog Ultimate v11.11 (64 bit)
MySQL - 5.5.34-0ubuntu0.12.04.1-log : Database - frontend
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`frontend` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE `frontend`;

/*Table structure for table `block` */

DROP TABLE IF EXISTS `block`;

CREATE TABLE `block` (
  `service` char(64) DEFAULT NULL,
  `method` char(32) DEFAULT NULL,
  `regex` varchar(512) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `hostport` */

DROP TABLE IF EXISTS `hostport`;

CREATE TABLE `hostport` (
  `cloudid` char(64) DEFAULT NULL,
  `service` char(64) DEFAULT NULL,
  `region` char(64) DEFAULT NULL,
  `hostport` char(128) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `user` */

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `cloudid` char(64) NOT NULL,
  `username` char(32) NOT NULL,
  `password` char(32) NOT NULL DEFAULT '',
  `oss_password` char(32) NOT NULL DEFAULT '',
  `role` char(32) DEFAULT NULL,
  PRIMARY KEY (`cloudid`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
