package day.happy365.shorturlservice.dao;

import day.happy365.shorturlservice.entity.UrlMapping;
import org.apache.ibatis.annotations.Mapper;

/**
* @author lozhu
* @description 针对表【t_url_mapping(短链接映射表)】的数据库操作Mapper
* @createDate 2024-04-13 08:42:32
* @Entity generator.domain.TUrlMapping
*/
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

    int updateVisitCount(String id);

}
