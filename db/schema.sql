create table if not exists tb_action
(
    id          int unsigned not null comment '主键'
        primary key,
    action_code varchar(200) not null comment '行为编号',
    action_name varchar(200) not null comment '行为名称',
    constraint unq_action_name
        unique (action_name)
)
    comment '行为表' charset = utf8mb4
                     row_format = DYNAMIC;

create table if not exists tb_address
(
    id          int auto_increment comment '地址 id'
        primary key,
    customer_id int         not null comment '用户 id',
    name        varchar(20) not null comment '收货人姓名',
    tel         char(11)    not null comment '收货人手机号',
    province    varchar(20) not null comment '省份（包括直辖市）',
    city        varchar(20) null comment '城市（不包括直辖市）',
    district    varchar(20) not null comment '县区',
    region_code json        not null comment '地区代码数组',
    detail      varchar(50) not null comment '详细地址',
    is_default  tinyint(1)  not null comment '是否为默认地址'
);

create index tb_address_customer_id_index
    on tb_address (customer_id);

create table if not exists tb_appointment
(
    id               int auto_increment comment '主键'
        primary key,
    uuid             char(32)                           not null comment 'UUID',
    order_id         int                                not null comment '订单编号',
    appointment_date date                               not null comment '预约日期',
    name             varchar(10)                        not null comment '姓名',
    sex              char                               not null comment '性别',
    pid              char(18)                           not null comment '身份证号',
    birthday         date                               not null comment '出生日期',
    tel              char(11)                           not null comment '电话号码',
    appointment_desc varchar(100)                       null comment '备注信息',
    status           tinyint                            not null comment '状态。1未签到，2已签到，3已完成，4已关闭',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '体检预约创建时间',
    checkin_time     datetime                           null comment '体检预约签到时间',
    completed_time   datetime                           null comment '体检预约完成时间',
    is_deleted       tinyint  default 0                 null comment '是否删除：0-未删除，1-已删除',
    deleted_time     datetime                           null comment '删除时间',
    constraint uk_pid_date
        unique (pid, appointment_date),
    constraint unq_uuid
        unique (uuid)
)
    comment '体检预约表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_date
    on tb_appointment (appointment_date);

create index idx_name
    on tb_appointment (name);

create index idx_order_id
    on tb_appointment (order_id);

create index idx_pid
    on tb_appointment (pid);

create index idx_status
    on tb_appointment (status);

create index idx_status_completed_time
    on tb_appointment (status, completed_time);

create index idx_tel
    on tb_appointment (tel);

create table if not exists tb_appointment_restriction
(
    id                 int auto_increment comment '主键'
        primary key,
    appointment_date   date                               not null comment '预约日期',
    actual_limit       int                                not null comment '实际限定体检人数',
    everyday_limit     int                                not null comment '每天体检人数上限',
    actual_appointment int                                not null comment '实际体检人数',
    remark             varchar(200)                       null comment '备注信息',
    create_time        datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint unq_date
        unique (appointment_date)
)
    comment '体检预约限流表' charset = utf8mb4
                             row_format = DYNAMIC;

create index idx_date
    on tb_appointment_restriction (appointment_date);

create table if not exists tb_banner
(
    id       int auto_increment comment '主键'
        primary key,
    name     varchar(20)  not null comment '轮播图名称',
    goods_id int          not null comment '推荐的商品 id',
    remarks  varchar(50)  null comment '备注',
    image    varchar(200) not null comment '推荐商品的 banner 图',
    status   tinyint(1)   not null comment '是否在 banner 投放广告',
    constraint unq_goods_id
        unique (goods_id)
)
    comment 'banner 推荐商品';

create index tb_banner_remarks_index
    on tb_banner (remarks);

create table if not exists tb_checkup_report
(
    id             int auto_increment comment '主键'
        primary key,
    appointment_id int          not null comment '体检预约ID',
    result_id      varchar(24)  not null comment '体检结果ID（MongoDB）',
    status         tinyint      not null comment '体检报告状态。1未生成，2生成中，3已生成，4已邮寄',
    file_path      varchar(300) null comment '提交报告存放在Minio服务器上的URL地址',
    error_message  varchar(500) null comment '生成失败时的错误信息',
    error_time     datetime     null comment '生成失败的时间',
    waybill_code   varchar(200) null comment '快递运单号',
    waybill_date   date         null comment '快递发出日期',
    date           date         not null comment '体检日期',
    generated_time datetime     null comment '体检报告生成时间',
    generate_type  tinyint      null comment '生成方式：1手动生成，2自动生成',
    constraint unq_appointment_id
        unique (appointment_id),
    constraint unq_result_id
        unique (result_id)
)
    comment '体检报告表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_appointment_id
    on tb_checkup_report (appointment_id);

create index idx_status
    on tb_checkup_report (status);

create index idx_status_error_time
    on tb_checkup_report (status, error_time);

create index idx_waybill_code
    on tb_checkup_report (waybill_code);

