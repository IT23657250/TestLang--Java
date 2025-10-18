package tl.compiler;

import tl.ast.Ast;
import tl.codegen.CodeGenerator;
import tl.parser.Lexer;
import tl.parser.Parser;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class TestLangCompiler {
  public static void main(String[] args) throws Exception {
    if (args.length < 1 || args.length > 2) {
      System.err.println("Usage: TestLangCompiler <input.test> [<outputDir>]");
      System.exit(2);
    }
    Path input = Paths.get(args[0]);
    Path outDir = (args.length == 2)
        ? Paths.get(args[1])
        : Paths.get("target/generated-test-sources/test");
    Files.createDirectories(outDir);

    Ast.Program program = parse(input);
    validate(program);
    Path outFile = outDir.resolve("GeneratedTests.java");
    new CodeGenerator().generate(program, outFile);

    System.out.println("Generated: " + outFile.toAbsolutePath());
  }

  private static Ast.Program parse(Path input) throws Exception {
    try (Reader r = Files.newBufferedReader(input)) {
      Lexer lexer = new Lexer(r);
      Parser parser = new Parser(lexer);
      Object result = parser.parse().value;
      return (Ast.Program) result;
    } catch (CompilerException e) {
      System.err.println(e.getMessage());
      System.exit(1);
      return null;
    }
  }

  private static void validate(Ast.Program p) {
    // Unique variable names
    Set<String> names = new HashSet<>();
    for (Ast.Let l : p.lets) {
      if (!names.add(l.name)) {
        throw new CompilerException("Duplicate variable: " + l.name);
      }
    }
    // Each test has >=1 request and >=2 assertions
    for (Ast.Test t : p.tests) {
      int reqs = 0, asserts = 0;
      for (Ast.Step s : t.steps) {
        if (s instanceof Ast.Step.Request) reqs++;
        if (s instanceof Ast.Step.Expect) asserts++;
      }
      if (reqs < 1) {
        throw new CompilerException("Test '" + t.name + "' must execute at least one request");
      }
      if (asserts < 2) {
        throw new CompilerException("Test '" + t.name + "' must have at least two assertions");
      }
    }
  }

  // Checked runtime exception to bubble out parse/validation messages
  public static class CompilerException extends RuntimeException {
    public CompilerException(String msg) { super(msg); }
  }
}