package com.zor;

import java.util.List;
import java.util.Map;

class ZorClass implements ZorCallable {
  final String name;
  private final Map<String, ZorFunction> methods;
  final ZorClass superclass;

  ZorClass(String name, ZorClass superclass, Map<String, ZorFunction> methods) {
    this.name = name;
    this.methods = methods;
    this.superclass = superclass;
  }

  public ZorFunction findMethod(String name) {
    if (methods.containsKey(name))
      return methods.get(name);

    if (superclass != null)
      return superclass.findMethod(name);

    return null;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    ZorInstance instance = new ZorInstance(this);

    ZorFunction initializer = findMethod("init");
    if (initializer != null)
      initializer.bind(instance).call(interpreter, arguments);

    return instance;
  }

  @Override
  public int arity() {
    ZorFunction initializer = findMethod("init");
    if (initializer == null)
      return 0;
    return initializer.arity();
  }

  @Override
  public String toString() {
    return name;
  }

}
