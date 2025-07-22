SET FOREIGN_KEY_CHECKS=0;


DROP TABLE IF EXISTS `ec_task`;
CREATE TABLE `ec_task` (
  `id` int NOT NULL AUTO_INCREMENT,
  `job_id` int DEFAULT NULL,
  `time_slot` int DEFAULT NULL,
  `source` varchar(20) DEFAULT NULL,
  `destination` varchar(200) DEFAULT NULL,
  `runtime_info` text,
  `avail_action` varchar(200) DEFAULT NULL,
  `action` varchar(200) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `task_size` int DEFAULT NULL,
  `task_complexity` int DEFAULT NULL,
  `cpu_cycle` bigint DEFAULT NULL,
  `deadline` int DEFAULT NULL,
  `transmission_waiting_time` int DEFAULT NULL,
  `transmission_time` int DEFAULT NULL,
  `execution_waiting_time` int DEFAULT NULL,
  `execution_time` int DEFAULT NULL,
  `task_reliability` double DEFAULT NULL,
  `reliability_requirement` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=94382138 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
