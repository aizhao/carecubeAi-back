-- ----------------------------
-- 知识库管理 — 菜单与权限
-- ----------------------------

-- 一级菜单：知识库管理
insert into sys_menu values('2000', '知识库管理', '0', '4', 'mag',  null, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', sysdate(), '', null, '知识库管理目录');

-- 二级菜单：知识库列表
insert into sys_menu values('2001', '知识库列表', '2000', '1', 'kb', 'mag/knowledge/index', '', 'KnowledgeBase', 1, 0, 'C', '0', '0', 'mag:kb:list', 'tree-table', 'admin', sysdate(), '', null, '知识库列表页面');

-- 三级按钮权限
insert into sys_menu values('2002', '知识库查询', '2001', '1', '',  null, '', '', 1, 0, 'F', '0', '0', 'mag:kb:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2003', '知识库新增', '2001', '2', '',  null, '', '', 1, 0, 'F', '0', '0', 'mag:kb:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2004', '知识库修改', '2001', '3', '',  null, '', '', 1, 0, 'F', '0', '0', 'mag:kb:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2005', '知识库删除', '2001', '4', '',  null, '', '', 1, 0, 'F', '0', '0', 'mag:kb:remove', '#', 'admin', sysdate(), '', null, '');

-- 隐藏页面：文件管理页
insert into sys_menu values('2006', '知识库文件管理', '2000', '5', 'kb/files/:id', 'mag/knowledge/files', '', 'KnowledgeFiles', 1, 0, 'C', '1', '0', 'mag:kb:list', '#', 'admin', sysdate(), '', null, '知识库文件管理页面');

-- 为超级管理员（role_id = 1）分配权限
insert into sys_role_menu values ('1', '2000');
insert into sys_role_menu values ('1', '2001');
insert into sys_role_menu values ('1', '2002');
insert into sys_role_menu values ('1', '2003');
insert into sys_role_menu values ('1', '2004');
insert into sys_role_menu values ('1', '2005');
insert into sys_role_menu values ('1', '2006');

-- ==============================
-- 文件管理 — 菜单与权限
-- ==============================

-- 一级菜单：文件管理
insert into sys_menu values('2007', '文件管理', '0', '5', 'mag/file', null, '', '', 1, 0, 'M', '0', '0', '', 'folder', 'admin', sysdate(), '', null, '文件管理目录');

-- 二级菜单：文件列表
insert into sys_menu values('2008', '文件列表', '2007', '1', 'index', 'mag/file/index', '', 'FileManager', 1, 0, 'C', '0', '0', 'mag:file:list', 'tree-table', 'admin', sysdate(), '', null, '文件管理页面');

-- 按钮权限
insert into sys_menu values('2009', '文件查询', '2008', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'mag:file:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2010', '文件新增', '2008', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'mag:file:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2011', '文件修改', '2008', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'mag:file:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2012', '文件删除', '2008', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'mag:file:remove', '#', 'admin', sysdate(), '', null, '');

-- 角色权限
insert into sys_role_menu values ('1', '2007');
insert into sys_role_menu values ('1', '2008');
insert into sys_role_menu values ('1', '2009');
insert into sys_role_menu values ('1', '2010');
insert into sys_role_menu values ('1', '2011');
insert into sys_role_menu values ('1', '2012');

-- ==============================
-- AI智能搜索 — 菜单与权限
-- ==============================

insert into sys_menu values('2013', '智能搜索', '2000', '2', 'kb/search', 'mag/knowledge/search', '', 'KnowledgeSearch', 1, 0, 'C', '0', '0', 'mag:kb:search', 'search', 'admin', sysdate(), '', null, 'AI智能搜索页面');

insert into sys_role_menu values ('1', '2013');

-- ==============================
-- AI 聊天助手 — 菜单与权限
-- ==============================

-- 一级菜单：AI 助手（直接进入聊天页，助手 ID 由前端环境变量 VITE_APP_CHAT_ASSISTANT_ID 配置）
insert into sys_menu values('2014', 'AI助手', '0', '6', 'ai-chat', 'mag/chat/chat', '', 'ChatConversation', 1, 1, 'C', '0', '0', 'mag:chat:query', 'chat', 'admin', sysdate(), '', null, 'AI助手聊天页面');

-- 角色权限
insert into sys_role_menu values ('1', '2014');
