lexer grammar FeelLexer;

Equal_Equal: '==';
Less_Equal: '<=';
Greater_Equal: '>=';
Not_Equal: '<>';

Dot: '.';
Comma: ',';

Equal: '=';
Less: '<';
Greater: '>';

Semi: ';';
Colon: ':';

Left_Paren: '(';
Right_Paren: ')';
Left_Brace: '{';
Right_Brace: '}';
Left_Brack: '[';
Right_Brack: ']';

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

Back_Quote: '`';
Sharp: '#';

Var: 'var';
Fun: 'fun';
Module: 'mod';
If: 'if';
Else: 'else';

Float_Literal: Digit (Exponent | '.' Digit Exponent?);
Decimal_Literal: Digit;
Binary_Literal: '0' [bB] [0-1_]* [0-1];
Octal_Literal: '0' [oO] [0-7_]* [0-7];
Hex_Literal: '0' [xX] [a-fA-F0-9_]* [a-fA-F0-9];
fragment Digit: [0-9] | [0-9] [0-9_]* [0-9];   // 单个数字
fragment Exponent: [eE] [+-]? [0-9]+;

Char_Literal: '\'' ('\\\'' | '\\' [btnfr\\] | .)*? '\''; // 单字符
Variable_Identifier: [a-z] [0-9a-zA-Z_]*; // 私有标识符
Constant_Identifier: [A-Z] [0-9a-zA-Z_]*; // 公有标识符
Discard: '_'; // 匿名变量

Comment_Block: '/*' .*? '*/' -> skip; // 块注释
Comment_Line: '//' ~[\r\n]* -> skip; // 行注释

White_Space: (' ' |'\t' |'\n' |'\r' )+ -> skip ;



