package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.BlockExpressionNode

internal fun DelegateVisitor.visitStatement(ctx: StatementContext): String {
    return when (val stat = ctx.getChild(0)) {
        is VariableDeclarationContext -> visitVariableDeclaration(stat)
        is ConstantDeclarationContext -> visitConstantDeclaration(stat)
        is IfStatementContext -> visitIfStatement(stat)
        is ExpressionContext -> visitExpression(stat).generateCode()
        else -> throw CompilingCheckException()
    }
}

internal fun DelegateVisitor.visitVariableDeclaration(ctx: VariableDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    val type = if (ctx.type() == null) {
        expr.type
    } else {
        val typeName = visitType(ctx.type())
        val type = getType(typeName)
        if (type == null) {
            println("type: '${typeName}' is undefined")
            throw CompilingCheckException()
        }
        if (expr.type != type) {
            println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
            throw CompilingCheckException()
        }
        type
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
    return "var $id: ${type.generateTypeName()} = ${expr.generateCode()}"
}

internal fun DelegateVisitor.visitConstantDeclaration(ctx: ConstantDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    val type = if (ctx.type() == null) {
        expr.type
    } else {
        val typeName = visitType(ctx.type())
        val type = getType(typeName)
        if (type == null) {
            println("type: '${typeName}' is undefined")
            throw CompilingCheckException()
        }
        if (expr.type != type) {
            println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
            throw CompilingCheckException()
        }
        type
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
    return "val $id: ${type.generateTypeName()} = ${expr.generateCode()}"
}

internal fun DelegateVisitor.visitIfStatement(ctx: IfStatementContext): String {
    val cond = visitExpression(ctx.expression())
    if (cond.type != builtinTypeBool) {
        println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
        throw CompilingCheckException()
    }
    val thenBranch = visitBlock(ctx.block(0))
    return if (ctx.ifStatement() != null) {
        "if (${cond.generateCode()}) { $thenBranch } else ${visitIfStatement(ctx.ifStatement())}"
    } else if (ctx.block().size == 1) {
        "if (${cond.generateCode()}) { $thenBranch }"
    } else {
        "if (${cond.generateCode()}) { $thenBranch } else { ${visitBlock(ctx.block(1))} }"
    }
}

internal fun DelegateVisitor.visitBlock(ctx: BlockContext): String {
    pushScope()
    val code = ctx.statement().fold(StringBuilder()) { acc, v -> acc.append("${visitStatement(v)};") }.toString()
    popScope()
    return code
}