package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

sealed interface Type {
    val name: String
    fun generateTypeName(): String = name
    fun getMember(name: String): Identifier? = null
}

class PrimitiveType(override val name: String, private val backendName: String? = null) : Type {
    override fun generateTypeName(): String {
        return backendName ?: name
    }
}

class FunctionType(val parameterTypes: List<Type>, val returnType: Type) : Type {
    override val name: String =
        "Func[${
            parameterTypes.foldIndexed("") { index, acc, type ->
                if (index == 0) type.name
                else "${acc}, ${type.name}"
            }
        },${returnType.name}]"

    override fun generateTypeName(): String {
        return "(${
            parameterTypes.foldIndexed("") { index, acc, type ->
                if (index == 0) type.generateTypeName()
                else "${acc}, ${type.generateTypeName()}"
            }
        })->${returnType.generateTypeName()}"
    }
}

class GenericsType(
    override val name: String,
    val typeParameter: List<Type>,
    val typeConstructor: (List<Type>) -> Type
) : Type

class TypeParameter(override val name: String, val constraint: Type) : Type {
    override fun getMember(name: String): Identifier? {
        return constraint.getMember(name)
    }
}

fun typeSubstitution(type: Type, typeMap: Map<String, Type>): Type {
    return when (type) {
        is TypeParameter -> typeMap.getValue(type.name)
        is FunctionType ->
            FunctionType(
                type.parameterTypes.map { typeSubstitution(it, typeMap) },
                typeSubstitution(type.returnType, typeMap)
            )
        is RecordType -> RecordType(
            type.name,
            type.member.mapValues {
                Identifier(
                    it.value.name,
                    typeSubstitution(it.value.type, typeMap),
                    it.value.kind
                )
            }.toMutableMap()
        )
        else -> type
    }
}

class RecordType(override val name: String, val member: MutableMap<String, Identifier>) : Type {
    override fun getMember(name: String): Identifier? {
        return member[name]
    }
}

class InterfaceType(
    override val name: String,
    private val member: MutableMap<String, Identifier>,
    val permits: MutableSet<Type>
) : Type {
    override fun getMember(name: String): Identifier? {
        return member[name]
    }
}

internal fun DelegateVisitor.visitType(ctx: TypeContext): String {
    return visitIdentifier(ctx.identifier())
}

internal fun Type.cannotAssignTo(ty: Type): Boolean {
    return !(this == ty || (ty is InterfaceType && ty.permits.contains(this)))
}