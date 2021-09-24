package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser

internal fun visitIdentifier(ctx: FeelParser.IdentifierContext): String {
    return ctx.Identifier().text
}

open class Identifier(val name: String, val type: Type, val kind: IdentifierKind = IdentifierKind.Immutable)

class VirtualIdentifier(name: String, type: Type, val hasImplement: Boolean): Identifier(name, type)

enum class IdentifierKind {
    Immutable, Mutable
}