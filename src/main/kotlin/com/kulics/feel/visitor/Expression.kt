package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.ExpressionNode
import com.kulics.feel.node.LiteralExpressionNode

internal fun DelegateVisitor.visitExpression(ctx: ExpressionContext): ExpressionNode {
    return if (ctx.primaryExpression() != null) {
        visitPrimaryExpression(ctx.primaryExpression())
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
        LiteralExpressionNode(ctx.integerExpression().text)
    } else {
        throw CompilingCheckException()
    }
}