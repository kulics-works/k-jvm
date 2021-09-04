package com.kulics.feel.node

class ProgramNode(private val code: String): Node() {
    fun generateCode(): String {
        return code
    }
}