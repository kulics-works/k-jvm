package com.kulics.feel.node

import com.kulics.feel.visitor.Type

sealed class ExpressionNode(val type: Type?) {
    abstract fun generateCode(): String
}

class LiteralExpressionNode(private val text: String) : ExpressionNode(Type("Int")) {
    override fun generateCode(): String {
        return text
    }
}