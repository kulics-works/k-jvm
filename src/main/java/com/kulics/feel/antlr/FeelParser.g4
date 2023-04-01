parser grammar FeelParser;

options { tokenVocab=FeelLexer; }

program: moduleDeclaration NewLine* (NewLine* globalDeclaration NewLine*)* NewLine* EOF;

moduleDeclaration: Module variableIdentifier SemiColon;

globalDeclaration
    : globalVariableDeclaration
    | globalFunctionDeclaration
    | globalRecordDeclaration
    | globalSumTypeDeclaration
    | globalInterfaceDeclaration
    | globalExtensionDeclaration
    ;

globalVariableDeclaration
    : Let Mut? variableIdentifier (Colon type)? Equal NewLine* expressionWithTerminator;
globalFunctionDeclaration
    : Let variableIdentifier typeParameterList? parameterList (Colon type)? Equal NewLine* expressionWithTerminator;
globalRecordDeclaration: Type typeIdentifier typeParameterList? fieldList (Colon type)? (SemiColon | With methodList);
globalInterfaceDeclaration: Type typeIdentifier typeParameterList? (SemiColon | Equal NewLine* virtualMethodList);
globalExtensionDeclaration: Given typeIdentifier typeParameterList? (Colon type)? With methodList SemiColon?;
globalSumTypeDeclaration
    : Type typeIdentifier typeParameterList? Equal NewLine*
     LeftBrace NewLine* recordConstructor (Comma NewLine* recordConstructor)* NewLine* RightBrace SemiColon?;

recordConstructor: typeIdentifier fieldList;

typeParameterList: LeftParen NewLine* typeParameter (Comma NewLine* typeParameter)* NewLine* RightParen;

typeParameter: typeIdentifier (Colon type)?;

parameterList: LeftParen NewLine* (parameter (Comma NewLine* parameter)*)? NewLine* RightParen;

parameter: variableIdentifier (Colon type)?;

fieldList: LeftParen NewLine* (field (Comma NewLine* field)*)? NewLine* RightParen;

field: Mut? variableIdentifier (Colon type)?;

methodList: LeftBrace NewLine* (NewLine* method NewLine*)* NewLine* RightBrace;

method: variableIdentifier parameterList (Colon type)? Equal NewLine* expressionWithTerminator;

virtualMethodList: LeftBrace NewLine* (NewLine* virtualMethod NewLine*)* NewLine* RightBrace;

virtualMethod: variableIdentifier parameterList Colon type (Equal NewLine* expression)? SemiColon;

statement
    : variableDeclaration
    | functionDeclaration
    | expressionStatement
    ;

expressionStatement
    : expression SemiColon
    | expressionWithBlock NewLine
    ;

expressionWithTerminator: expressionWithBlock NewLine | expression SemiColon;

expression
    : primaryExpression
    | expressionWithBlock
    | ifDoExpression
    | ifThenElseExpression
    | whileDoExpression
    | assignmentExpression
    | expression memberAccessCallSuffix
    | expression callSuffix
    | expression memberAccess
    | expression multiplicativeOperator expression
    | expression additiveOperator expression
    | expression compareOperator expression
    | expression logicOperator expression
    | lambdaExpression
    ;

expressionWithBlock
    : blockExpression
    | ifDoExpressionWithBlock
    | ifThenElseExpressionWithBlock
    | whileDoExpressionWithBlock
    | assignmentExpressionWithBlock
    ;

variableDeclaration
    : Let Mut? variableIdentifier (Colon type)? Equal NewLine* expressionWithTerminator;
functionDeclaration
    : Let variableIdentifier parameterList (Colon type)? Equal NewLine* expressionWithTerminator;

memberAccessCallSuffix
    : NewLine? Dot variableIdentifier
     (LeftParen NewLine* type (Comma NewLine* type)* NewLine* RightParen)?
      LeftParen NewLine* (expression (Comma NewLine* expression)*)? NewLine* RightParen;

callSuffix
    : (LeftParen NewLine* type (Comma NewLine* type)* NewLine* RightParen)?
     LeftParen NewLine* (expression (Comma NewLine* expression)*)? NewLine* RightParen;

memberAccess: NewLine? Dot variableIdentifier;

assignmentExpression: variableIdentifier Equal NewLine* expression;

assignmentExpressionWithBlock: variableIdentifier Equal NewLine* expressionWithBlock;

lambdaExpression: Fn parameterList (Colon type)? Equal NewLine* expression;

ifDoExpression
    : If NewLine* expression (As NewLine* pattern)? Do NewLine* expression
    ;

ifDoExpressionWithBlock
    : If NewLine* expression (As NewLine* pattern)? Do NewLine* expressionWithBlock
    ;

ifThenElseExpression
    : If NewLine* expression (As NewLine* pattern)? Then NewLine* expression Else NewLine* expression
    ;

ifThenElseExpressionWithBlock
    : If NewLine* expression (As NewLine* pattern)? Then NewLine* expression Else NewLine* expressionWithBlock
    ;

whileDoExpression
    : While NewLine* expression Do NewLine* expression
    ;

whileDoExpressionWithBlock
    : While NewLine* expression Do NewLine* expressionWithBlock
    ;

blockExpression
    : LeftBrace NewLine* (NewLine* statement)* NewLine* expression NewLine* RightBrace
    | LeftBrace NewLine* (NewLine* statement)* NewLine* RightBrace
    ;

pattern
    : typePattern
    | literalPattern
    | identifierPattern
    | deconstructPattern
    | wildcardPattern
    ;

typePattern
    : identifierPattern Colon type
    | wildcardPattern Colon type
    | type deconstructPattern
    ;

deconstructPattern
    : LeftParen NewLine* (pattern (Comma NewLine* pattern)*)? NewLine* RightParen
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
    : typeIdentifier (LeftParen NewLine* type (Comma NewLine* type)* NewLine* RightParen)?
    | functionType
    ;

functionType: LeftParen NewLine* parameterTypeList? NewLine* RightParen Arrow type;

parameterTypeList: type (Comma NewLine* type)*;

typeIdentifier: UpperIdentifier;

variableIdentifier: LowerIdentifier;

stringExpression: StringLiteral;

characterExpression: CharLiteral;

floatExpression: FloatLiteral;

boolExpression: True | False;

integerExpression: DecimalLiteral | BinaryLiteral | OctalLiteral | HexLiteral;

multiplicativeOperator: (Mul | Div | Mod) NewLine?;

additiveOperator: (Add | Sub)  NewLine?;

logicOperator: (BitAnd | BitOr) NewLine?;

compareOperator: (Less | Greater | LessEqual | GreaterEqual | EqualEqual | NotEqual) NewLine?;