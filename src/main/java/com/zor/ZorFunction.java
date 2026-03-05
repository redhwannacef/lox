package com.zor;

import java.util.List;

class ZorFunction implements ZorCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  private final boolean isInitializer;

  ZorFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.declaration = declaration;
    this.closure = closure;
    this.isInitializer = isInitializer;
  }

  ZorFunction bind(ZorInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    return new ZorFunction(declaration, environment, isInitializer);
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);

    for (int i = 0; i < declaration.params.size(); i++)
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      if (isInitializer)
        return closure.getAt(0, "this");

      return returnValue.value;
    }

    if (isInitializer)
      return closure.getAt(0, "this");

    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}
