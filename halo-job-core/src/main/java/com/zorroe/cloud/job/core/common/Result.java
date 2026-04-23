package com.zorroe.cloud.job.core.common;

import lombok.Data;

@Data
public class Result<T> {

    private int code;

    private String msg;

    private T data;

    /**
     * 构造一个不携带业务数据的成功响应。
     *
     * @param <T> 响应数据类型
     * @return 通用成功结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 构造一个携带业务数据的成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 通用成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    /**
     * 构造一个使用默认失败文案的失败响应。
     *
     * @param <T> 响应数据类型
     * @return 通用失败结果
     */
    public static <T> Result<T> fail() {
        return fail(ResultCode.FAIL.getMsg());
    }

    /**
     * 构造一个自定义失败文案的失败响应。
     *
     * @param msg 失败提示信息
     * @param <T> 响应数据类型
     * @return 通用失败结果
     */
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.FAIL.getCode());
        result.setMsg(msg);
        return result;
    }

    /**
     * 按照指定结果码构造响应对象。
     *
     * @param resultCode 结果码枚举
     * @param <T> 响应数据类型
     * @return 基于结果码构建的响应
     */
    public static <T> Result<T> build(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMsg());
        return result;
    }
}
