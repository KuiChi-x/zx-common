package com.zx.common.crawl.request;

import com.zx.common.crawl.enums.ActuatorStrategyEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.net.Proxy;
import java.util.Map;

/**
 * @author ZhaoXu
 */
@Data
@Builder
public class CrawlerRequest implements Serializable {
    private static final long serialVersionUID = 1840130531011692239L;
    private String url;
    private Map<String, String> params;
    private Map<String, String> cookies;
    private Map<String, String> headers;
    private String userAgent;
    private String referrer;

    @Builder.Default
    private Boolean post = Boolean.FALSE;

    @Builder.Default
    private Long timeoutMillis = 8000L;

    private Proxy proxy;

    @Builder.Default
    private ActuatorStrategyEnum strategy = ActuatorStrategyEnum.JSOUP;
}