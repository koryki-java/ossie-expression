# ossie-expression

An [ANTLR 4](https://www.antlr.org/) grammar, and the lexer and parser generated from it, for the
**Ossie expression language** (`OSSIE_SQL_2026`): the portable SQL expression subset that
[Apache Ossie](https://github.com/apache/ossie) uses for metrics, fields and filters in the logical
layer of a semantic model.

## Why

The expression language is specified in prose
([`core-spec/expression_language.md`](https://github.com/apache/ossie/blob/main/core-spec/expression_language.md)).
Every implementation, converter or validator that has to read an expression writes its own parser
from that text. A shared, executable grammar gives them one definition of what is syntactically
valid, and a test corpus to check against.

## What is in the grammar

[`ossie.g4`](src/main/antlr/ai/koryki/ossie/ossie.g4) is a combined lexer and parser grammar. Entry
rule: `expression`.

| Area | Covered |
| :--- | :--- |
| Operators | `+ - * / %`, `\|\|`, `= <> != < > <= >=`, `AND OR NOT` |
| Predicates | `BETWEEN`, `IN (list)`, `LIKE`, `ILIKE`, `IS [NOT] NULL`, `IS [NOT] DISTINCT FROM` |
| Conditionals | searched and simple `CASE` |
| Conversion | `CAST`, `TRY_CAST`, typed literals (`DATE '…'`, `TIME '…'`, `TIMESTAMP_NTZ '…'`) |
| Special forms | `EXTRACT(part FROM x)`, `POSITION(a IN b)`, `CURRENT_DATE` with or without `()` |
| Aggregates | `COUNT(*)`, `DISTINCT`, `FILTER (WHERE …)`, `WITHIN GROUP (ORDER BY …)` |
| Windows | `OVER (PARTITION BY … ORDER BY … [NULLS FIRST\|LAST] ROWS\|RANGE …)` |
| Names | qualified names (`dataset.field`), `"quoted"` identifiers with `""` escapes |
| Lexical | case-insensitive keywords, `'string'` literals, numbers, `--` and `/* */` comments |

Subqueries, `SELECT`/`FROM`/`WHERE`/`GROUP BY`, CTEs and set operations are out of scope, as in the
specification.

### Design decisions

* **Functions are parsed generically** as `name(args)`. `SUM`, `DATE_TRUNC` or `COALESCE` have no
  rules of their own; which functions exist, their arity and their types are semantic questions,
  and dialect extensions pass through unchanged, as the specification asks. Only constructs with
  their own syntax get their own rule.
* **Few reserved words.** Words that only mean something in one place (`date`, `first`, `range`,
  `over`, …) stay usable as column names.
* **Precedence follows ANSI SQL**, with two deliberate departures from the list in the
  specification: `NOT` binds looser than comparisons (the spec lists it above `*`, which would
  make `NOT a = b` mean `(NOT a) = b`), and `||` binds like `+`. Operands of comparisons and
  predicates are arithmetic expressions, so `a = b = c` needs parentheses. These points are worth
  settling in the specification itself.
* **Not enforced by the grammar:** the 128-character identifier limit, identifier normalisation
  (upper-casing of regular identifiers) and the dialect declaration around an expression.

## Build and test

Requires a JDK; the build uses a Java 21 toolchain.

```bash
./gradlew build
```

This generates the parser (with `-Werror`, so a misspelt token fails the build rather than
silently never matching), checks formatting with Spotless and runs the tests.

The generated classes are `ai.koryki.ossie.ossieLexer`, `ossieParser` and the visitor/listener
classes. Using them:

```java
ossieParser parser = new ossieParser(
        new CommonTokenStream(new ossieLexer(CharStreams.fromString("SUM(amount) FILTER (WHERE status = 'completed')"))));
ossieParser.ExpressionContext tree = parser.expression();
```

Only `antlr4-runtime` is exposed to consumers (`api`); the ANTLR tool is not.

## Tests and fixtures

`src/test/resources/ai/koryki/ossie`:

| File | Content |
| :--- | :--- |
| `spec-examples.txt` | Examples from the specification, plus one or more per construct it defines |
| `ossie-examples.txt` | `ANSI_SQL` expressions from [`examples/`](https://github.com/apache/ossie/tree/main/examples) in apache/ossie |
| `invalid.txt` | Inputs that must be rejected |

One expression per line, `# ` starts a comment. The tests additionally pin down precedence
and associativity. Apache Ossie has no expression test corpus of its own yet (`compliance/` is
empty), so growing these files, for instance from the converters in
[apache/ossie-converters](https://github.com/apache/ossie-converters), is the most useful next step.

## Repository layout

```
src/main/antlr/ai/koryki/ossie/ossie.g4   the grammar
src/test/java/…/OssieParserTest.java      fixture-driven and precedence tests
src/test/resources/…                      fixtures
expression_language.md                    development copy of the specification (see below)
```

`expression_language.md` is a copy of upstream's
[`core-spec/expression_language.md`](https://github.com/apache/ossie/blob/main/core-spec/expression_language.md),
kept here for convenience while developing the grammar. The upstream file is authoritative and the
copy is not meant to be changed; it should be removed once the project lives inside apache/ossie.

## Status

Initial version. The grammar accepts everything the specification defines and every `ANSI_SQL`
expression in the upstream examples. No semantic layer yet: no function catalogue, type checking or
identifier resolution.

## License

Apache License, Version 2.0. See [LICENSE](LICENSE). `expression_language.md` and the fixtures
taken from apache/ossie are © The Apache Software Foundation, under the same license.
