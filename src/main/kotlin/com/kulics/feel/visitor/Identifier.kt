package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser
import com.kulics.feel.node.IdentifierNode

internal fun DelegateVisitor.visitIdentifier(ctx: FeelParser.IdentifierContext): String {
    if (ctx.variableIdentifier() != null) {
        return visitVariableIdentifier(ctx.variableIdentifier())
    }
    return visitConstantIdentifier(ctx.constantIdentifier())
}

internal fun DelegateVisitor.visitVariableIdentifier(ctx: FeelParser.VariableIdentifierContext): String {
    return ctx.VariableIdentifier().text
}

internal fun DelegateVisitor.visitConstantIdentifier(ctx: FeelParser.ConstantIdentifierContext): String {
    return ctx.ConstantIdentifier().text
}