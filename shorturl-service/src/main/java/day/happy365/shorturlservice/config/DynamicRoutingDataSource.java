package day.happy365.shorturlservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String routingDataSource = DynamicRoutingDataSourceContext.getRoutingDataSource();
        log.info("【动态数据源】本次使用数据库: {}", routingDataSource);
        return routingDataSource;
    }

}
