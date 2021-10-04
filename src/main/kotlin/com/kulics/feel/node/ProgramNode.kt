package com.kulics.feel.node

class ProgramNode(val module: ModuleDeclarationNode, val declarations: List<DeclarationNode>) : Node() {
    override fun accept(visitor: NodeVisitor) {
        visitor.visit(this)
    }
}

