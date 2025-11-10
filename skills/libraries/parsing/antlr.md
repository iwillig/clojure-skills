---
name: antlr-parser-generator
description: |
  Build parsers and lexers using ANTLR 4 grammar definitions. Use when creating parsers for 
  custom languages, implementing DSLs, parsing structured text formats, tokenizing input, 
  building compilers or interpreters, or when the user mentions ANTLR, grammar definitions, 
  lexer rules, parser rules, LL(*) parsing, abstract syntax trees (AST), or language recognition.
  ANTLR (ANother Tool for Language Recognition) is a powerful parser generator that uses 
  LL(*) parsing to build parsers from grammar definitions. In Clojure, use clj-antlr to 
  create and use ANTLR parsers without compilation, with grammars defined inline or from files.
---

# ANTLR Parser Generator with clj-antlr

## Quick Start

ANTLR 4 builds parsers from grammar definitions. With clj-antlr, you can define grammars inline and parse immediately.

```clojure
(require '[clj-antlr.core :as antlr])

;; Define a simple calculator grammar inline
(def calc-grammar
  "grammar Calc;
   expr : term (('+' | '-') term)* ;
   term : factor (('*' | '/') factor)* ;
   factor : NUMBER | '(' expr ')' ;
   NUMBER : [0-9]+ ;
   WS : [ \\t\\r\\n]+ -> skip ;")

;; Create parser (no compilation needed!)
(def calc (antlr/parser calc-grammar))

;; Parse expressions
(calc "1+2*3")
;; => (:expr
;;     (:term (:factor "1"))
;;     "+"
;;     (:term (:factor "2") "*" (:factor "3")))

(calc "(5+3)*2")
;; => (:expr
;;     (:term
;;      (:factor "(" (:expr (:term (:factor "5")) "+" (:term (:factor "3"))) ")")
;;      "*"
;;      (:factor "2")))
```

**Key benefits:**
- **No compilation** - Define grammars inline, parse immediately
- **LL(*) parsing** - Adaptive LL parsing with unlimited lookahead
- **Rich syntax** - Character sets, ranges, EBNF operators, fragments
- **Error recovery** - Built-in error reporting and recovery
- **Tree output** - S-expressions representing parse trees

## Core Concepts

### Grammar Structure

A grammar has a header followed by rules:

```antlr
grammar Name;  // Grammar declaration

// Parser rules (lowercase)
expr : term (('+' | '-') term)* ;
term : factor (('*' | '/') factor)* ;

// Lexer rules (UPPERCASE)
NUMBER : [0-9]+ ;
WS : [ \t\r\n]+ -> skip ;
```

**Grammar types:**
- **Combined grammar** - Both lexer and parser rules (default)
- **Parser grammar** - `parser grammar Name;` - Only parser rules
- **Lexer grammar** - `lexer grammar Name;` - Only lexer rules

### Parser Rules vs Lexer Rules

**Parser Rules** (lowercase):
- Define structure of language
- Reference other parser rules and tokens
- Build parse tree structure

**Lexer Rules** (UPPERCASE):
- Define tokens (terminal symbols)
- Match character sequences
- Can reference other lexer rules

```clojure
(def grammar
  "grammar Example;
   
   // Parser rule - defines structure
   statement : ID '=' expr ';' ;
   expr : NUMBER | STRING ;
   
   // Lexer rules - define tokens
   ID : [a-zA-Z]+ ;
   NUMBER : [0-9]+ ;
   STRING : '\"' .*? '\"' ;
   WS : [ \\t\\r\\n]+ -> skip ;")
```

### Parse Tree Structure

clj-antlr returns S-expressions where:
- Each list starts with rule name keyword
- Terminal tokens are strings
- Children follow in order

```clojure
(calc "1+2")
;; => (:expr (:term (:factor "1")) "+" (:term (:factor "2")))
;;     ^rule  ^subrule        ^token ^token
```

### EBNF Operators

ANTLR supports Extended Backus-Naur Form:

