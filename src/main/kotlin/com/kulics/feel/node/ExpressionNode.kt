package com.kulics.feel.node

import com.kulics.feel.visitor.Type

sealed class ExpressionNode(val type: Type) : Node() {
    abstract fun generateCode(): String
}

class LiteralExpressionNode(private val text: String, ty: Type) : ExpressionNode(ty) {
    override fun generateCode(): String {
        return text
    }
}

enum class AdditiveOperator {
    Add, Sub
}

class AdditiveExpressionNode(
    private val lhs: ExpressionNode,
    private val rhs: ExpressionNode,
    private val op: AdditiveOperator,
    ty: Type
) : ExpressionNode(ty) {
    override fun generateCode(): String {
        return when (op) {
            AdditiveOperator.Add -> "(${lhs.generateCode()} + ${rhs.generateCode()})"
            AdditiveOperator.Sub -> "(${lhs.generateCode()} - ${rhs.generateCode()})"
        }
    }
}

enum class MultiplicativeOperator {
    Mul, Div, Mod
}

class MultiplicativeExpressionNode(
    private val lhs: ExpressionNode,
    private val rhs: ExpressionNode,
    private val op: MultiplicativeOperator,
    ty: Type
) : ExpressionNode(ty) {
    override fun generateCode(): String {
        return when (op) {
            MultiplicativeOperator.Mul -> "(${lhs.generateCode()} * ${rhs.generateCode()})"
            MultiplicativeOperator.Div -> "(${lhs.generateCode()} / ${rhs.generateCode()})"
            MultiplicativeOperator.Mod -> "(${lhs.generateCode()} % ${rhs.generateCode()})"
        }
    }
}