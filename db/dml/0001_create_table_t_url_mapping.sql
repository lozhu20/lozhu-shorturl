drop table if exists public.t_url_mapping;

create table public.t_url_mapping
(
    id          varchar(36) default gen_random_uuid() not null,
    url         varchar(512)                          not null,
    short_url   varchar(6),
    expire_time timestamp,
    status      int         default 0,
    visit_count int         default 0,
    create_time timestamp   default current_timestamp,
    create_by   varchar(100),
    update_time timestamp   default current_timestamp,
    update_by   varchar(100),
    primary key (id, create_time)
) partition by range (create_time);

comment on table public.t_url_mapping is '短链接映射表';
comment on column public.t_url_mapping.id is '主键';
comment on column public.t_url_mapping.url is '原始链接';
comment on column public.t_url_mapping.short_url is '短链接';
comment on column public.t_url_mapping.expire_time is '过期时间';
comment on column public.t_url_mapping.status is '状态(0:正常,1:失效)';
comment on column public.t_url_mapping.visit_count is '访问次数';
comment on column public.t_url_mapping.create_time is '创建时间';
comment on column public.t_url_mapping.create_by is '创建人';
comment on column public.t_url_mapping.update_time is '更新时间';
comment on column public.t_url_mapping.update_by is '更新人';
