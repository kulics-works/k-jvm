package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.*

internal fun DelegateVisitor.visitExpression(ctx: ExpressionContext): ExpressionNode = when (ctx.childCount) {
    1 -> when (val expr = ctx.getChild(0)) {
        is PrimaryExpressionContext -> visitPrimaryExpression(expr)
        is ParenExpressionContext -> visitParenExpression(expr)
        is BlockExpressionContext -> visitBlockExpression(expr)
        is ConditionExpressionContext -> visitConditionExpression(expr)
        else -> throw CompilingCheckException()
    }
    2 -> {
        val expr = visitExpression(ctx.expression(0))
        val callArgs = visitCallSuffix(ctx.callSuffix())
        val type = expr.type
        if (type !is FunctionType) {
            println("the type of expression is not a function")
            throw CompilingCheckException()
        }
        if (type.parameterTypes.size != callArgs.size) {
            println("the size of args is ${callArgs.size}, but need ${type.parameterTypes.size}")
            throw CompilingCheckException()
        }
        for ((i, v) in type.parameterTypes.withIndex()) {
            if (callArgs[i].type != v) {
                println("the type of args${i}: '${callArgs[i].type.name}' is not '${v.name}'")
                throw CompilingCheckException()
            }
        }
        CallExpressionNode(expr, callArgs, type.returnType)
    }
    3 -> {
        val lhs = visitExpression(ctx.expression(0))
        val rhs = visitExpression(ctx.expression(1))
        when (val op = ctx.getChild(1)) {
            is AdditiveOperatorContext -> {
                checkCalculateExpressionType(lhs, rhs)
                val symbol = if (op.Add() != null) {
                    AdditiveOperator.Add
                } else {
                    AdditiveOperator.Sub
                }
                AdditiveExpressionNode(lhs, rhs, symbol, lhs.type)
            }
            is MultiplicativeOperatorContext -> {
                checkCalculateExpressionType(lhs, rhs)
                val symbol = if (op.Mul() != null) {
                    MultiplicativeOperator.Mul
                } else if (op.Div() != null) {
                    MultiplicativeOperator.Div
                } else {
                    MultiplicativeOperator.Mod
                }
                MultiplicativeExpressionNode(lhs, rhs, symbol, lhs.type)
            }
            is CompareOperatorContext -> {
                checkCompareExpressionType(lhs, rhs)
                val symbol = if (op.EqualEqual() != null) {
                    CompareOperator.Equal
                } else if (op.NotEqual() != null) {
                    CompareOperator.NotEqual
                } else if (op.Less() != null) {
                    CompareOperator.Less
                } else if (op.LessEqual() != null) {
                    CompareOperator.LessEqual
                } else if (op.Greater() != null) {
                    CompareOperator.Greater
                } else {
                    CompareOperator.GreaterEqual
                }
                CompareExpressionNode(lhs, rhs, symbol)
            }
            is LogicOperatorContext -> {
                checkLogicExpressionType(lhs, rhs)
                val symbol = if (op.And() != null) {
                    LogicOperator.And
                } else {
                    LogicOperator.Or
                }
                LogicExpressionNode(lhs, rhs, symbol)
            }
            else -> throw CompilingCheckException()
        }
    }
    else -> throw CompilingCheckException()
}

fun checkCalculateExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeInt -> if (rhs.type != builtinTypeInt) {
            println("the type of right value is not '${builtinTypeInt.name}'")
            throw CompilingCheckException()
        }
        builtinTypeFloat -> if (rhs.type != builtinTypeFloat) {
            println("the type of right value is not '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeInt.name}' or '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
    }
}

fun checkCompareExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeInt -> if (rhs.type != builtinTypeInt) {
            println("the type of right value is not '${builtinTypeInt.name}'")
            throw CompilingCheckException()
        }
        builtinTypeFloat -> if (rhs.type != builtinTypeFloat) {
            println("the type of right value is not '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeInt.name}' or '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
    }
}

fun checkLogicExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeBool -> if (rhs.type != builtinTypeBool) {
            println("the type of right value is not '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
    }
}

internal fun DelegateVisitor.visitCallSuffix(ctx: CallSuffixContext): List<ExpressionNode> {
    val args = ctx.expression()
    return args.map { visitExpression(it) }
}

internal fun DelegateVisitor.visitParenExpression(ctx: ParenExpressionContext): ExpressionNode {
    return ParenExpressionNode(visitExpression(ctx.expression()))
}

internal fun DelegateVisitor.visitPrimaryExpression(ctx: PrimaryExpressionContext): ExpressionNode {
    return if (ctx.literalExpression() != null) {
        visitLiteralExpression(ctx.literalExpression())
    } else {
        val name = visitIdentifier(ctx.identifier())
        val id = getIdentifier(name)
        if (id == null) {
            println("the identifier '${name}' is not define")
            throw CompilingCheckException()
        } else {
            IdentifierExpressionNode(id)
        }
    }
}

internal fun DelegateVisitor.visitLiteralExpression(ctx: LiteralExpressionContext): ExpressionNode {
    return if (ctx.integerExpression() != null) {
        LiteralExpressionNode(ctx.integerExpression().text, builtinTypeInt)
    } else {
        LiteralExpressionNode(ctx.floatExpression().text, builtinTypeFloat)
    }
}

internal fun DelegateVisitor.visitStatement(ctx: StatementContext): String {
    return when (val stat = ctx.getChild(0)) {
        is VariableDeclarationContext -> visitVariableDeclaration(stat)
        is ConstantDeclarationContext -> visitConstantDeclaration(stat)
        is ConditionExpressionContext -> visitConditionExpression(stat).generateCode()
        is ExpressionContext -> visitExpression(stat).generateCode()
        else -> throw CompilingCheckException()
    }
}

internal fun DelegateVisitor.visitVariableDeclaration(ctx: VariableDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    val type = if (ctx.type() == null) {
        expr.type
    } else {
        val typeName = visitType(ctx.type())
        val type = getType(typeName)
        if (type == null) {
            println("type: '${typeName}' is undefined")
            throw CompilingCheckException()
        }
        if (expr.type != type) {
            println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
            throw CompilingCheckException()
        }
        type
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
    return "var $id: ${type.generateTypeName()} = ${expr.generateCode()}"
}

internal fun DelegateVisitor.visitConstantDeclaration(ctx: ConstantDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    val type = if (ctx.type() == null) {
        expr.type
    } else {
        val typeName = visitType(ctx.type())
        val type = getType(typeName)
        if (type == null) {
            println("type: '${typeName}' is undefined")
            throw CompilingCheckException()
        }
        if (expr.type != type) {
            println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
            throw CompilingCheckException()
        }
        type
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
    return "val $id: ${type.generateTypeName()} = ${expr.generateCode()}"
}

internal fun DelegateVisitor.visitConditionExpression(ctx: ConditionExpressionContext): ExpressionNode {
    val cond = visitExpression(ctx.expression(0))
    if (cond.type != builtinTypeBool) {
        println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
        throw CompilingCheckException()
    }
    val thenBranch = visitExpression(ctx.expression(1))
    val elseBranch = visitExpression(ctx.expression(2))
    if (thenBranch.type != elseBranch.type) {
        println("the type of then branch is '${thenBranch.type.name}', and the type of else branch is '${elseBranch.type.name}', they are not equal")
        throw CompilingCheckException()
    }
    return ConditionExpressionNode(cond, thenBranch, elseBranch, thenBranch.type)
}