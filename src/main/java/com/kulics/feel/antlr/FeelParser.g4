parser grammar FeelParser;

options { tokenVocab=FeelLexer; }

program: moduleDeclaration globalDeclaration*;

moduleDeclaration: Module variableIdentifier Semi;

globalDeclaration
    : (globalVariableDeclaration
    | globalFunctionDeclaration
    | globalRecordDeclaration
    | globalInterfaceDeclaration
    | globalExtensionDeclaration
    ) Semi
    ;

globalVariableDeclaration: Let Mut? variableIdentifier type? Equal expression;
globalFunctionDeclaration: Let variableIdentifier typeParameterList? parameterList type? Equal expression;
globalRecordDeclaration: Def typeIdentifier typeParameterList? fieldList type? methodList?;
globalInterfaceDeclaration: Def typeIdentifier typeParameterList? virtualMethodList?;
globalExtensionDeclaration: Ext typeIdentifier typeParameterList? type? methodList?;

typeParameterList: LeftBrack typeParameter (Comma typeParameter)* RightBrack;

typeParameter: typeIdentifier type;

parameterList: LeftParen (parameter (Comma parameter)*)? RightParen;

parameter: variableIdentifier type;

fieldList: LeftParen (field (Comma field)*)? RightParen;

field: Mut? variableIdentifier type;

methodList: LeftBrace (method Semi)* RightBrace;

method: variableIdentifier parameterList type Equal expression;

virtualMethodList: LeftBrace (virtualMethod Semi)* RightBrace;

virtualMethod: variableIdentifier parameterList type (Equal expression)?;

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
    | blockExpression
    | ifExpression
    | expression memberAccessCallSuffix
    | expression callSuffix
    | expression memberAccess
    | expression multiplicativeOperator expression
    | expression additiveOperator expression
    | expression compareOperator expression
    | expression logicOperator expression
    | lambdaExpression
    ;

variableDeclaration: Let Mut? variableIdentifier type? Equal expression;
functionDeclaration: Let variableIdentifier parameterList type? Equal expression;

memberAccessCallSuffix: Dot variableIdentifier (LeftBrack type (Comma type)* RightBrack)? LeftParen (expression (Comma expression)*)? RightParen;

callSuffix: (LeftBrack type (Comma type)* RightBrack)? LeftParen (expression (Comma expression)*)? RightParen;

memberAccess: Dot variableIdentifier;

assignment: variableIdentifier Equal expression;

lambdaExpression: Fn parameterList type? Equal expression;

ifStatement
    : If expression (Is pattern)? Then block (Else (block | ifStatement))?
    ;

ifExpression
    : If expression (Is pattern)? Then expression Else expression
    ;

whileStatement
    : While expression Then block
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
    : variableIdentifier
    ;

wildcardPattern
    : Discard
    ;

literalPattern
    : literalExpression
    ;

primaryExpression
    : literalExpression
    | variableIdentifier
    | typeIdentifier
    ;

literalExpression
    : integerExpression
    | floatExpression
    | characterExpression
    | stringExpression
    | boolExpression
    ;

type
    : typeIdentifier (LeftBrack type (Comma type)* RightBrack)?
    | functionType
    ;

functionType: Fn LeftParen parameterTypeList? RightParen type;

parameterTypeList: type (Comma type)*;

typeIdentifier: UpperIdentifier;

variableIdentifier: LowerIdentifier;

stringExpression: StringLiteral;

characterExpression: CharLiteral;

floatExpression: FloatLiteral;

boolExpression: True | False;

integerExpression: DecimalLiteral | BinaryLiteral | OctalLiteral | HexLiteral;

multiplicativeOperator: Mul | Div | Mod;

additiveOperator: Add | Sub ;

logicOperator: AndAnd | OrOr ;

compareOperator: Less | Greater | LessEqual | GreaterEqual | EqualEqual | NotEqual;