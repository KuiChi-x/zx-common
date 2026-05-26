package com.zx.common.crawl.strategy;

import com.zx.common.crawl.request.CrawlerRequest;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;

/**
 * @author ZhaoXu
 * @date 2022/9/22 23:27
 */
public class JsoupCrawlStrategy implements CrawlStrategy {
    private static final Logger log = LoggerFactory.getLogger(JsoupCrawlStrategy.class);

    @Override
    public Document loadPage(CrawlerRequest request) {
        Connection connect = Jsoup.connect(request.getUrl());

        connect.data(Optional.ofNullable(request.getParams()).orElse(Collections.emptyMap()));
        connect.cookies(Optional.ofNullable(request.getCookies()).orElse(Collections.emptyMap()));
        connect.headers(Optional.ofNullable(request.getHeaders()).orElse(Collections.emptyMap()));
        connect.userAgent(Optional.ofNullable(request.getUserAgent()).orElse(""));
        connect.referrer(Optional.ofNullable(request.getReferrer()).orElse(""));
        connect.timeout(Optional.ofNullable(request.getTimeoutMillis()).orElse(6000L).intValue());
        connect.maxBodySize(0);
        if (request.getProxy() != null) {
            connect.proxy(request.getProxy());
        }
        Document document;
        try {
            if (request.getPost()) {
                document = connect.post();
            } else {
                document = connect.get();
            }
        } catch (Exception e) {
            log.error("jsoup请求失败，message:{}", e.getMessage());
            throw new RuntimeException(e);
        }
        return document;
    }
}
