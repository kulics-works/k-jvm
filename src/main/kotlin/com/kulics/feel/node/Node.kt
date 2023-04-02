package com.kulics.feel.node

import com.kulics.feel.visitor.CompilingCheckException

sealed class Node {
    open fun <T> accept(visitor: NodeVisitor<T>): T {
        throw CompilingCheckException()
    }
}

interface NodeVisitor<T> {
    fun visit(node: ProgramNode): T

    fun visit(node: ModuleDeclarationNode): T
    fun visit(node: GlobalRecordDeclarationNode): T
    fun visit(node: GlobalFunctionDeclarationNode): T
    fun visit(node: ParameterDeclarationNode): T
    fun visit(node: GlobalVariableDeclarationNode): T
    fun visit(node: GlobalInterfaceDeclarationNode): T
    fun visit(node: GlobalExtensionDeclarationNode): T
    fun visit(node: GlobalSumTypeDeclarationNode): T

    fun visit(node: StatementNode): T
    fun visit(node: VariableStatementNode): T
    fun visit(node: ExpressionStatementNode): T
    fun visit(node: FunctionStatementNode): T
    fun visit(node: WhileDoExpressionNode): T

    fun visit(node: ExpressionNode): T
    fun visit(node: IdentifierExpressionNode): T
    fun visit(node: LiteralExpressionNode): T
    fun visit(node: CalculativeExpressionNode): T
    fun visit(node: CompareExpressionNode): T
    fun visit(node: LogicExpressionNode): T
    fun visit(node: BlockExpressionNode): T
    fun visit(node: LambdaExpressionNode): T
    fun visit(node: CallExpressionNode): T
    fun visit(node: GenericsCallExpressionNode): T
    fun visit(node: MemberExpressionNode): T
    fun visit(node: IfThenElseExpressionNode): T
    fun visit(node: IfDoExpressionNode): T
    fun visit(node: CastExpressionNode): T
    fun visit(node: AssignmentExpressionNode): T
}