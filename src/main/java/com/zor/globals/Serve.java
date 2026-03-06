package com.zor.globals;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zor.Interpreter;
import com.zor.RuntimeError;
import com.zor.Token;
import com.zor.ZorCallable;

public final class Serve implements ZorCallable {

  @Override
  public int arity() {
    return 1;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments, Token token) {
    try {
      var response = parseResponse(interpreter, arguments.get(0), token);
      var server = HttpServer.create(new InetSocketAddress(8080), 0);
      server.createContext("/", new DefaultHandler(response));
      server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
      server.start();
    } catch (Exception error) {
      throw new RuntimeError(token, error.getMessage());
    }

    return null;
  }

  @Override
  public String toString() {
    return "<native serve fn>";
  }

  private static String parseResponse(Interpreter interpreter, Object callback, Token token) {
    if (!(callback instanceof ZorCallable))
      throw new RuntimeError(token, "Expected function as argument 0");

    var function = (ZorCallable) callback;
    return function.call(interpreter, List.of(), token).toString();
  }

  private static class DefaultHandler implements HttpHandler {
    private final String response;

    private DefaultHandler(String response) {
      this.response = response;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      var body = response.getBytes(UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
      exchange.sendResponseHeaders(200, body.length);
      try (var output = exchange.getResponseBody()) {
        output.write(body);
      }
    }
  }
}
