package com.kulics.k.node

class ProgramNode(val module: ModuleDeclarationNode, val declarations: List<DeclarationNode>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

