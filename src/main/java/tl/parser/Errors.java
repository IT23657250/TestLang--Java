package tl.parser;

public final class Errors {
  public static RuntimeException error(String message, int line, int column) {
    return new RuntimeException("Line " + line + ", Col " + column + ": " + message);
  }
}