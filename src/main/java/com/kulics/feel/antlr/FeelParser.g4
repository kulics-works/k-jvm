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

globalVariableDeclaration: Var variableIdentifier type Equal expression;
globalConstantDeclaration: Var constantIdentifier type Equal expression;
globalFunctionDeclaration: Fun constantIdentifier LeftParen RightParen type (block|blockExpression);

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

variableDeclaration: Var variableIdentifier type? Equal expression;
constantDeclaration: Var constantIdentifier type? Equal expression;

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
    : constantIdentifier
    ;

identifier: variableIdentifier | constantIdentifier;

variableIdentifier: VariableIdentifier;
constantIdentifier: ConstantIdentifier;

floatExpression: FloatLiteral;

integerExpression: DecimalLiteral | BinaryLiteral | OctalLiteral | HexLiteral;

multiplicativeOperator: Mul | Div | Mod;

additiveOperator: Add | Sub ;
