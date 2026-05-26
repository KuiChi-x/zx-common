package com.zx.common.rpc.util;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author: zhaoxu
 * @description:
 */
@Slf4j
public class HttpClientUtil {
    private static final HttpClientInstance HTTP_CLIENT_INSTANCE = new HttpClientInstance();

    public static <T> T get(String url, Class<T> tClass) {
        return get(null, url, tClass);
    }

    public static <T> T get(Map<?, ?> headers, String url, TypeReference<T> typeReference) {
        return get(headers, url, typeReference.getType());
    }

    /**
     * get请求
     *
     * @param headers 请求头，可为空
     * @param url     请求地址
     * @param type    返回类型
     * @param <T>     返回类型泛型
     * @return
     */
    public static <T> T get(Map<?, ?> headers, String url, Type type) {
        return HTTP_CLIENT_INSTANCE.get(headers, url, type);
    }

    public static <T> T post(String url, Object requestBody, Class<T> tClass) {
        return post(null, url, requestBody, tClass);
    }

    public static <T> T post(Map<?, ?> headers, String url, Object requestBody, TypeReference<T> typeReference) {
        return post(headers, url, requestBody, typeReference.getType());
    }

    /**
     * post请求
     *
     * @param headers     请求头，可为空
     * @param url         请求地址
     * @param requestBody 请求体，可为空
     * @param type        返回类型
     * @param <T>         返回类型泛型
     * @return
     */
    public static <T> T post(Map<?, ?> headers, String url, Object requestBody, Type type) {
        return HTTP_CLIENT_INSTANCE.post(headers, url, requestBody, type);
    }

    public static <T> T put(String url, Object requestBody, Class<T> tClass) {
        return put(null, url, requestBody, tClass);
    }

    /**
     * put请求
     *
     * @param headers     请求头，可为空
     * @param url         请求地址
     * @param requestBody 请求体，可为空
     * @param type        返回类型
     * @param <T>         返回类型泛型
     * @return
     */
    public static <T> T put(Map<?, ?> headers, String url, Object requestBody, Type type) {
        return HTTP_CLIENT_INSTANCE.put(headers, url, requestBody, type);
    }

    public static <T> T put(Map<?, ?> headers, String url, Object requestBody, TypeReference<T> typeReference) {
        return put(headers, url, requestBody, typeReference.getType());
    }

    public static <T> T delete(String url, Type type) {
        return delete(null, url, null, type);
    }

    public static <T> T delete(Map<?, ?> headers, String url, Object requestBody, TypeReference<T> typeReference) {
        return delete(headers, url, requestBody, typeReference.getType());
    }

    public static <T> T delete(String url, Object requestBody, Type type) {
        return delete(null, url, requestBody, type);
    }

    /**
     * delete请求
     *
     * @param headers     请求头，可为空
     * @param url         请求地址
     * @param requestBody 请求体，可为空
     * @param type        返回类型
     * @param <T>         返回类型泛型
     * @return
     */
    public static <T> T delete(Map<?, ?> headers, String url, Object requestBody, Type type) {
        return HTTP_CLIENT_INSTANCE.delete(headers, url, requestBody, type);
    }

    /**
     * 下载文件为二进制字节流
     *
     * @param fileUrl
     * @return
     * @throws IOException
     */
    public static byte[] downloadFileAsByteArray(Map<String, String> headers, String fileUrl) {
        return HTTP_CLIENT_INSTANCE.downloadFileAsByteArray(headers, fileUrl);
    }

    /**
     * 上传二进制字节流到服务器
     *
     * @param fileData
     * @param targetUrl
     */
    public static void uploadFile(Map<String, String> headers, byte[] fileData, String targetUrl, String newFileName) {
        HTTP_CLIENT_INSTANCE.uploadFile(headers, fileData, targetUrl, newFileName);
    }

    public static CloseableHttpResponse head(Map<String, String> headers, String url) {
        return HTTP_CLIENT_INSTANCE.head(headers, url);
    }

    /**
     *
     * @param url 请求地址
     * @param binaryData 二进制字节流
     * @param contentType 内容类型，如 "image/png"
     * @param type 返回类型
     * @return
     * @param <T> 返回类型泛型
     */
    public static <T> T postBinaryFile(String url, byte[] binaryData, String contentType, Type type) {
        return HTTP_CLIENT_INSTANCE.postBinaryFile(null, url, binaryData, contentType, type);
    }

    public static <T> T postBinaryFile(Map<?, ?> headers, String url, byte[] binaryData, Type type) {
        return HTTP_CLIENT_INSTANCE.postBinaryFile(headers, url, binaryData, type);
    }
}