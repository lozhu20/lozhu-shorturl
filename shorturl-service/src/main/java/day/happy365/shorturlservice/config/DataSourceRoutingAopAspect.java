package day.happy365.shorturlservice.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

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
