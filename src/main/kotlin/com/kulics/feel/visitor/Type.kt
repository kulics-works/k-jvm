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

class FunctionType(val parameterTypes: List<Type>, val returnType: Type) : Type {
    override val name: String =
        "Func[${
            parameterTypes.foldIndexed("") { index, acc, type -> if (index == 0) type.name else "${acc}, ${type.name}" }
        },${returnType.name}]"

    override fun generateTypeName(): String {
        return "(${
            parameterTypes.foldIndexed("") { index, acc, type -> if (index == 0) type.name else "${acc}, ${type.name}" }
        })->${returnType.generateTypeName()}"
    }
}

internal fun DelegateVisitor.visitType(ctx: TypeContext): String {
    return visitIdentifier(ctx.identifier())
}