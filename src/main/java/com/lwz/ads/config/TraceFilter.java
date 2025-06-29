package com.lwz.ads.config;

import com.lwz.ads.util.MDCUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        MDCUtils.putContext("url=" + request.getServletPath());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDCUtils.clearContext();
        }
    }
}
