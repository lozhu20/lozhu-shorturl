---
title: 动手实现一个短链接服务
date: 2024-03-09 11:45:00
categories:
  - 项目日志
tags:
  - SpringBoot
  - Vue.js
  - Docker
  - 短链接
---



## 背景

在知乎上看到一篇关于短链生成的设计文档 [系统设计之路：如何设计一个URL短链服务](https://zhuanlan.zhihu.com/p/370475544)，其中涉及到的知识点较多，包括：短链方案设计及取舍、分库分表、高可用设计等。一个看似简单的需求想要真正上线，需要需要实现功能，还要兼顾性能、安全性、可靠性等各个方面。于是想亲自上线一个短链生成服务，锻炼一下动手能力。



## 价值

短链服务在微博类网站中较为流行，使用短链服务好处包括：

1. 精简目标网址，缩短文本长度，便于记忆和传播；
2. 隐藏目标地址及参数；
3. 控制目标网址的跳转，不安全网址可以限制跳转。



## 需求

### 功能性需求

1. 实现长链接转换短链接，长度缩短且唯一；
2. 生成之后的短链接能正确跳转至原地址；
3. 短链接可以设置失效时长，默认5年；

### 非功能性需求

1. 性能：短链接跳转时长与原链接跳转时长无明显差异；
2. 安全：短链接不能被遍历；
3. 可用：不能存在单点故障。



## 系统数据量预期

写请求数：10个/s

读请求数：100个/s

5年内产生短链数量：10 * 60 * 60 * 24 * 365 * 5 = 1,576,800,000 （约16亿）

62 ^ n >= 16亿 => n >= 6

短码长度设置为6位中英文大小写字母和数字可满足需求。



## 短链生成方案

### 自增id

每次请求生成一个递增唯一的id，根据生成的id转换到62进制得到一个唯一的短链接。但是此种方式生成的短链接是有规律的，如果接口被恶意调用，可能会导致短链接被迅速消耗完，并且浪费掉大部分性能，正常的请求得不到处理。

### 普通随机数

每次请求随机生成一个随机数，再根据这个随机数进行转换到62进制得到短链接，如果该短链接已被占用，则重新生成随机数。实现起来很简单，但缺点也很明显：1.随着生成的短链接数量的增加，碰撞的概率越来越大；2.伪随机数可以被攻击者预测。

### Hash

使用 MurmurHash3 （对比 md5 随机分布性更好，发生 Hash 碰撞的几率更低，可以提高性能），对原始链接进行哈希，得到哈希值，转换成 62 进制得到短链接。如果存在碰撞，则在原始链接后增加特定后缀再进行 Hash。

⚠️ 要特别注意，hashcode 可能为负数！



## 数据库设计方案

### 数据库选择

选择 Postgresql 做为系统数据库。

### 分库方案

单条记录占用内存大小：

 (36 + 512 + 6 + 8 + 4 + 4 + 8 + 100 + 8 + 100) byte = 786 byte

5年内所有记录占用内存大小：

16 亿 * 786 byte / 1024KB / 1024MB / 1024 GB = 1.13 TB

分 3 个库，主库写，从库读，主从复制。

### 分表方案

单表记录不超过 500 万行，16 亿 / 500 万 =  320 张表，此时单表容量为 1130 GB / 320 = 3.5 GB。平均到 5 年共 60 个月，大约 5.5 天需要新增一张表，为了简表方便，按 5 天新增一张表实现。

### 表结构设计

URL映射表（t_url_mapping）：

| 字段名      | 类型         | 默认值            | 是否可空 | 是否主键 | 字段含义            | 备注     |
| ----------- | ------------ | ----------------- | -------- | -------- | ------------------- | -------- |
| id          | varchar(36)  | uuid              |          | Y        | 主键id              |          |
| url         | varchar(512) |                   |          |          | 原始URL             |          |
| short_url   | varchar(6)   |                   |          |          | 短链接              | 唯一索引 |
| expire_time | timestamp    |                   | Y        |          | 失效时间            |          |
| status      | int          | 0                 |          |          | 状态(0:正常,1:失效) |          |
| visit_count | int          | 0                 |          |          | 短链接访问次数      |          |
| create_time | timestamp    | current_timestamp |          |          | 创建时间            |          |
| create_by   | varchar(100) | unknow            |          |          | 创建人              |          |
| update_time | timestamp    | current_timestamp |          |          | 更新时间            |          |
| update_by   | varchar(100) | unknow            |          |          | 更新人              |          |

### 创建表

父表建表 SQL 如下：

```sql
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

```

创建分区表并在时间字段上加索引：

```sql
drop table if exists public.t_url_mapping_20240413_to_20240417;
drop index if exists idx_t_url_mapping_create_time;
drop index if exists uk_t_url_mapping_short_url;

-- 创建分表
create table if not exists public.t_url_mapping_20240413_to_20240417
    partition of public.t_url_mapping
        for values from ('2024-04-13') to ('2024-04-17');
-- 创建分表分区字段索引
create index idx_t_url_mapping_create_time on public.t_url_mapping_20240413_to_20240417 (create_time);
-- 创建唯一索引
create unique index if not exists uk_t_url_mapping_short_url on public.t_url_mapping_20240413_to_20240417 (short_url);

```

pg10 已经对分区表的支持已经很好了，网上一些资料中使用继承的方式创建分区表已经很过时了。

参考：[实战 PostgreSQL 分区表](https://zhuanlan.zhihu.com/p/110927990)

### 实现分区表创建自动化

需要使用触发器，在执行插入操作时如果发现分区表不存在，则先创建对应的分区表。

参考：[postgresql 自动创建分区表](https://www.cnblogs.com/shijiehaiyang/p/17406142.html)



## 技术选型

### 数据库

* Postgresql 16
* Redis 7.2

### 后端

* JDK17
* Springboot 3.1.x

### 前端

* Nginx 1.24.0
* Vue3
* Bootstrap5
* Webpack

### 容器

* Docker 



## 使用 Docker 部署数据库

创建 3 个持久化数据卷，shorturl00 为主库，其他两个为从库。

```sh
docker create volume shorturl00
docker create volume shorturl01
docker create volume shorturl02
```

创建网络

```sh
docker network create --subnet 172.12.0.0/16 --gateway 172.12.0.1 postgresqlnet
```

拉取镜像

```sh
docker pull postgres
```

运行容器

```sh
docker run -id --name=shorturl00 --network postgresqlnet --network-alias shorturl00 --ip 172.12.0.2 -v shorturl00:/var/lib/postgresql/data -p 5440:5440 -e POSTGRES_PASSWORD=12345678 -e LANG=C.UTF-8 postgres

docker run -id --name=shorturl01 --network postgresqlnet --network-alias shorturl01 --ip 172.12.0.3 -v shorturl01:/var/lib/postgresql/data -p 5441:5441 -e POSTGRES_PASSWORD=12345678 -e LANG=C.UTF-8 postgres

docker run -id --name=shorturl02 --network postgresqlnet --network-alias shorturl02 --ip 172.12.0.4 -v shorturl02:/var/lib/postgresql/data -p 5442:5442 -e POSTGRES_PASSWORD=12345678 -e LANG=C.UTF-8 postgres
```

进入容器 /var/lib/postgresql/data 目录修改 pg_hba.conf 端口

```
port=5440
```

修改 pg_hba.conf 允许外部连接

```
# IPv4 local connections:
host    all             all             0.0.0.0/0               trust
```

在容器内创建数据库 shorturl，外部连接测试。

注：在容器内编辑配置文件需要用到 vim，替换一下软件源速度会更快。

```sh
cd /etc/apt
mv sources.list.d sources.list.d.bak
# 使用中科大的源，并且不要用 https，不然会报证书错误
echo '
deb http://mirrors.ustc.edu.cn/debian/ bookworm main contrib non-free non-free-firmware
deb-src http://mirrors.ustc.edu.cn/debian/ bookworm main contrib non-free non-free-firmware
deb http://mirrors.ustc.edu.cn/debian/ bookworm-updates main contrib non-free non-free-firmware
deb-src http://mirrors.ustc.edu.cn/debian/ bookworm-updates main contrib non-free non-free-firmware
deb http://mirrors.ustc.edu.cn/debian/ bookworm-backports main contrib non-free non-free-firmware
deb-src http://mirrors.ustc.edu.cn/debian/ bookworm-backports main contrib non-free non-free-firmware
deb http://mirrors.ustc.edu.cn/debian-security/ bookworm-security main contrib non-free non-free-firmware
deb-src http://mirrors.ustc.edu.cn/debian-security/ bookworm-security main contrib non-free non-free-firmware
' > sources.list
apt-get update
```



## Docker 部署 Postgresql





## 缓存

使用缓存提高性能。生成新的短链接写入数据库的同时写入缓存中，访问短链接时首先从缓存中获取，缓存中获取不到再从数据库中获取，获取成功后将其放入缓存中。

使用 Redis 做为缓存中间件，缓存大小设置为 1G

1G / 782 byte = 137 万

137 万 / 10条 / s

缓存过期时间设置为 1 天。



## 后端接口设计

### 短链生成接口

接口名：api/v1/shortenURL

请求方式：GET

参数：

| 参数名 | 含义                       |
| ------ | -------------------------- |
| sign   | 请求签名（对参数进行编码） |
| URL    | 原始URL                    |

返回值：

| 参数           | 含义                   |
| -------------- | ---------------------- |
| code           | 0:处理成功,-1:处理失败 |
| message        | 处理结果               |
| data: shortURL | 短链接                 |

### 短链统计数据接口

接口名：api/v1/statistics

请求方式：GET

参数：

| 参数名 | 解释     |
| ------ | -------- |
| sign   | 请求签名 |

返回值：

| 参数名     | 解释                    |
| ---------- | ----------------------- |
| code       | 返回 0 时成功，否则失败 |
| message    | 处理结果                |
| data:      |                         |
| totalCount | 共生成短链条数          |

### 短链访问接口

接口名：api/v1/visitShortURL

请求方式：GET

参数：

| 参数名   | 含义   |
| -------- | ------ |
| shortURL | 短链接 |

返回值：

| 参数名  | 含义                 |
| ------- | -------------------- |
| code    | 301:跳转,-1:处理失败 |
| message | 处理结果             |
| data:   |                      |
| URL     | 原始URL              |



## 高可用

### 数据库高可用

一主两从，主从复制，主库写，从库读。

### 后端服务高可用

后端服务分布式部署，Nginx做负载均衡。



## nginx 配置

前端通过 nginx 将前端请求转发至后端，后端同时运行多个实例，通过 nginx 实现负载均衡。

docker 部署 nginx 参考文档：[Docker 安装 Nginx 容器 (完整详细版)](https://blog.csdn.net/BThinker/article/details/123507820) 

### 镜像拉取

```sh
docker pull nginx:latest
```

### 准备配置文件

nginx 配置文件放在宿主机管理，启动容器，nginx 会生成默认配置文件，将容器中的配置文件拷贝到宿主机自定义目录中。

```sh
docker run -dp 80:80 --name nginx nginx
```

建立本地 nginx 配置文件夹：

```sh
mkdir /home/lozhu/Documents/nginx_config/conf

mkdir /home/lozhu/Documents/nginx_config/html

mkdir /home/lozhu/Documents/nginx_config/log
```

拷贝配置文件至宿主机：

```sh
docker cp nginx:/etc/nginx/nginx.conf /home/lozhu/Documents/nginx_config/conf

docker cp nginx:/etc/nginx/html /home/lozhu/Documents/nginx_config/html

docker cp nginx:/etc/nginx/conf.d /home/lozhu/Documents/nginx_config/conf
```

### 启动容器

先删除之前启动的容器：

```sh
docker stop nginx

docker rm nginx
```

为了后面 nginx 能将请求转发至后端接口，将 nginx 和数据库容器、后端服务容器放在同一个网络中。

```sh
docker run -dp 80:80 \
--name nginx \
--network postgresqlnet \
--ip 172.12.0.10 \
-v /home/lozhu/Documents/nginx_config/conf/nginx.conf:/etc/nginx/nginx.conf \
-v /home/lozhu/Documents/nginx_config/conf/conf.d:/etc/nginx/conf.d \
-v /home/lozhu/Documents/nginx_config/html:/usr/share/nginx/html \
nginx:latest
```

小插曲：容器启动时路径 /home/lozhu/Documents/nginx_config/conf/conf.d 错写为 /home/lozhu/Documents/nginx_config/conf.d，少了一层 conf，容器启动后无法访问：

```
$ curl http://localhost
curl: (56) Recv failure: Connection reset by peer
```

路径修正后再启动就可以正常访问了。



## 前端项目

本来想趁此机会学习一下 React 和 Next.js ，但是发现 Next.js 相关的入门中文资料太少，中文网站上翻译都不全。一个点击按钮调用后端接口获取数据的例子都很难找到，还是到了 stackoverflow 上才看到有人贴了英文官网的文档。国内各种博客文章上来就是服务端渲染、客户端渲染，难道 Next.js 做不了交互吗？留到后面再深入学习吧。此次需求先使用熟悉的 Vue.js 来实现。

### Vue 项目搭建

创建项目：

```sh
vue init webpack shorturl-web
```

### Axios 配置

使用的 Vue.js 版本是 2.5.2，如果安装 axios 最新版本的话项目启动会报错：

```
in ../node_modules/axios/lib/platform/index.js
```

解决方法是将低 axios 版本，先删除 node_modules 目录，然后将 package.json 中 axios 版本改为 1.5.0，再重新 npm install。

参考：[Vue引入axios报错](https://blog.csdn.net/AGLQYP/article/details/136199938)

先修改 config/index.js 中的跨域设置：

```js
module.exports = {
  dev: {

    // Paths
    assetsSubDirectory: 'static',
    assetsPublicPath: '/',
    proxyTable: {
      '^/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        secure: false,
        pathRewrite: {
          '^/api': '/api'
        }
      }
    },
    ...
  }
```

在 main.js 中引入 axios

```js
import axios from 'axios'

Vue.prototype.$axios = axios
```



### 页面代码实现

主要代码：

```vue
<template>
  <div class="content">
    <div class="main-content">
      <h2>Lozhu 的在线短链服务</h2>
      <h4>累计生成短链: {{ historyData.length }}</h4>
      <div class="input-group mb-3" style="margin-top: 40px;">
        <input v-model="URL" type="text" class="form-control" placeholder="请输入链接，一次一个哦～">
        <button v-on:click="shortenURL" class="btn btn-outline-secondary" type="button" id="button-addon2">🚀
          生成短链</button>
      </div>
      <div v-if="errorMessage" class="alert alert-danger fade show" role="alert">
        {{ errorMessage }}
      </div>
      <div v-if="warningMessage" class="alert alert-warning fade show" role="alert">
        {{ warningMessage }}
      </div>
      <div style="margin-top: 50px;">
        <table class="table table-hover">
          <thead>
            <tr>
              <th>序号</th>
              <th>原始链接</th>
              <th>短链</th>
              <th>生成时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in historyData" :key="index">
              <td>{{ index + 1 }}</td>
              <td>
                <a :href=item.URL target="_blank" rel="noopener noreferrer">{{ item.URL }}</a>
              </td>
              <td>
                <a :href=item.shortURL target="_blank" rel="noopener noreferrer">{{ item.shortURL }}</a>
              </td>
              <td>{{ item.generateTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div class="footer">
      <p>
        <span>
          📝 <a :href="documentURL" target="_blank" rel="noopener noreferrer"> 项目文档</a>
        </span>
        |
        <span>
          <a :href="sourceCodeURL" target="_blank" rel="noopener noreferrer">源码</a>
        </span>
      </p>
      <p>
        <span>
          <div class="heart"></div>
        </span>
        <span>已勉强运行 {{ runningDays }} 天</span>
      </p>
      <p>©️ 2024 lozhu 保留所有权利</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      URL: '',
      shortURL: '',
      errorMessage: '',
      warningMessage: '',
      totalCount: 0,
      runningDays: 0,
      historyData: [],
      documentURL: 'https://lozhu.happy365.day',
      sourceCodeURL: 'https://lozhu.happy365.day'
    }
  },
  created() {
    let nowTime = new Date().getTime() / 1000
    // 开始运行时间: 2024-04-12
    let startTime = new Date(2024, 3, 12).getTime() / 1000
    let days = (nowTime - startTime) / (60 * 60 * 24)
    this.runningDays = parseInt(days + '')
  },
  methods: {
    shortenURL() {
      this.warningMessage = ''
      this.errorMessage = ''
      if (this.URL === '') {
        this.warningMessage = '请先输入链接哦～'
        return false
      }
      this.$axios.get('/api/v1/shortenURL', {
        params: {
          URL: this.URL,
          sign: ''
        }
      }).then((res) => {
        if (res.data && res.data.code === 0) {
          let tableRow = {
            URL: this.URL,
            shortURL: res.data.data,
            generateTime: this.formatDateTime(new Date())
          }
          this.historyData.push(tableRow)
        } else {
          this.errorMessage = res.data.message
        }
        this.URL = ''
      }).catch((err) => {
        this.errorMessage = '当前服务不可用'
        this.warningMessage = ''
      })
    },
    formatDateTime(date) {
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hour = date.getHours();
      const minute = date.getMinutes();
      const second = date.getSeconds();
      return `${year}-${this.pad(month)}-${this.pad(day)} ${this.pad(hour)}:${this.pad(minute)}:${this.pad(second)}`;
    },
    pad(num) {
      return num.toString().padStart(2, '0');
    }
  }
}
</script>

<style scoped>
</style>

```

样式直接引入 Bootstrap 实现。CSS 代码省略。

### 打包部署至 nginx

打包：

```sh
npm run build
```

打包完成之后，将 dist 路径下的 index.html 和 static 两个文件拷贝至 docker nginx 映射的宿主机 html 目录下，访问宿主机 80 端口，即可看到页面。

### Docker 镜像生成

```dockerfile
FROM nginx

EXPOSE 80

COPY /dist /usr/share/nginx/html

ENTRYPOINT nginx -g "daemon off;"

```

可能是我自己电脑性能比较低的原因，打包比较慢，要好几分钟才能跑完。

```sh
docker build -t chenxii81/shorturl-web-image:v1.1.0 .
```

打包完成后推送至 Docker Hub：

```sh
docker push chenxii81/shorturl-web-image:v1.1.0
```

运行容器：

```sh
docker run -dp 3000:80 --name shorturl-web --network postgresqlnet --ip 172.12.0.10 chenxii81/shorturl-web-image:v1.1.0
```

访问 http://localhost:3000 可以看到首页。





## Springboot + MyBatis 多数据源配置

首先引入数据库配置相关依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.11</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- 使用 aop + 注解 的方式动态切换数据源 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
</dependency>
```

实体类定义：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlMapping {

    private String id;

    private String url;

    private String shortUrl;

    private Date expireTime;

    private int status = 0;

    private Date createTime = new Date();

    private String createBy = "system";

    private Date updateTime = new Date();

    private String updateBy = "system";

}
```

数据库访问接口：

```java
@Mapper
public interface UrlMappingDao {

    int insert(UrlMapping urlMapping);

    UrlMapping selectByShortUrl(String shortURL);

    /**
     * 更新过期链接的有效性
     *
     * @return 被更新的短链数量
     */
    int updateStatusByExpireTime();

}

```

```java
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="day.happy365.shorturlservice.dao.UrlMappingDao">

    <resultMap id="BaseResultMap" type="day.happy365.shorturlservice.entity.UrlMapping">
        <id property="id" column="id" jdbcType="VARCHAR"/>
        <result property="url" column="url" jdbcType="VARCHAR"/>
        <result property="shortUrl" column="short_url" jdbcType="VARCHAR"/>
        <result property="expireTime" column="expire_time" jdbcType="TIMESTAMP"/>
        <result property="status" column="status" jdbcType="INTEGER"/>
        <result property="createTime" column="create_time" jdbcType="TIMESTAMP"/>
        <result property="createBy" column="create_by" jdbcType="VARCHAR"/>
        <result property="updateTime" column="update_time" jdbcType="TIMESTAMP"/>
        <result property="updateBy" column="update_by" jdbcType="VARCHAR"/>
    </resultMap>

    <sql id="Base_Column_List">
        id,url,short_url,
        expire_time,status,create_time,
        create_by,update_time,update_by
    </sql>

    <insert id="insert" parameterType="day.happy365.shorturlservice.entity.UrlMapping">
        insert into public.t_url_mapping(url, short_url, expire_time, create_by, update_by)
        values (#{url},
                #{shortUrl},
                #{expireTime},
                #{createBy},
                #{updateBy});
    </insert>

    <update id="updateStatusByExpireTime">
        update public.t_url_mapping
        set status = 1,
            update_time = now(),
            update_by = 'ExpiredURLCheckJob'
        where expire_time <![CDATA[ <= ]]> now()::timestamp
    </update>

    <select id="selectByShortUrl" resultMap="BaseResultMap">
        select
        <include refid="Base_Column_List"/>
        from public.t_url_mapping
        where short_url = #{shortURL}
        order by create_time desc
        limit 1
    </select>
</mapper>

```



数据库配置：

```yaml
spring:
  application:
    name: shorturl-service
  datasource:
    # 主库，用于写
    shorturl00:
      jdbc-url: jdbc:postgresql://vm1:5440/shorturl
      username: postgres
      password: *******
      driver-class-name: org.postgresql.Driver
    # 从库1，用于读
    shorturl01:
      jdbc-url: jdbc:postgresql://vm1:5441/shorturl
      username: postgres
      password: *******
      driver-class-name: org.postgresql.Driver
    # 从库2，用于读
    shorturl02:
      jdbc-url: jdbc:postgresql://vm1:5442/shorturl
      username: postgres
      password: *******
      driver-class-name: org.postgresql.Driver
```

数据源配置类：

```java
/**
 * 主从数据源配置
 */
@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "day.happy365.shorturlservice.dao", sqlSessionFactoryRef = "sqlSessionFactory")
public class DataSourceConfig {

    @Bean(name = "masterDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.shorturl00")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "slaveDataSource1")
    @ConfigurationProperties(prefix = "spring.datasource.shorturl01")
    public DataSource slaveDataSource1() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "slaveDataSource2")
    @ConfigurationProperties(prefix = "spring.datasource.shorturl02")
    public DataSource slaveDataSource2() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean("dynamicDataSource")
    public DynamicRoutingDataSource dynamicDataSource(@Qualifier(value = "masterDataSource") DataSource masterDataSource,
                                                      @Qualifier(value = "slaveDataSource1") DataSource slaveDataSource1,
                                                      @Qualifier(value = "slaveDataSource2") DataSource slaveDataSource2) {
        Map<Object, Object> targetDataSources = new HashMap<>(3);
        targetDataSources.put(DynamicRoutingDataSourceContext.MASTER, masterDataSource);
        targetDataSources.put(DynamicRoutingDataSourceContext.SLAVE1, slaveDataSource1);
        targetDataSources.put(DynamicRoutingDataSourceContext.SLAVE2, slaveDataSource2);
        DynamicRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();
        // 设置数据源
        dynamicRoutingDataSource.setTargetDataSources(targetDataSources);
        // 设置默认选择的数据源
        dynamicRoutingDataSource.setDefaultTargetDataSource(masterDataSource);
        dynamicRoutingDataSource.afterPropertiesSet();
        return dynamicRoutingDataSource;
    }

    @Bean(name = "sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dynamicDataSource") DataSource dynamicDataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dynamicDataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:**/*.xml"));
        return bean.getObject();
    }

}
```

维护一个数据源 Map：

```java
public class DynamicRoutingDataSourceContext {

    public static final String MASTER = "master";

    public static final String SLAVE1 = "slave1";

    public static final String SLAVE2 = "slave2";

    private static final ThreadLocal<String> THREAD_LOCAL_DATA_SOURCE = new ThreadLocal<>();

    public static void setRoutingDataSource(String dataSource) {
        if (dataSource == null) {
            throw new NullPointerException();
        }
        THREAD_LOCAL_DATA_SOURCE.set(dataSource);
    }

    public static String getRoutingDataSource() {
        String dataSourceType = THREAD_LOCAL_DATA_SOURCE.get();
        if (dataSourceType == null) {
            THREAD_LOCAL_DATA_SOURCE.set(DynamicRoutingDataSourceContext.MASTER);
            return getRoutingDataSource();
        }
        return dataSourceType;
    }

    public static void removeRoutingDataSource() {
        THREAD_LOCAL_DATA_SOURCE.remove();
    }
}

```

继承 AbstractRoutingDataSource 类实现 determineCurrentLookupKey 方法：

```java
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String routingDataSource = DynamicRoutingDataSourceContext.getRoutingDataSource();
        log.info("【动态数据源】本次使用数据库: {}", routingDataSource);
        return routingDataSource;
    }

}

```

增加一个 TargetDataSource 注解，在执行数据库操作时指定数据库，使用起来更方便：

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {
    String value();
}

```

根据自定义注解的值切换数据源：

```java
@Order(0)
@Aspect
@Component
public class DataSourceRoutingAopAspect {

    @Around("@annotation(targetDataSource)")
    public Object routingWithDataSource(ProceedingJoinPoint joinPoint, TargetDataSource targetDataSource) throws Throwable {
        try {
            String value = targetDataSource.value();
            if ("slave".equals(value)) {
                if (new Random(7).nextInt() % 2 == 0) {
                    DynamicRoutingDataSourceContext.setRoutingDataSource(DynamicRoutingDataSourceContext.SLAVE1);
                } else {
                    DynamicRoutingDataSourceContext.setRoutingDataSource(DynamicRoutingDataSourceContext.SLAVE2);
                }
            } else {
                DynamicRoutingDataSourceContext.setRoutingDataSource(value);
            }
            return joinPoint.proceed();
        } finally {
            DynamicRoutingDataSourceContext.removeRoutingDataSource();
        }
    }
}

```

⚠️ 最后还有最重要的一步，从启动类上排除 SpringBoot 的数据源相关的自动配置，否则会报找不到数据库配置的错误。没意识到这点，在这里卡了半个小时。

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@EnableScheduling
@EnableAsync
public class ShorturlServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShorturlServiceApplication.class, args);
    }

}

```



## Docker 部署后端服务

### jar 包生成

```sh
mvn clean

mvn package -DskipTests
```



### Dockerfile 编写

```dockerfile
FROM openjdk:17-oracle

ADD target/shorturl-service-v1.1.0.jar /shorturl-service-v1.1.0.jar

RUN bash -c 'touch /shorturl-service-v1.1.0.jar'

ENV dataSource1Url=""
ENV dataSource2Url=""
ENV dataSource3Url=""
ENV dataSourceUsername=""
ENV dataSourcePassword=""
ENV sysConfigDomain=""

EXPOSE 8000

MAINTAINER chenxii81

ENTRYPOINT ["java", "-Dspring.datasource.shorturl00.jdbc-url=${dataSource1Url}", "-Dspring.datasource.shorturl00.username=${dataSourceUsername}", "-Dspring.datasource.shorturl00.password=${dataSourcePassword}",  "-Dspring.datasource.shorturl01.jdbc-url=${dataSource2Url}", "-Dspring.datasource.shorturl01.username=${dataSourceUsername}", "-Dspring.datasource.shorturl01.password=${dataSourcePassword}", "-Dspring.datasource.shorturl02.jdbc-url=${dataSource3Url}", "-Dspring.datasource.shorturl02.username=${dataSourceUsername}", "-Dspring.datasource.shorturl02.password=${dataSourcePassword}", "-Dsys.config.domain=${sysConfigDomain}", "-jar", "/shorturl-service-v1.1.0.jar"]

```

**要注意 ENTRYPOINT 括号内不能有换行。**

### 打包

```sh
docker build -t chenxii81/shorturl-service-app:v1.1.0 .
```

推送至 Docker Hub：

```sh
docker push chenxii81/shorturl-service-app:v1.1.0   
```

运行镜像：

```sh
docker run -d --name shorturl-service \
--network postgresqlnet --ip 172.12.0.10 \
-e dataSource1Url="jdbc:postgresql://pg:172.12.0.2:5440/shorturl" \
-e dataSource2Url="jdbc:postgresql://pg:172.12.0.3:5441/shorturl" \
-e dataSource3Url="jdbc:postgresql://pg:172.12.0.4:5442/shorturl" \
-e dataSourceUsername="postgres" \
-e dataSourcePassword="12345678" \
-e sysConfigDomain="http://localhost:9000/" \
-p 9000:8000 \
chenxii81/shorturl-service-app:v1.1.0
```



## nginx 转发配置

配置 nginx 请求后端接口：





## 问题记录

报错1：

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.

Reason: Failed to determine a suitable driver class


Action:

Consider the following:
	If you want an embedded database (H2, HSQL or Derby), please put it on the classpath.
	If you have database settings to be loaded from a particular profile you may need to activate it (the profiles dev are currently active).


Process finished with exit code 1
```

确认数据库配置无误，但是启动一直报错找不到数据库相关的配置。可参考：[Spring boot遇坑之配置数据源启动报错](https://blog.csdn.net/qq_39629277/article/details/97782030)

需要从启动类上排除数据库自动配置。

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@EnableScheduling
@EnableAsync
public class ShorturlServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShorturlServiceApplication.class, args);
    }

}
```



报错2：

```
Caused by: java.io.FileNotFoundException: class path resource [**/*.xml] cannot be opened because it does not exist
```

可参考 [Springboot 项目 无法读取resources下的mapper文件夹的.xml文件](https://www.cnblogs.com/jev-0987/p/12839193.html) 中的说明：

> 关于PathMatchingResourcePatternResolver : https://www.cnblogs.com/loveLands/articles/9863195.html
> 果然getResource 和 getResources不同
>
> getResource()：
> 1.从类的根路径下获取文件
> getResources():
> 1.获取所有类路径下的指定文件
> 可以通过classpath*前缀指定，从所有的类路径下获取指定文件，与classpath前缀的区别是classpath前缀只能获取当类路径下的资源文件，而classpath*前缀可以获取所有类路径下的资源文件，包括jar包中的。



报错3：项目正常启动后访问接口报 404

检查了 Controller 类上 RequestMapping 路径和方法上的 RequestMapping 路径都没问题，启动日志里的启动端口正常，确认 context-path=''。排查了半天之后发现，是刚才在解决数据源启动报错时在 Application 类上加了 @ComponentScan，并且只指向了 dao 的路径，导致其他 controller bean 没有加载到容器里，删除启动类上的 @ComponentScan 之后重启项目，就能正常访问到接口了。



报错4：通过 Docker Desktop 推送前端项目 docker 镜像至镜像仓库时报错：

```
denied: requested access to the resource is denied
```

在 stackoverflow 找到解决方案：镜像名称需要包含自己的 docker id 才能推送。改一下镜像名称，重新打包推送即可。

```
docker build -t your-docker-id/image-name:tag .
```





## 其他问题记录

### hashcode 可能为负数

数据量较大的场景下，不同的长链接生成的 hashcode 可能会重复，进而导致生成的短链接重复。为了避免这种情况，在发现短链接重复时，需要进行重试，我采用的方案是发现重复时直接在原始链接后面拼接一个当前时间戳的参数，再重新生成。这里有个坑，就是生成的短链接 hashcode 可能为负数，导致 base62 转换时数组越界。

解决方案也很简单，就是在进行 base62 转码时，先将负的 hashcode 转为正的。本来想简单直接取绝对值，后来发现有更好的实现方式：直接用位操作去掉负号。参考 [hashcode返回值可能为负数](https://www.cnblogs.com/k-class/p/13773161.html)

```
long hash = key.hashCode() & Integer.MAX_VALUE; // caution, make value not minus
```

### @Async 失效问题

开发过程中想把每个短链接访问次数记录下来，于是又加了一个字段：visit_count。每个短链接被访问时就将此值加 1。想把更新操作放在主库中异步去更新，遇到了这个问题。

这算是一个老生常谈的问题了，先排查以下几点：

* 启动类上是否加了 @EnableAsync
* 异步方法的返回值只能是 void 或 Future
* 没有走 Spring 的代理类

3 个问题中两个 😭

参考：[Spring boot 注解@Async无效,不起作用](https://blog.csdn.net/YoungLee16/article/details/88398045)

