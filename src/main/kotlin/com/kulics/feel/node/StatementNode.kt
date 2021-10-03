package com.kulics.feel.node

import com.kulics.feel.visitor.*
import com.kulics.feel.visitor.joinString

sealed class StatementNode : Node()

open class GlobalFunctionStatementNode(
    val id: Identifier,
    val parameterTypes: List<ParameterNode>,
    val returnType: Type,
    val body: ExpressionNode
) : StatementNode() {
    override fun generateCode(): String {
        return "fun ${id.name}(${
            joinString(parameterTypes) {
                it.generateCode()
            }
        }): ${returnType.generateTypeName()} {${Wrap}return (${body.generateCode()});$Wrap}$Wrap"
    }
}

class GlobalGenericsFunctionStatementNode(
    id: Identifier,
    val typeParameter: List<TypeParameter>,
    parameterTypes: List<ParameterNode>,
    returnType: Type,
    body: ExpressionNode
) : GlobalFunctionStatementNode(id, parameterTypes, returnType, body) {
    override fun generateCode(): String {
        return "fun <${
            joinString(typeParameter) {
                "${it.name}: ${
                    when (val constraintType = it.constraint) {
                        is GenericsType -> constraintType.typeConstructor(listOf(it)).generateTypeName()
                        is InterfaceType -> constraintType.generateTypeName()
                    }
                }"
            }
        }> ${id.name}(${
            joinString(parameterTypes) {
                it.generateCode()
            }
        }): ${returnType.generateTypeName()} {${Wrap}return (${body.generateCode()});$Wrap}$Wrap"
    }
}

class ParameterNode(val id: Identifier, val paramType: Type) : Node() {
    override fun generateCode(): String {
        return "${id.name}: ${paramType.generateTypeName()}"
    }
}

class GlobalVariableStatementNode(val id: Identifier, val initValue: ExpressionNode) : StatementNode() {
    override fun generateCode(): String {
        return if (id.kind == IdentifierKind.Immutable) {
            "val ${id.name}: ${id.type.generateTypeName()} = ${initValue.generateCode()}$Wrap"
        } else {
            "var ${id.name}: ${id.type.generateTypeName()} = ${initValue.generateCode()}$Wrap"
        }
    }
}

fun <T : StatementNode> genericsSubstitution(node: T, typeMap: Map<String, Type>): T {
    return when (node) {
        is GlobalVariableStatementNode -> node
        is GlobalFunctionStatementNode -> {
            GlobalFunctionStatementNode(
                node.id,
                node.parameterTypes.map {
                    ParameterNode(it.id, typeSubstitution(it.paramType, typeMap))
                },
                typeSubstitution(node.returnType, typeMap),
                node.body
            ) as T
        }
        else -> throw CompilingCheckException()
    }
}
