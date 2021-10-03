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

class FunctionType(val parameterTypes: List<Type>, val returnType: Type, val isGenericType: Boolean = false) : Type() {
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
    override val uniqueName: String = name,
    val isGenericType: Boolean = false
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
    override val uniqueName: String = name,
    val isGenericType: Boolean = false
) : Type(), ConstraintType {
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
) : Type(), ConstraintType {
    override val uniqueName: String =
        generateGenericsUniqueName(name, partialTypeArgument ?: typeParameter)
}

fun generateGenericsUniqueName(name: String, typeArgs: List<Type>): String {
    return "${name}_OP_${joinString(typeArgs, "_") { it.uniqueName }}_ED"
}

sealed interface ConstraintType

class TypeParameter(override val name: String, var constraint: ConstraintType) : Type() {
    override val uniqueName: String = "For_All_${name}"

    var constraintObjectTypeName: String = ""
}

fun typeSubstitution(type: Type, typeMap: Map<String, Type>): Type {
    return typeSubstitutionImplement(type, typeMap)
}

fun typeSubstitutionImplement(type: Type, typeMap: Map<String, Type>): Type {
    return when (type) {
        is TypeParameter -> typeMap.getValue(type.name)
        is FunctionType -> FunctionType(
            type.parameterTypes.map { typeSubstitutionImplement(it, typeMap) },
            typeSubstitutionImplement(type.returnType, typeMap)
        )
        is RecordType ->
            if (type.isGenericType) RecordType(
                type.name,
                type.member.mapValues {
                    Identifier(
                        it.value.name,
                        typeSubstitutionImplement(it.value.type, typeMap),
                        it.value.kind
                    )
                }.toMutableMap(),
                type.backendName,
                type.uniqueName
            )
            else type
        is InterfaceType ->
            if (type.isGenericType) InterfaceType(
                type.name,
                type.member.mapValues {
                    Identifier(
                        it.value.name,
                        typeSubstitutionImplement(it.value.type, typeMap),
                        it.value.kind
                    )
                }.toMutableMap(),
                type.backendName,
                type.uniqueName
            )
            else type
        else -> type
    }
}

internal fun DelegateVisitor.checkType(typeNode: TypeNode): Type {
    return when (val targetType = getType(typeNode.id)) {
        null -> {
            println("type: '${typeNode.id}' is undefined")
            throw CompilingCheckException()
        }
        is GenericsType -> {
            if (typeNode.typeArguments.isEmpty() || targetType.typeParameter.size != typeNode.typeArguments.size) {
                println("the type args size need '${targetType.typeParameter.size}', but found '${typeNode.typeArguments.size}'")
                throw CompilingCheckException()
            }
            val list = mutableListOf<Type>()
            for (v in typeNode.typeArguments) {
                list.add(checkType(v))
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

internal fun DelegateVisitor.visitType(ctx: TypeContext): TypeNode {
    return TypeNode(visitIdentifier(ctx.identifier()), ctx.type().map { visitType(it) })
}

class TypeNode(val id: String, val typeArguments: List<TypeNode>)

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
