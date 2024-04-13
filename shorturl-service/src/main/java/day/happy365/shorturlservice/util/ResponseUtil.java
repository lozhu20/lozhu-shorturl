package day.happy365.shorturlservice.util;

import day.happy365.shorturlservice.entity.Response;

public class ResponseUtil {

    public static <T> Response<T> buildResponse(int code, String message, T data) {
        return new Response<>(code, message, data);
    }

}
