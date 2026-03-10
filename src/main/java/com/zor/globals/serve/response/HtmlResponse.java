package com.zor.globals.serve.response;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.zor.Interpreter;
import com.zor.RuntimeError;
import com.zor.Token;
import com.zor.ZorCallable;
import com.zor.globals.serve.ZodHandler;

public final class HtmlResponse implements ZorCallable {

  @Override
  public int arity() {
    return 1;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments, Token token) {
    if (!(arguments.get(0) instanceof String body))
      throw new RuntimeError(token, "htmlResponse(body) expects a string body.");

    return new HtmlHandler(body);
  }

  @Override
  public String toString() {
    return "<native htmlResponse fn>";
  }

  private static final class HtmlHandler implements ZodHandler {
    private final String response;

    private HtmlHandler(String response) {
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
