lexer grammar FeelLexer;

Arrow: '->';
FatArrow: '=>';

EqualEqual: '==';
LessEqual: '<=';
GreaterEqual: '>=';
NotEqual: '<>';

AndAnd: '&&';
OrOr: '||';

Dot: '.';
Comma: ',';

Equal: '=';
Less: '<';
Greater: '>';

Semi: ';';
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

And: '&';
Or: '|';
Caret: '^';

BackQuote: '`';
Sharp: '#';

Mut: 'mut';
Let: 'let';
Ext: 'ext';
Module: 'mod';
If: 'if';
Then: 'then';
Else: 'else';
While: 'while';
For: 'for';
Do: 'do';
Is: 'is';
True: 'true';
False: 'false';
Type: 'type';

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

CommentBlock: '/*' .*? '*/' -> skip;
CommentLine: '//' ~[\r\n]* -> skip;

WhiteSpace: (' ' |'\t' |'\n' |'\r' )+ -> skip ;



