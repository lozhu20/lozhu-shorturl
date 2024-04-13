package day.happy365.shorturlservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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

    private int visitCount = 0;

    private Date createTime = new Date();

    private String createBy = "system";

    private Date updateTime = new Date();

    private String updateBy = "system";

}