| Operator | Meaning | Example |
|----------|---------|---------|
| `?` | Optional (0 or 1) | `sign? NUMBER` |
| `*` | Zero or more | `[0-9]*` |
| `+` | One or more | `[a-zA-Z]+` |
| `\|` | Alternative | `'+'  \| '-'` |
| `( )` | Group | `('a' \| 'b')+` |

## Common Workflows

### Workflow 1: Building a Simple Expression Parser

Parse arithmetic expressions with operator precedence:

```clojure
(require '[clj-antlr.core :as antlr])

(def expr-grammar
  "grammar Expr;
   
   // Parser rules - define structure with precedence
   expr   : term (('+' | '-') term)* ;
   term   : factor (('*' | '/') factor)* ;
   factor : NUMBER 
          | '(' expr ')' ;
   
   // Lexer rules - define tokens
   NUMBER : [0-9]+ ('.' [0-9]+)? ;  // Integer or decimal
   WS     : [ \\t\\r\\n]+ -> skip ;  // Skip whitespace
   ")

(def expr-parser (antlr/parser expr-grammar))

;; Parse with correct precedence
(expr-parser "2+3*4")
;; => (:expr 
;;     (:term (:factor "2"))
;;     "+"
;;     (:term (:factor "3") "*" (:factor "4")))

;; Parse with parentheses
(expr-parser "(2+3)*4")
;; => (:expr
;;     (:term
;;      (:factor "(" (:expr (:term (:factor "2")) "+" (:term (:factor "3"))) ")")
;;      "*"
;;      (:factor "4")))

;; Parse decimals
(expr-parser "3.14+2.5")
;; => (:expr (:term (:factor "3.14")) "+" (:term (:factor "2.5")))
```

### Workflow 2: Lexer with Character Sets and Ranges

Define tokens using character classes:

```clojure
(def identifier-grammar
  "grammar Identifier;
   
   program : statement+ ;
   statement : ID '=' value ';' ;
   value : NUMBER | STRING ;
   
   // Lexer rules with character sets
   ID     : [a-zA-Z_][a-zA-Z0-9_]* ;  // C-style identifiers
   NUMBER : [0-9]+ ;
   STRING : '\"' (~[\"\\\\] | '\\\\' .)* '\"' ;  // Strings with escapes
   WS     : [ \\t\\r\\n]+ -> skip ;
   ")

(def id-parser (antlr/parser identifier-grammar))

(id-parser "count = 42;
            name = \"Alice\";
            _value = 123;")
;; => (:program
;;     (:statement "count" "=" (:value "42") ";")
;;     (:statement "name" "=" (:value "\"Alice\"") ";")
;;     (:statement "_value" "=" (:value "123") ";"))
```

**Character set syntax:**
- `[abc]` - Any of a, b, or c
- `[a-z]` - Range from a to z
- `[^abc]` - Anything except a, b, or c
- `~[abc]` - Same as `[^abc]` in lexer
- `\n \r \t \f \b` - Escape sequences
- `\uXXXX` - Unicode characters

### Workflow 3: Fragment Rules for Reusable Components

Use fragments to build complex lexer rules:

```clojure
(def number-grammar
  "grammar Numbers;
   
   value : number+ ;
   
   // Main token rules
   number : INT | FLOAT | HEX | BINARY ;
   
   // Fragment rules (not tokens themselves)
   INT    : DIGIT+ ;
   FLOAT  : DIGIT+ '.' DIGIT+ EXPONENT? ;
   HEX    : '0x' HEX_DIGIT+ ;
   BINARY : '0b' [01]+ ;
   
   fragment DIGIT     : [0-9] ;
   fragment HEX_DIGIT : [0-9a-fA-F] ;
   fragment EXPONENT  : [eE] [+-]? DIGIT+ ;
   
   WS : [ \\t\\r\\n]+ -> skip ;
   ")

(def num-parser (antlr/parser number-grammar))

(num-parser "42 3.14 0xFF 0b1010")
;; => (:value
;;     (:number "42")
;;     (:number "3.14")
;;     (:number "0xFF")
;;     (:number "0b1010"))
```

