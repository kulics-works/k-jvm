package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser

fun visitIdentifier(ctx: FeelParser.VariableIdentifierContext): String {
    return ctx.LowerIdentifier().text
}

fun visitIdentifier(ctx: FeelParser.TypeIdentifierContext): String {
    return ctx.UpperIdentifier().text
}

open class Identifier(val name: String, val type: Type, val kind: IdentifierKind = IdentifierKind.Immutable)

enum class IdentifierKind {
    Immutable, Mutable
}

class VirtualIdentifier(
    name: String,
    type: Type,
    kind: IdentifierKind = IdentifierKind.Immutable,
    var hasImplement: Boolean = false
) : Identifier(name, type, kind)