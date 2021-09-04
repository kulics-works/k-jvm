package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

data class Type(val name: String)

internal fun DelegateVisitor.visitType(ctx: TypeContext): String {
    return visitConstantIdentifier(ctx.constantIdentifier())
}