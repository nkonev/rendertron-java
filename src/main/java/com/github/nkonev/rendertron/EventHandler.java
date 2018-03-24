package com.github.nkonev.rendertron;


import org.apache.http.HttpResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface EventHandler {

    /**
     * If returned String is not blank than Rendertron won't called. It useful for cache.
     * @param clientRequest
     * @return
     */
    String beforeRender(HttpServletRequest clientRequest);

    String afterRender(HttpServletRequest clientRequest, HttpServletResponse clientResponse, HttpResponse renderServiceResponse, String responseHtml);

    void destroy();
}
