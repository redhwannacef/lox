package com.zor.globals.serve;

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
      var responseHandler = parseResponse(interpreter, arguments.get(0), token);
      var server = HttpServer.create(new InetSocketAddress(8080), 0);
      server.createContext("/", responseHandler);
      server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
      server.start();
      registerShutdownHook(server);
    } catch (Exception error) {
      throw new RuntimeError(token, error.getMessage());
    }

    return null;
  }

  @Override
  public String toString() {
    return "<native serve fn>";
  }

  private static void registerShutdownHook(HttpServer server) {
    var shutdownHook = new Thread(() -> server.stop(1), "zor-serve-shutdown-hook");
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  private static HttpHandler parseResponse(Interpreter interpreter, Object callback, Token token) {
    if (!(callback instanceof ZorCallable))
      throw new RuntimeError(token, "serve(handler) expects a function handler.");

    var function = (ZorCallable) callback;
    return new CallbackHttpHandler(interpreter, function, token);
  }

  private static record CallbackHttpHandler(Interpreter interpreter, ZorCallable callable, Token token)
      implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws java.io.IOException {
      var request = createRequest(exchange).toZorInstance();
      var response = callable.call(interpreter, List.of(request), token);
      if (!(response instanceof ZodHandler handler))
        throw new RuntimeError(token,
            "handler(request) must return a response like htmlResponse(body) or staticResponse(path).");

      handler.handle(exchange);
    }

    private static Request createRequest(HttpExchange exchange) {
      var path = exchange.getRequestURI().getPath();
      var method = exchange.getRequestMethod();
      return new Request(path, method);
    }
  }
}
