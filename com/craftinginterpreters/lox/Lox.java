package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Unary;

public class Lox {
  private static final Interpreter interpreter = new Interpreter();
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    // Indicate an error in the exit code.
    if (hadError)
      System.exit(65);
    if (hadRuntimeError)
      System.exit(70);
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null)
        break;
      run(line);
      hadError = false;
    }
  }

  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();
    Parser parser = new Parser(tokens);
    Expr expression = parser.parse();

    // Stop if there was a syntax error.
    if (hadError)
      return;

    interpreter.interpret(expression);
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }

  static class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
      return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
      return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
      return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
      if (expr.value == null)
        return "nil";
      return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
      return parenthesize(expr.operator.lexeme, expr.right);
    }

    private String parenthesize(String name, Expr... exprs) {
      StringBuilder builder = new StringBuilder();

      builder.append("(").append(name);
      for (Expr expr : exprs) {
        builder.append(" ");
        builder.append(expr.accept(this));
      }
      builder.append(")");

      return builder.toString();
    }
  }
}
