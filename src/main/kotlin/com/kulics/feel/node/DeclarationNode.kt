package com.kulics.feel.node

import com.kulics.feel.visitor.*
import com.kulics.feel.visitor.joinString

sealed class DeclarationNode : Node()

open class GlobalFunctionDeclarationNode(
    val id: Identifier,
    val parameterTypes: List<ParameterDeclarationNode>,
    val returnType: Type,
    val body: ExpressionNode
) : DeclarationNode() {
    override fun generateCode(): String {
        return "fun ${id.name}(${
            joinString(parameterTypes) {
                it.generateCode()
            }
        }): ${returnType.generateTypeName()} {${Wrap}return (${body.generateCode()});$Wrap}$Wrap"
    }
}

class GlobalGenericsFunctionDeclarationNode(
    id: Identifier,
    val typeParameter: List<TypeParameter>,
    parameterTypes: List<ParameterDeclarationNode>,
    returnType: Type,
    body: ExpressionNode
) : GlobalFunctionDeclarationNode(id, parameterTypes, returnType, body) {
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

class ParameterDeclarationNode(val id: Identifier, val paramType: Type) : Node() {
    override fun generateCode(): String {
        return "${id.name}: ${paramType.generateTypeName()}"
    }
}

class GlobalVariableDeclarationNode(val id: Identifier, val initValue: ExpressionNode) : DeclarationNode() {
    override fun generateCode(): String {
        return if (id.kind == IdentifierKind.Immutable) {
            "val ${id.name}: ${id.type.generateTypeName()} = ${initValue.generateCode()}$Wrap"
        } else {
            "var ${id.name}: ${id.type.generateTypeName()} = ${initValue.generateCode()}$Wrap"
        }
    }
}

