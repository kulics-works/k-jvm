package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.*

fun DelegateVisitor.visitStatement(ctx: StatementContext): StatementNode {
    return when (val stat = ctx.getChild(0)) {
        is VariableDeclarationContext -> visitVariableDeclaration(stat)
        is FunctionDeclarationContext -> visitFunctionDeclaration(stat)
        is AssignmentContext -> visitAssignment(stat)
        is IfStatementContext -> visitIfStatement(stat)
        is WhileStatementContext -> visitWhileStatement(stat)
        is ExpressionContext -> ExpressionStatementNode(visitExpression(stat))
        else -> throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitVariableDeclaration(ctx: VariableDeclarationContext): VariableStatementNode {
    val idName = visitIdentifier(ctx.variableIdentifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
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
        val expr = visitExpression(ctx.expression())
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
        val expr = visitExpression(ctx.expression())
        if (cannotAssign(expr.type, returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        FunctionStatementNode(id, params.map { ParameterDeclarationNode(it, it.type) }, returnType, expr)
    }
}

fun DelegateVisitor.visitAssignment(ctx: AssignmentContext): AssignmentStatementNode {
    val idName = visitIdentifier(ctx.variableIdentifier())
    val id = getIdentifier(idName)
    if (id == null) {
        println("the identifier '${idName}' is not defined")
        throw CompilingCheckException()
    }
    if (id.kind == IdentifierKind.Immutable) {
        println("the identifier '${idName}' is not mutable")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    val type = id.type
    if (cannotAssign(expr.type, id.type)) {
        println("the type of assign value '${expr.type.name}' is not confirm '${id.type.name}'")
        throw CompilingCheckException()
    }
    return AssignmentStatementNode(id, expr)
}

fun DelegateVisitor.visitIfStatement(ctx: IfStatementContext): IfStatementNode {
    val cond = visitExpression(ctx.expression())
    if (ctx.pattern() == null) {
        if (cond.type != builtinTypeBool) {
            println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
        val thenBranch = visitBlock(ctx.block(0))
        return ifStatementNode(ctx, cond, null, thenBranch)
    } else {
        pushScope()
        val pattern = visitPattern(ctx.pattern())
        if (pattern is IdentifierPattern) {
            val identifier = Identifier(pattern.identifier, cond.type)
            addIdentifier(identifier)
        }
        when (pattern) {
            is TypePattern -> if (cond.type !is InterfaceType) {
                println("the type of condition is not interface, only interface type can use type pattern")
                throw CompilingCheckException()
            }
            is LiteralPattern ->
                checkCompareExpressionType(cond, pattern.expr)
            else -> {
            }
        }
        val thenBranch = visitBlock(ctx.block(0))
        popScope()
        return ifStatementNode(ctx, cond, pattern, thenBranch)
    }
}

private fun DelegateVisitor.ifStatementNode(
    ctx: IfStatementContext,
    cond: ExpressionNode,
    pattern: Pattern?,
    thenBranch: List<StatementNode>
): IfStatementNode {
    return if (ctx.ifStatement() != null) {
        IfStatementNode(cond, pattern, thenBranch, visitIfStatement(ctx.ifStatement()))
    } else if (ctx.block().size == 1) {
        IfStatementNode(cond, pattern, thenBranch, null)
    } else {
        IfStatementNode(cond, pattern, thenBranch, ElseBranch(visitBlock(ctx.block(1))))
    }
}

fun DelegateVisitor.visitWhileStatement(ctx: WhileStatementContext): WhileStatementNode {
    val cond = visitExpression(ctx.expression())
    if (cond.type != builtinTypeBool) {
        println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
        throw CompilingCheckException()
    }
    return WhileStatementNode(cond, visitBlock(ctx.block()))
}

fun DelegateVisitor.visitBlock(ctx: BlockContext): List<StatementNode> {
    val stats = mutableListOf<StatementNode>()
    pushScope()
    for (v in ctx.statement()) {
        stats.add(visitStatement(v))
    }
    popScope()
    return stats
}