create table if not exists tb_customer
(
    id          int auto_increment comment '主键'
        primary key,
    username    varchar(20)                        not null comment '用户名',
    password    varchar(200)                       not null comment '密码',
    name        varchar(200)                       null comment '姓名',
    sex         char                               null comment '性别',
    tel         char(11)                           not null comment '电话',
    email       varchar(50)                        null comment '客户邮箱',
    photo       varchar(200)                       null comment '照片URL',
    third_party varchar(200)                       null comment '第三方',
    create_time datetime default CURRENT_TIMESTAMP not null comment '注册时间',
    constraint tb_customer_pk
        unique (username)
)
    comment '客户表' charset = utf8mb4
                     row_format = DYNAMIC;

create index idx_tel
    on tb_customer (tel);

create table if not exists tb_customer_im
(
    id          int auto_increment comment '主键ID'
        primary key,
    customer_id int      not null comment '客户ID',
    login_time  datetime not null comment '创建时间',
    constraint unq_customer_id
        unique (customer_id)
)
    comment '客户IM帐户表' charset = utf8mb4
                           row_format = DYNAMIC;

create table if not exists tb_customer_third_party
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    customer_id int          not null comment '用户ID',
    platform    varchar(50)  not null comment '平台（gitee, github, wechat等）',
    open_id     varchar(100) not null comment '平台用户唯一标识',
    nickname    varchar(100) null comment '平台用户昵称',
    avatar      varchar(255) null comment '平台用户头像',
    remark      varchar(500) null comment '备注信息',
    create_time datetime     null comment '创建时间',
    update_time datetime     null comment '更新时间',
    constraint uk_platform_openid
        unique (platform, open_id)
)
    comment '用户第三方账号绑定表' charset = utf8mb4;

create index IDX_CUST_PLATFORM
    on tb_customer_third_party (customer_id, platform);

create index idx_user_id
    on tb_customer_third_party (customer_id);

create table if not exists tb_dept
(
    id        int unsigned auto_increment comment '主键'
        primary key,
    dept_name varchar(200) not null comment '部门名称',
    tel       varchar(20)  null comment '部门电话',
    email     varchar(200) null comment '部门邮箱',
    `desc`    varchar(50)  null comment '备注',
    constraint unq_dept_name
        unique (dept_name)
)
    comment '部门表' charset = utf8mb4
                     row_format = DYNAMIC;

create table if not exists tb_flow_regulation
(
    id         int auto_increment comment '主键'
        primary key,
    place      varchar(50)       not null comment '科室名称',
    real_num   int     default 0 not null comment '排队人数',
    max_num    int               not null comment '最大人数',
    weight     tinyint default 1 not null comment '权重（自动调流使用）',
    priority   tinyint default 1 not null comment '优先级（手动调流使用）',
    blue_uuid  varchar(64)       not null comment '蓝牙信标ID',
    is_deleted tinyint default 0 not null comment '是否删除：0-未删除，1-已删除，2-停用',
    constraint unq_blue_uuid
        unique (blue_uuid),
    constraint unq_place
        unique (place)
)
    comment '人员调流表' charset = utf8mb4
                         row_format = DYNAMIC;

create table if not exists tb_goods
(
    id            int auto_increment comment '主键'
        primary key,
    code          varchar(200)                                                                not null comment '编号',
    title         varchar(50)                                                                 not null comment '商品标题',
    description   varchar(200)                                                                not null comment '商品描述',
    checkup_1     json                                                                        null comment '科室检查',
    checkup_2     json                                                                        null comment '实验室检查',
    checkup_3     json                                                                        null comment '医技检查',
    checkup_4     json                                                                        null comment '其他检查',
    checkup       json                                                                        null comment '检查内容',
    image         json                                                                        not null comment '商品封面',
    initial_price decimal(10, 2)                                                              not null comment '原价',
    current_price decimal(10, 2)                                                              not null comment '现价',
    sales_volume  int      default 0                                                          not null comment '销量',
    type          enum ('不限', '父母体检', '入职体检', '职场白领', '个人高端', '中青年体检') not null comment '套餐类型',
    tag           json                                                                        null comment '套餐标签',
    part_id       tinyint                                                                     null comment '1活动专区，2热卖套餐，3新品推荐，4孝敬父母，5,白领精英',
    rule_id       int                                                                         null comment '促销优惠规则的ID',
    status        tinyint(1)                                                                  not null comment '状态(1上架，0下架)',
    md5           varchar(200)                                                                not null comment 'MD5信息',
    update_time   datetime                                                                    null on update CURRENT_TIMESTAMP comment '最后修改时间',
    create_time   datetime default CURRENT_TIMESTAMP                                          not null comment '创建时间',
    constraint unq_code
        unique (code)
)
    comment '体检套餐表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_goods_rule_id
    on tb_goods (rule_id);

create index idx_status
    on tb_goods (status);

create index idx_type
    on tb_goods (type);

create table if not exists tb_module
(
    id          int unsigned not null comment '主键'
        primary key,
    module_code varchar(200) not null comment '模块编号',
    module_name varchar(200) not null comment '模块名称',
    constraint unq_module_id
        unique (module_code)
)
    comment '模块资源表' charset = utf8mb4
                         row_format = DYNAMIC;

