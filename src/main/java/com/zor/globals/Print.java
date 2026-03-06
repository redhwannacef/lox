package com.zor.globals;

import java.util.List;

import com.zor.Interpreter;
import com.zor.Token;
import com.zor.ZorCallable;

public final class Print implements ZorCallable {
  @Override
  public int arity() {
    return 1;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments, Token token) {
    System.out.println(stringify(arguments.get(0)));
    return null;
  }

  @Override
  public String toString() {
    return "<native prin fn>";
  }

  private String stringify(Object object) {
    if (object == null)
      return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0"))
        text = text.substring(0, text.length() - 2);
      return text;
    }

    return object.toString();
  }
}
