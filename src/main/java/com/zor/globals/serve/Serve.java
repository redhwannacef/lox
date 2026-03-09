package com.zor.globals.serve;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

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
    var response = function.call(interpreter, List.of(), token);
    if (!(response instanceof HttpHandler handler))
      throw new RuntimeError(token,
          "handler must return an HttpHandler like htmlResponse(body) or staticResponse(path).");

    return handler;
  }
}
