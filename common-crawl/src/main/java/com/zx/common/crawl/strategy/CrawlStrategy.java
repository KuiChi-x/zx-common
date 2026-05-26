package com.zx.common.crawl.strategy;

import com.zx.common.crawl.request.CrawlerRequest;
import org.jsoup.nodes.Document;

/**
 * @author ZhaoXu
 * @date 2022/9/22 23:23
 */
public interface CrawlStrategy {
    /**
     * 加载页面
     * @param request
     * @return
     */
    Document loadPage(CrawlerRequest request);
}
