/*
 * Copyright 2025-2026 Johannes Zemlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

grammar ossie;

// Initial expression grammar: literals, (qualified) identifiers, function calls, arithmetic,
// comparison and boolean logic. Alternatives of `expression` are listed from highest to lowest
// precedence.

expression
 : expr EOF
;

expr
 : LPAREN expr RPAREN                                   # parenExpr
 | functionCall                                         # functionExpr
 | (PLUS | MINUS) expr                                  # unaryExpr
 | expr (STAR | SLASH | PERCENT) expr                   # multiplicativeExpr
 | expr (PLUS | MINUS) expr                             # additiveExpr
 | expr (EQ | NEQ | LT | LTE | GT | GTE) expr           # comparisonExpr
 | NOT expr                                             # notExpr
 | expr AND expr                                        # andExpr
 | expr OR expr                                         # orExpr
 | literal                                              # literalExpr
 | qualifiedName                                        # referenceExpr
;

functionCall
 : IDENTIFIER LPAREN (expr (COMMA expr)*)? RPAREN
;

qualifiedName
 : IDENTIFIER (DOT IDENTIFIER)*
;

literal
 : NUMBER
 | STRING
 | TRUE
 | FALSE
 | NULL
;

// Keywords are case-insensitive and must precede IDENTIFIER.
AND   : [aA][nN][dD] ;
OR    : [oO][rR] ;
NOT   : [nN][oO][tT] ;
TRUE  : [tT][rR][uU][eE] ;
FALSE : [fF][aA][lL][sS][eE] ;
NULL  : [nN][uU][lL][lL] ;

EQ      : '=' ;
NEQ     : '<>' | '!=' ;
LTE     : '<=' ;
GTE     : '>=' ;
LT      : '<' ;
GT      : '>' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
PERCENT : '%' ;
LPAREN  : '(' ;
RPAREN  : ')' ;
COMMA   : ',' ;
DOT     : '.' ;

NUMBER     : DIGIT+ ('.' DIGIT+)? ([eE] [+-]? DIGIT+)? ;
STRING     : '\'' ( ~'\'' | '\'\'' )* '\'' ;
IDENTIFIER : [a-zA-Z_] [a-zA-Z_0-9]* | '"' ( ~'"' | '""' )* '"' ;

WS : [ \t\r\n]+ -> skip ;

fragment DIGIT : [0-9] ;
