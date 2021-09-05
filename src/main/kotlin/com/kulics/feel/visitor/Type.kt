package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

sealed interface Type {
    val name: String
    fun generateTypeName(): String
}

class PrimitiveType(override val name: String, private val backendName: String? = null) : Type {
    override fun generateTypeName(): String {
        return backendName ?: name
    }
}

class FunctionType(val parameterTypes: Type, val returnType: Type) : Type {
    override val name: String = "Func[${parameterTypes.name},${returnType.name}]"

    override fun generateTypeName(): String {
        return "(${parameterTypes.generateTypeName()})->${returnType.generateTypeName()}"
    }
}

internal fun DelegateVisitor.visitType(ctx: TypeContext): String {
    return visitIdentifier(ctx.identifier())
}