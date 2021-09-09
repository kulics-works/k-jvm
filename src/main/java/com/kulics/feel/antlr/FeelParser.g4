parser grammar FeelParser;

options { tokenVocab=FeelLexer; }

program: moduleDeclaration globalDeclaration*;

moduleDeclaration: Module identifier Semi;

globalDeclaration
    : (globalVariableDeclaration
    | globalConstantDeclaration
    | globalFunctionDeclaration
    ) Semi
    ;

globalVariableDeclaration: Let Mut identifier type Equal expression;
globalConstantDeclaration: Let identifier type Equal expression;
globalFunctionDeclaration: Let identifier parameterList type Equal expression;

parameterList: LeftParen (parameter (Comma parameter)*)? RightParen;

parameter: identifier type;

block: LeftBrace (statement Semi)* RightBrace;

statement
    : variableDeclaration
    | constantDeclaration
    | expression
    ;

expression
    : primaryExpression
    | parenExpression
    | blockExpression
    | conditionExpression
    | expression callSuffix
    | expression multiplicativeOperator expression
    | expression additiveOperator expression
    | expression compareOperator expression
    | expression logicOperator expression
    ;

variableDeclaration: Let Mut identifier type? Equal expression;
constantDeclaration: Let identifier type? Equal expression;

callSuffix: LeftParen (expression (Comma expression)*)? RightParen;

conditionExpression
    : If LeftParen expression RightParen expression Else expression
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