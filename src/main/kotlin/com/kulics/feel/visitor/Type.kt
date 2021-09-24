package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

sealed interface Type {
    val name: String
    fun generateTypeName(): String = name
    fun getMember(name: String): Identifier? = null
}

class FunctionType(val parameterTypes: List<Type>, val returnType: Type) : Type {
    override val name: String =
        "Func[${joinTypeName(parameterTypes) { it.name }},${returnType.name}]"

    override fun generateTypeName(): String =
        "(${joinTypeName(parameterTypes) { it.generateTypeName() }})->${returnType.generateTypeName()}"
}

class RecordType(
    override val name: String,
    val member: MutableMap<String, Identifier>,
    val backendName: String?
) : Type {
    override fun getMember(name: String): Identifier? {
        return member[name]
    }

    override fun generateTypeName(): String {
        return backendName ?: name
    }
}

class InterfaceType(
    override val name: String,
    val member: MutableMap<String, VirtualIdentifier>,
    val permits: MutableSet<Type>,
    val backendName: String?
) : Type {
    override fun getMember(name: String): Identifier? {
        return member[name]
    }

    override fun generateTypeName(): String {
        return backendName ?: name
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
            }.toMutableMap(),
            type.backendName
        )
        is InterfaceType -> InterfaceType(
            type.name,
            type.member.mapValues {
                VirtualIdentifier(
                    it.value.name,
                    typeSubstitution(it.value.type, typeMap),
                    it.value.hasImplement
                )
            }.toMutableMap(),
            type.permits,
            type.backendName
        )
        else -> type
    }
}

internal fun DelegateVisitor.checkType(typeInfo: Pair<String, List<String>>): Type {
    return when (val type = getType(typeInfo.first)) {
        null -> {
            println("type: '${typeInfo.first}' is undefined")
            throw CompilingCheckException()
        }
        is GenericsType -> {
            if (typeInfo.second.isEmpty()) {
                println("the type args size need '${type.typeParameter.size}', but found '${typeInfo.second.size}'")
                throw CompilingCheckException()
            }
            val list = mutableListOf<Type>()
            for (v in typeInfo.second) {
                val typeArg = getType(v)
                if (typeArg == null) {
                    println("type: '${v}' is undefined")
                    throw CompilingCheckException()
                }
                list.add(typeArg)
            }
            type.typeConstructor(list)
        }
        else -> type
    }
}

internal inline fun joinTypeName(list: List<Type>, select: (Type) -> String): String {
    return list.foldIndexed("") { index, acc, type ->
        if (index == 0) select(type) else "${acc}, ${select(type)}"
    }
}

internal fun DelegateVisitor.visitType(ctx: TypeContext): Pair<String, List<String>> {
    return visitIdentifier(ctx.identifier()) to ctx.type().map { visitType(it).first }
}

internal fun Type.cannotAssignTo(ty: Type): Boolean {
    return !(this.name == ty.name || (ty is InterfaceType && ty.permits.any { it.name == this.name }))
}