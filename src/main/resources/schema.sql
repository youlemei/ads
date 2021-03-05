
CREATE TABLE `advertisement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `company_id` bigint(20) NOT NULL COMMENT '广告主id',
  `ad_type` smallint(6) DEFAULT '1' COMMENT '类型 1.直链广告',
  `ad_name` varchar(128) NOT NULL COMMENT '广告',
  `ad_category` smallint(6) NOT NULL COMMENT '类别',
  `system_type` smallint(6) NOT NULL DEFAULT '1' COMMENT '系统 1.IOS 2.Android',
  `end_time` datetime NOT NULL COMMENT '截止时间',
  `trace_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '推广开关',
  `settle_type` smallint(6) DEFAULT '1' COMMENT '结算类型 1.CPA',
  `in_price` decimal(20,4) DEFAULT NULL COMMENT '接入单价',
  `trace_type` varchar(16) NOT NULL COMMENT '追踪方式 ASYNC:异步 REDIRECT:302跳转',
  `preview_url` varchar(1024) DEFAULT NULL COMMENT '预览链接',
  `trace_url` varchar(1024) NOT NULL COMMENT '广告追踪链接',
  `deduplicate_type` smallint(6) DEFAULT '1' COMMENT '去重策略',
  `creator` varchar(32) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editor` varchar(32) DEFAULT NULL COMMENT '编辑者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `json_data` varchar(1024) DEFAULT NULL,
  `click_day_limit` int(10) unsigned DEFAULT NULL COMMENT '每日点击上限',
  `convert_day_limit` int(10) unsigned DEFAULT NULL COMMENT '每日转化上限',
  PRIMARY KEY (`id`),
  KEY `advertisement_ad_name_index` (`ad_name`),
  KEY `advertisement_company_id_trace_status_index` (`company_id`,`trace_status`),
  KEY `advertisement_creator_create_time_index` (`creator`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告';


CREATE TABLE `advertisement_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `ad_date` date NOT NULL COMMENT '日期',
  `channel_id` bigint(20) NOT NULL COMMENT '渠道id',
  `ad_id` bigint(20) NOT NULL COMMENT '广告id',
  `channel_creator` varchar(32) DEFAULT NULL COMMENT '渠道创建者',
  `ad_creator` varchar(32) DEFAULT NULL COMMENT '广告创建者',
  `click_sum` int(11) DEFAULT '0' COMMENT '点击',
  `deduplicate_click_sum` int(11) DEFAULT '0' COMMENT '去重点击',
  `src_convert_sum` int(11) DEFAULT '0' COMMENT '原始转化',
  `convert_sum` int(11) DEFAULT '0' COMMENT '转化',
  `in_price` decimal(20,4) DEFAULT NULL COMMENT '接入单价',
  `out_price` decimal(20,4) DEFAULT NULL COMMENT '投放单价',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `json_data` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `advertisement_report_ad_id_channel_id_ad_date_uindex` (`ad_id`,`channel_id`,`ad_date`),
  KEY `advertisement_report_ad_date_index` (`ad_date`),
  KEY `advertisement_report_ad_id_ad_date_index` (`ad_id`,`ad_date`),
  KEY `advertisement_report_channel_id_ad_date_index` (`channel_id`,`ad_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告报表, 按日汇总';


CREATE TABLE `channel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `channel_name` varchar(64) NOT NULL COMMENT '渠道名',
  `channel_mailbox` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `channel_type` smallint(6) NOT NULL COMMENT '类型',
  `channel_web` varchar(1024) DEFAULT NULL COMMENT '官网',
  `contact_name` varchar(64) NOT NULL COMMENT '联系人姓名',
  `contact_mailbox` varchar(64) DEFAULT NULL COMMENT '联系人邮箱',
  `contact_phone_number` varchar(32) NOT NULL COMMENT '手机号码',
  `contact_qq` varchar(16) DEFAULT NULL COMMENT 'QQ',
  `contact_wechat` varchar(64) DEFAULT NULL COMMENT '微信',
  `contact_position` varchar(32) DEFAULT NULL COMMENT '职位',
  `creator` varchar(32) NOT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editor` varchar(32) DEFAULT NULL COMMENT '编辑者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `json_data` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `channel_creator_create_time_index` (`creator`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道';


CREATE TABLE `click_record` (
  `id` varchar(32) NOT NULL,
  `channel_id` bigint(20) NOT NULL COMMENT '渠道id',
  `ad_id` bigint(20) NOT NULL COMMENT '广告id',
  `channel_creator` varchar(32) DEFAULT NULL COMMENT '渠道创建者',
  `ad_creator` varchar(32) DEFAULT NULL COMMENT '广告创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `trace_type` varchar(16) NOT NULL COMMENT '追踪方式 ASYNC:异步 REDIRECT:302跳转',
  `ip` varchar(64) DEFAULT NULL COMMENT 'IP',
  `mac` varchar(64) DEFAULT NULL COMMENT '设备id iOS:IDFA Android:IMEI',
  `param_json` varchar(1024) DEFAULT NULL COMMENT '参数json',
  `click_status` smallint(6) NOT NULL DEFAULT '0' COMMENT '点击状态 0.收到 1.未转化 2.已转化 3.已核减',
  `retry_times` int(11) DEFAULT '0' COMMENT '重试次数',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `editor` varchar(64) DEFAULT NULL COMMENT '编辑标记',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `json_data` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `click_record_ad_id_channel_id_ip_mac_index` (`ad_id`,`channel_id`,`ip`,`mac`),
  KEY `click_record_create_time_index` (`create_time`),
  KEY `click_record_mac_index` (`mac`),
  KEY `click_record_click_status_retry_times_index` (`click_status`,`retry_times`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点击记录, 按日分表';


CREATE TABLE `company` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `company_name` varchar(64) NOT NULL COMMENT '广告主名',
  `company_mailbox` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `company_type` smallint(6) NOT NULL COMMENT '类型',
  `company_web` varchar(1024) DEFAULT NULL COMMENT '官网',
  `contact_name` varchar(64) NOT NULL COMMENT '联系人姓名',
  `contact_mailbox` varchar(64) DEFAULT NULL COMMENT '联系人邮箱',
  `contact_phone_number` varchar(32) NOT NULL COMMENT '手机号码',
  `contact_qq` varchar(16) DEFAULT NULL COMMENT 'QQ',
  `contact_wechat` varchar(64) DEFAULT NULL COMMENT '微信',
  `contact_position` varchar(32) DEFAULT NULL COMMENT '职位',
  `creator` varchar(32) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editor` varchar(32) DEFAULT NULL COMMENT '编辑者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `json_data` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `company_creator_create_time_index` (`creator`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告主';


CREATE TABLE `convert_record` (
  `click_id` varchar(32) NOT NULL COMMENT '点击id',
  `click_time` datetime NOT NULL COMMENT '点击时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `channel_id` bigint(20) NOT NULL COMMENT '渠道id',
  `ad_id` bigint(20) NOT NULL COMMENT '广告id',
  `channel_creator` varchar(32) DEFAULT NULL COMMENT '渠道创建者',
  `ad_creator` varchar(32) DEFAULT NULL COMMENT '广告创建者',
  `callback` varchar(1024) DEFAULT NULL COMMENT '回调url',
  `convert_status` smallint(6) NOT NULL DEFAULT '0' COMMENT '转化状态 0.收到 1.已转化 2.已通知渠道 3.已核减',
  `retry_times` int(11) DEFAULT '0' COMMENT '重试次数',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `editor` varchar(64) DEFAULT NULL COMMENT '编辑标记',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `json_data` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`click_id`),
  KEY `convert_record_ad_id_channel_id_create_time_index` (`ad_id`,`channel_id`,`create_time`),
  KEY `convert_record_channel_id_create_time_index` (`channel_id`,`create_time`),
  KEY `convert_record_create_time_index` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转化记录';


CREATE TABLE `promote_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `ad_id` bigint(20) NOT NULL COMMENT '广告id',
  `channel_id` bigint(20) NOT NULL COMMENT '渠道id',
  `ad_creator` varchar(32) DEFAULT NULL COMMENT '广告创建者',
  `channel_creator` varchar(32) DEFAULT NULL COMMENT '渠道创建者',
  `promote_status` smallint(6) NOT NULL DEFAULT '0' COMMENT '推广状态 0.生成中 1.运行 2.暂停',
  `in_price` decimal(20,4) DEFAULT NULL COMMENT '投放单价',
  `out_price` decimal(20,4) DEFAULT NULL COMMENT '投放单价',
  `deduct_rate` smallint(6) DEFAULT NULL COMMENT '扣量比例',
  `trace_url` varchar(1024) DEFAULT NULL COMMENT '追踪链接',
  `trace_type` varchar(16) NOT NULL COMMENT '追踪类型 ASYNC:异步 REDIRECT:302跳转',
  `click_day_limit` int(11) DEFAULT NULL COMMENT '每日点击上限',
  `convert_day_limit` int(11) DEFAULT NULL COMMENT '每日转化上限',
  `creator` varchar(32) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editor` varchar(32) DEFAULT NULL COMMENT '编辑者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `json_data` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `promote_record_ad_id_channel_id_uindex` (`ad_id`,`channel_id`),
  KEY `promote_record_channel_id_create_time_index` (`channel_id`,`create_time`),
  KEY `promote_record_creator_create_time_index` (`creator`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告投放';


CREATE TABLE `sys_config` (
  `name` varchar(128) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `remark` varchar(64) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `sys_config_name_uindex` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';


