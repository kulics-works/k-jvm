parser grammar FeelParser;

options { tokenVocab=FeelLexer; }

program: module_declaration global_declaration*;

module_declaration: Module identifier Semi;

global_declaration
    : (global_variable_declaration
    | global_constant_declaration
    | global_function_declaration
    ) Semi
    ;

global_variable_declaration: Var variable_identifier type Equal expression;
global_constant_declaration: Var constant_identifier type Equal expression;
global_function_declaration: Fun constant_identifier Left_Paren Right_Paren type (block|block_expression);

block: Left_Brace statement* Right_Brace;

block_expression: Left_Brace statement* expression Right_Brace;

statement: expression Semi;

expression
    : primary_expression
    | variable_declaration
    | constant_declaration
    | condition_expression
    | expression multiplicative_operator expression
    | expression additive_operator expression
    ;

variable_declaration: Var variable_identifier type? Equal expression;
constant_declaration: Var constant_identifier type? Equal expression;

condition_expression
    : If Left_Paren expression Right_Paren block Else block
    | If Left_Paren expression Right_Paren block_expression Else block_expression
    ;

primary_expression
    : literal_expression
    | identifier
    ;

literal_expression
    : integer_expression
    | float_expression
    ;

type
    : constant_identifier
    ;

identifier: variable_identifier | constant_identifier;

variable_identifier: Variable_Identifier;
constant_identifier: Constant_Identifier;

float_expression: Float_Literal;

integer_expression: Decimal_Literal | Binary_Literal | Octal_Literal | Hex_Literal;

multiplicative_operator: Mul | Div | Mod;

additive_operator: Add | Sub ;
