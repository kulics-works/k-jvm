package com.kulics.feel.node

class ProgramNode(val preloadCode: String,val declarations: List<DeclarationNode>): Node() {
    override fun generateCode(): String {
        return ""
    }
}

