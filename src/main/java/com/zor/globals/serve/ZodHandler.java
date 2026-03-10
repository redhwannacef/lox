package com.zor.globals.serve;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

@FunctionalInterface
public interface ZodHandler {
  void handle(HttpExchange exchange) throws IOException;
}
