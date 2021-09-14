parser grammar FeelParser;

options { tokenVocab=FeelLexer; }

program: moduleDeclaration globalDeclaration*;

moduleDeclaration: Module identifier Semi;

globalDeclaration
    : (globalVariableDeclaration
    | globalConstantDeclaration
    | globalFunctionDeclaration
    | globalRecordDeclaration
    | globalEnumDeclaration
    ) Semi
    ;

globalVariableDeclaration: Let Mut identifier type Equal expression;
globalConstantDeclaration: Let identifier type Equal expression;
globalFunctionDeclaration: Let identifier parameterList type Equal expression;
globalRecordDeclaration: Def identifier fieldList methodList?;
globalEnumDeclaration: Def identifier Case constructorList methodList?;

parameterList: LeftParen (parameter (Comma parameter)*)? RightParen;

parameter: identifier type;

fieldList: LeftParen (field (Comma field)*)? RightParen;

field: Mut? identifier type;

methodList: LeftBrace (method Semi)* RightBrace;

method: identifier parameterList type Equal expression;

constructorList: Or? constructor (Or constructor)*;

constructor: identifier fieldList;

block: LeftBrace (statement Semi)* RightBrace;

statement
    : variableDeclaration
    | constantDeclaration
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
    | expression callSuffix
    | expression memberAccess
    | expression multiplicativeOperator expression
    | expression additiveOperator expression
    | expression compareOperator expression
    | expression logicOperator expression
    ;

variableDeclaration: Let Mut identifier type? Equal expression;
constantDeclaration: Let identifier type? Equal expression;
functionDeclaration: Let identifier parameterList type? Equal expression;

callSuffix: LeftParen (expression (Comma expression)*)? RightParen;

memberAccess: Dot identifier;

assignment: identifier Equal expression;

ifStatement
    : If LeftParen expression RightParen block (Else (block | ifStatement))?
    ;

ifExpression
    : If LeftParen expression RightParen expression Else expression
    ;

whileStatement
    : While LeftParen expression RightParen block
    ;

blockExpression: LeftBrace (statement Semi)* expression? RightBrace;

primaryExpression
    : literalExpression
    | identifier
    ;

parenExpression: LeftParen expression RightParen;

literalExpression
    : integerExpression
    | floatExpression
    ;

type
    : identifier
    ;

identifier: Identifier;

floatExpression: FloatLiteral;

integerExpression: DecimalLiteral | BinaryLiteral | OctalLiteral | HexLiteral;

multiplicativeOperator: Mul | Div | Mod;

additiveOperator: Add | Sub ;

logicOperator: And | Or ;

compareOperator: Less | Greater | LessEqual | GreaterEqual | EqualEqual | NotEqual;