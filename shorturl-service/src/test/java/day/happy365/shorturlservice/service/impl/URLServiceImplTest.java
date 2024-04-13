package day.happy365.shorturlservice.service.impl;

import day.happy365.shorturlservice.util.Base62Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MurmurHash3;
import org.junit.jupiter.api.Test;

import java.util.Date;

class URLServiceImplTest {

    @Test
    public void test1() {
        int hash32 = MurmurHash3.hash32("https://www.baidu.com");
        System.out.println("hash32: " + hash32);

        String encode = Base62Util.encode(hash32);
        System.out.println("encode: " + encode);
    }

    @Test
    public void test2() {
        String URL = "https://baidu.com";
        String secretKey = "Best wishes!";
        String message = secretKey + URL;
        String md5Crypt = DigestUtils.md5Hex(message);
        System.out.println("md5: " + md5Crypt);
    }

    @Test
    public void test3() {
        String URL = "https://www.baidu.com/getReady.html?t=1234";
        String newURL;
        if (URL.contains("?") && URL.contains("=")) {
            newURL = URL + "&shortenT=" + new Date().getTime();
        } else {
            newURL = URL + "?shortenT=" + new Date().getTime();
        }
        System.out.println("newURL: " + newURL);
    }
}
