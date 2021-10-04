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

open class GlobalRecordDeclarationNode(
    val type: Type,
    val fields: List<Identifier>,
    val methods: List<MethodNode>,
    val implements: Type?
) : DeclarationNode() {
    override fun generateCode(): String {
        return "class ${type.name}(${
            joinString(fields) {
                "${it.name}: ${it.type.generateTypeName()}"
            }
        }) ${
            if (implements != null) ": ${implements.generateTypeName()}" else ""
        } { $Wrap${
            joinString(methods, Wrap) {
                "${
                    if (it.isOverride) {
                        "override "
                    } else {
                        ""
                    }
                }fun ${it.generateCode()}"
            }
        }$Wrap }$Wrap"
    }
}

class GlobalGenericsRecordDeclarationNode(
    type: Type,
    val typeParameter: List<TypeParameter>,
    fields: List<Identifier>,
    methods: List<MethodNode>,
    implements: Type?
) : GlobalRecordDeclarationNode(type, fields, methods, implements) {
    override fun generateCode(): String {
        return "class ${type.name}<${
            joinString(typeParameter) {
                "${it.name}: ${
                    when (val constraintType = it.constraint) {
                        is GenericsType -> constraintType.typeConstructor(listOf(it)).generateTypeName()
                        is InterfaceType -> constraintType.generateTypeName()
                    }
                }"
            }
        }>(${fields}) ${
            if (implements != null) ": ${implements.generateTypeName()}" else ""
        } {$Wrap${
            joinString(methods, Wrap) {
                "${
                    if (it.isOverride) {
                        "override "
                    } else {
                        ""
                    }
                }fun ${it.generateCode()}"
            }
        } }$Wrap"
    }
}

open class GlobalInterfaceDeclarationNode(val type: Type, val methods: List<VirtualMethodNode>) : DeclarationNode() {
    override fun generateCode(): String {
        return "interface ${type.generateTypeName()} {${
            joinString(methods, Wrap) {
                it.generateCode()
            }
        }}$Wrap"
    }
}

class GlobalGenericsInterfaceDeclarationNode(
    type: Type,
    val typeParameter: List<TypeParameter>,
    methods: List<VirtualMethodNode>
) : GlobalInterfaceDeclarationNode(type, methods) {
    override fun generateCode(): String {
        return "interface ${type.name}<${
            joinString(typeParameter) {
                when (val constraintType = it.constraint) {
                    is GenericsType -> {
                        val ty = constraintType.typeConstructor(listOf(it))
                        "${it.name}: ${ty.generateTypeName()}"
                    }
                    is InterfaceType -> "${it.name}: ${constraintType.generateTypeName()}"
                }
            }
        }> {${
            joinString(methods, Wrap) {
                it.generateCode()
            }
        }}$Wrap"
    }
}

open class GlobalExtensionDeclarationNode(val type: Type, val methods: List<MethodNode>, val implements: Type?) :
    DeclarationNode() {
    override fun generateCode(): String {
        TODO("Not yet implemented")
    }
}

class GlobalGenericsExtensionDeclarationNode(
    type: Type,
    val typeParameter: List<TypeParameter>,
    methods: List<MethodNode>,
    implements: Type?
) : GlobalExtensionDeclarationNode(type, methods, implements)

class MethodNode(
    val id: Identifier,
    val params: List<Identifier>,
    val returnType: Type,
    val body: ExpressionNode,
    val isOverride: Boolean
) : DeclarationNode() {
    override fun generateCode(): String {
        return "${if (isOverride) "override" else ""} fun ${id.name}(${
            joinString(params) { "${it.name}: ${it.type.generateTypeName()}" }
        }): ${returnType.generateTypeName()} { return run{ ${body.generateCode()} } }"
    }
}

class VirtualMethodNode(
    val id: VirtualIdentifier,
    val params: ArrayList<Identifier>,
    val returnType: Type,
    val body: ExpressionNode?
) : DeclarationNode() {
    override fun generateCode(): String {
        return "fun ${id.name}(${
            joinString(params) { "${it.name}: ${it.type.generateTypeName()}" }
        }): ${returnType.generateTypeName()} ${
            if (body != null) {
                "{ return run{ ${body.generateCode()} } }"
            } else {
                ""
            }
        } "
    }
}