package com.kulics.feel.node

import com.kulics.feel.visitor.CompilingCheckException

sealed class Node {
    open fun generateCode(): String {
        return ""
    }

    open fun accept(visitor: NodeVisitor) {
        throw CompilingCheckException()
    }
}

interface NodeVisitor {
    fun visit(node: ProgramNode)
    fun visit(node: ModuleDeclarationNode)
    fun visit(node: GlobalRecordDeclarationNode)
    fun visit(node: GlobalGenericsRecordDeclarationNode)
    fun visit(node: GlobalFunctionDeclarationNode)
    fun visit(node: ParameterDeclarationNode)
    fun visit(node: GlobalVariableDeclarationNode)
    fun visit(node: GlobalInterfaceDeclarationNode)
    fun visit(node: GlobalExtensionDeclarationNode)
}