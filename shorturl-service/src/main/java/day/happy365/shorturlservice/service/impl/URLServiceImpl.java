package day.happy365.shorturlservice.service.impl;

import day.happy365.shorturlservice.config.TargetDataSource;
import day.happy365.shorturlservice.constant.ResultEnum;
import day.happy365.shorturlservice.dao.UrlMappingDao;
import day.happy365.shorturlservice.entity.Response;
import day.happy365.shorturlservice.entity.UrlMapping;
import day.happy365.shorturlservice.service.AsyncTaskService;
import day.happy365.shorturlservice.service.URLService;
import day.happy365.shorturlservice.util.Base62Util;
import day.happy365.shorturlservice.util.ResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class URLServiceImpl implements URLService {

    @Value("${sys.config.domain}")
    private String sysDomain;

    @Value("${sys.config.sign}")
    private String signCheck;

    @Autowired
    private UrlMappingDao urlMappingDao;

    @Autowired
    private AsyncTaskService asyncTaskService;

    /**
     * 加签私钥
     */
    private static final String SECRET_KEY = "Best";

    /**
     * hash 碰撞时的重试次数
     */
    private static final int RETRY_TIME_WHEN_ERROR = 3;

    @TargetDataSource(value = "master")
    @Override
    public Response<String> shortenURL(String URL, String sign) {
        if (StringUtils.isBlank(URL)) {
            return ResponseUtil.buildResponse(ResultEnum.FAIL.getCode(), "URL 不能为空", null);
        }

        if ("ON".equals(signCheck)) {
            String message = SECRET_KEY + URL;
            String md5Crypt = DigestUtils.md5Hex(message);
            log.info("md5: {}", md5Crypt);
            if (!md5Crypt.equals(sign)) {
                return ResponseUtil.buildResponse(ResultEnum.FAIL.getCode(), "签名参数错误", null);
            }
        }

        int hash32 = MurmurHash3.hash32(URL);
        String shortURL = Base62Util.encode(hash32);

        log.info("【短链生成】本次生成的 shortURL 为 {}", shortURL);

        UrlMapping urlMapping = UrlMapping.builder()
                .url(URL)
                .shortUrl(shortURL)
                .expireTime(DateUtils.addYears(new Date(), 5))
                .createBy("unknown")
                .updateBy("unknown")
                .build();

        int retryTime = 0;
        while (retryTime <= RETRY_TIME_WHEN_ERROR) {
            try {
                urlMappingDao.insert(urlMapping);
                break;
            } catch (Exception e) {
                log.error("【入库出错】错误信息: {}", e.getMessage());
                log.error("【短链生成】短链入库失败，开始重试第 {} 次", (retryTime + 1));

                retryTime++;

                String newURL;
                if (URL.contains("?") && URL.contains("=")) {
                    newURL = URL + "&shortenT" + retryTime + "=" + new Date().getTime();
                } else {
                    newURL = URL + "?shortenT" + retryTime + "=" + new Date().getTime();
                }

                hash32 = MurmurHash3.hash32(newURL);
                shortURL = Base62Util.encode(hash32);
                log.info("【短链生成】本次生成的 shortURL 为 {}", shortURL);
                urlMapping.setShortUrl(shortURL);
            }
        }

        String fullShortURL = sysDomain + shortURL;

        if (retryTime <= RETRY_TIME_WHEN_ERROR) {
            return ResponseUtil.buildResponse(ResultEnum.OK.getCode(), "处理成功", fullShortURL);
        } else {
            return ResponseUtil.buildResponse(ResultEnum.FAIL.getCode(), "短链生成失败", null);
        }
    }

    @TargetDataSource(value = "slave")
    @Override
    public Response<Void> visitShortURL(HttpServletResponse response, String shortURL) {

        log.info("【短链访问】要转发 shortURL 为 {}", shortURL);

        UrlMapping urlMapping = urlMappingDao.selectByShortUrl(shortURL);
        if (urlMapping == null) {
            return ResponseUtil.buildResponse(ResultEnum.FAIL.getCode(), "该短链不存在，请先生成短链", null);
        }

        if (urlMapping.getStatus() != 0) {
            return ResponseUtil.buildResponse(ResultEnum.FAIL.getCode(), "该短链已失效，无法访问", null);
        }

        if (urlMapping.getExpireTime().before(new Date())) {
            return ResponseUtil.buildResponse(ResultEnum.FAIL.getCode(), "该短链已过期，无法访问", null);
        }

        // TODO: 2024/4/13 将访问的地址写入缓存

        String targetURL = urlMapping.getUrl();

        log.info("【短链访问】目标地址 {}", targetURL);

        asyncTaskService.updateVisitCount(urlMapping.getId());

        response.setStatus(HttpStatus.MOVED_PERMANENTLY.value());
        response.setHeader("Location", targetURL);
        return null;
    }
}
