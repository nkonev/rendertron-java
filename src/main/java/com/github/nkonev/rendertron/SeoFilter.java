package com.github.nkonev.rendertron;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeoFilter implements Filter {
    public static final List<String> PARAMETER_NAMES = Arrays.asList(
            Constants.InitFilterParams.RENDERTRON_EVENT_HANDLER, Constants.InitFilterParams.PROXY,
            Constants.InitFilterParams.PROXY_PORT, Constants.InitFilterParams.PRERENDER_TOKEN,
            Constants.InitFilterParams.FORWARDED_URL_HEADER, Constants.InitFilterParams.FORWARDED_URL_PREFIX_HEADER,
            Constants.InitFilterParams.FORWARDED_URL_PREFIX, Constants.InitFilterParams.CRAWLER_USER_AGENTS,
            Constants.InitFilterParams.EXTENSIONS_TO_IGNORE, Constants.InitFilterParams.WHITELIST,
            Constants.InitFilterParams.BLACKLIST, Constants.InitFilterParams.RENDERTRON_SERVICE_URL
    );
    private SeoService seoService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.seoService = new SeoService(toMap(filterConfig));
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        boolean isPrerendered = seoService.prerenderIfEligible(
                (HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
        if (!isPrerendered) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
        seoService.destroy();
    }

    protected void setSeoService(SeoService seoService) {
        this.seoService = seoService;
    }

    protected Map<String, String> toMap(FilterConfig filterConfig) {
        Map<String, String> config = new HashMap<String, String>();
        for (String parameterName : PARAMETER_NAMES) {
            config.put(parameterName, filterConfig.getInitParameter(parameterName));
        }
        return config;
    }
}

