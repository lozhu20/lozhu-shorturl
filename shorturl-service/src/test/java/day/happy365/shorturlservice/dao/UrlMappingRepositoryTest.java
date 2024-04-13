package day.happy365.shorturlservice.dao;

import day.happy365.shorturlservice.entity.UrlMapping;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class UrlMappingRepositoryTest {

    @Autowired
    private UrlMappingDao repository;

    @Test
    public void createUrlMapping() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setUrl("https://baidu.com");
        urlMapping.setExpireTime(new Date());
        repository.insert(urlMapping);
    }

    @Test
    public void query() {
        UrlMapping shortUrl = repository.selectByShortUrl("123456");
        log.info("shortUrl: {}", shortUrl);
    }
}