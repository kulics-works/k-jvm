package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.BlockExpressionNode
import com.kulics.feel.node.ExpressionNode

internal fun DelegateVisitor.visitBlockExpression(ctx: BlockExpressionContext): BlockExpressionNode {
    pushScope()
    val code = ctx.statement().fold(StringBuilder()) { acc, v -> acc.append("${visitStatement(v)};") }.toString()
    val node = BlockExpressionNode(code, when (val expr = ctx.expression()) {
        null -> null
        else -> visitExpression(expr)
    })
    popScope()
    return node
}