**Fragment rules:**
- Marked with `fragment` keyword
- Don't create tokens themselves
- Only used by other lexer rules
- Help organize and reuse lexer logic

### Workflow 4: Handling JSON with Recursive Rules

Parse nested structures:

```clojure
(def json-grammar
  "grammar Json;

   jsonText : jsonValue ;

   jsonObject
      : '{' pair (',' pair)* '}'
      | '{' '}'
      ;

   pair : STRING ':' jsonValue ;

   jsonArray
      : '[' jsonValue (',' jsonValue)* ']'
      | '[' ']'
      ;

   // Recursive rule - jsonValue can contain objects/arrays
   jsonValue
      : STRING
      | NUMBER
      | jsonObject  // Recursion
      | jsonArray   // Recursion
      | 'true'
      | 'false'
      | 'null'
      ;

   STRING : '\"' (ESC | ~[\"\\\\])* '\"' ;
   fragment ESC : '\\\\' ([\"\\\\bfnrt] | UNICODE) ;
   fragment UNICODE : 'u' HEX HEX HEX HEX ;
   fragment HEX : [0-9a-fA-F] ;

   NUMBER : '-'? INT ('.' [0-9]+)? EXP? ;
   fragment INT : '0' | [1-9] [0-9]* ;
   fragment EXP : [Ee] [+-]? INT ;

   WS : [ \\t\\n\\r]+ -> skip ;")

(def json-parser (antlr/parser json-grammar))

;; Parse nested JSON
(json-parser "{\"name\":\"Alice\",\"scores\":[95,87,92]}")
;; => (:jsonText
;;     (:jsonValue
;;      (:jsonObject
;;       "{"
;;       (:pair "\"name\"" ":" (:jsonValue "\"Alice\""))
;;       ","
;;       (:pair
;;        "\"scores\""
;;        ":"
;;        (:jsonValue
;;         (:jsonArray
;;          "["
;;          (:jsonValue "95")
;;          ","
;;          (:jsonValue "87")
;;          ","
;;          (:jsonValue "92")
;;          "]")))
;;       "}")))
```

### Workflow 5: Error Handling and Recovery

ANTLR provides detailed error information:

```clojure
;; Parse with errors disabled to get partial parse tree
(antlr/parse json-parser {:throw? false} "{\"incomplete")
;; Returns parse tree with :errors in metadata

;; Get error details
(try
  (json-parser "{invalid json")
  (catch clojure.lang.ExceptionInfo e
    ;; Deref exception for error details
    (let [errors @e]
      (doseq [err errors]
        (println (format "Line %d:%d - %s"
                        (:line err)
                        (:char err)
                        (:message err)))))))
;; Prints:
;; Line 1:1 - token recognition error at: 'i'
;; Line 1:2 - token recognition error at: 'nv'
;; ...

;; Parse with errors suppressed
(let [result (antlr/parse json-parser {:throw? false} "{bad}")
      errors (-> result meta :errors)]
  {:tree result
   :error-count (count errors)
   :first-error (first errors)})
```

**Error information includes:**
- `:line` - Line number (1-based)
- `:char` - Character position in line
- `:message` - Human-readable error description
- `:token` - The problematic token
- `:expected` - What tokens were expected
- `:state` - Parser state (for debugging)

### Workflow 6: Multiple Alternatives and Labels

Use `|` for alternatives and labels to distinguish them:

```clojure
(def expr-alt-grammar
  "grammar ExprAlt;
   
   expr
      : expr '*' expr   # Mult
      | expr '/' expr   # Div
      | expr '+' expr   # Add
      | expr '-' expr   # Sub
      | NUMBER          # Number
      | '(' expr ')'    # Parens
      ;
   
   NUMBER : [0-9]+ ;
   WS : [ \\t\\r\\n]+ -> skip ;
   ")

;; With :use-alternates? true, uses labels instead of rule names
(def expr-alt-parser 
  (antlr/parser expr-alt-grammar {:use-alternates? true}))

(expr-alt-parser "2+3*4")
;; => (:Add
;;     (:Number "2")
;;     "+"
;;     (:Mult (:Number "3") "*" (:Number "4")))
```

