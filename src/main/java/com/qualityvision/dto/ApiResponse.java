package com.qualityvision.dto;

import lombok.Data;

/** 统一接口返回体 */
@Data
public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(0);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    public static <T> ApiResponse<T> fail(String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(1);
        r.setMsg(msg);
        return r;
    }
}
