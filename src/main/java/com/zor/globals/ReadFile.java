package com.zor.globals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.zor.Interpreter;
import com.zor.RuntimeError;
import com.zor.Token;
import com.zor.ZorCallable;

public final class ReadFile implements ZorCallable {
  @Override
  public int arity() {
    return 1;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments, Token token) {
    var path = parsePath(arguments.get(0), token);

    try {
      return Files.readString(Path.of(path));
    } catch (IOException error) {
      throw new RuntimeError(token, "Failed to read file: " + path);
    }
  }

  @Override
  public String toString() {
    return "<native readFile fn>";
  }

  private static String parsePath(Object path, Token token) {
    if (!(path instanceof String))
      throw new RuntimeError(token, "readFile(path) expects a string path.");

    return (String) path;
  }
}
