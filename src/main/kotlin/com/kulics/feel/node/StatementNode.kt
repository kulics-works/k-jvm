package com.kulics.feel.node

import com.kulics.feel.visitor.*
import com.kulics.feel.visitor.joinString

sealed class StatementNode : Node()

class GlobalFunctionStatementNode(
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