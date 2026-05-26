package com.zx.common.crawl.strategy;

import com.zx.common.crawl.request.CrawlerRequest;
import org.htmlunit.BrowserVersion;
import org.htmlunit.HttpMethod;
import org.htmlunit.ProxyConfig;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.htmlunit.util.NameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author ZhaoXu
 * @date 2022/9/22 23:29
 */
public class HtmlUnitCrawlStrategy implements CrawlStrategy {
    @Override
    public Document loadPage(CrawlerRequest request) {
        if (request == null || request.getUrl() == null) {
            return null;
        }
        HtmlPage page = getHtmlPage(request);

        String pageAsXml = page.asXml();
        if (pageAsXml != null) {
            return Jsoup.parse(pageAsXml);
        }
        return null;
    }

    public static HtmlPage getHtmlPage(CrawlerRequest request) {
        try (WebClient webClient = new WebClient(BrowserVersion.FIREFOX)) {
            // 请求设置
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setJavaScriptEnabled(true);

            WebRequest webRequest = new WebRequest(new URL(request.getUrl()));
            // 请求参数
            List<NameValuePair> params = new ArrayList<>();
            Optional.ofNullable(request.getParams()).orElse(Collections.emptyMap()).forEach((k, v) -> params.add(new NameValuePair(k, v)));
            webRequest.setRequestParameters(params);

            // cookie
            Optional.ofNullable(request.getCookies()).orElse(Collections.emptyMap()).forEach((k, v) -> webClient.getCookieManager().addCookie(new Cookie("", k, v)));

            // 请求头
            Optional.ofNullable(request.getHeaders()).orElse(Collections.emptyMap()).forEach(webRequest::setAdditionalHeader);

            if (request.getUserAgent() != null) {
                webRequest.setAdditionalHeader("User-Agent", request.getUserAgent());
            }
            if (request.getReferrer() != null) {
                webRequest.setAdditionalHeader("Referer", request.getReferrer());
            }

            // 代理
            if (request.getProxy() != null) {
                InetSocketAddress address = (InetSocketAddress) request.getProxy().address();
                boolean isSocks = request.getProxy().type() == Proxy.Type.SOCKS;
                String proxyScheme = null;
                webClient.getOptions().setProxyConfig(new ProxyConfig(address.getHostName(), address.getPort(), proxyScheme, isSocks));
            }
            webRequest.setHttpMethod(request.getPost() ? HttpMethod.POST : HttpMethod.GET);

            webClient.waitForBackgroundJavaScriptStartingBefore(request.getTimeoutMillis());
            webClient.waitForBackgroundJavaScript(request.getTimeoutMillis());
            return webClient.getPage(webRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
