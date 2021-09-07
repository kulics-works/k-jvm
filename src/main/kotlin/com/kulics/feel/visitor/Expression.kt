package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.*

internal fun DelegateVisitor.visitExpression(ctx: ExpressionContext): ExpressionNode {
    return if (ctx.primaryExpression() != null) {
        visitPrimaryExpression(ctx.primaryExpression())
    } else if (ctx.parenExpression() != null) {
        visitParenExpression(ctx.parenExpression())
    } else if (ctx.childCount == 2) {
        val expr = visitExpression(ctx.expression(0))
        val callArgs = visitCallSuffix(ctx.callSuffix())
        val type = expr.type
        if (type is FunctionType) {
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
        } else {
            println("the type of expression is not a function")
            throw CompilingCheckException()
        }
    } else if (ctx.childCount == 3) {
        val op = ctx.getChild(1)
        val lhs = visitExpression(ctx.expression(0))
        val rhs = visitExpression(ctx.expression(1))
        when (op) {
            is AdditiveOperatorContext -> {
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
                if (op.Add() != null) {
                    AdditiveExpressionNode(lhs, rhs, AdditiveOperator.Add, lhs.type)
                } else {
                    AdditiveExpressionNode(lhs, rhs, AdditiveOperator.Sub, lhs.type)
                }
            }
            is MultiplicativeOperatorContext -> {
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
                if (op.Mul() != null) {
                    MultiplicativeExpressionNode(lhs, rhs, MultiplicativeOperator.Mul, lhs.type)
                } else if (op.Div() != null) {
                    MultiplicativeExpressionNode(lhs, rhs, MultiplicativeOperator.Div, lhs.type)
                } else {
                    MultiplicativeExpressionNode(lhs, rhs, MultiplicativeOperator.Mod, lhs.type)
                }
            }
            else -> throw CompilingCheckException()
        }
    } else {
        throw CompilingCheckException()
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