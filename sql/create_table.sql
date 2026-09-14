-- AI Coding 数据库初始化脚本
create database if not exists ai_coding default charset utf8mb4 collate utf8mb4_unicode_ci;
use ai_coding;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    user_account varchar(64)                           not null comment '账号',
    user_password varchar(128)                         not null comment '密码（BCrypt）',
    user_name    varchar(64)                           null comment '用户昵称',
    user_avatar  varchar(1024)                         null comment '用户头像',
    user_profile varchar(1024)                         null comment '用户简介',
    user_role    varchar(16) default 'user'            not null comment '用户角色：user/admin',
    edit_time    datetime    default CURRENT_TIMESTAMP comment '编辑时间',
    create_time  datetime    default CURRENT_TIMESTAMP comment '创建时间',
    update_time  datetime    default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint     default 0                 not null comment '是否删除',
    unique index uk_user_account (user_account)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table if not exists app
(
    id            bigint auto_increment comment 'id' primary key,
    app_name      varchar(128)                          null comment '应用名称',
    cover         varchar(1024)                         null comment '应用封面',
    init_prompt   text                                  not null comment '初始提示词',
    code_gen_type varchar(32)                           null comment '代码生成类型（首次生成时由 AI 路由决策）',
    deploy_key    varchar(64)                           null comment '部署标识（子域名）',
    deployed_time datetime                              null comment '部署时间',
    priority      int         default 0                 not null comment '优先级（精选排序）',
    is_featured   tinyint     default 0                 not null comment '是否精选',
    user_id       bigint                                not null comment '创建用户 id',
    edit_time     datetime    default CURRENT_TIMESTAMP comment '编辑时间',
    create_time   datetime    default CURRENT_TIMESTAMP comment '创建时间',
    update_time   datetime    default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint     default 0                 not null comment '是否删除',
    index idx_user_id (user_id),
    unique index uk_deploy_key (deploy_key)
) comment '应用' collate = utf8mb4_unicode_ci;

-- 对话历史表（游标分页核心索引：(app_id, create_time)）
create table if not exists chat_history
(
    id           bigint auto_increment comment 'id' primary key,
    message      text                                not null comment '消息内容',
    message_type varchar(16)                         not null comment '消息类型：user/ai',
    app_id       bigint                              not null comment '应用 id',
    user_id      bigint                              not null comment '用户 id',
    create_time  datetime default CURRENT_TIMESTAMP  comment '创建时间',
    is_delete    tinyint  default 0                  not null comment '是否删除',
    index idx_app_id_create_time (app_id, create_time),
    index idx_user_id (user_id)
) comment '对话历史' collate = utf8mb4_unicode_ci;

-- 应用版本快照表（版本时光机）
create table if not exists app_version
(
    id            bigint auto_increment comment 'id' primary key,
    app_id        bigint                                  not null comment '应用 id',
    version       int                                     not null comment '版本号（应用内递增）',
    snapshot      longtext                                null comment '版本快照（文件路径 -> 内容 JSON）',
    parent_version int          default 0                 null comment '父版本号（版本树）',
    message       varchar(512)                            null comment '版本说明（触发本次修改的对话）',
    gen_type      varchar(32)                             null comment '生成类型',
    user_id       bigint                                  not null comment '用户 id',
    create_time   datetime     default CURRENT_TIMESTAMP  comment '创建时间',
    is_delete     tinyint      default 0                  not null comment '是否删除',
    index idx_app_id_version (app_id, version),
    index idx_user_id (user_id)
) comment '应用版本快照' collate = utf8mb4_unicode_ci;

-- 工作流执行记录表（透明工作流）
create table if not exists workflow_run
(
    id         bigint auto_increment comment 'id' primary key,
    app_id     bigint                                 not null comment '应用 id',
    run_id     varchar(64)                            not null comment '一次执行编号',
    node       varchar(64)                            not null comment '工作流节点名',
    status     varchar(16)                            not null comment '节点状态：running/success/failed',
    payload    text                                   null comment '节点产物/错误摘要（JSON）',
    cost_ms    bigint       default 0                 not null comment '节点耗时',
    create_time datetime     default CURRENT_TIMESTAMP comment '创建时间',
    is_delete  tinyint      default 0                 not null comment '是否删除',
    index idx_app_id (app_id),
    index idx_run_id (run_id)
) comment '工作流执行记录' collate = utf8mb4_unicode_ci;

-- 模型调用计量表（用量中心）
create table if not exists model_invocation
(
    id            bigint auto_increment comment 'id' primary key,
    user_id       bigint                                 not null comment '用户 id',
    app_id        bigint                                 null comment '应用 id',
    model         varchar(64)                            not null comment '模型名',
    purpose       varchar(32)                            null comment '用途：router/generate/fix/edit',
    input_tokens  int          default 0                 not null comment '输入 token',
    output_tokens int          default 0                 not null comment '输出 token',
    cost_ms       bigint       default 0                 not null comment '耗时',
    status        varchar(16)  default 'success'         not null comment '状态：success/failed',
    create_time   datetime     default CURRENT_TIMESTAMP comment '创建时间',
    is_delete     tinyint      default 0                 not null comment '是否删除',
    index idx_user_id (user_id),
    index idx_app_id (app_id)
) comment '模型调用计量' collate = utf8mb4_unicode_ci;

-- 模板表（模板广场）
create table if not exists template
(
    id            bigint auto_increment comment 'id' primary key,
    name          varchar(128)                          not null comment '模板名称',
    description   varchar(1024)                         null comment '模板描述',
    init_prompt   text                                  not null comment '推荐提示词',
    code_gen_type varchar(32)                       null comment '代码生成类型',
    cover         varchar(1024)                         null comment '封面',
    sort          int         default 0                 not null comment '排序',
    create_time   datetime    default CURRENT_TIMESTAMP comment '创建时间',
    is_delete     tinyint     default 0                 not null comment '是否删除'
) comment '模板' collate = utf8mb4_unicode_ci;
