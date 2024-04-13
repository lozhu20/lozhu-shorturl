package day.happy365.shorturlservice.controller;

import day.happy365.shorturlservice.entity.Response;
import day.happy365.shorturlservice.service.URLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1")
@RestController
public class ShortenUrlController {

    @Autowired
    private URLService urlService;

    @GetMapping("/shortenURL")
    public Response<String> shortenURL(String URL, String sign) {
        return urlService.shortenURL(URL, sign);
    }
}
