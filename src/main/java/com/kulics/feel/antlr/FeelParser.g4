parser grammar FeelParser;

options { tokenVocab=FeelLexer; }

program: moduleDeclaration globalDeclaration*;

moduleDeclaration: Module identifier Semi;

globalDeclaration
    : (globalVariableDeclaration
    | globalFunctionDeclaration
    | globalRecordDeclaration
    | globalInterfaceDeclaration
    ) Semi
    ;

globalVariableDeclaration: Let Mut? identifier type Equal expression;
globalFunctionDeclaration: Let identifier typeParameterList? parameterList type Equal expression;
globalRecordDeclaration: Def identifier typeParameterList? fieldList type? methodList?;
globalInterfaceDeclaration: Def identifier typeParameterList? virtualMethodList?;

typeParameterList: LeftBrack typeParameter (Comma typeParameter)* RightBrack;

typeParameter: identifier type;

parameterList: LeftParen (parameter (Comma parameter)*)? RightParen;

parameter: identifier type;

fieldList: LeftParen (field (Comma field)*)? RightParen;

field: Mut? identifier type;

methodList: LeftBrace (method Semi)* RightBrace;

method: identifier parameterList type Equal expression;

virtualMethodList: LeftBrace (virtualMethod Semi)* RightBrace;

virtualMethod: identifier parameterList type (Equal expression)?;

block: LeftBrace (statement Semi)* RightBrace;

statement
    : variableDeclaration
    | functionDeclaration
    | assignment
    | ifStatement
    | whileStatement
    | expression
    ;

expression
    : primaryExpression
    | parenExpression
    | blockExpression
    | ifExpression
    | expression memberAccessCallSuffix
    | expression callSuffix
    | expression memberAccess
    | expression multiplicativeOperator expression
    | expression additiveOperator expression
    | expression compareOperator expression
    | expression logicOperator expression
    ;

variableDeclaration: Let Mut? identifier type? Equal expression;
functionDeclaration: Let identifier parameterList type? Equal expression;

memberAccessCallSuffix: Dot identifier (LeftBrack type (Comma type)* RightBrack)? LeftParen (expression (Comma expression)*)? RightParen;

callSuffix: (LeftBrack type (Comma type)* RightBrack)? LeftParen (expression (Comma expression)*)? RightParen;

memberAccess: Dot identifier;

assignment: identifier Equal expression;

ifStatement
    : If LeftParen expression (Is pattern)? RightParen block (Else (block | ifStatement))?
    ;

ifExpression
    : If LeftParen expression (Is pattern)? RightParen expression Else expression
    ;

whileStatement
    : While LeftParen expression RightParen block
    ;

blockExpression: LeftBrace (statement Semi)* expression? RightBrace;

pattern
    : typePattern
    | literalPattern
    | identifierPattern
    | deconstructPattern
    | wildcardPattern
    ;

typePattern
    : type identifierPattern
    | type wildcardPattern
    | type deconstructPattern
    ;

deconstructPattern
    : LeftParen (pattern (Comma pattern)*)? RightParen
    ;

identifierPattern
    : identifier
    ;

wildcardPattern
    : Discard
    ;

literalPattern
    : literalExpression
    ;

primaryExpression
    : literalExpression
    | identifier
    ;

parenExpression: LeftParen expression RightParen;

literalExpression
    : integerExpression
    | floatExpression
    | characterExpression
    | stringExpression
    ;

type
    : identifier (LeftBrack type (Comma type)* RightBrack)?
    ;

identifier: Identifier;

stringExpression: StringLiteral;

characterExpression: CharLiteral;

floatExpression: FloatLiteral;

integerExpression: DecimalLiteral | BinaryLiteral | OctalLiteral | HexLiteral;

multiplicativeOperator: Mul | Div | Mod;

additiveOperator: Add | Sub ;

logicOperator: AndAnd | OrOr ;

compareOperator: Less | Greater | LessEqual | GreaterEqual | EqualEqual | NotEqual;