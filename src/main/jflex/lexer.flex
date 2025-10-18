/* JFlex scanner for TestLang++ */
package tl.parser;

import java_cup.runtime.Symbol;

%%

%public
%class Lexer
%unicode
%cup
%line
%column
%type java_cup.runtime.Symbol

%{

  private Symbol sym(int id) {
    return new Symbol(id, yyline+1, yycolumn+1);
  }
  private Symbol sym(int id, Object val) {
    return new Symbol(id, yyline+1, yycolumn+1, val);
  }

  private String unescape(String s) {
    // s includes surrounding quotes
    StringBuilder b = new StringBuilder(s.length());
    for (int i = 1; i < s.length()-1; i++) {
      char c = s.charAt(i);
      if (c == '\\') {
        if (i+1 >= s.length()-1) { b.append('\\'); break; }
        char n = s.charAt(++i);
        switch (n) {
          case '\\': b.append('\\'); break;
          case '"': b.append('"'); break;
          case 'n': b.append('\n'); break;
          case 'r': b.append('\r'); break;
          case 't': b.append('\t'); break;
          default: b.append(n); break;
        }
      } else {
        b.append(c);
      }
    }
    return b.toString();
  }

%}

LineTerminator = \r|\n|\r\n
WhiteSpace     = [ \t\f]+
Comment        = "//".* {LineTerminator}?

Identifier = [A-Za-z_][A-Za-z0-9_]*
Number     = [0-9]+
String     = \"([^\"\\]|\\[\"nrt\\]|\\\\)*\"

%%

{WhiteSpace}   { /* skip */ }
{Comment}      { /* skip */ }
{LineTerminator} { /* skip */ }

/* Keywords */
"config"       { return sym(Sym.CONFIG); }
"base_url"     { return sym(Sym.BASE_URL); }
"header"       { return sym(Sym.HEADER); }
"let"          { return sym(Sym.LET); }
"test"         { return sym(Sym.TEST); }
"GET"          { return sym(Sym.GET); }
"POST"         { return sym(Sym.POST); }
"PUT"          { return sym(Sym.PUT); }
"DELETE"       { return sym(Sym.DELETE); }
"expect"       { return sym(Sym.EXPECT); }
"status"       { return sym(Sym.STATUS); }
"body"         { return sym(Sym.BODY); }
"contains"     { return sym(Sym.CONTAINS); }

/* Tokens */
{Identifier}   { return sym(Sym.IDENT, yytext()); }
{Number}       { return sym(Sym.NUMBER, Integer.parseInt(yytext())); }
{String}       { return sym(Sym.STRING, unescape(yytext())); }

/* Symbols */
"{"            { return sym(Sym.LBRACE); }
"}"            { return sym(Sym.RBRACE); }
"="            { return sym(Sym.EQ); }
";"            { return sym(Sym.SEMI); }

/* Anything else is an error */
.              { throw Errors.error("Unexpected character: '" + yytext() + "'", yyline+1, yycolumn+1); }