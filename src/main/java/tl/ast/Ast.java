package tl.ast;

import java.util.*;

public final class Ast {

  public static final class Program {
    public final Config config; // may be null
    public final List<Let> lets;
    public final List<Test> tests;

    public Program(Config config, List<Let> lets, List<Test> tests) {
      this.config = config;
      this.lets = Collections.unmodifiableList(new ArrayList<>(lets));
      this.tests = Collections.unmodifiableList(new ArrayList<>(tests));
    }
  }

  public static final class Config {
    public final String baseUrl; // may be null
    public final LinkedHashMap<String, String> defaultHeaders;

    public Config(String baseUrl, LinkedHashMap<String, String> defaultHeaders) {
      this.baseUrl = baseUrl;
      this.defaultHeaders = defaultHeaders;
    }
  }

  public static final class Let {
    public final String name;
    public final Value value;

    public Let(String name, Value value) {
      this.name = name;
      this.value = value;
    }
  }

  public static abstract class Value {
    public static Value str(String s) { return new Str(s); }
    public static Value num(int n) { return new Num(n); }

    public static final class Str extends Value {
      public final String v;
      public Str(String v) { this.v = v; }
      @Override public String toString() { return v; }
    }

    public static final class Num extends Value {
      public final int v;
      public Num(int v) { this.v = v; }
      @Override public String toString() { return Integer.toString(v); }
    }
  }

  public enum Method { GET, POST, PUT, DELETE }

  public static final class Test {
    public final String name;
    public final List<Step> steps;

    public Test(String name, List<Step> steps) {
      this.name = name;
      this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }
  }

  public static abstract class Step {
    public static final class Request extends Step {
      public final Method method;
      public final String path; // string literal
      public final LinkedHashMap<String, String> headers; // per-request
      public final String body; // may be null

      public Request(Method method, String path, LinkedHashMap<String, String> headers, String body) {
        this.method = method; this.path = path; this.headers = headers; this.body = body;
      }
    }

    public static abstract class Expect extends Step {
      public static final class StatusEq extends Expect {
        public final int status;
        public StatusEq(int status) { this.status = status; }
      }
      public static final class HeaderEq extends Expect {
        public final String key, expected;
        public HeaderEq(String key, String expected) { this.key = key; this.expected = expected; }
      }
      public static final class HeaderContains extends Expect {
        public final String key, needle;
        public HeaderContains(String key, String needle) { this.key = key; this.needle = needle; }
      }
      public static final class BodyContains extends Expect {
        public final String needle;
        public BodyContains(String needle) { this.needle = needle; }
      }
    }
  }
}