create table if not exists tb_order
(
    id                int auto_increment comment '主键'
        primary key,
    customer_id       int                                not null comment '客户ID',
    goods_id          int                                not null comment '商品ID',
    snapshot_id       varchar(200)                       not null comment '商品快照ID',
    address_id        int                                not null comment '订单收货地址',
    goods_title       varchar(50)                        not null comment '商品标题',
    goods_price       decimal(10, 2)                     not null comment '商品价格',
    number            int                                not null comment '购买数量',
    appointed_num     int      default 0                 not null comment '订单中已经预约体检的商品数量',
    total_amount      decimal(10, 2)                     not null comment '订单总额',
    discount_amount   decimal(10, 2)                     null comment '折扣金额',
    payable_amount    decimal(10, 2)                     not null comment '应付金额',
    goods_image       varchar(300)                       not null comment '商品封面',
    goods_description varchar(200)                       not null comment '商品描述',
    order_notes       varchar(300)                       null comment '备注信息',
    out_trade_no      char(32)                           not null comment '订单流水号',
    payment_type      varchar(10)                        null comment '支付方式',
    transaction_id    char(32)                           null comment '付款单ID',
    out_refund_no     char(64)                           null comment '退款单流水号',
    refund_amount     decimal(10, 2)                     null comment '实际退款金额',
    version           int      default 0                 null comment '乐观锁版本号',
    status            tinyint                            not null comment '订单状态。1未付款，2已预约，3已付款，4已退款，5已全部预约，6已结束，7退款中，8退款失败，9已关闭',
    create_date       date                               not null comment '下单日期',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '下单日期时间',
    update_time       datetime                           null on update CURRENT_TIMESTAMP comment '订单更新时间',
    refund_date       date                               null comment '退款日期',
    refund_time       datetime                           null comment '退款日期时间',
    constraint unq_out_trade_no
        unique (out_trade_no),
    constraint unq_transaction_id
        unique (transaction_id)
)
    comment '订单表' charset = utf8mb4
                     row_format = DYNAMIC;

create index idx_customer_id
    on tb_order (customer_id);

create index idx_goods_id
    on tb_order (goods_id);

create index idx_goods_title
    on tb_order (goods_title);

create index idx_order_address_id
    on tb_order (address_id);

create index idx_snapshot_id
    on tb_order (snapshot_id);

create index idx_status_create_time
    on tb_order (status, create_time);

create index idx_version
    on tb_order (version);

create table if not exists tb_permission
(
    id              int unsigned not null comment '主键'
        primary key,
    permission_name varchar(200) not null comment '权限',
    module_id       int unsigned not null comment '模块ID',
    action_id       int unsigned not null comment '行为ID',
    constraint unq_permission
        unique (permission_name)
)
    comment '权限表' charset = utf8mb4
                     row_format = DYNAMIC;

create table if not exists tb_role
(
    id                  int unsigned auto_increment comment '主键'
        primary key,
    role_name           varchar(200)         not null comment '角色名称',
    permissions         json                 not null comment '权限集合',
    `desc`              varchar(200)         null comment '描述',
    default_permissions json                 null comment '系统角色内置权限',
    systemic            tinyint(1) default 0 null comment '是否为系统内置角色',
    constraint unq_role_name
        unique (role_name)
)
    comment '角色表' charset = utf8mb4
                     row_format = DYNAMIC;

create table if not exists tb_rule
(
    id     int auto_increment comment '主键'
        primary key,
    name   varchar(200) not null comment '规则名称',
    rule   text         not null comment '价格计算规则',
    remark varchar(200) null comment '备注',
    constraint unq_name
        unique (name)
)
    comment '规则表' charset = utf8mb4
                     row_format = DYNAMIC;

create table if not exists tb_system
(
    id     int auto_increment comment '主键'
        primary key,
    item   varchar(200) not null comment '设置项',
    value  varchar(200) not null comment '设定值',
    remark varchar(255) null comment '备注'
)
    comment '系统表' charset = utf8mb4
                     row_format = DYNAMIC;

create table if not exists tb_user
(
    id          int auto_increment comment '主键'
        primary key,
    username    varchar(200)                         not null comment '用户名',
    password    varchar(200)                         not null comment '密码',
    open_id     varchar(200)                         null comment '长期授权字符串',
    photo       varchar(200)                         null comment '头像网址',
    name        varchar(20)                          not null comment '姓名',
    sex         enum ('男', '女')                    not null comment '性别',
    tel         char(11)                             not null comment '手机号码',
    email       varchar(200)                         not null comment '邮箱',
    hiredate    date                                 null comment '入职日期',
    role        json                                 not null comment '角色',
    root        tinyint(1) default 0                 not null comment '是否是超级管理员',
    dept_id     int                                  null comment '部门编号',
    status      tinyint                              not null comment '状态',
    create_time datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint UK_USERS_USERNAME
        unique (username),
    constraint unq_open_id
        unique (open_id),
    constraint unq_username
        unique (username)
)
    comment '用户表' charset = utf8mb4
                     row_format = DYNAMIC;

create index idx_dept_id
    on tb_user (dept_id);

create index idx_status
    on tb_user (status);

