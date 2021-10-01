package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

sealed class Type {
    abstract val name: String
    open fun generateTypeName(): String = name
    open fun getMember(name: String): Identifier? = null
    abstract val uniqueName: String

    override fun hashCode(): Int {
        return uniqueName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Type) return false
        if (uniqueName != other.uniqueName) return false
        return true
    }
}

class FunctionType(val parameterTypes: List<Type>, val returnType: Type) : Type() {
    override val name: String =
        "Func[${joinString(parameterTypes) { it.name }},${returnType.name}]"

    override fun generateTypeName(): String =
        "(${joinString(parameterTypes) { it.generateTypeName() }})->${returnType.generateTypeName()}"

    override val uniqueName: String =
        generateGenericsUniqueName("Func", parameterTypes.plus(returnType))
}

class RecordType(
    override val name: String,
    val member: MutableMap<String, Identifier>,
    val backendName: String?,
    override val uniqueName: String = name
) : Type() {
    override fun getMember(name: String): Identifier? {
        return member[name]
    }

    override fun generateTypeName(): String {
        return backendName ?: name
    }
}

class InterfaceType(
    override val name: String,
    val member: MutableMap<String, Identifier>,
    val backendName: String?,
    override val uniqueName: String = name
) : Type() {
    override fun getMember(name: String): Identifier? {
        return member[name]
    }

    override fun generateTypeName(): String {
        return backendName ?: name
    }
}

class GenericsType(
    override val name: String,
    val typeParameter: List<TypeParameter>,
    partialTypeArgument: List<Type>? = null,
    val typeConstructor: (List<Type>) -> Type
) : Type() {
    override val uniqueName: String =
        if (partialTypeArgument == null) "${name}[${joinString(typeParameter) { it.uniqueName }}]"
        else generateGenericsUniqueName(name, partialTypeArgument)
}

fun generateGenericsUniqueName(name: String, typeArgs: List<Type>): String {
    return "${name}_OP_${joinString(typeArgs, "_") { it.uniqueName }}_ED"
}

class TypeParameter(override val name: String, var constraint: InterfaceType) : Type() {
    override fun getMember(name: String): Identifier? {
        return constraint.getMember(name)
    }

    override val uniqueName: String = "For_All_${name}"
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
                Identifier(
                    it.value.name,
                    typeSubstitution(it.value.type, typeMap),
                    it.value.kind
                )
            }.toMutableMap(),
            type.backendName
        )
        else -> type
    }
}

internal fun DelegateVisitor.checkType(typeInfo: Pair<String, List<String>>): Type {
    return when (val targetType = getType(typeInfo.first)) {
        null -> {
            println("type: '${typeInfo.first}' is undefined")
            throw CompilingCheckException()
        }
        is GenericsType -> {
            if (typeInfo.second.isEmpty() || targetType.typeParameter.size != typeInfo.second.size) {
                println("the type args size need '${targetType.typeParameter.size}', but found '${typeInfo.second.size}'")
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
            val instanceType = targetType.typeConstructor(list)
            getImplementType(targetType)?.forEach {
                addImplementType(instanceType, if (it is GenericsType) it.typeConstructor(list) else it)
            }
            instanceType
        }
        else -> targetType
    }
}

internal inline fun <T> joinString(list: List<T>, splitSymbol: String = ", ", select: (T) -> String): String {
    return list.foldIndexed(StringBuffer()) { index, acc, type ->
        if (index == 0) acc.append(select(type))
        else acc.append(splitSymbol).append(select(type))
    }.toString()
}

internal fun DelegateVisitor.visitType(ctx: TypeContext): Pair<String, List<String>> {
    return visitIdentifier(ctx.identifier()) to ctx.type().map { visitType(it).first }
}

internal fun DelegateVisitor.cannotAssign(rightValue: Type, leftValue: Type): Boolean {
    return !canAssignTo(rightValue, leftValue)
}

internal fun DelegateVisitor.canAssignTo(rightValue: Type, leftValue: Type): Boolean {
    if (rightValue == leftValue) {
        return true
    }
    if (leftValue is InterfaceType) {
        if (rightValue is TypeParameter && rightValue.constraint == leftValue) {
            return true
        }
        return checkSubtype(rightValue, leftValue)
    }
    return false
}
