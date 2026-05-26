package com.zx.common.rpc.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ZhaoXu
 * @date 2024/9/24 10:51
 */
@Data
public class HttpClientConfigDTO implements Serializable {
    private static final long serialVersionUID = 1493857621423451046L;

    /**
     * 压缩阈值
     */
    private Long compressThreshold;

    /**
     * 建立连接超时默认
     */
    private Integer defaultConnectTimeout;

    /**
     * 数据读写超时默认
     */
    private Integer defaultTimeout;

    private Integer defaultPoolMax;
    private Integer defaultMaxPerRoute;

    /**
     * 默认重试次
     */
    private Integer defaultRetryCount;

    /**
     * 默认重试间隔秒
     */
    private Integer defaultRetryInterval;
}
