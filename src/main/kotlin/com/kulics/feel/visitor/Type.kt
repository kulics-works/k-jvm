package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

sealed class Type {
    abstract val name: String
    open fun getMember(name: String): Identifier? = null
    abstract val uniqueName: String

    override fun hashCode(): Int {
        return uniqueName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other !is Type -> false
            uniqueName != other.uniqueName -> false
            else -> true
        }
    }
}

val builtinTypeAny = InterfaceType("Any", mutableMapOf(), "Any")
val builtinTypeVoid = RecordType("Void", mutableMapOf(), "Unit")
val builtinTypeInt = RecordType("Int", mutableMapOf(), "Int")
val builtinTypeFloat = RecordType("Float", mutableMapOf(), "Double")
val builtinTypeBool = RecordType("Bool", mutableMapOf(), "Boolean")
val builtinTypeChar = RecordType("Char", mutableMapOf(), "Char")
val builtinTypeString = RecordType("String", mutableMapOf(), "String")
val builtinTypeArray = run {
    val typeParameter = TypeParameter("T", builtinTypeAny)
    val members = mutableMapOf<String, Identifier>()
    val get = "get"
    members[get] = Identifier(get, FunctionType(listOf(builtinTypeInt), typeParameter))
    val set = "set"
    members[set] = Identifier(set, FunctionType(listOf(builtinTypeInt, typeParameter), builtinTypeVoid))
    GenericsType("Array", listOf(typeParameter)) { li ->
        val typeMap = mutableMapOf<String, Type>()
        for (i in li.indices) {
            typeMap[typeParameter.name] = li[i]
        }
        typeSubstitution(
            RecordType(
                "Array[${joinString(li) { it.name }}]",
                members,
                generateGenericsUniqueName("Array", li),
                "Array" to li
            ), typeMap
        )
    }
}

class FunctionType(val parameterTypes: List<Type>, val returnType: Type) : Type() {
    override val name: String =
        "fn(${joinString(parameterTypes) { it.name }}) ${returnType.name}"

    override val uniqueName: String =
        generateGenericsUniqueName("Func", parameterTypes.plus(returnType))
}

class RecordType(
    override val name: String,
    val member: MutableMap<String, Identifier>,
    override val uniqueName: String = name,
    val rawGenericsType: Pair<String, List<Type>>? = null
) : Type() {
    override fun getMember(name: String): Identifier? {
        return member[name]
    }
}

class InterfaceType(
    override val name: String,
    val member: MutableMap<String, VirtualIdentifier>,
    override val uniqueName: String = name,
    val rawGenericsType: Pair<String, List<Type>>? = null
) : Type(), ConstraintType {
    override fun getMember(name: String): Identifier? {
        return member[name]
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

class TypeParameter(
    override val name: String,
    var constraint: ConstraintType,
    var isFromExtension: Boolean = false
) : Type() {
    override val uniqueName: String = "For_All_${name}"
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
            if (type.rawGenericsType != null) RecordType(
                type.name,
                type.member.mapValues {
                    Identifier(
                        it.value.name,
                        typeSubstitutionImplement(it.value.type, typeMap),
                        it.value.kind
                    )
                }.toMutableMap(),
                type.uniqueName,
                type.rawGenericsType
            )
            else type
        is InterfaceType ->
            if (type.rawGenericsType != null) InterfaceType(
                type.name,
                type.member.mapValues {
                    VirtualIdentifier(
                        it.value.name,
                        typeSubstitutionImplement(it.value.type, typeMap),
                        it.value.kind,
                        it.value.hasImplement
                    )
                }.toMutableMap(),
                type.uniqueName,
                type.rawGenericsType
            )
            else type
        else -> type
    }
}

fun DelegateVisitor.checkTypeNode(node: TypeNode): Type {
    return when (node) {
        is FunctionTypeNode -> FunctionType(node.parameterTypes.map {
            checkTypeNode(it)
        }, checkTypeNode(node.returnType))
        is NominalTypeNode -> checkNominalTypeNode(node)
    }
}

fun DelegateVisitor.checkNominalTypeNode(node: NominalTypeNode): Type {
    return when (val targetType = getType(node.id)) {
        null -> {
            println("type: '${node.id}' is undefined")
            throw CompilingCheckException()
        }
        is GenericsType -> {
            if (node.typeArguments.isEmpty() || targetType.typeParameter.size != node.typeArguments.size) {
                println("the type args size need '${targetType.typeParameter.size}', but found '${node.typeArguments.size}'")
                throw CompilingCheckException()
            }
            val list = mutableListOf<Type>()
            for (v in node.typeArguments) {
                list.add(checkTypeNode(v))
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

inline fun <T> joinString(list: List<T>, splitSymbol: String = ", ", select: (T) -> String): String {
    return list.foldIndexed(StringBuffer()) { index, acc, type ->
        if (index == 0) acc.append(select(type))
        else acc.append(splitSymbol).append(select(type))
    }.toString()
}

fun DelegateVisitor.visitType(ctx: TypeContext): TypeNode {
    if (ctx.functionType() != null) {
        return visitFunctionType(ctx.functionType())
    }
    return NominalTypeNode(visitIdentifier(ctx.typeIdentifier()), ctx.type().map { visitType(it) })
}

fun DelegateVisitor.visitFunctionType(ctx: FunctionTypeContext): FunctionTypeNode {
    return FunctionTypeNode(
        visitParameterTypeList(ctx.parameterTypeList()),
        visitType(ctx.type())
    )
}

fun DelegateVisitor.visitParameterTypeList(ctx: ParameterTypeListContext?): List<TypeNode> {
    return ctx?.type()?.map {
        visitType(it)
    } ?: listOf()
}


fun DelegateVisitor.cannotAssign(rightValue: Type, leftValue: Type): Boolean {
    return !canAssignTo(rightValue, leftValue)
}

fun DelegateVisitor.canAssignTo(rightValue: Type, leftValue: Type): Boolean {
    if (rightValue == leftValue) {
        return true
    }
    if (leftValue is InterfaceType) {
        return checkSubtype(
            if (rightValue is TypeParameter) when (val ty = rightValue.constraint) {
                is InterfaceType -> ty
                is GenericsType -> ty.typeConstructor(listOf(rightValue))
            } else rightValue, leftValue
        )
    } else if (leftValue is TypeParameter) {
        val leftType = when (val leftTy = leftValue.constraint) {
            is InterfaceType -> leftTy
            is GenericsType -> leftTy.typeConstructor(listOf(leftTy))
        }
        return checkSubtype(
            if (rightValue is TypeParameter) when (val ty = rightValue.constraint) {
                is InterfaceType -> ty
                is GenericsType -> ty.typeConstructor(listOf(rightValue))
            } else rightValue, leftType
        )
    }
    return false
}

fun DelegateVisitor.checkSubtype(subtype: Type, type: Type): Boolean {
    if (type == builtinTypeAny) {
        return true
    }
    if (type == subtype) {
        return true
    }
    val implements = getImplementType(subtype)
    if (implements != null) {
        for (v in implements) {
            if (v.uniqueName == type.uniqueName) {
                return true
            }
        }
    }
    return false
}

sealed class TypeNode

class NominalTypeNode(val id: String, val typeArguments: List<TypeNode>) : TypeNode()

class FunctionTypeNode(val parameterTypes: List<TypeNode>, val returnType: TypeNode) : TypeNode()