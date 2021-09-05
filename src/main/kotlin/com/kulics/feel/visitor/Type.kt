package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

data class Type(val name: String, private val backendName: String? = null) {
    fun generateTypeName(): String {
        return backendName ?: name
    }
}

internal fun DelegateVisitor.visitType(ctx: TypeContext): String {
    return visitConstantIdentifier(ctx.constantIdentifier())
}