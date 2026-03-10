package com.zor.globals.serve.response;

import java.io.IOException;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.zor.Interpreter;
import com.zor.RuntimeError;
import com.zor.Token;
import com.zor.ZorCallable;
import com.zor.globals.serve.ZodHandler;

public final class RedirectResponse implements ZorCallable {

  @Override
  public int arity() {
    return 1;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments, Token token) {
    if (!(arguments.get(0) instanceof String location))
      throw new RuntimeError(token, "redirectResponse(path) expects a string path.");

    return new RedirectHandler(location);
  }

  @Override
  public String toString() {
    return "<native redirectResponse fn>";
  }

  private static final class RedirectHandler implements ZodHandler {
    private final String location;

    private RedirectHandler(String location) {
      this.location = location;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      exchange.getResponseHeaders().set("Location", location);
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    }
  }
}
