package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.*

fun DelegateVisitor.visitStatement(ctx: StatementContext): StatementNode {
    return when (val stat = ctx.getChild(0)) {
        is VariableDeclarationContext -> visitVariableDeclaration(stat)
        is FunctionDeclarationContext -> visitFunctionDeclaration(stat)
        is ExpressionStatementContext -> visitExpressionStatement(stat)
        else -> throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitVariableDeclaration(ctx: VariableDeclarationContext): VariableStatementNode {
    val idName = visitIdentifier(ctx.variableIdentifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
    val type = if (ctx.type() == null) {
        expr.type
    } else {
        val type = checkTypeNode(visitType(ctx.type()))
        if (cannotAssign(expr.type, type)) {
            println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
            throw CompilingCheckException()
        }
        type
    }
    val id = Identifier(idName, type, if (ctx.Mut() != null) IdentifierKind.Mutable else IdentifierKind.Immutable)
    addIdentifier(id)
    return VariableStatementNode(id, expr)
}

fun DelegateVisitor.visitFunctionDeclaration(ctx: FunctionDeclarationContext): FunctionStatementNode {
    val idName = visitIdentifier(ctx.variableIdentifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    return if (ctx.type() == null) {
        val params = visitParameterList(ctx.parameterList())
        pushScope()
        for (v in params) {
            if (isRedefineIdentifier(v.name)) {
                println("identifier: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addIdentifier(v)
        }
        val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
        val returnType = expr.type
        popScope()
        val type = FunctionType(params.map { it.type }, returnType)
        val id = Identifier(idName, type, IdentifierKind.Immutable)
        addIdentifier(id)
        FunctionStatementNode(id, params.map { ParameterDeclarationNode(it, it.type) }, returnType, expr)
    } else {
        val returnType = checkTypeNode(visitType(ctx.type()))
        val params = visitParameterList(ctx.parameterList())
        val type = FunctionType(params.map { it.type }, returnType)
        val id = Identifier(idName, type, IdentifierKind.Immutable)
        addIdentifier(id)
        pushScope()
        for (v in params) {
            if (isRedefineIdentifier(v.name)) {
                println("identifier: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addIdentifier(v)
        }
        val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
        if (cannotAssign(expr.type, returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        FunctionStatementNode(id, params.map { ParameterDeclarationNode(it, it.type) }, returnType, expr)
    }
}

fun DelegateVisitor.visitExpressionStatement(ctx: ExpressionStatementContext): ExpressionStatementNode {
    return ExpressionStatementNode(
        if (ctx.expression() != null) visitExpression(ctx.expression())
        else visitExpressionWithBlock(ctx.expressionWithBlock())
    )
}