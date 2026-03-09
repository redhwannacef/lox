package com.zor.globals.serve.response;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zor.Interpreter;
import com.zor.RuntimeError;
import com.zor.Token;
import com.zor.ZorCallable;

public final class StaticResponse implements ZorCallable {

  @Override
  public int arity() {
    return 1;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments, Token token) {
    var rawPath = parsePath(arguments.get(0), token);

    try {
      var root = Path.of(rawPath).toRealPath();
      if (!Files.isDirectory(root))
        throw new RuntimeError(token, "staticResponse(path) expects a directory path.");

      return new StaticHandler(root);
    } catch (IOException error) {
      throw new RuntimeError(token, "Failed to open static directory: " + rawPath);
    }

  }

  @Override
  public String toString() {
    return "<native staticResponse fn>";
  }

  private static String parsePath(Object path, Token token) {
    if (!(path instanceof String))
      throw new RuntimeError(token, "staticResponse(path) expects a string path.");

    return (String) path;
  }

  private static final class StaticHandler implements HttpHandler {
    private final Path root;

    private StaticHandler(Path root) {
      this.root = root;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      var allowedMethods = List.of("GET", "HEAD");
      var method = exchange.getRequestMethod();
      if (!allowedMethods.contains(method)) {
        sendStatus(exchange, 405);
        return;
      }

      var target = resolvePath(root, exchange.getRequestURI().getRawPath());
      if (target == null || !Files.exists(target) || !Files.isRegularFile(target)) {
        sendStatus(exchange, 404);
        return;
      }

      Path realTarget;
      try {
        realTarget = target.toRealPath();
      } catch (IOException error) {
        sendStatus(exchange, 404);
        return;
      }

      if (!realTarget.startsWith(root)) {
        sendStatus(exchange, 404);
        return;
      }

      var contentType = Files.probeContentType(realTarget);
      exchange.getResponseHeaders().set("Content-Type", contentType == null ? "application/text" : contentType);

      if ("HEAD".equals(method)) {
        sendStatus(exchange, 200);
        return;
      }

      var body = Files.readAllBytes(realTarget);
      exchange.sendResponseHeaders(200, body.length);
      try (var output = exchange.getResponseBody()) {
        output.write(body);
      }
    }
  }

  private static Path resolvePath(Path root, String rawPath) {
    var path = rawPath == null || rawPath.isEmpty() ? "/" : rawPath;
    var decodedPath = URLDecoder.decode(path, UTF_8);
    var normalizedPath = decodedPath.replace('\\', '/');
    if (normalizedPath.equals("/"))
      normalizedPath = "/index.html";

    if (!normalizedPath.startsWith("/"))
      return null;

    var relativePath = normalizedPath.substring(1);
    var candidate = root.resolve(relativePath).normalize();

    if (!candidate.startsWith(root))
      return null;

    return candidate;
  }

  private static void sendStatus(HttpExchange exchange, int status) throws IOException {
    exchange.sendResponseHeaders(status, -1);
    exchange.close();
  }
}
