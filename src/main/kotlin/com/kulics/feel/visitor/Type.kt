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

class FunctionType(val inType: Type, val outType: Type) : Type {
    override val name: String = "Func[${inType.name},${outType.name}]"

    override fun generateTypeName(): String {
        return "(${inType.generateTypeName()})->${outType.generateTypeName()}"
    }
}

internal fun DelegateVisitor.visitType(ctx: TypeContext): String {
    return visitConstantIdentifier(ctx.constantIdentifier())
}