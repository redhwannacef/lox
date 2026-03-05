package com.zor;

import java.util.List;

public interface ZorCallable {
  int arity();

  Object call(Interpreter interpreter, List<Object> arguments);
}
