package com.kulics.k.visitor

import com.kulics.k.grammar.KParser.*

fun visitIdentifier(ctx: VariableIdentifierContext): String {
    return ctx.LowerIdentifier().text
}

fun visitIdentifier(ctx: TypeIdentifierContext): String {
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