package com.github.nkonev.rendertron;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.HOST;

public class SeoService {
    private final static Logger log = LoggerFactory.getLogger(SeoService.class);
    /**
     * These are the "hop-by-hop" headers that should not be copied.
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
     * I use an HttpClient HeaderGroup class instead of Set<String> because this
     * approach does case insensitive lookup faster.
     */
    private static final HeaderGroup hopByHopHeaders;
    private CloseableHttpClient httpClient;
    private Config config;
    private EventHandler eventHandler;

    public SeoService(Map<String, String> config) {
        this.config = new Config(config);
        this.httpClient = getHttpClient();
        this.eventHandler = this.config.getEventHandler();
    }

    static {
        hopByHopHeaders = new HeaderGroup();
        String[] headers = new String[]{
                "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization",
                "TE", "Trailers", "Transfer-Encoding", "Upgrade"};
        for (String header : headers) {
            hopByHopHeaders.addHeader(new BasicHeader(header, null));
        }
    }

    public void destroy() {
        if (eventHandler != null) {
            eventHandler.destroy();
        }
        closeQuietly(httpClient);
    }

    public boolean renderIfEligible(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        try {
            if (handleRender(servletRequest, servletResponse)) {
                return true;
            }
        } catch (Exception e) {
            log.error("Render service error", e);
        }
        return false;
    }

    private boolean handleRender(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws URISyntaxException, IOException {
        if (shouldShowRenderedPage(servletRequest)) {
            if (beforeRender(servletRequest, servletResponse) || proxyRenderedPageResponse(servletRequest, servletResponse)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldShowRenderedPage(HttpServletRequest request) throws URISyntaxException {
        final String userAgent = request.getHeader("User-Agent");
        final String url = getRequestURL(request);
        final String referer = request.getHeader("Referer");

        log.trace(String.format("checking request for %s from User-Agent %s and referer %s", url, userAgent, referer));

        if (!HttpGet.METHOD_NAME.equals(request.getMethod())) {
            log.trace("Request is not HTTP GET; intercept: no");
            return false;
        }

        if (isInResources(url)) {
            log.trace("request is for a (static) resource; intercept: no");
            return false;
        }

        final List<String> whiteList = config.getWhitelist();
        if (whiteList != null && !isInWhiteList(url, whiteList)) {
            log.trace("Whitelist is enabled, but this request is not listed; intercept: no");
            return false;
        }

        final List<String> blacklist = config.getBlacklist();
        if (blacklist != null && isInBlackList(url, referer, blacklist)) {
            log.trace("Blacklist is enabled, and this request is listed; intercept: no");
            return false;
        }

        if (StringUtils.isBlank(userAgent)) {
            log.trace("Request has blank userAgent; intercept: no");
            return false;
        }

        if (!isInSearchUserAgent(userAgent)) {
            log.trace("Request User-Agent is not a search bot; intercept: no");
            return false;
        }

        log.trace(String.format("Defaulting to request intercept(user-agent=%s): yes", userAgent));
        return true;
    }

    protected HttpGet getHttpGet(String apiUrl) {
        return new HttpGet(apiUrl);
    }

    protected CloseableHttpClient getHttpClient() {
        return config.getHttpClient();
    }

    /**
     * Copy request headers from the servlet client to the proxy request.
     *
     * @throws java.net.URISyntaxException
     */
    private void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest)
            throws URISyntaxException {
        // Get an Enumeration of all of the header names sent by the client
        Enumeration<?> enumerationOfHeaderNames = servletRequest.getHeaderNames();
        while (enumerationOfHeaderNames.hasMoreElements()) {
            String headerName = (String) enumerationOfHeaderNames.nextElement();
            //Instead the content-length is effectively set via InputStreamEntity
            if (!headerName.equalsIgnoreCase(CONTENT_LENGTH) && !hopByHopHeaders.containsHeader(headerName)) {
                Enumeration<?> headers = servletRequest.getHeaders(headerName);
                while (headers.hasMoreElements()) {//sometimes more than one value
                    String headerValue = (String) headers.nextElement();
                    // In case the proxy host is running multiple virtual servers,
                    // rewrite the Host header to ensure that we get content from
                    // the correct virtual server
                    if (headerName.equalsIgnoreCase(HOST)) {
                        HttpHost host = URIUtils.extractHost(new URI(config.getServiceUrl()));
                        headerValue = host.getHostName();
                        if (host.getPort() != -1) {
                            headerValue += ":" + host.getPort();
                        }
                    }
                    proxyRequest.addHeader(headerName, headerValue);
                }
            }
        }
    }

    private String getRequestURL(HttpServletRequest request) {

        if (config.getForwardedURLPrefixHeader() != null) {
            String url = request.getHeader(config.getForwardedURLPrefixHeader());
            if (url != null) {
                return url + request.getRequestURI();
            }
        }

        if (config.getForwardedURLHeader() != null) {
            String url = request.getHeader(config.getForwardedURLHeader());
            if (url != null) {
                return url;
            }
        }

        if (config.getForwardedURLPrefix() != null) {
            String url = config.getForwardedURLPrefix();
            if (url != null) {
                return url + request.getRequestURI();
            }
        }

        return request.getRequestURL().toString();
    }

    private String getApiUrl(String url) {
        String renderServiceUrl = config.getServiceUrl();
        if (!renderServiceUrl.endsWith("/")) {
            renderServiceUrl += "/";
        }
        return renderServiceUrl + url;
    }

    /**
     * Copy proxied response headers back to the servlet client.
     */
    private void copyResponseHeaders(HttpResponse proxyResponse, final HttpServletResponse servletResponse) {
        servletResponse.setCharacterEncoding(getContentCharSet(proxyResponse.getEntity()));
        for (Header proxyResponseHeader: proxyResponse.getAllHeaders()){
            if (shouldCopyHeader(proxyResponseHeader)){
                servletResponse.addHeader(proxyResponseHeader.getName(), proxyResponseHeader.getValue());
            }
        }
    }

    private boolean shouldCopyHeader(Header header) {
        return !hopByHopHeaders.containsHeader(header.getName());
    }
    
    /**
     * Get the charset used to encode the http entity.
     */
    private String getContentCharSet(final HttpEntity entity) throws ParseException {
        if (entity == null) {
            return null;
        }
        String charset = null;
        if (entity.getContentType() != null) {
            HeaderElement values[] = entity.getContentType().getElements();
            if (values.length > 0) {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) {
                    charset = param.getValue();
                }
            }
        }        
        return charset;
    }

    private String getResponseHtml(HttpResponse proxyResponse)
            throws IOException {
        HttpEntity entity = proxyResponse.getEntity();
        return entity != null ? EntityUtils.toString(entity) : "";
    }

    /**
     * Copy response body data (the entity) from the proxy to the servlet client.
     */
    private void responseEntity(String html, HttpServletResponse servletResponse)
            throws IOException {
        PrintWriter printWriter = servletResponse.getWriter();
        try {
            printWriter.write(html);
            printWriter.flush();
        } finally {
            closeQuietly(printWriter);
        }
    }


    protected void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            log.error("Close proxy error", e);
        }
    }

