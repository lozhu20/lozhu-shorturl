drop table if exists public.t_url_mapping_20240423_to_20240427;
drop index if exists idx_t_url_mapping_create_time;
drop index if exists uk_t_url_mapping_short_url;

-- 创建分表
create table if not exists public.t_url_mapping_20240423_to_20240427
    partition of public.t_url_mapping
        for values from ('2024-04-23') to ('2024-04-27');
-- 创建分表分区字段索引
create index idx_t_url_mapping_20240423_to_20240427_create_time on public.t_url_mapping_20240423_to_20240427 (create_time);
-- 创建唯一索引
create unique index if not exists uk_t_url_mapping_20240423_to_20240427_short_url on public.t_url_mapping_20240423_to_20240427 (short_url);
