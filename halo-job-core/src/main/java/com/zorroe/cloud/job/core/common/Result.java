package com.zorroe.cloud.job.core.common;

import lombok.Data;

@Data
public class Result<T> {

    private int code;

    private String msg;

    private T data;

    // 成功
    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    // 失败
    public static <T> Result<T> fail() {
        return fail(ResultCode.FAIL.getMsg());
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.FAIL.getCode());
        result.setMsg(msg);
        return result;
    }

    // 自定义状态码
    public static <T> Result<T> build(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMsg());
        return result;
    }
}
