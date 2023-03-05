package com.kulics.feel.node

import com.kulics.feel.visitor.Identifier
import com.kulics.feel.visitor.Type

sealed class StatementNode : Node()

class VariableStatementNode(
    val id: Identifier,
    val initValue: ExpressionNode
) : StatementNode() {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class ExpressionStatementNode(val expr: ExpressionNode) : StatementNode() {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class FunctionStatementNode(
    val id: Identifier,
    val parameterTypes: List<ParameterDeclarationNode>,
    val returnType: Type,
    val body: ExpressionNode
) : StatementNode() {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}
