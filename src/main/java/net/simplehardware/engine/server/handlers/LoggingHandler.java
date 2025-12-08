package net.simplehardware.engine.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Wrapper handler that adds debug logging to any handler
 */
public class LoggingHandler implements HttpHandler {
    private final HttpHandler delegate;
    
    public LoggingHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();
        
        System.out.printf("[%s] %s %s from %s%n",
            getCurrentTimestamp(), method, path, remoteAddr);
        
        try {
            delegate.handle(exchange);
            
            long duration = System.currentTimeMillis() - startTime;
            int responseCode = exchange.getResponseCode();
            
            if (duration > 1000) {
                System.err.printf("[SLOW] %s %s took %d ms (response: %d)%n",
                    method, path, duration, responseCode);
            } else {
                System.out.printf("[%s] %s %s completed in %d ms (response: %d)%n",
                    getCurrentTimestamp(), method, path, duration, responseCode);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.printf("[ERROR] %s %s failed after %d ms: %s%n",
                method, path, duration, e.getMessage());
            throw e;
        }
    }
    
    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
    }
}
