package com.zx.common.crawl.enums;

import com.zx.common.crawl.strategy.CrawlStrategy;
import com.zx.common.crawl.strategy.HtmlUnitCrawlStrategy;
import com.zx.common.crawl.strategy.JsoupCrawlStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author ZhaoXu
 * @date 2022/9/22 23:21
 */
@Getter
@AllArgsConstructor
public enum ActuatorStrategyEnum {
    JSOUP(JsoupCrawlStrategy.class),
    HTML_UNIT(HtmlUnitCrawlStrategy.class),

    ;

    final Class<? extends CrawlStrategy> crawlStrategy;

    public Class<? extends CrawlStrategy> getCrawlStrategy() {
        return crawlStrategy;
    }

    public static CrawlStrategy getInstance(ActuatorStrategyEnum crawlStrategy) {
        try {
            for (ActuatorStrategyEnum value : ActuatorStrategyEnum.values()) {
                if (Objects.equals(value, crawlStrategy)) {
                    return value.getCrawlStrategy().newInstance();
                }
            }
            return JSOUP.crawlStrategy.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
