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
globalFunctionDeclaration: Let variableIdentifier typeParameterList? parameterList (Colon type)? Equal expression;
globalRecordDeclaration: Type typeIdentifier typeParameterList? fieldList (Colon type)? methodList?;
globalInterfaceDeclaration: Type typeIdentifier typeParameterList? (Equal virtualMethodList)?;
globalExtensionDeclaration: Ext typeIdentifier typeParameterList? (Colon type)? methodList?;

typeParameterList: LeftBrack typeParameter (Comma typeParameter)* RightBrack;

typeParameter: typeIdentifier (Colon type)?;

parameterList: LeftParen (parameter (Comma parameter)*)? RightParen;

parameter: variableIdentifier (Colon type)?;

fieldList: LeftParen (field (Comma field)*)? RightParen;

field: Mut? variableIdentifier (Colon type)?;

methodList: LeftBrace (method Semi)* RightBrace;

method: variableIdentifier parameterList (Colon type)? Equal expression;

virtualMethodList: LeftBrace (virtualMethod Semi)* RightBrace;

virtualMethod: variableIdentifier parameterList Colon type (Equal expression)?;

block: LeftBrace (statement Semi)* RightBrace;

statement
    : variableDeclaration
    | functionDeclaration
    | assignment
    | whileStatement
    | expression
    ;

expression
    : primaryExpression
    | blockExpression
    | ifDoExpression
    | ifThenElseExpression
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
functionDeclaration: Let variableIdentifier parameterList (Colon type)? Equal expression;

memberAccessCallSuffix: Dot variableIdentifier (LeftBrack type (Comma type)* RightBrack)? LeftParen (expression (Comma expression)*)? RightParen;

callSuffix: (LeftBrack type (Comma type)* RightBrack)? LeftParen (expression (Comma expression)*)? RightParen;

memberAccess: Dot variableIdentifier;

assignment: variableIdentifier Equal expression;

lambdaExpression: parameterList (Colon type)? Arrow expression;

ifDoExpression
    : If expression (As pattern)? Do expression
    ;

ifThenElseExpression
    : If expression (As pattern)? Then expression Else expression
    ;

whileStatement
    : While expression Do block
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