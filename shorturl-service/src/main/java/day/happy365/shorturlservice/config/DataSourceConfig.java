package day.happy365.shorturlservice.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

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
