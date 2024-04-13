package day.happy365.shorturlservice.job;

import day.happy365.shorturlservice.dao.UrlMappingDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExpireUrlCheckJob {

    @Autowired
    private UrlMappingDao urlMappingDao;

    /**
     * 每隔 3 分钟检查一次
     */
    @Scheduled(cron = "0 */3 * * * ?")
    public void expireUrlCheck() {

        // 更新数据库中的链接有效性
        int count = urlMappingDao.updateStatusByExpireTime();

        log.info("【短链检查】本次共失效短链 {} 条", count);

    }
}
