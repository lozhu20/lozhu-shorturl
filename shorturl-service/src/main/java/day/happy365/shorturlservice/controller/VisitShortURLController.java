package day.happy365.shorturlservice.controller;

import day.happy365.shorturlservice.entity.Response;
import day.happy365.shorturlservice.service.URLService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/")
@RestController
public class VisitShortURLController {

    @Autowired
    private URLService urlService;

    @GetMapping("/{shortURL}")
    public Response<Void> visitShortURL(HttpServletResponse response, @PathVariable String shortURL) {
        return urlService.visitShortURL(response, shortURL);
    }
}
