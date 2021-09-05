package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.*

internal fun DelegateVisitor.visitExpression(ctx: ExpressionContext): ExpressionNode {
    return if (ctx.primaryExpression() != null) {
        visitPrimaryExpression(ctx.primaryExpression())
    } else if (ctx.childCount == 3) {
        val op = ctx.getChild(1)
        val lhs = visitExpression(ctx.expression(0))
        val rhs = visitExpression(ctx.expression(1))
        when (op) {
            is AdditiveOperatorContext -> {
                when (lhs.type) {
                    Type("Int") -> if (rhs.type != Type("Int")) {
                        println("the type of right value is not 'Int'")
                        throw CompilingCheckException()
                    }
                    Type("Float") -> if (rhs.type != Type("Float")) {
                        println("the type of right value is not 'Float'")
                        throw CompilingCheckException()
                    }
                    else -> {
                        println("the type of left value is not 'Int' or 'Float'")
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
                    Type("Int") -> if (rhs.type != Type("Int")) {
                        println("the type of right value is not 'Int'")
                        throw CompilingCheckException()
                    }
                    Type("Float") -> if (rhs.type != Type("Float")) {
                        println("the type of right value is not 'Float'")
                        throw CompilingCheckException()
                    }
                    else -> {
                        println("the type of left value is not 'Int' or 'Float'")
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

internal fun DelegateVisitor.visitPrimaryExpression(ctx: PrimaryExpressionContext): ExpressionNode {
    return if (ctx.literalExpression() != null) {
        visitLiteralExpression(ctx.literalExpression())
    } else {
        throw CompilingCheckException()
    }
}

internal fun DelegateVisitor.visitLiteralExpression(ctx: LiteralExpressionContext): ExpressionNode {
    return if (ctx.integerExpression() != null) {
        LiteralExpressionNode(ctx.integerExpression().text, Type("Int"))
    } else {
        LiteralExpressionNode(ctx.floatExpression().text, Type("Float"))
    }
}