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
