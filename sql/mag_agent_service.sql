-- ----------------------------
-- 智能分析服务 — Agent 配置、会话、消息
-- ----------------------------

create table if not exists mag_agent_config (
  id bigint not null auto_increment comment '主键',
  agent_code varchar(64) not null comment '业务智能体编码',
  agent_name varchar(100) not null comment '智能分析服务名称',
  agent_id varchar(128) not null comment 'RAGFlow Agent ID',
  release_value varchar(64) default null comment 'RAGFlow release 配置',
  enabled char(1) default '1' comment '是否启用（1启用 0停用）',
  description varchar(500) default null comment '说明',
  sort int default 0 comment '排序',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (id),
  unique key uk_mag_agent_code (agent_code)
) engine=innodb auto_increment=1 comment='智能分析服务配置表';

create table if not exists mag_agent_input_field (
  id bigint not null auto_increment comment '主键',
  agent_code varchar(64) not null comment '业务智能体编码',
  field_key varchar(64) not null comment '前端字段名',
  field_label varchar(100) not null comment '字段标签',
  field_type varchar(32) default 'text' comment '字段类型',
  required char(1) default '0' comment '是否必填（1是 0否）',
  options_json text comment '选项JSON',
  default_value varchar(500) default null comment '默认值',
  ragflow_input_key varchar(128) not null comment 'RAGFlow inputs 字段名',
  sort int default 0 comment '排序',
  enabled char(1) default '1' comment '是否启用（1启用 0停用）',
  primary key (id),
  key idx_mag_agent_input_code (agent_code)
) engine=innodb auto_increment=1 comment='智能分析输入字段配置表';

create table if not exists mag_agent_session (
  session_id varchar(64) not null comment 'CareCubeAi业务会话ID',
  agent_code varchar(64) not null comment '业务智能体编码',
  ragflow_session_id varchar(128) default null comment 'RAGFlow会话ID',
  user_id bigint not null comment '用户ID',
  session_title varchar(200) default null comment '会话标题',
  status varchar(32) default 'active' comment '状态',
  last_message_time datetime default null comment '最后消息时间',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  primary key (session_id),
  key idx_mag_agent_session_user (user_id, agent_code, create_time)
) engine=innodb comment='智能分析业务会话表';

create table if not exists mag_agent_patient_snapshot (
  snapshot_id varchar(64) not null comment '快照ID',
  session_id varchar(64) not null comment '业务会话ID',
  agent_code varchar(64) not null comment '业务智能体编码',
  user_id bigint not null comment '用户ID',
  input_json longtext comment '患者结构化输入JSON',
  input_summary varchar(500) default null comment '脱敏摘要',
  create_time datetime default null comment '创建时间',
  primary key (snapshot_id),
  key idx_mag_agent_snapshot_session (session_id)
) engine=innodb comment='智能分析患者输入快照表';

create table if not exists mag_agent_message (
  message_id varchar(64) not null comment '消息ID',
  session_id varchar(64) not null comment '业务会话ID',
  agent_code varchar(64) not null comment '业务智能体编码',
  user_id bigint not null comment '用户ID',
  role varchar(20) not null comment '角色 user/assistant/system',
  content longtext comment '消息内容',
  event_type varchar(32) default null comment '事件类型',
  structured_json longtext comment '结构化结果JSON',
  reference_json longtext comment '引用JSON',
  attachment_json longtext comment '附件JSON',
  ragflow_message_id varchar(128) default null comment 'RAGFlow消息ID',
  create_time datetime default null comment '创建时间',
  primary key (message_id),
  key idx_mag_agent_message_session (session_id, create_time)
) engine=innodb comment='智能分析消息表';

-- 菜单与权限
insert into sys_menu values('2020', '智能分析', '0', '7', 'mag/agent', null, '', '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), '', null, '智能分析目录');
insert into sys_menu values('2021', '智能分析服务', '2020', '1', 'index', 'mag/agent/index', '', 'MagAgent', 1, 1, 'C', '0', '0', 'mag:agent:list', 'chat', 'admin', sysdate(), '', null, '智能分析服务页面');
insert into sys_menu values('2022', '智能分析查询', '2021', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'mag:agent:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2023', '智能分析对话', '2021', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'mag:agent:chat', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2024', '智能分析删除', '2021', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'mag:agent:remove', '#', 'admin', sysdate(), '', null, '');

insert into sys_role_menu values ('1', '2020');
insert into sys_role_menu values ('1', '2021');
insert into sys_role_menu values ('1', '2022');
insert into sys_role_menu values ('1', '2023');
insert into sys_role_menu values ('1', '2024');
