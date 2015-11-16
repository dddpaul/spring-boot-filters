package com.github.dddpaul.filters;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.tomcat.util.http.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Service
    static class SharedService {
        public String getMessage(String name) {
            return String.format("Hello, %s, I'm shared service", name);
        }
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    Filter coyoteRequestManipulator() {
        return new CoyoteRequestManipulator();
    }

    @Controller
    @RequestMapping("/one")
    public static class ControllerOne {
        @Autowired
        private SharedService service;

        @RequestMapping(produces = "text/plain;charset=utf-8")
        @ResponseBody
        public String getMessage(String name) {
            return "ControllerOne says \"" + service.getMessage(name) + "\"";
        }
    }

    @Controller
    @RequestMapping("/two")
    public static class ControllerTwo {
        @Autowired
        private SharedService service;

        @RequestMapping(produces = "text/plain;charset=utf-8")
        @ResponseBody
        public String getMessage(String name) {
            return "ControllerTwo says \"" + service.getMessage(name) + "\"";
        }
    }

    static class CoyoteRequestManipulator extends OncePerRequestFilter {

        private static Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                Class superClass = clazz.getSuperclass();
                if (superClass == null) {
                    throw e;
                } else {
                    return getField(superClass, fieldName);
                }
            }
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            if (StringUtils.hasText(request.getQueryString()) && request.getQueryString().contains("%")) {
                RequestFacade facade = (RequestFacade) request;
                try {
                    // First hack is to get org.apache.coyote.Request instance
                    Field requestField = getField(RequestFacade.class, "request");
                    requestField.setAccessible(true);
                    Request connRequest = (Request) requestField.get(facade);
                    org.apache.coyote.Request coyoteRequest = connRequest.getCoyoteRequest();

                    // But it's already filled with decoded query parameters, so query string has to be re-handled
                    // after URI encoding switch. So, in fact, query string is processed twice.
                    // Yet, org.apache.coyote.Request instances are reusable, so query encoding has to set every time.
                    Parameters parameters = coyoteRequest.getParameters();
                    parameters.setQueryStringEncoding(request.getServletPath().startsWith("/two") ? "cp1251" : "utf-8");
                    parameters.recycle();
                    parameters.handleQueryParameters();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            chain.doFilter(request, response);
        }
    }
}
