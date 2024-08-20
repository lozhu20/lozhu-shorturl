drop table if exists public.t_url_mapping_20240418_to_20240422;
drop index if exists idx_t_url_mapping_create_time;
drop index if exists uk_t_url_mapping_short_url;

-- 创建分表
create table if not exists public.t_url_mapping_20240418_to_20240422
    partition of public.t_url_mapping
        for values from ('2024-04-18') to ('2024-04-22');
-- 创建分表分区字段索引
create index idx_t_url_mapping_20240418_to_20240422_create_time on public.t_url_mapping_20240418_to_20240422 (create_time);
-- 创建唯一索引
create unique index if not exists uk_t_url_mapping_20240418_to_20240422_short_url on public.t_url_mapping_20240418_to_20240422 (short_url);
