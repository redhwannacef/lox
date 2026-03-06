package com.zor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zor.Expr.Assign;
import com.zor.Expr.Binary;
import com.zor.Expr.Grouping;
import com.zor.Expr.Literal;
import com.zor.Expr.Unary;
import com.zor.Expr.Variable;
import com.zor.Stmt.Block;
import com.zor.Stmt.Expression;
import com.zor.Stmt.If;
import com.zor.Stmt.Var;
import com.zor.globals.Print;
import com.zor.globals.Clock;
import com.zor.globals.ReadFile;
import com.zor.globals.Serve;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    globals.define("clock", new Clock());
    globals.define("print", new Print());
    globals.define("readFile", new ReadFile());
    globals.define("serve", new Serve());
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements)
        execute(statement);
    } catch (RuntimeError error) {
      Zor.runtimeError(error);
    }
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }

        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }

        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
      default:
        return null;
    }

  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
      default:
        return null;
    }
  }

  @Override
  public Void visitExpressionStmt(Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitVarStmt(Var stmt) {
    Object value = null;
    if (stmt.initializer != null)
      value = evaluate(stmt.initializer);

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitVariableExpr(Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    Object value = evaluate(expr.value);

    Integer distance = locals.get(expr);
    if (distance != null)
      environment.assignAt(distance, expr.name, value);
    else
      globals.assign(expr.name, value);

    return value;
  }

  @Override
  public Void visitBlockStmt(Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitIfStmt(If stmt) {
    if (isTruthy(evaluate(stmt.condition)))
      execute(stmt.thenBranch);
    else if (stmt.elseBranch != null)
      execute(stmt.elseBranch);

    return null;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left))
        return left;
    } else {
      if (!isTruthy(left))
        return left;
    }

    return evaluate(expr.right);
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition)))
      execute(stmt.body);

    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();

    for (Expr argument : expr.arguments)
      arguments.add(evaluate(argument));

    if (!(callee instanceof ZorCallable))
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");

    ZorCallable function = (ZorCallable) callee;

    if (arguments.size() != function.arity())
      throw new RuntimeError(expr.paren,
          "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");

    return function.call(this, arguments, expr.paren);
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    ZorFunction function = new ZorFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null)
      value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof ZorClass))
        throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
    }

    environment.define(stmt.name.lexeme, null);

    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }

    Map<String, ZorFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      ZorFunction function = new ZorFunction(method, environment, method.name.lexeme.equals("init"));
      methods.put(method.name.lexeme, function);
    }

    ZorClass klass = new ZorClass(stmt.name.lexeme, (ZorClass) superclass, methods);

    if (superclass != null)
      environment = environment.enclosing;

    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);

    if (object instanceof ZorInstance)
      return ((ZorInstance) object).get(expr.name);

    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof ZorInstance))
      throw new RuntimeError(expr.name, "Only instances have fields.");

    Object value = evaluate(expr.value);
    ((ZorInstance) object).set(expr.name, value);

    return value;
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    ZorClass superclass = (ZorClass) environment.getAt(distance, "super");
    ZorInstance object = (ZorInstance) environment.getAt(distance - 1, "this");

    ZorFunction method = superclass.findMethod(expr.method.lexeme);

    if (method == null)
      throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");

    return method.bind(object);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double)
      return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double)
      return;

    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private boolean isTruthy(Object object) {
    if (object == null)
      return false;
    if (object instanceof Boolean)
      return (boolean) object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null)
      return true;
    if (a == null)
      return false;

    return a.equals(b);
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements)
        execute(statement);
    } finally {
      this.environment = previous;
    }
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null)
      return environment.getAt(distance, name.lexeme);
    else
      return globals.get(name);

  }

}
