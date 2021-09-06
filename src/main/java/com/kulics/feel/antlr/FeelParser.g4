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

block: LeftBrace statement* RightBrace;

blockExpression: LeftBrace statement* expression RightBrace;

statement: expression Semi;

expression
    : primaryExpression
    | parenExpression
    | variableDeclaration
    | constantDeclaration
    | conditionExpression
    | expression callSuffix
    | expression multiplicativeOperator expression
    | expression additiveOperator expression
    ;

variableDeclaration: Let Mut identifier type? Equal expression;
constantDeclaration: Let identifier type? Equal expression;

callSuffix: LeftParen (expression (Comma expression)*)? RightParen;

conditionExpression
    : If LeftParen expression RightParen block Else block
    | If LeftParen expression RightParen blockExpression Else blockExpression
    ;

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