### Workflow 7: Lexer Modes for Context-Sensitive Lexing

Use modes to handle different contexts (lexer grammars only):

```clojure
(def xml-lexer-grammar
  "lexer grammar XMLLexer;

   // Default mode - outside tags
   OPEN     : '<' -> pushMode(INSIDE) ;
   TEXT     : ~[<]+ ;

   mode INSIDE;
   CLOSE    : '>' -> popMode ;
   SLASH    : '/' ;
   NAME     : [a-zA-Z]+ ;
   EQUALS   : '=' ;
   STRING   : '\"' ~[<\"]* '\"' ;
   WS_INSIDE: [ \\t\\r\\n]+ -> skip ;
   ")

;; Note: Would need separate parser grammar for full XML parser
;; This example shows lexer mode concept
```

**Mode commands:**
- `-> pushMode(MODE)` - Push mode onto stack
- `-> popMode` - Pop current mode
- `-> mode(MODE)` - Switch to mode
- `-> skip` - Ignore token
- `-> channel(HIDDEN)` - Send to hidden channel

### Workflow 8: Using Grammar Files

Load grammars from files for larger projects:

```clojure
;; Save grammar to file
(spit "MyLang.g4"
      "grammar MyLang;
       program : statement+ ;
       statement : 'print' STRING ';' ;
       STRING : '\"' .*? '\"' ;
       WS : [ \\t\\r\\n]+ -> skip ;")

;; Load from file
(def my-lang-parser (antlr/parser "MyLang.g4"))

(my-lang-parser "print \"Hello\"; print \"World\";")
;; => (:program
;;     (:statement "print" "\"Hello\"" ";")
;;     (:statement "print" "\"World\"" ";"))

;; Split lexer and parser grammars
(def split-parser 
  (antlr/parser "MyLangLexer.g4" "MyLangParser.g4"))
```

## Grammar Writing Guidelines

### Rule Naming Conventions

**Parser rules** (lowercase):
- Start with lowercase letter
- Use descriptive names: `statement`, `expression`, `declaration`
- CamelCase or snake_case: `methodCall` or `method_call`

**Lexer rules** (UPPERCASE):
- Start with uppercase letter
- Use ALL_CAPS: `IDENTIFIER`, `NUMBER`, `STRING`
- Can use underscores: `BLOCK_COMMENT`, `LINE_COMMENT`

### Left Recursion

ANTLR 4 handles direct left recursion automatically:

```antlr
// This works! (direct left recursion)
expr : expr '+' expr
     | expr '*' expr
     | NUMBER
     ;

// This doesn't work (indirect left recursion)
expr : term ;
term : expr '+' NUMBER ;  // Indirect - expr -> term -> expr
```

**Handling indirect left recursion**: Refactor to eliminate it:

```antlr
// Fixed version - no indirect left recursion
expr : term ('+' term)* ;
term : NUMBER ;
```

### Operator Precedence

Control precedence by rule ordering (higher rules = lower precedence):

```antlr
expr   : term (('+' | '-') term)* ;    // Lowest precedence
term   : factor (('*' | '/') factor)* ;  // Higher precedence
factor : NUMBER | '(' expr ')' ;         // Highest precedence
```

Alternative: Use single rule with left-recursive alternatives (ANTLR resolves):

```antlr
expr : expr '*' expr      // Higher precedence (deeper in tree)
     | expr '+' expr      // Lower precedence
     | NUMBER
     ;
```

### Ambiguity Resolution

ANTLR uses **first-match-wins** for lexer rules:

```antlr
// 'if' matches IF, not ID
IF  : 'if' ;
ID  : [a-z]+ ;  // Would also match 'if', but IF comes first
```

For parser rules, ANTLR tries alternatives in order:

```antlr
statement
    : ifStatement    // Try this first
    | assignment     // Then this
    | expression     // Finally this
    ;
```

## Best Practices

