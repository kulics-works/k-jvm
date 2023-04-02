package com.kulics.feel.node

import com.kulics.feel.visitor.*

sealed class ExpressionNode(val type: Type) : Node() {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class IdentifierExpressionNode(val id: Identifier) : ExpressionNode(id.type) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class LiteralExpressionNode(val text: String, ty: Type) : ExpressionNode(ty) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

enum class CalculativeOperator {
    Add, Sub, Mul, Div, Mod
}

class CalculativeExpressionNode(
    val lhs: ExpressionNode, val rhs: ExpressionNode, val operator: CalculativeOperator, ty: Type
) : ExpressionNode(ty) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

enum class CompareOperator {
    Equal, NotEqual, Less, LessEqual, Greater, GreaterEqual
}

class CompareExpressionNode(
    val lhs: ExpressionNode, val rhs: ExpressionNode, val operator: CompareOperator
) : ExpressionNode(builtinTypeBool) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

enum class LogicOperator {
    And, Or
}

class LogicExpressionNode(
    val lhs: ExpressionNode, val rhs: ExpressionNode, val operator: LogicOperator
) : ExpressionNode(builtinTypeBool) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class BlockExpressionNode(val stats: List<StatementNode>, val expr: ExpressionNode?) :
    ExpressionNode(expr?.type ?: builtinTypeVoid) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class LambdaExpressionNode(
    val parameterTypes: List<ParameterDeclarationNode>, val returnType: Type, val body: ExpressionNode
) : ExpressionNode(FunctionType(parameterTypes.map { it.paramType }, returnType)) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class CallExpressionNode(val expr: ExpressionNode, val args: List<ExpressionNode>, type: Type) : ExpressionNode(type) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class GenericsCallExpressionNode(
    val expr: ExpressionNode, val types: List<Type>, val args: List<ExpressionNode>, type: Type
) : ExpressionNode(type) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class MemberExpressionNode(val expr: ExpressionNode, val member: Identifier) : ExpressionNode(member.type) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class CastExpressionNode(val expr: ExpressionNode, val targetType: Type) : ExpressionNode(targetType) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class IfThenElseExpressionNode(
    val condition: ConditionNode, val thenExpr: ExpressionNode, val elseExpr: ExpressionNode, ty: Type
) : ExpressionNode(ty) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class IfDoExpressionNode(
    val condition: ConditionNode,
    val doExpr: ExpressionNode,
) : ExpressionNode(builtinTypeVoid) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

class WhileDoExpressionNode(val condition: ConditionNode, val doExpr: ExpressionNode) :
    ExpressionNode(builtinTypeVoid) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

sealed class ConditionNode(val hasPattern: Boolean)

class ExpressionConditionNode(val expr: ExpressionNode) : ConditionNode(false)

class PatternMatchConditionNode(val condExpr: ExpressionNode, val pattern: Pattern) : ConditionNode(true)

class LogicalConditionNode(
    val left: ConditionNode, val right: ConditionNode, val operator: LogicOperator
) : ConditionNode(left.hasPattern || right.hasPattern)

class AssignmentExpressionNode(
    val id: Identifier, val newValue: ExpressionNode
) : ExpressionNode(builtinTypeVoid) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}