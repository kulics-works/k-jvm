package com.kulics.feel.node

class ProgramNode(private val code: String): Node() {
    override fun generateCode(): String {
        return code
    }
}