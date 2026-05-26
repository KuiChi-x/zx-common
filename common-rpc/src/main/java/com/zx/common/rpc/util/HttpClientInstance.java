package com.zx.common.rpc.util;

import com.zx.common.base.utils.JsonUtils;
import com.zx.common.rpc.constant.RpcConstants;
import com.zx.common.rpc.dto.HttpClientConfigDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author ZhaoXu
 * @date 2024/6/11 17:09
 */
@Slf4j
public class HttpClientInstance {
    private static HttpClientConfigDTO httpClientConfigDTO;

    /**
     * 建立连接超时默认五秒
     */
    private static Integer DEFAULT_CONNECT_TIMEOUT = 5000;

    /**
     * 数据读写超时默认30秒
     */
    private static Integer DEFAULT_TIMEOUT = 30000;

    private static Integer DEFAULT_POOL_MAX = 200;
    private static Integer DEFAULT_MAX_PER_ROUTE = 40;

    /**
     * 默认重试3次
     */
    private static Integer DEFAULT_RETRY_COUNT = 3;

    /**
     * 默认重试间隔15秒
     */
    private static Integer DEFAULT_RETRY_INTERVAL = 15000;

    private RequestConfig requestConfig;
    private CloseableHttpClient httpClient;

    /**
     * 默认请求不压缩请求体
     */
    @Setter
    private Boolean isEnabledRequestCompress = Boolean.FALSE;

