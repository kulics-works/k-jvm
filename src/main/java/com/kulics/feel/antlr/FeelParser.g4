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

globalVariableDeclaration: Let Mut? variableIdentifier (Colon type)? Equal expression;
globalFunctionDeclaration: Let variableIdentifier typeParameterList? parameterList (Colon type)? FatArrow expression;
globalRecordDeclaration: Let typeIdentifier typeParameterList? fieldList (Colon type)? methodList?;
globalInterfaceDeclaration: Let typeIdentifier typeParameterList? virtualMethodList?;
globalExtensionDeclaration: Ext typeIdentifier typeParameterList? (Colon type)? methodList?;

typeParameterList: LeftParen typeParameter (Comma typeParameter)* RightParen;

typeParameter: typeIdentifier (Colon type)?;

parameterList: LeftParen (parameter (Comma parameter)*)? RightParen;

parameter: variableIdentifier (Colon type)?;

fieldList: LeftParen (field (Comma field)*)? RightParen;

field: Mut? variableIdentifier (Colon type)?;

methodList: LeftBrace (method Semi)* RightBrace;

method: variableIdentifier parameterList (Colon type)? FatArrow expression;

virtualMethodList: LeftBrace (virtualMethod Semi)* RightBrace;

virtualMethod: variableIdentifier parameterList Colon type (FatArrow expression)?;

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

variableDeclaration: Let Mut? variableIdentifier (Colon type)? Equal expression;
functionDeclaration: Let variableIdentifier parameterList (Colon type)? FatArrow expression;

memberAccessCallSuffix: Dot variableIdentifier (LeftParen type (Comma type)* RightParen)? LeftParen (expression (Comma expression)*)? RightParen;

callSuffix: (LeftParen type (Comma type)* RightParen)? LeftParen (expression (Comma expression)*)? RightParen;

memberAccess: Dot variableIdentifier;

assignment: variableIdentifier Equal expression;

lambdaExpression: parameterList (Colon type)? FatArrow expression;

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
    : typeIdentifier (LeftParen type (Comma type)* RightParen)?
    | functionType
    ;

functionType: LeftParen parameterTypeList? RightParen Arrow type;

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