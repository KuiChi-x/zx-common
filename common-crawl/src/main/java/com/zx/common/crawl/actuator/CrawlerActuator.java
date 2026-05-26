package com.zx.common.crawl.actuator;

import com.zx.common.crawl.enums.ActuatorStrategyEnum;
import com.zx.common.crawl.request.CrawlerRequest;
import com.zx.common.crawl.strategy.CrawlStrategy;
import com.zx.common.crawl.util.ParseUtils;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author ZhaoXu
 * @date 2022/9/22 16:59
 */
public class CrawlerActuator {
    private static final Logger log = LoggerFactory.getLogger(CrawlerActuator.class);

    public static <T> T crawl(CrawlerRequest request, Class<T> clazz) {
        if (clazz == null || !Optional.ofNullable(request).map(CrawlerRequest::getUrl).isPresent()) {
            return null;
        }
        // 不同策略执行不同的加载方式
        CrawlStrategy crawlStrategy = ActuatorStrategyEnum.getInstance(request.getStrategy());
        Document document = crawlStrategy.loadPage(request);

        return ParseUtils.parseAndMapping(document, clazz);
    }


}
