SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `ec_edge_node`;
CREATE TABLE `ec_edge_node` (
  `id` int NOT NULL AUTO_INCREMENT,
  `cpu_num` int DEFAULT NULL,
  `execution_failure_rate` double DEFAULT NULL,
  `task_rate` double DEFAULT NULL,
  `name` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7411 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
