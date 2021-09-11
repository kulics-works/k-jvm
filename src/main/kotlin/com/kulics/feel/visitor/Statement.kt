package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

internal fun DelegateVisitor.visitStatement(ctx: StatementContext): String {
    return when (val stat = ctx.getChild(0)) {
        is VariableDeclarationContext -> visitVariableDeclaration(stat)
        is ConstantDeclarationContext -> visitConstantDeclaration(stat)
        is FunctionDeclarationContext -> visitFunctionDeclaration(stat)
        is AssignmentContext -> visitAssignment(stat)
        is IfStatementContext -> visitIfStatement(stat)
        is WhileStatementContext -> visitWhileStatement(stat)
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
    addIdentifier(Identifier(id, type, IdentifierKind.Mutable))
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

internal fun DelegateVisitor.visitFunctionDeclaration(ctx: FunctionDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    return if (ctx.type() == null) {
        val params = visitParameterList(ctx.parameterList())
        pushScope()
        for (v in params.first) {
            if (isRedefineIdentifier(v.name)) {
                println("identifier: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addIdentifier(v)
        }
        val expr = visitExpression(ctx.expression())
        val returnType = expr.type
        popScope()
        val type = FunctionType(params.first.map { it.type }, returnType)
        addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
        "fun ${id}(${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${expr.generateCode()});$Wrap}$Wrap"
    } else {
        val returnTypeName = visitType(ctx.type())
        val returnType = getType(returnTypeName)
        if (returnType == null) {
            println("type: '${returnTypeName}' is undefined")
            throw CompilingCheckException()
        }
        val params = visitParameterList(ctx.parameterList())
        val type = FunctionType(params.first.map { it.type }, returnType)
        addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
        pushScope()
        for (v in params.first) {
            if (isRedefineIdentifier(v.name)) {
                println("identifier: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addIdentifier(v)
        }
        val expr = visitExpression(ctx.expression())
        if (expr.type != returnType) {
            println("the return is '${returnTypeName}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        "fun ${id}(${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${expr.generateCode()});$Wrap}$Wrap"
    }
}

internal fun DelegateVisitor.visitAssignment(ctx: AssignmentContext): String {
    val idName = visitIdentifier(ctx.identifier())
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
    return "${id.name} = ${expr.generateCode()}"
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

internal fun DelegateVisitor.visitWhileStatement(ctx: WhileStatementContext): String {
    val cond = visitExpression(ctx.expression())
    if (cond.type != builtinTypeBool) {
        println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
        throw CompilingCheckException()
    }
    val block = visitBlock(ctx.block())
    return "while (${cond.generateCode()}) { $block }"
}

internal fun DelegateVisitor.visitBlock(ctx: BlockContext): String {
    pushScope()
    val code = ctx.statement().fold(StringBuilder()) { acc, v -> acc.append("${visitStatement(v)};") }.toString()
    popScope()
    return code
}