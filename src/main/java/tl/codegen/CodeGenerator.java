package tl.codegen;

import tl.ast.Ast;
import tl.ast.Ast.Step.Expect;
import tl.ast.Ast.Step.Request;
import tl.util.Strings;
import tl.compiler.TestLangCompiler.CompilerException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.*;

public final class CodeGenerator {

  public void generate(Ast.Program p, Path outFile) throws IOException {
    Map<String, Ast.Value> vars = new LinkedHashMap<>();
    for (Ast.Let l : p.lets) vars.put(l.name, l.value);

    String base = (p.config != null && p.config.baseUrl != null) ? p.config.baseUrl : "";
    LinkedHashMap<String, String> defaultHeaders =
        (p.config != null) ? p.config.defaultHeaders : new LinkedHashMap<>();

    StringBuilder sb = new StringBuilder(16_384);

    sb.append("import org.junit.jupiter.api.*;\n")
      .append("import static org.junit.jupiter.api.Assertions.*;\n")
      .append("import java.net.http.*;\n")
      .append("import java.net.*;\n")
      .append("import java.time.Duration;\n")
      .append("import java.nio.charset.StandardCharsets;\n")
      .append("import java.util.*;\n\n")
      .append("public class GeneratedTests {\n")
      .append("  static String BASE = ").append(quote(base)).append(";\n")
      .append("  static Map<String,String> DEFAULT_HEADERS = new LinkedHashMap<>();\n")
      .append("  static HttpClient client;\n\n")
      .append("  @BeforeAll\n")
      .append("  static void setup() {\n")
      .append("    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();\n");
    for (Map.Entry<String,String> e : defaultHeaders.entrySet()) {
      sb.append("    DEFAULT_HEADERS.put(").append(quote(e.getKey())).append(", ")
        .append(quote(substitute(e.getValue(), vars))).append(");\n");
    }
    sb.append("  }\n\n");

    int idx = 0;
    for (Ast.Test t : p.tests) {
      idx++;
      sb.append("  @Test\n")
        .append("  void test_").append(t.name).append("() throws Exception {\n");

      boolean hadRequest = false;
      for (Ast.Step s : t.steps) {
        if (s instanceof Request) {
          hadRequest = true;
          Request r = (Request) s;
          String rawPath = substitute(r.path, vars);
          String urlExpr = buildUrlExpr(base, rawPath);

          sb.append("    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(")
            .append(urlExpr).append("))\n")
            .append("      .timeout(Duration.ofSeconds(10))\n");

          switch (r.method) {
            case GET:
              if (r.body != null) throw new CompilerException("GET cannot have a body");
              sb.append("      .GET();\n");
              break;
            case DELETE:
              if (r.body != null) throw new CompilerException("DELETE cannot have a body");
              sb.append("      .DELETE();\n");
              break;
            case POST:
              String bodyPost = (r.body == null) ? "" : substitute(r.body, vars);
              sb.append("      .POST(HttpRequest.BodyPublishers.ofString(")
                .append(quote(bodyPost)).append("));\n");
              break;
            case PUT:
              String bodyPut = (r.body == null) ? "" : substitute(r.body, vars);
              sb.append("      .PUT(HttpRequest.BodyPublishers.ofString(")
                .append(quote(bodyPut)).append("));\n");
              break;
          }

          // Apply config headers
          sb.append("    for (var e: DEFAULT_HEADERS.entrySet()) b.header(e.getKey(), e.getValue());\n");
          // Apply request headers (override with setHeader)
          for (Map.Entry<String,String> e : r.headers.entrySet()) {
            sb.append("    b.setHeader(").append(quote(e.getKey())).append(", ")
              .append(quote(substitute(e.getValue(), vars))).append(");\n");
          }
          sb.append("    HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));\n\n");
        } else if (s instanceof Expect) {
          if (!hadRequest) {
            throw new CompilerException("Assertion appears before any request in test '" + t.name + "'");
          }
          if (s instanceof Expect.StatusEq) {
            Expect.StatusEq e = (Expect.StatusEq) s;
            sb.append("    assertEquals(").append(e.status).append(", resp.statusCode());\n");
          } else if (s instanceof Expect.HeaderEq) {
            Expect.HeaderEq e = (Expect.HeaderEq) s;
            sb.append("    assertEquals(")
              .append(quote(substitute(e.expected, vars)))
              .append(", resp.headers().firstValue(").append(quote(e.key)).append(").orElse(\"\"));\n");
          } else if (s instanceof Expect.HeaderContains) {
            Expect.HeaderContains e = (Expect.HeaderContains) s;
            sb.append("    assertTrue(resp.headers().firstValue(").append(quote(e.key)).append(").orElse(\"\")")
              .append(".contains(").append(quote(substitute(e.needle, vars))).append("));\n");
          } else if (s instanceof Expect.BodyContains) {
            Expect.BodyContains e = (Expect.BodyContains) s;
            sb.append("    assertTrue(resp.body().contains(")
              .append(quote(substitute(e.needle, vars))).append("));\n");
          }
        }
      }

      sb.append("  }\n\n");
    }

    sb.append("}\n");

    Files.createDirectories(outFile.getParent());
    Files.write(outFile, sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String substitute(String s, Map<String, Ast.Value> vars) {
    StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); ) {
      char c = s.charAt(i);
      if (c == '$') {
        int j = i + 1;
        while (j < s.length()) {
          char cj = s.charAt(j);
          if (j == i + 1 && !Character.isJavaIdentifierStart(cj)) break;
          if (j > i + 1 && !Character.isJavaIdentifierPart(cj)) break;
          j++;
        }
        if (j == i + 1) { // literal '$'
          out.append('$'); i++;
        } else {
          String name = s.substring(i + 1, j);
          Ast.Value v = vars.get(name);
          if (v == null) throw new CompilerException("Undefined variable $" + name);
          out.append(v.toString());
          i = j;
        }
      } else {
        out.append(c); i++;
      }
    }
    return out.toString();
  }

  private static String quote(String s) {
    return "\"" + Strings.javaEscape(s) + "\"";
  }

  private static String buildUrlExpr(String base, String pathOrUrl) {
    if (pathOrUrl.startsWith("/")) {
      if (base == null || base.isEmpty()) {
        throw new CompilerException("Path '" + pathOrUrl + "' requires config.base_url");
      }
      return "BASE + " + quote(pathOrUrl);
    } else if (Strings.isAbsoluteUrl(pathOrUrl)) {
      return quote(pathOrUrl);
    } else {
      // treat as relative path without leading slash
      if (base == null || base.isEmpty()) {
        throw new CompilerException("Relative path '" + pathOrUrl + "' requires config.base_url");
      }
      String sep = base.endsWith("/") ? "" : "/";
      return "BASE + " + quote(sep + pathOrUrl);
    }
  }
}