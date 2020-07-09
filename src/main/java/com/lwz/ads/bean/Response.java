package com.lwz.ads.bean;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class Response<T> {

    private int code = 200;

    private String msg = "OK";

    private T data;

    public static <T> Response<T> success(){
        return new Response<>();
    }

    public static <T> Response<T> success(T data){
        Response<T> response = new Response<>();
        response.setData(data);
        return response;
    }

    public static <T> Response<T> with(HttpStatus httpStatus){
        Response<T> response = new Response<>();
        response.setCode(httpStatus.value());
        response.setMsg(httpStatus.getReasonPhrase());
        return response;
    }

    public static <T> Response<T> fail(int code, String msg){
        Response<T> response = new Response<>();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }

}
