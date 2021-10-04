package com.kulics.feel.node

import com.kulics.feel.visitor.*
import com.kulics.feel.visitor.joinString

sealed class DeclarationNode : Node()

class ModuleDeclarationNode(val name: String) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class GlobalFunctionDeclarationNode(
    val id: Identifier,
    val typeParameter: List<TypeParameter>,
    val parameterTypes: List<ParameterDeclarationNode>,
    val returnType: Type,
    val body: ExpressionNode
) : DeclarationNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class ParameterDeclarationNode(val id: Identifier, val paramType: Type) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class GlobalVariableDeclarationNode(val id: Identifier, val initValue: ExpressionNode) : DeclarationNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

open class GlobalRecordDeclarationNode(
    val type: Type,
    val typeParameter: List<TypeParameter>,
    val fields: List<Identifier>,
    val methods: List<MethodNode>,
    val implements: Type?
) : DeclarationNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

class GlobalInterfaceDeclarationNode(
    val type: Type,
    val typeParameter: List<TypeParameter>,
    val methods: List<VirtualMethodNode>
) : DeclarationNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

open class GlobalExtensionDeclarationNode(
    val type: Type,
    val typeParameter: List<TypeParameter>,
    val methods: List<MethodNode>, val implements: Type?
) : DeclarationNode() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

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