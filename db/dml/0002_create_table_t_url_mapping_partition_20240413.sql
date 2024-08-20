drop table if exists public.t_url_mapping_20240413_to_20240417;
drop index if exists idx_t_url_mapping_create_time;
drop index if exists uk_t_url_mapping_short_url;

-- 创建分表
create table if not exists public.t_url_mapping_20240413_to_20240417
    partition of public.t_url_mapping
        for values from ('2024-04-13') to ('2024-04-17');
-- 创建分表分区字段索引
create index idx_t_url_mapping_20240413_to_20240417_create_time on public.t_url_mapping_20240413_to_20240417 (create_time);
-- 创建唯一索引
create unique index if not exists uk_t_url_mapping_20240413_to_20240417_short_url on public.t_url_mapping_20240413_to_20240417 (short_url);
