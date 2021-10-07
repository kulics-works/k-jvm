package com.kulics.feel.node

class ProgramNode(val module: ModuleDeclarationNode, val declarations: List<DeclarationNode>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) {
        visitor.visit(this)
    }
}

