package com.example.ordertools.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.ConnectException;

/**
 * Command-line MCP client. Connects to the order-tools MCP server over
 * Streamable HTTP, runs a single command, prints the result, and exits.
 */
@SpringBootApplication
public class OrderClientApplication {

    public static void main(String[] args) {
        try {
            // Run the CommandLineRunner, then exit with its status (no web server is started).
            System.exit(SpringApplication.exit(
                    SpringApplication.run(OrderClientApplication.class, args)));
        } catch (Exception e) {
            // The MCP client connects eagerly at startup; turn a connection failure
            // into a short, actionable message instead of a long stack trace.
            if (hasCause(e, ConnectException.class)) {
                System.err.println("""
                        ✗ Cannot reach the MCP server (connection refused).
                          Start it first:  mvn -pl mcp-server spring-boot:run""");
            } else {
                System.err.println("✗ Client error: " + rootMessage(e));
            }
            System.exit(1);
        }
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage() != null ? c.getMessage() : c.toString();
    }
}
