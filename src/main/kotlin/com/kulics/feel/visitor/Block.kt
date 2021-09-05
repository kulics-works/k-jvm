package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.BlockExpressionNode
import com.kulics.feel.node.ExpressionNode

internal fun DelegateVisitor.visitBlockExpression(ctx: BlockExpressionContext): BlockExpressionNode {
    val expr = visitExpression(ctx.expression())
    return BlockExpressionNode(expr)
}

