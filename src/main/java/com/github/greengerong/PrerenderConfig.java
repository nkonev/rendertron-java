package com.github.greengerong;


import com.google.common.collect.Lists;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PrerenderConfig {
    private final static Logger log = LoggerFactory.getLogger(PrerenderConfig.class);
    public static final String PRERENDER_IO_SERVICE_URL = "http://service.prerender.io/";
    private Map<String, String> config;

    public PrerenderConfig(Map<String, String> config) {
        this.config = config;
    }

    public PreRenderEventHandler getEventHandler() {
        final String preRenderEventHandler = config.get(PreRenderConstants.InitFilterParams.PRE_RENDER_EVENT_HANDLER);
        if (isNotBlank(preRenderEventHandler)) {
            try {
                return (PreRenderEventHandler) Class.forName(preRenderEventHandler).newInstance();
            } catch (Exception e) {
                log.error("PreRenderEventHandler class not find or can not new a instance", e);
            }
        }
        return null;
    }

    public CloseableHttpClient getHttpClient() {
        HttpClientBuilder builder = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .disableRedirectHandling();

        configureProxy(builder);
        configureTimeout(builder);
        return builder.build();
    }

    private HttpClientBuilder configureProxy(HttpClientBuilder builder) {
        final String proxy = config.get(PreRenderConstants.InitFilterParams.PROXY);
        if (isNotBlank(proxy)) {
            final int proxyPort = Integer.parseInt(config.get(PreRenderConstants.InitFilterParams.PROXY_PORT));
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(new HttpHost(proxy, proxyPort));
            builder.setRoutePlanner(routePlanner);
        }
        return builder;
    }

    private HttpClientBuilder configureTimeout(HttpClientBuilder builder) {
        final String socketTimeout = getSocketTimeout();
        if (socketTimeout != null) {
            RequestConfig config = RequestConfig.custom().setSocketTimeout(Integer.parseInt(socketTimeout)).build();
            builder.setDefaultRequestConfig(config);
        }
        return builder;
    }

    public String getSocketTimeout() {
        return config.get("socketTimeout");
    }

    public String getPrerenderToken() {
        return config.get(PreRenderConstants.InitFilterParams.PRERENDER_TOKEN);
    }

    public String getForwardedURLHeader() {
        return config.get(PreRenderConstants.InitFilterParams.FORWARDED_URL_HEADER);
    }

    public String getForwardedURLPrefixHeader() {
        return config.get(PreRenderConstants.InitFilterParams.FORWARDED_URL_PREFIX_HEADER);
    }

    public List<String> getCrawlerUserAgents() {
        List<String> crawlerUserAgents = Lists.newArrayList("baiduspider",
                "facebookexternalhit", "twitterbot", "rogerbot", "linkedinbot", "embedly", "quora link preview",
                "showyoubo", "outbrain", "pinterest", "developers.google.com/+/web/snippet", "slackbot", "vkShare",
                "W3C_Validator", "redditbot", "Applebot", "yandex", "Googlebot");
        final String crawlerUserAgentsFromConfig = config.get(PreRenderConstants.InitFilterParams.CRAWLER_USER_AGENTS);
        if (isNotBlank(crawlerUserAgentsFromConfig)) {
            crawlerUserAgents.addAll(Arrays.asList(crawlerUserAgentsFromConfig.trim().split(",")));
        }

        return crawlerUserAgents;
    }

    public List<String> getExtensionsToIgnore() {
        List<String> extensionsToIgnore = Lists.newArrayList(".js", ".json", ".css", ".xml", ".less", ".png", ".jpg",
                ".jpeg", ".gif", ".pdf", ".doc", ".txt", ".ico", ".rss", ".zip", ".mp3", ".rar", ".exe", ".wmv",
                ".doc", ".avi", ".ppt", ".mpg", ".mpeg", ".tif", ".wav", ".mov", ".psd", ".ai", ".xls", ".mp4",
                ".m4a", ".swf", ".dat", ".dmg", ".iso", ".flv", ".m4v", ".torrent", ".woff", ".ttf");
        final String extensionsToIgnoreFromConfig = config.get(PreRenderConstants.InitFilterParams.EXTENSIONS_TO_IGNORE);
        if (isNotBlank(extensionsToIgnoreFromConfig)) {
            extensionsToIgnore.addAll(Arrays.asList(extensionsToIgnoreFromConfig.trim().split(",")));
        }

        return extensionsToIgnore;
    }

    public List<String> getWhitelist() {
        final String whitelist = config.get(PreRenderConstants.InitFilterParams.WHITELIST);
        if (isNotBlank(whitelist)) {
            return Arrays.asList(whitelist.trim().split(","));
        }
        return null;
    }

    public List<String> getBlacklist() {
        final String blacklist = config.get(PreRenderConstants.InitFilterParams.BLACKLIST);
        if (isNotBlank(blacklist)) {
            return Arrays.asList(blacklist.trim().split(","));
        }
        return null;
    }

    public String getPrerenderServiceUrl() {
        final String prerenderServiceUrl = config.get(PreRenderConstants.InitFilterParams.PRERENDER_SERVICE_URL);
        return isNotBlank(prerenderServiceUrl) ? prerenderServiceUrl : getDefaultPrerenderIoServiceUrl();
    }

    private String getDefaultPrerenderIoServiceUrl() {
        final String prerenderServiceUrlInEnv = System.getProperty(PreRenderConstants.SystemParams.PRERENDER_SERVICE_URL);
        return isNotBlank(prerenderServiceUrlInEnv) ? prerenderServiceUrlInEnv : PRERENDER_IO_SERVICE_URL;
    }

    public String getForwardedURLPrefix() {
        return config.get(PreRenderConstants.InitFilterParams.FORWARDED_URL_PREFIX);
    }
}
