package com.kulics.feel.node

import com.kulics.feel.visitor.*

sealed class DeclarationNode : Node()

class ModuleDeclarationNode(val name: String) : Node() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
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
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

class ParameterDeclarationNode(val id: Identifier, val paramType: Type) : Node() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

class GlobalVariableDeclarationNode(val id: Identifier, val initValue: ExpressionNode) : DeclarationNode() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
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
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

class GlobalInterfaceDeclarationNode(
    val type: Type,
    val typeParameter: List<TypeParameter>,
    val methods: List<VirtualMethodNode>
) : DeclarationNode() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

open class GlobalExtensionDeclarationNode(
    val type: Type,
    val typeParameter: List<TypeParameter>,
    val methods: List<MethodNode>, val implements: Type?
) : DeclarationNode() {
    override fun <T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

sealed class MemberNode : Node()

class MethodNode(
    val id: Identifier,
    val params: List<Identifier>,
    val returnType: Type,
    val body: ExpressionNode,
    val isOverride: Boolean
) : MemberNode()

class VirtualMethodNode(
    val id: VirtualIdentifier,
    val params: List<Identifier>,
    val returnType: Type,
    val body: ExpressionNode?
) : MemberNode()