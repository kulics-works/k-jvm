package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.BlockExpressionNode
import com.kulics.feel.node.ExpressionNode

internal fun DelegateVisitor.visitBlockExpression(ctx: BlockExpressionContext): BlockExpressionNode {
    pushScope()
    val node = BlockExpressionNode(ctx.statement().fold(StringBuilder()) { acc, v ->
        acc.append("${visitStatement(v)};")
    }.toString(), visitExpression(ctx.expression()))
    popScope()
    return node
}