    static {
        try {
            try (InputStream resourceAsStream = HttpClientInstance.class.getClassLoader().getResourceAsStream("commons_http_config.json");) {
                if (Objects.nonNull(resourceAsStream)) {
                    int i = resourceAsStream.available();
                    byte[] bytes = new byte[i];
                    int read = resourceAsStream.read(bytes);
                    String configJson = new String(bytes);
                    if (ObjectUtils.isNotEmpty(configJson)) {
                        httpClientConfigDTO = JsonUtils.fromJson(configJson, HttpClientConfigDTO.class);
                        DEFAULT_CONNECT_TIMEOUT = Optional.ofNullable(httpClientConfigDTO).map(HttpClientConfigDTO::getDefaultConnectTimeout).orElse(DEFAULT_CONNECT_TIMEOUT);
                        DEFAULT_TIMEOUT = Optional.ofNullable(httpClientConfigDTO).map(HttpClientConfigDTO::getDefaultTimeout).orElse(DEFAULT_TIMEOUT);
                        DEFAULT_POOL_MAX = Optional.ofNullable(httpClientConfigDTO).map(HttpClientConfigDTO::getDefaultPoolMax).orElse(DEFAULT_POOL_MAX);
                        DEFAULT_MAX_PER_ROUTE = Optional.ofNullable(httpClientConfigDTO).map(HttpClientConfigDTO::getDefaultMaxPerRoute).orElse(DEFAULT_MAX_PER_ROUTE);
                        DEFAULT_RETRY_COUNT = Optional.ofNullable(httpClientConfigDTO).map(HttpClientConfigDTO::getDefaultRetryCount).orElse(DEFAULT_RETRY_COUNT);
                        DEFAULT_RETRY_INTERVAL = Optional.ofNullable(httpClientConfigDTO).map(HttpClientConfigDTO::getDefaultRetryInterval).orElse(DEFAULT_RETRY_INTERVAL);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public HttpClientInstance(Integer poolMax,
                              Integer defaultMaxPerRoute,
                              Integer requestTimeout,
                              Integer connectTimeout,
                              Integer retryCount,
                              Integer retryInterval) {
        poolMax = Optional.ofNullable(poolMax).orElse(DEFAULT_POOL_MAX);
        defaultMaxPerRoute = Optional.ofNullable(defaultMaxPerRoute).orElse(DEFAULT_MAX_PER_ROUTE);
        requestTimeout = Optional.ofNullable(requestTimeout).orElse(DEFAULT_TIMEOUT);
        retryCount = Optional.ofNullable(retryCount).orElse(DEFAULT_RETRY_COUNT);
        retryInterval = Optional.ofNullable(retryInterval).orElse(DEFAULT_RETRY_INTERVAL);
        connectTimeout = Optional.ofNullable(connectTimeout).orElse(DEFAULT_CONNECT_TIMEOUT);

        initHttpClient(poolMax, defaultMaxPerRoute, requestTimeout, connectTimeout, retryCount, retryInterval);
    }

    public HttpClientInstance() {
        initHttpClient(DEFAULT_POOL_MAX, DEFAULT_MAX_PER_ROUTE, DEFAULT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_INTERVAL);
    }

    private void initHttpClient(Integer poolMax, Integer defaultMaxPerRoute, Integer requestTimeout, Integer connectTimeout, Integer retryCount, Integer retryInterval) {
        LayeredConnectionSocketFactory sslsf = null;
        try {
            // 信任所有
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true).build();
            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
            sslsf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("创建SSL连接失败", e);
        }
        assert sslsf != null;
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        cm.setMaxTotal(poolMax);
        cm.setDefaultMaxPerRoute(defaultMaxPerRoute);

        HttpRequestRetryHandler retryHandler = getHttpRequestRetryHandler(retryCount, retryInterval);

        requestConfig = getRequestConfigWithTimeOut(connectTimeout, requestTimeout);

        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setRetryHandler(retryHandler)
                .setConnectionManagerShared(Boolean.TRUE)
                .build();
    }

    private static HttpRequestRetryHandler getHttpRequestRetryHandler(Integer retryCount, Integer retryInterval) {
        return (exception, executionCount, context) -> {
            log.error("http client instance request error, {}/{}, detail:{}", executionCount, retryCount, ((HttpClientContext) context).getRequest().toString(), exception);
            if (executionCount > retryCount) {
                return false;
            }
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return true;
        };
    }


    private static RequestConfig getRequestConfigWithTimeOut(Integer connectTimeout, Integer requestTimeout) {
        return RequestConfig.custom()
                .setSocketTimeout(requestTimeout)
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectTimeout)
                .setExpectContinueEnabled(true)
                .setCookieSpec(CookieSpecs.STANDARD)
                .setContentCompressionEnabled(Boolean.TRUE)
                .build();
    }

    /**
     * 开启请求压缩请求体功能
     */
    public void ableRequestCompress() {
        setIsEnabledRequestCompress(Boolean.TRUE);
    }

    public <T> T get(String url, Class<T> tClass) {
        return get(null, url, tClass);
    }

    public <T> T get(Map<?, ?> headers, String url, TypeReference<T> typeReference) {
        return get(headers, url, typeReference.getType());
    }

    public <T> T get(Map<?, ?> headers, String url, Map<String, Object> params, TypeReference<T> typeReference) {
        return get(headers, convertParamsUrl(url, params), typeReference.getType());
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
    public <T> T get(Map<?, ?> headers, String url, Type type) {
        // 创建get请求
        HttpGet httpGet = new HttpGet(url);
        initHttpRequest(headers, httpGet);
        return executeRequest(httpGet, type);
    }

    public <T> T post(String url, Object requestBody, Class<T> tClass) {
        return post(null, url, requestBody, tClass);
    }

    public <T> T post(Map<?, ?> headers, String url, Object requestBody, TypeReference<T> typeReference) {
        return post(headers, url, requestBody, typeReference.getType());
    }

    public <T> T post(Map<?, ?> headers, String url, Map<String, Object> params, Object requestBody, TypeReference<T> typeReference) {
        return post(headers, convertParamsUrl(url, params), requestBody, typeReference.getType());
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
    public <T> T post(Map<?, ?> headers, String url, Object requestBody, Type type) {
        // 创建post请求
        HttpPost httpRequest = new HttpPost(url);
        return executeRequestWithBody(headers, requestBody, type, httpRequest);
    }

    public <T> T put(String url, Object requestBody, Class<T> tClass) {
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
    public <T> T put(Map<?, ?> headers, String url, Object requestBody, Type type) {
        // 创建put请求
        HttpPut httpRequest = new HttpPut(url);
        return executeRequestWithBody(headers, requestBody, type, httpRequest);
    }

    public <T> T put(Map<?, ?> headers, String url, Object requestBody, TypeReference<T> typeReference) {
        return put(headers, url, requestBody, typeReference.getType());
    }

    public <T> T delete(String url, Type type) {
        return delete(null, url, null, type);
    }

    public <T> T delete(Map<?, ?> headers, String url, Object requestBody, TypeReference<T> typeReference) {
        return delete(headers, url, requestBody, typeReference.getType());
    }

    public <T> T delete(String url, Object requestBody, Type type) {
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
    public <T> T delete(Map<?, ?> headers, String url, Object requestBody, Type type) {
        // 创建delete请求，HttpDeleteWithBody 为内部类，类在下面
        HttpDeleteWithBody httpRequest = new HttpDeleteWithBody(url);
        return executeRequestWithBody(headers, requestBody, type, httpRequest);
    }

    public <T> T postBinaryFile(Map<?, ?> headers, String url, byte[] binaryData, String mimeType, Type type) {
        HttpPost httpRequest = new HttpPost(url);
        if (headers != null) {
            headers.forEach((k, v) -> httpRequest.addHeader(k.toString(), v.toString()));
        }
        httpRequest.setConfig(requestConfig);

        try {
            ContentType contentType = ContentType.create(mimeType);
            HttpEntity entity = new ByteArrayEntity(binaryData, contentType);
            httpRequest.setEntity(entity);

            return executeRequest(httpRequest, type);
        } catch (Exception e) {
            log.error("上传二进制文件失败，url：{}", url, e);
            throw new RuntimeException(e);
        }
    }

    public <T> T postBinaryFile(Map<?, ?> headers, String url, byte[] binaryData, Type type) {
        HttpPost httpRequest = new HttpPost(url);
        if (headers != null) {
            headers.forEach((k, v) -> httpRequest.addHeader(k.toString(), v.toString()));
        }
        httpRequest.setConfig(requestConfig);

        try {
            HttpEntity entity = new ByteArrayEntity(binaryData);
            httpRequest.setEntity(entity);

            return executeRequest(httpRequest, type);
        } catch (Exception e) {
            log.error("上传二进制文件失败，url：{}", url, e);
            throw new RuntimeException(e);
        }
    }

    public static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        public static final String METHOD_NAME = "DELETE";

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }

    private <T> T executeRequestWithBody(Map<?, ?> headers, Object requestBody, Type type, HttpEntityEnclosingRequestBase requestBase) {
        initHttpRequest(headers, requestBase);
        setRequestBody(requestBody, requestBase);
        return executeRequest(requestBase, type);
    }

    private void initHttpRequest(Map<?, ?> headers, HttpRequestBase httpRequestBase) {
        if (headers != null) {
            headers.forEach((k, v) -> httpRequestBase.addHeader(k.toString(), v.toString()));
        }
        httpRequestBase.addHeader("Content-Type", "application/json");
        httpRequestBase.setConfig(requestConfig);
    }

    private void setRequestBody(Object requestBody, HttpEntityEnclosingRequest entityEnclosingRequest) {
        if (requestBody != null) {
            String requestJsonString = JsonUtils.toJson(requestBody);
            HttpEntity httpEntity = new StringEntity(requestJsonString, ContentType.APPLICATION_JSON);
            Long compressThreshold = Optional.ofNullable(httpClientConfigDTO).map(HttpClientConfigDTO::getCompressThreshold).orElse(1024 * 2L);
            if (isEnabledRequestCompress && !entityEnclosingRequest.containsHeader(HttpHeaders.CONTENT_ENCODING) && httpEntity.getContentLength() >= compressThreshold) {
                httpEntity = new GzipCompressingEntity(httpEntity);
                entityEnclosingRequest.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            }
            entityEnclosingRequest.setEntity(httpEntity);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T executeRequest(HttpRequestBase httpRequestBase, Type type) {
        try (CloseableHttpResponse closeableHttpResponse = httpClient.execute(httpRequestBase)) {
            HttpEntity entity = closeableHttpResponse.getEntity();
            if (Objects.isNull(entity)) {
                return null;
            }
            String resposneString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (ObjectUtils.anyNull(type, resposneString)) {
                return null;
            }
            if (Objects.equals(type.getTypeName(), String.class.getTypeName())) {
                return (T) resposneString;
            }
            return JsonUtils.fromJson(resposneString, JsonUtils.constructType(type));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下载文件为二进制字节流
     *
     * @param fileUrl
     * @return
     */
    public byte[] downloadFileAsByteArray(Map<String, String> headers, String fileUrl) {
        // 创建一个HttpGet对象用于请求文件
        HttpGet httpGet = new HttpGet(fileUrl);
        if (ObjectUtils.isNotEmpty(headers)) {
            headers.forEach(httpGet::addHeader);
        }
        httpGet.setConfig(requestConfig);
        // 执行请求
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            // 获取响应实体
            HttpEntity entity = response.getEntity();
            // 将响应实体转换为字节数组
            if (entity != null) {
                return EntityUtils.toByteArray(entity);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new byte[0];
    }

    /**
     * 追加字节流的形式下载文件至本地，支持断点续传
     * @param headers
     * @param url
     * @param localFilePath
     */
    public void downloadFile(Map<?, ?> headers, String url, String localFilePath) {
        HttpGet httpGet = new HttpGet(url);
        initHttpRequest(headers, httpGet);

        File file = new File(localFilePath);
        long existingFileSize = file.exists() ? file.length() : 0;
        if (existingFileSize > 0) {
            httpGet.addHeader("Range", "bytes=" + existingFileSize + "-");
        }
        try (CloseableHttpResponse closeableHttpResponse = httpClient.execute(httpGet);
             InputStream inputStream = closeableHttpResponse.getEntity().getContent();
             RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.seek(existingFileSize);

            byte[] buffer = new byte[1024 * 64];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                randomAccessFile.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error("接口请求 I/O 异常，url：{}", url, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传二进制字节流到服务器
     *
     * @param fileData
     * @param targetUrl
     */
    public void uploadFile(Map<String, String> headers, byte[] fileData, String targetUrl, String newFileName) {
        try {
            HttpPost uploadFile = new HttpPost(targetUrl);
            if (ObjectUtils.isNotEmpty(headers)) {
                headers.forEach(uploadFile::addHeader);
            }

            uploadFile.setConfig(requestConfig);
            // 使用ByteArrayBody直接从byte[]创建文件内容，同时指定新的文件名
            ByteArrayBody fileBody = new ByteArrayBody(fileData, ContentType.DEFAULT_BINARY, newFileName);

            // 构建multipart/form-data请求体
            HttpEntity requestEntity = MultipartEntityBuilder.create()
                    .addPart("file", fileBody)
                    .build();

            uploadFile.setEntity(requestEntity);
            httpClient.execute(uploadFile);
        } catch (Exception e) {
            log.error("上传文件出现异常，targetUrl：{}", targetUrl, e);
            throw new RuntimeException(e);
        }
    }

    public CloseableHttpResponse head(Map<String, String> headers, String url) {
        HttpHead httpHead = new HttpHead(url);
        initHttpRequest(headers, httpHead);
        try {
            return httpClient.execute(httpHead);
        } catch (IOException e) {
            log.error("head请求异常，url：{}", url, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 转换带参数的url
     * @return
     */
    public static String convertParamsUrl(String url, Map<String, Object> params) {
        if (ObjectUtils.isNotEmpty(params)) {
            List<BasicNameValuePair> paramList = new ArrayList<>();
            params.forEach((k, v) -> {
                paramList.add(new BasicNameValuePair(k, String.valueOf(v)));
            });
            try {
                if (url.contains(RpcConstants.QUESTION)) {
                    return url + "&" + EntityUtils.toString(new UrlEncodedFormEntity(paramList, Consts.UTF_8));
                } else {
                    return url + "?" + EntityUtils.toString(new UrlEncodedFormEntity(paramList, Consts.UTF_8));
                }
            } catch (IOException ignored) {
            }
        }
        return url;
    }
}
