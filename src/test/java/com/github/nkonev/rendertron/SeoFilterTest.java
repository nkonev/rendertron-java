package com.github.nkonev.rendertron;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.client.methods.HttpGet.METHOD_NAME;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SeoFilterTest {

    private SeoFilter seoFilter;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpGet httpGet;

    @Mock
    private PrintWriter printWriter;

    public static final String DEFAULT_RENDERTRON_URL = "http://example.com:3000/render";

    @Before
    public void setUp() throws Exception {
        seoFilter = new SeoFilter() {
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                setSeoService(new SeoService(toMap(filterConfig)) {
                    @Override
                    protected CloseableHttpClient getHttpClient() {
                        return httpClient;
                    }

                    @Override
                    protected HttpGet getHttpGet(String apiUrl) {
                        return httpGet;
                    }
                });
            }
        };
    }

    @Test
    public void should_not_handle_when_non_get_request() throws Exception {
        //given
        seoFilter.init(filterConfig);
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer());
        when(servletRequest.getMethod()).thenReturn(HttpPost.METHOD_NAME);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient, never()).execute(httpGet);
        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_not_handle_when_user_agent_is_not_crawler() throws Exception {
        //given
        seoFilter.init(filterConfig);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String>());
        when(servletRequest.getHeader("User-Agent")).thenReturn("no");
        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient, never()).execute(httpGet);
        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_not_handle_when_url_is_a_resource() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2");
        seoFilter.init(filterConfig);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test.js"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String>());
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler1");
        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient, never()).execute(httpGet);
        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_not_handle_when_white_list_is_not_empty_and_url_is_not_in_white_list() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.WHITELIST)).thenReturn("whitelist1,whitelist2");
        seoFilter.init(filterConfig);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String>());
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler1");
        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient, never()).execute(httpGet);
        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_not_handle_when_black_list_is_not_empty_and_url_is_in_black_list() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.BLACKLIST)).thenReturn("blacklist1,http://localhost/test");
        seoFilter.init(filterConfig);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getParameterMap()).thenReturn(new HashMap<String, String>());
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler1");
        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient, never()).execute(httpGet);
        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_handle_when_user_agent_is_crawler_and_url_is_not_resource_and_white_list_is_empty_and_black_list_is_empty() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler1");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGet)).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);
        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient).execute(httpGet);
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_handle_when_every_thing_is_ok_but_prerender_server_response_is_not_200() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler1");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGet)).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_NOT_FOUND);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);


        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient).execute(httpGet);
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
        verify(servletResponse).setStatus(SC_NOT_FOUND);
    }


    @Test
    public void should_handle_when_user_agent_is_crawler_and_url_is_not_resource_and_in_white_list_and_not_in_black_list() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.WHITELIST)).thenReturn("whitelist1,http://localhost/test");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.BLACKLIST)).thenReturn("blacklist1,blacklist2");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);

        seoFilter.init(filterConfig);

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler1");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGet)).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient).execute(httpGet);
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_use_request_url_from_custom_header_if_available() throws Exception {
        //given
        final String headerName = "X-Forwarded-URL";
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2,crawler3");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.FORWARDED_URL_HEADER)).thenReturn(headerName);
        when(filterConfig.getInitParameter(Constants.InitFilterParams.WHITELIST)).thenReturn("http://my.public.domain.com/");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.BLACKLIST)).thenReturn("http://localhost/test");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader(headerName)).thenReturn("http://my.public.domain.com/");
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler3");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGet)).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient).execute(httpGet);
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
    }
}
