SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `ec_link`;
CREATE TABLE `ec_link` (
  `id` int NOT NULL AUTO_INCREMENT,
  `source` varchar(20) NOT NULL,
  `destination` varchar(20) NOT NULL,
  `transmission_rate` double NOT NULL,
  `transmission_failure_rate` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=74101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
