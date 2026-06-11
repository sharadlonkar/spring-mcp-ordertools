
# spring-mcp-ordertools

A [Spring AI](https://docs.spring.io/spring-ai/reference/) **MCP server** that exposes
order-management operations as [Model Context Protocol](https://modelcontextprotocol.io/)
tools. An MCP client (Claude Desktop, an IDE, or another Spring AI app) can connect and
call the tools to create, query, and update orders.

## Tools

| Tool                | Description                                              |
|---------------------|----------------------------------------------------------|
| `createOrder`       | Create an order for a customer with one or more items.   |
| `getOrder`          | Look up a single order by id.                            |
| `listOrders`        | List orders, optionally filtered by status.              |
| `updateOrderStatus` | Move an order to NEW / PAID / SHIPPED / DELIVERED.       |
| `cancelOrder`       | Cancel an order.                                         |

Orders are held in an in-memory store (`OrderService`) — swap it for a JPA
repository to persist.

## Requirements

- Java 21+
- Maven 3.9+

## Build & test

```bash
mvn clean verify
```

## Run

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080` with the WebMVC/SSE transport:

- SSE stream: `GET /sse`
- Message endpoint: `POST /mcp/message`

## Connect from an MCP client

Example Claude Desktop config (`claude_desktop_config.json`) using the SSE transport:

```json
{
  "mcpServers": {
    "order-tools": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

For clients that require STDIO instead of SSE, swap the dependency in `pom.xml`
from `spring-ai-starter-mcp-server-webmvc` to `spring-ai-starter-mcp-server`
and run the built jar directly.

## Project layout

```
domain/   Order, OrderItem, OrderStatus  (records + enum)
service/  OrderService                   (in-memory store + business rules)
tools/    OrderTools                      (@Tool methods exposed over MCP)
OrderToolsApplication                     (registers tools as a ToolCallbackProvider)
```

## Tech stack

- Spring Boot 3.4.5
- Spring AI 1.0.0 (`spring-ai-starter-mcp-server-webmvc`)