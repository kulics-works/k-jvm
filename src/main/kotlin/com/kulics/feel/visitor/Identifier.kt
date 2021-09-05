package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser

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

data class Identifier(val name: String, val type: Type, val kind: IdentifierKind)

enum class IdentifierKind {
    Immutable, Mutable
}