**Do:**
- Use fragments for reusable lexer components
- Put most specific lexer rules first (e.g., keywords before identifiers)
- Use character sets `[a-z]` instead of alternatives `'a'|'b'|'c'`
- Skip whitespace in lexer: `WS : [ \t\r\n]+ -> skip ;`
- Test grammar with both valid and invalid inputs
- Use meaningful rule and token names
- Add comments explaining complex rules
- Separate lexer and parser concerns

```clojure
;; Good: Clear structure with fragments
(def good-grammar
  "grammar Good;
   
   // Parser rules
   statement : ID '=' expr ;
   expr : NUMBER | STRING ;
   
   // Lexer rules with fragments
   ID     : LETTER (LETTER | DIGIT)* ;
   NUMBER : DIGIT+ ;
   STRING : '\"' ESC_CHAR* '\"' ;
   
   fragment LETTER   : [a-zA-Z_] ;
   fragment DIGIT    : [0-9] ;
   fragment ESC_CHAR : ~[\"\\\\] | '\\\\' . ;
   
   WS : [ \\t\\r\\n]+ -> skip ;
   ")
```

**Don't:**
- Use indirect left recursion (causes infinite loops)
- Forget to skip whitespace (will fail to parse)
- Make lexer rules too general (causes ambiguity)
- Use parser rules in lexer rules (not allowed)
- Create ambiguous grammars without understanding resolution
- Forget quotes around literal strings: `'+'` not `+`

```clojure
;; Bad: Ambiguous lexer rules
(def bad-grammar
  "grammar Bad;
   
   value : ID | KEYWORD ;
   
   // Bad: KEYWORD can't override ID for specific words
   ID      : [a-z]+ ;     // Matches 'if', 'while', etc.
   KEYWORD : 'if' | 'while' ;  // Never matches!
   ")

;; Good: Keywords before identifiers
(def fixed-grammar
  "grammar Fixed;
   
   value : KEYWORD | ID ;
   
   // Good: Specific rules first
   KEYWORD : 'if' | 'while' ;
   ID      : [a-z]+ ;
   ")
```

## Common Issues

### Issue: Parser can't find lexer rules

```clojure
(def broken
  "grammar Broken;
   statement : id '=' NUMBER ;  // Lowercase - parser rule reference
   id : ID ;                    // Should reference ID token
   NUMBER : [0-9]+ ;
   ")
;; Error: No such parser rule 'ID'
```

**Solution**: Lexer rules must be UPPERCASE:

```clojure
(def fixed
  "grammar Fixed;
   statement : ID '=' NUMBER ;  // Uppercase - lexer rule
   ID : [a-zA-Z]+ ;
   NUMBER : [0-9]+ ;
   ")
```

### Issue: Keywords not recognized

```clojure
(def keyword-problem
  "grammar KW;
   statement : ID | 'if' | 'while' ;
   ID : [a-z]+ ;  // Matches 'if' and 'while' first!
   ")
```

**Solution**: Put literal keywords before ID rule:

```clojure
(def keyword-fixed
  "grammar KW;
   statement : IF | WHILE | ID ;
   IF    : 'if' ;
   WHILE : 'while' ;
   ID    : [a-z]+ ;
   ")
```

### Issue: Whitespace not ignored

```clojure
(def no-ws
  "grammar NoWS;
   expr : NUMBER '+' NUMBER ;
   NUMBER : [0-9]+ ;
   // Missing: WS : [ \t\r\n]+ -> skip ;
   ")

(def p (antlr/parser no-ws))
(p "1 + 2")  ;; Fails! Whitespace not skipped
```

**Solution**: Always skip whitespace:

```clojure
(def with-ws
  "grammar WithWS;
   expr : NUMBER '+' NUMBER ;
   NUMBER : [0-9]+ ;
   WS : [ \\t\\r\\n]+ -> skip ;  // Skip whitespace
   ")
```

### Issue: Indirect left recursion

```clojure
(def indirect-lr
  "grammar IndirectLR;
   expr : term ;
   term : expr '+' NUMBER ;  // Indirect left recursion
   NUMBER : [0-9]+ ;
   ")
;; Causes stack overflow or infinite loop
```