    private boolean isInBlackList(final String url, final String referer, List<String> blacklist) {
        for (String regex: blacklist) {
            final Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher(url).matches() ||
                    (!StringUtils.isBlank(referer) && pattern.matcher(referer).matches())){
                return true;
            }
        }
        return false;
    }

    private boolean isInSearchUserAgent(final String userAgent) {
        for(String item: config.getCrawlerUserAgents()){
            if (userAgent.toLowerCase().contains(item.toLowerCase())){
                return true;
            }
        }
        return false;
    }


    private boolean isInResources(final String url) {
        for(String item: config.getExtensionsToIgnore()){
            if ((url.indexOf('?') >= 0 ? url.substring(0, url.indexOf('?')) : url)
                    .toLowerCase().endsWith(item)){
                return true;
            }
        }
        return false;
    }

    private boolean isInWhiteList(final String url, List<String> whitelist) {
        for (String regex: whitelist) {
            if (Pattern.compile(regex).matcher(url).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean beforeRender(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (eventHandler != null) {
            final String html = eventHandler.beforeRender(request);
            if (isNotBlank(html)) {
                final PrintWriter writer = response.getWriter();
                writer.write(html);
                writer.flush();
                closeQuietly(writer);
                return true;
            }
        }
        return false;
    }

    private boolean proxyRenderedPageResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException, URISyntaxException {
        final String apiUrl = getApiUrl(getFullUrl(request));
        log.trace(String.format("Render proxy will send request to:%s", apiUrl));
        final HttpGet getMethod = getHttpGet(apiUrl);
        copyRequestHeaders(request, getMethod);
        CloseableHttpResponse prerenderServerResponse = null;

        try {
            prerenderServerResponse = httpClient.execute(getMethod);
            response.setStatus(prerenderServerResponse.getStatusLine().getStatusCode());
            copyResponseHeaders(prerenderServerResponse, response);
            String html = getResponseHtml(prerenderServerResponse);
            html = afterRender(request, response, prerenderServerResponse, html);
            responseEntity(html, response);
            return true;
        } finally {
            closeQuietly(prerenderServerResponse);
        }
    }

    private String afterRender(HttpServletRequest clientRequest, HttpServletResponse clientResponse, CloseableHttpResponse prerenderServerResponse, String responseHtml) {
        if (eventHandler != null) {
            return eventHandler.afterRender(clientRequest, clientResponse, prerenderServerResponse, responseHtml);
        }
        return responseHtml;
    }

    private String getFullUrl(HttpServletRequest request) {
        final String url = getRequestURL(request);
        final String queryString = request.getQueryString();
        return isNotBlank(queryString) ? String.format("%s?%s", url, queryString) : url;
    }
}
