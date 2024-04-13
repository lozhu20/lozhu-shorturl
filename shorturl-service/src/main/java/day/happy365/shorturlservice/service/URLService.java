package day.happy365.shorturlservice.service;

import day.happy365.shorturlservice.entity.Response;
import jakarta.servlet.http.HttpServletResponse;

public interface URLService {

    Response<String> shortenURL(String URL, String sign);

    Response<Void> visitShortURL(HttpServletResponse response, String shortURL);

}