**Solution**: Eliminate indirect recursion:

```clojure
(def fixed-lr
  "grammar FixedLR;
   expr : NUMBER ('+' NUMBER)* ;
   NUMBER : [0-9]+ ;
   ")
```

### Issue: Escape sequences in strings

```antlr
// Wrong - backslash not escaped
STRING : '"' ~["]* '"' ;  // Allows \" inside string!

// Right - properly escape backslash
STRING : '\"' (~[\"\\\\])* '\"' ;
```

## Advanced Topics

### Tokens and Token Specifications

Predefine token types without rules:

```antlr
tokens { BEGIN, END, IF, THEN }

@lexer::members {
  // Map keywords to token types
  Map<String,Integer> keywords = ...
}
```

### Semantic Predicates

Use predicates for context-sensitive parsing:

```antlr
// Predicate controls rule visibility
ID : [a-zA-Z]+ { isKeyword() }? ;
```

### Custom Start Rules

Specify which rule to start parsing from:

```clojure
(def grammar "grammar G;
              start : expr ;
              expr : NUMBER ;
              stmt : ID ;
              NUMBER : [0-9]+ ;
              ID : [a-z]+ ;")

;; Parse from different start rules
(def expr-parser (antlr/parser grammar {:root "expr"}))
(def stmt-parser (antlr/parser grammar {:root "stmt"}))

(expr-parser "123")  ;; Uses expr rule
(stmt-parser "abc")  ;; Uses stmt rule
```

### Case-Insensitive Lexing

Make lexer case-insensitive:

```clojure
(def case-insensitive
  (antlr/parser "grammar CI;
                 word : WORD ;
                 WORD : [a-z]+ ;"
                {:case-sensitive? false}))

(case-insensitive "HeLLo")  ;; Matches despite mixed case
```

### Accessing Raw Parse Tree

Get ANTLR's raw parse tree instead of S-expressions:

```clojure
(def raw-parser
  (antlr/parser grammar {:format :raw}))

;; Returns org.antlr.v4.runtime.tree.ParseTree object
(raw-parser "1+2")
```

## Related Tools and Libraries

- **ANTLR 4** - The underlying parser generator
- **instaparse** - Alternative parser combinator library (slower but pure Clojure)
- **clojure.spec** - For data validation after parsing
- **kern** - Parser combinator library
- **parsley** - PEG parser for Clojure

## Resources

- ANTLR 4 docs: https://github.com/antlr/antlr4/tree/4.6/doc
- ANTLR 4 book: "The Definitive ANTLR 4 Reference" by Terence Parr
- clj-antlr: https://github.com/aphyr/clj-antlr
- Grammar repository: https://github.com/antlr/grammars-v4
- ANTLR tutorial: https://tomassetti.me/antlr-mega-tutorial/

## Summary

ANTLR with clj-antlr enables powerful parsing in Clojure:

1. **No Compilation** - Define grammars inline, parse immediately
2. **LL(*) Parsing** - Adaptive parsing with unlimited lookahead
3. **Rich Grammar Syntax** - Character sets, EBNF operators, fragments, modes
4. **Error Recovery** - Detailed error reporting and recovery
5. **Parse Trees** - S-expression output ready for processing

**Most common patterns:**

```clojure
;; Simple grammar inline
(def parser (antlr/parser "grammar G; rule : TOKEN ; TOKEN : [a-z]+ ;"))

;; Parse with error handling
(try
  (parser input)
  (catch clojure.lang.ExceptionInfo e
    (println "Parse errors:" @e)))

;; Parse with partial results on error
(antlr/parse parser {:throw? false} input)

;; Load from file
(def parser (antlr/parser "MyGrammar.g4"))

;; Character sets and fragments
fragment DIGIT : [0-9] ;
NUMBER : DIGIT+ ;

;; Skip whitespace
WS : [ \t\r\n]+ -> skip ;
```

Perfect for building DSLs, compilers, interpreters, and parsing structured text formats in Clojure.
