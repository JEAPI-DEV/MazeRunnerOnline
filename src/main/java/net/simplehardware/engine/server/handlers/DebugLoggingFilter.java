package net.simplehardware.engine.server.handlers;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Filter to log request timing and debug information
 */
public class DebugLoggingFilter extends Filter {
    
    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();
        
        System.out.println(String.format("[%s] %s %s from %s", 
            getCurrentTimestamp(), method, path, remoteAddr));
        
        try {
            chain.doFilter(exchange);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int responseCode = exchange.getResponseCode();
            
            if (duration > 1000) {
                System.err.println(String.format("[SLOW] %s %s took %d ms (response: %d)", 
                    method, path, duration, responseCode));
            } else {
                System.out.println(String.format("[%s] %s %s completed in %d ms (response: %d)", 
                    getCurrentTimestamp(), method, path, duration, responseCode));
            }
        }
    }
    
    @Override
    public String description() {
        return "Debug logging filter for performance monitoring";
    }
    
    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
    }
}
