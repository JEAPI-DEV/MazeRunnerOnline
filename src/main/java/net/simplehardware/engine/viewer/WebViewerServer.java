package net.simplehardware.engine.viewer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;

/**
 * Simple HTTP server to serve the web-based game viewer
 */
public class WebViewerServer {
    private static final int DEFAULT_PORT = 9000;
    private HttpServer server;
    private final int port;

    public WebViewerServer() {
        this(DEFAULT_PORT);
    }

    public WebViewerServer(int port) {
        this.port = port;
    }

    /**
     * Start the HTTP server and open the browser
     */
    public void start() throws IOException {
        // Find an available port if the default is taken
        int actualPort = 7050;

        server = HttpServer.create(new InetSocketAddress(actualPort), 0);

        // Serve static files
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // Use default executor
        server.start();

        String url = "http://localhost:" + actualPort + "/game-viewer.html";
        System.out.println("\n=================================");
        System.out.println("Web Viewer Server Started");
        System.out.println("=================================");
        System.out.println("URL: " + url);
        System.out.println("Press Ctrl+C to stop the server");
        System.out.println("=================================\n");

        // Open browser
        openBrowser(url);
    }

    /**
     * Stop the HTTP server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Web viewer server stopped.");
        }
    }

    /**
     * Open the URL in the default browser
     */
    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println("Please open the following URL in your browser:");
                System.out.println(url);
            }
        } catch (Exception e) {
            System.err.println("Could not open browser automatically: " + e.getMessage());
            System.out.println("Please open the following URL in your browser:");
            System.out.println(url);
        }
    }

    /**
     * HTTP handler for serving static files
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            // Default to index
            if (path.equals("/")) {
                path = "/game-viewer.html";
            }

            // Remove leading slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            File file = new File(path);

            if (!file.exists() || !file.isFile()) {
                // File not found
                String response = "404 - File Not Found: " + path;
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            // Determine content type
            String contentType = getContentType(path);

            // Read file
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            // Send response
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, fileBytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(fileBytes);
            os.close();
        }

        private String getContentType(String path) {
            if (path.endsWith(".html"))
                return "text/html";
            if (path.endsWith(".css"))
                return "text/css";
            if (path.endsWith(".js"))
                return "application/javascript";
            if (path.endsWith(".json"))
                return "application/json";
            if (path.endsWith(".png"))
                return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
                return "image/jpeg";
            if (path.endsWith(".gif"))
                return "image/gif";
            if (path.endsWith(".svg"))
                return "image/svg+xml";
            return "application/octet-stream";
        }
    }

    /**
     * Start server and keep it running
     */
    public void startAndWait() throws IOException {
        start();

        // Keep the server running
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down web viewer server...");
            stop();
        }));

        // Keep main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
