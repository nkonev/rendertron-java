package com.github.nkonev.rendertron;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;

import static com.github.nkonev.rendertron.SeoFilterTest.DEFAULT_RENDERTRON_URL;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.client.methods.HttpGet.METHOD_NAME;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SeoFilterArgumentCaptorTest {


    private SeoFilter seoFilter;

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private CloseableHttpClient httpClient;

    @Captor
    private ArgumentCaptor<HttpGet> httpGetCaptor;

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
                });
            }
        };

        TestEventHandler.resetFlags();
    }

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter printWriter;


    @Test
    public void should_use_prefix_url_from_init_param_if_available_root() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2,crawler3");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.FORWARDED_URL_PREFIX)).thenReturn("http://my.sweet.example.com");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler3");
        when(servletRequest.getRequestURI()).thenReturn("");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGetCaptor.capture())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient).execute(httpGetCaptor.capture());
        Assert.assertEquals("http://example.com:3000/render/http://my.sweet.example.com", httpGetCaptor.getValue().getURI().toString());
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_use_prefix_url_from_init_param_if_available_root_slash() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2,crawler3");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.FORWARDED_URL_PREFIX)).thenReturn("http://my.sweet.example.com");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler3");
        when(servletRequest.getRequestURI()).thenReturn("/");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGetCaptor.capture())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient).execute(httpGetCaptor.capture());
        Assert.assertEquals("http://example.com:3000/render/http://my.sweet.example.com/", httpGetCaptor.getValue().getURI().toString());
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_use_prefix_url_from_init_param_if_available_root_path() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2,crawler3");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.FORWARDED_URL_PREFIX)).thenReturn("http://my.sweet.example.com");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler3");
        when(servletRequest.getRequestURI()).thenReturn("/path");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGetCaptor.capture())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient).execute(httpGetCaptor.capture());
        Assert.assertEquals("http://example.com:3000/render/http://my.sweet.example.com/path", httpGetCaptor.getValue().getURI().toString());
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void should_use_prefix_url_from_init_param_if_available_null() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2,crawler3");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.FORWARDED_URL_PREFIX)).thenReturn("http://my.sweet.example.com");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler3");
        when(servletRequest.getRequestURI()).thenReturn(null);

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGetCaptor.capture())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);

        //then
        verify(httpClient).execute(httpGetCaptor.capture());
        Assert.assertEquals("http://example.com:3000/render/http://my.sweet.example.com", httpGetCaptor.getValue().getURI().toString());
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
    }


    static class TestEventHandler implements com.github.nkonev.rendertron.EventHandler {
        static boolean beforeRender = false;
        static boolean afterRender = false;
        static boolean destroy = false;

        static String beforeRenderResponse = null;

        public static void resetFlags(){
            beforeRender = false;
            afterRender = false;
            destroy = false;
            beforeRenderResponse = null;
        }

        @Override
        public String beforeRender(HttpServletRequest clientRequest) {
            beforeRender=true;
            return beforeRenderResponse;
        }

        @Override
        public String afterRender(HttpServletRequest clientRequest, HttpServletResponse clientResponse, HttpResponse renderServiceResponse, String responseHtml) {
            afterRender=true;
            return responseHtml;
        }

        @Override
        public void destroy() {
            destroy=true;
        }
    }


    @Test
    public void test_event_handler_before_render_html() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_EVENT_HANDLER)).thenReturn("com.github.nkonev.rendertron.SeoFilterArgumentCaptorTest$TestEventHandler");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2,crawler3");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.FORWARDED_URL_PREFIX)).thenReturn("http://my.sweet.example.com");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        TestEventHandler.beforeRenderResponse = "<html></html>";

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler3");
        when(servletRequest.getRequestURI()).thenReturn("/path");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGetCaptor.capture())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);
        seoFilter.destroy();

        //then
        verify(httpClient, never()).execute(httpGetCaptor.capture());
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);

        Assert.assertEquals(true, TestEventHandler.beforeRender);
        Assert.assertEquals(false, TestEventHandler.afterRender);
        Assert.assertEquals(true, TestEventHandler.destroy);
    }

    @Test
    public void test_event_handler_before_render_blank() throws Exception {
        //given
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_EVENT_HANDLER)).thenReturn("com.github.nkonev.rendertron.SeoFilterArgumentCaptorTest$TestEventHandler");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.CRAWLER_USER_AGENTS)).thenReturn("crawler1,crawler2,crawler3");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.FORWARDED_URL_PREFIX)).thenReturn("http://my.sweet.example.com");
        when(filterConfig.getInitParameter(Constants.InitFilterParams.RENDERTRON_SERVICE_URL)).thenReturn(DEFAULT_RENDERTRON_URL);
        seoFilter.init(filterConfig);

        TestEventHandler.beforeRenderResponse = null;

        final CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);

        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        when(servletRequest.getMethod()).thenReturn(METHOD_NAME);
        when(servletRequest.getHeader("User-Agent")).thenReturn("crawler3");
        when(servletRequest.getRequestURI()).thenReturn("/path");

        when(servletRequest.getHeaderNames()).thenReturn(mock(Enumeration.class));
        when(httpClient.execute(httpGetCaptor.capture())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        final HashMap<String, String> map = new HashMap<String, String>();
        when(servletRequest.getParameterMap()).thenReturn(map);
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        //when
        seoFilter.doFilter(servletRequest, servletResponse, filterChain);
        seoFilter.destroy();

        //then
        verify(httpClient).execute(httpGetCaptor.capture());
        Assert.assertEquals("http://example.com:3000/render/http://my.sweet.example.com/path", httpGetCaptor.getValue().getURI().toString());
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);

        Assert.assertEquals(true, TestEventHandler.beforeRender);
        Assert.assertEquals(true, TestEventHandler.afterRender);
        Assert.assertEquals(true, TestEventHandler.destroy);
    }

}
