lexer grammar FeelLexer;

EqualEqual: '==';
LessEqual: '<=';
GreaterEqual: '>=';
NotEqual: '<>';

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

Var: 'var';
Fun: 'fun';
Module: 'mod';
If: 'if';
Else: 'else';

FloatLiteral: Digit (Exponent | '.' Digit Exponent?);
DecimalLiteral: Digit;
BinaryLiteral: '0' [bB] [0-1_]* [0-1];
OctalLiteral: '0' [oO] [0-7_]* [0-7];
HexLiteral: '0' [xX] [a-fA-F0-9_]* [a-fA-F0-9];
fragment Digit: [0-9] | [0-9] [0-9_]* [0-9];   // 单个数字
fragment Exponent: [eE] [+-]? [0-9]+;

CharLiteral: '\'' ('\\\'' | '\\' [btnfr\\] | .)*? '\''; // 单字符
VariableIdentifier: [a-z] [0-9a-zA-Z_]*; // 私有标识符
ConstantIdentifier: [A-Z] [0-9a-zA-Z_]*; // 公有标识符
Discard: '_'; // 匿名变量

CommentBlock: '/*' .*? '*/' -> skip; // 块注释
CommentLine: '//' ~[\r\n]* -> skip; // 行注释

WhiteSpace: (' ' |'\t' |'\n' |'\r' )+ -> skip ;



