package com.example.ordertools.client;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Parses the command line and dispatches to the matching MCP tool on the
 * order-tools server. The MCP {@link McpSyncClient} beans are auto-configured
 * from {@code spring.ai.mcp.client.streamable-http.connections} in
 * {@code application.properties}.
 */
@Component
public class OrderMcpCli implements ApplicationRunner {

    private final McpSyncClient mcp;

    public OrderMcpCli(List<McpSyncClient> clients) {
        if (clients.isEmpty()) {
            throw new IllegalStateException("No MCP client connection configured");
        }
        // With the "external" profile, additional clients (github, jira, splunk from
        // mcp-servers.json) are also present, so select the order-tools server by name
        // rather than assuming it is first.
        this.mcp = clients.stream()
                .filter(c -> "order-tools".equals(c.getServerInfo().name()))
                .findFirst()
                .orElse(clients.getFirst());
    }

    @Override
    public void run(ApplicationArguments appArgs) {
        // Positional args only; Spring has already consumed any --property overrides.
        String[] args = appArgs.getNonOptionArgs().toArray(new String[0]);
        if (args.length == 0 || isHelp(args[0])) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();
        try {
            switch (command) {
                case "tools" -> listTools();
                case "create" -> create(args);
                case "get" -> call("getOrder", Map.of("orderId", required(args, 1, "orderId")));
                case "list" -> list(args);
                case "status" -> call("updateOrderStatus", Map.of(
                        "orderId", required(args, 1, "orderId"),
                        "status", required(args, 2, "status").toUpperCase()));
                case "cancel" -> call("cancelOrder", Map.of("orderId", required(args, 1, "orderId")));
                default -> {
                    System.err.println("Unknown command: " + command);
                    printUsage();
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
        } catch (Exception e) {
            System.err.println("Tool call failed: " + e.getMessage());
        }
    }

    private void listTools() {
        System.out.println("Available tools on '" + mcp.getServerInfo().name() + "':\n");
        for (Tool tool : mcp.listTools().tools()) {
            System.out.printf("  %-18s %s%n", tool.name(), tool.description());
        }
    }

    private void create(String[] args) {
        // create <customer> <sku> <name> <quantity> <unitPrice>
        String customer = required(args, 1, "customer");
        String sku = required(args, 2, "sku");
        String name = required(args, 3, "name");
        int quantity = Integer.parseInt(required(args, 4, "quantity"));
        BigDecimal unitPrice = new BigDecimal(required(args, 5, "unitPrice"));

        Map<String, Object> item = Map.of(
                "sku", sku, "name", name, "quantity", quantity, "unitPrice", unitPrice);
        call("createOrder", Map.of("customer", customer, "items", List.of(item)));
    }

    private void list(String[] args) {
        // list [STATUS]
        Map<String, Object> arguments = args.length > 1
                ? Map.of("status", args[1].toUpperCase())
                : Map.of();
        call("listOrders", arguments);
    }

    private void call(String toolName, Map<String, Object> arguments) {
        CallToolResult result = mcp.callTool(new CallToolRequest(toolName, arguments));
        if (Boolean.TRUE.equals(result.isError())) {
            System.err.println("Server reported an error:");
        }
        for (Content content : result.content()) {
            if (content instanceof TextContent text) {
                System.out.println(text.text());
            }
        }
    }

    private static String required(String[] args, int index, String name) {
        if (index >= args.length) {
            throw new IllegalArgumentException("missing argument: " + name);
        }
        return args[index];
    }

    private static boolean isHelp(String arg) {
        return arg.equals("help") || arg.equals("-h") || arg.equals("--help");
    }

    private void printUsage() {
        System.out.println("""

                Order MCP client — calls the order-tools server over Streamable HTTP.

                Usage: <command> [args]

                  tools                                          list available tools
                  create <customer> <sku> <name> <qty> <price>   create an order
                  get <orderId>                                  fetch one order
                  list [STATUS]                                  list orders (optional status filter)
                  status <orderId> <STATUS>                      update an order's status
                  cancel <orderId>                               cancel an order

                STATUS is one of: NEW, PAID, SHIPPED, DELIVERED, CANCELLED
                """);
    }
}