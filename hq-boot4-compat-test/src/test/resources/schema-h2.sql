SET MODE MySQL;

CREATE TABLE IF NOT EXISTS `t_area` (
  `id` varchar(32) NOT NULL,
  `name` varchar(32) NOT NULL,
  `parent_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- 同一个内存库会被多个测试上下文复用，seed 必须幂等
DELETE FROM `t_area`;

INSERT INTO `t_area` (`id`, `name`, `parent_id`) VALUES
('A1', 'Beijing', NULL),
('A2', 'Haidian', 1),
('A3', 'Chaoyang', 1);
