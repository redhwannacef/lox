package com.zor;

import java.util.List;

interface ZorCallable {
  int arity();

  Object call(Interpreter interpreter, List<Object> arguments);
}
