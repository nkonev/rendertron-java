package com.github.greengerong;

public class PreRenderConstants {

    public static class InitFilterParams {
        public static final String PRE_RENDER_EVENT_HANDLER = "preRenderEventHandler";
        public static final String PROXY = "proxy";
        public static final String PROXY_PORT = "proxyPort";
        public static final String PRERENDER_TOKEN = "prerenderToken";
        public static final String FORWARDED_URL_HEADER = "forwardedURLHeader";
        public static final String FORWARDED_URL_PREFIX_HEADER = "forwardedURLPrefixHeader";
        public static final String FORWARDED_URL_PREFIX = "forwardedURLPrefix";
        public static final String CRAWLER_USER_AGENTS = "crawlerUserAgents";
        public static final String EXTENSIONS_TO_IGNORE = "extensionsToIgnore";
        public static final String WHITELIST = "whitelist";
        public static final String BLACKLIST = "blacklist";
        public static final String PRERENDER_SERVICE_URL = "prerenderServiceUrl";
    }

    public static class SystemParams {
        public static final String PRERENDER_SERVICE_URL = "PRERENDER_SERVICE_URL";
    }
}
