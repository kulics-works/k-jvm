package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser

internal fun visitIdentifier(ctx: FeelParser.IdentifierContext): String {
    return ctx.Identifier().text
}

data class Identifier(val name: String, val type: Type, val kind: IdentifierKind = IdentifierKind.Immutable)

enum class IdentifierKind {
    Immutable, Mutable
}