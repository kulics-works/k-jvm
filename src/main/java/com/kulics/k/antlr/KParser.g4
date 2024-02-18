parser grammar KParser;

options { tokenVocab=KLexer; }

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
    : Let Mut? variableIdentifier (type)? Equal NewLine* expressionWithTerminator;
globalFunctionDeclaration
    : Let variableIdentifier typeParameterList? parameterList (type)? Equal NewLine* expressionWithTerminator;
globalRecordDeclaration: Type typeIdentifier typeParameterList? fieldList (Colon type)? (SemiColon | With methodList);
globalInterfaceDeclaration: Type typeIdentifier typeParameterList? (SemiColon | Equal NewLine* virtualMethodList);
globalExtensionDeclaration: Given typeIdentifier typeParameterList? (Colon type)? With methodList SemiColon?;
globalSumTypeDeclaration
    : Type typeIdentifier typeParameterList? Equal NewLine*
     LeftBrace NewLine* recordConstructor (Comma NewLine* recordConstructor)* NewLine* RightBrace SemiColon?;

recordConstructor: typeIdentifier fieldList;

typeParameterList: LeftParen NewLine* typeParameter (Comma NewLine* typeParameter)* NewLine* RightParen;

typeParameter: typeIdentifier (type)?;

parameterList: LeftParen NewLine* (parameter (Comma NewLine* parameter)*)? NewLine* RightParen;

parameter: variableIdentifier (type)?;

fieldList: LeftParen NewLine* (field (Comma NewLine* field)*)? NewLine* RightParen;

field: Mut? variableIdentifier (type)?;

methodList: LeftBrace NewLine* (NewLine* method NewLine*)* NewLine* RightBrace;

method: variableIdentifier parameterList (type)? Equal NewLine* expressionWithTerminator;

virtualMethodList: LeftBrace NewLine* (NewLine* virtualMethod NewLine*)* NewLine* RightBrace;

virtualMethod: variableIdentifier parameterList type (Equal NewLine* expression)? SemiColon;

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
    | expression logicAndOperator expression
    | expression logicOrOperator expression
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
    : Let Mut? variableIdentifier (type)? Equal NewLine* expressionWithTerminator;
functionDeclaration
    : Let variableIdentifier parameterList (type)? Equal NewLine* expressionWithTerminator;

memberAccessCallSuffix
    : NewLine? Dot (LeftParen NewLine* type (Comma NewLine* type)* NewLine* RightParen)?
     variableIdentifier LeftParen NewLine* (expression (Comma NewLine* expression)*)? NewLine* RightParen;

callSuffix
    : LeftParen NewLine* (expression (Comma NewLine* expression)*)? NewLine* RightParen
    ;

memberAccess: NewLine? Dot variableIdentifier;

assignmentExpression: variableIdentifier Equal NewLine* expression;

assignmentExpressionWithBlock: variableIdentifier Equal NewLine* expressionWithBlock;

lambdaExpression: parameterList (type)? FatArrow NewLine* expression;

ifDoExpression
    : If NewLine* condition Do NewLine* expression
    ;

ifDoExpressionWithBlock
    : If NewLine* condition Do NewLine* expressionWithBlock
    ;

ifThenElseExpression
    : If NewLine* condition Then NewLine* expression Else NewLine* expression
    ;

ifThenElseExpressionWithBlock
    : If NewLine* condition Then NewLine* expression Else NewLine* expressionWithBlock
    ;

whileDoExpression
    : While NewLine* condition Do NewLine* expression
    ;

whileDoExpressionWithBlock
    : While NewLine* condition Do NewLine* expressionWithBlock
    ;

condition
    : (Case NewLine* pattern In NewLine*)? expression
    | condition AndAnd condition
    | condition OrOr condition
    | LeftParen NewLine* condition NewLine* RightParen
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
    | constructExpression
    | functionCallExpression
    | variableIdentifier
    ;

constructExpression
    : typeIdentifier (LeftParen NewLine* type (Comma NewLine* type)* NewLine* RightParen)?
        LeftParen NewLine* (expression (Comma NewLine* expression)*)? NewLine* RightParen
    ;

functionCallExpression
    : variableIdentifier (LeftParen NewLine* type (Comma NewLine* type)* NewLine* RightParen)?
              LeftParen NewLine* (expression (Comma NewLine* expression)*)? NewLine* RightParen
    ;

literalExpression
    : integerExpression
    | floatExpression
    | runeExpression
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

runeExpression: RuneLiteral;

floatExpression: FloatLiteral;

boolExpression: True | False;

integerExpression: DecimalLiteral | BinaryLiteral | OctalLiteral | HexLiteral;

multiplicativeOperator: (Mul | Div | Mod) NewLine?;

additiveOperator: (Add | Sub)  NewLine?;

logicAndOperator: And NewLine?;

logicOrOperator: Or NewLine?;

compareOperator: (Less | Greater | LessEqual | GreaterEqual | EqualEqual | NotEqual) NewLine?;