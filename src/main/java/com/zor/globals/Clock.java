package com.zor.globals;

import java.util.List;

import com.zor.Interpreter;
import com.zor.Token;
import com.zor.ZorCallable;

public final class Clock implements ZorCallable {
  @Override
  public int arity() {
    return 0;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments, Token token) {
    return (double) System.currentTimeMillis() / 1000.0;
  }

  @Override
  public String toString() {
    return "<native clock fn>";
  }
}
