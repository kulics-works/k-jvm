lexer grammar FeelLexer;

Arrow: '->';
DoubleArrow: '->>';
FatArrow: '=>';

EqualEqual: '==';
NotEqual: '!=';
LessEqual: '<=';
GreaterEqual: '>=';

Dot: '.';
Comma: ',';

Equal: '=';
Less: '<';
Greater: '>';

SemiColon: ';';
Colon: ':';

LeftParen: '(';
RightParen: ')';
LeftBrace: '{';
RightBrace: '}';
LeftBrack: '[';
RightBrack: ']';

Question: '?';
At: '@';
Bang: '!';
Coin: '$';
Tilde: '~';

Add: '+';
Sub: '-';
Mul: '*';
Div: '/';
Mod: '%';

AndAnd: '&&';
OrOr: '||';
And: '&';
Or: '|';
Caret: '^';

BackQuote: '`';
Sharp: '#';

Mut: 'mut';
Let: 'let';
Module: 'mod';
If: 'if';
Then: 'then';
Else: 'else';
While: 'while';
For: 'for';
Do: 'do';
Is: 'is';
As: 'as';
In: 'in';
Case: 'case';
True: 'true';
False: 'false';
Type: 'type';
With: 'with';
Given: 'given';

FloatLiteral: Digit (Exponent | '.' Digit Exponent?);
DecimalLiteral: Digit;
BinaryLiteral: '0' [bB] [0-1_]* [0-1];
OctalLiteral: '0' [oO] [0-7_]* [0-7];
HexLiteral: '0' [xX] [a-fA-F0-9_]* [a-fA-F0-9];
fragment Digit: [0-9] | [0-9] [0-9_]* [0-9];
fragment Exponent: [eE] [+-]? [0-9]+;

CharLiteral: '\'' ('\\\'' | '\\' [btnfr\\] | .)*? '\'';
StringLiteral: '"' ('\\' [btnfr"\\] | ~('\\' | '"' )+)* '"';
UpperIdentifier: [A-Z] [0-9a-zA-Z_]*;
LowerIdentifier: [a-z] [0-9a-zA-Z_]*;
Discard: '_';

CommentBlock: '#*' .*? '*#' -> skip;
CommentLine: '##' ~[\r\n]* -> skip;

NewLine: '\r'? '\n';

WhiteSpace: (' ' | '\t')+ -> skip ;



