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

globalVariableDeclaration: Var identifier type Equal expression;
globalConstantDeclaration: Let identifier type Equal expression;
globalFunctionDeclaration: Let identifier LeftParen RightParen type Equal expression;

block: LeftBrace statement* RightBrace;

blockExpression: LeftBrace statement* expression RightBrace;

statement: expression Semi;

expression
    : primaryExpression
    | parenExpression
    | variableDeclaration
    | constantDeclaration
    | conditionExpression
    | expression multiplicativeOperator expression
    | expression additiveOperator expression
    ;

variableDeclaration: Var identifier type? Equal expression;
constantDeclaration: Let identifier type? Equal expression;

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
