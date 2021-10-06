package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

fun DelegateVisitor.visitStatement(ctx: StatementContext): String {
    return when (val stat = ctx.getChild(0)) {
        is VariableDeclarationContext -> visitVariableDeclaration(stat)
        is FunctionDeclarationContext -> visitFunctionDeclaration(stat)
        is AssignmentContext -> visitAssignment(stat)
        is IfStatementContext -> visitIfStatement(stat)
        is WhileStatementContext -> visitWhileStatement(stat)
        is ExpressionContext -> visitExpression(stat).generateCode()
        else -> throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitVariableDeclaration(ctx: VariableDeclarationContext): String {
    val idName = visitIdentifier(ctx.identifier())
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
    return "var $idName: ${type.generateTypeName()} = ${expr.generateCode()}"
}

fun DelegateVisitor.visitFunctionDeclaration(ctx: FunctionDeclarationContext): String {
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
        val returnType = checkTypeNode(visitType(ctx.type()))
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
        if (cannotAssign(expr.type, returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        "fun ${id}(${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${expr.generateCode()});$Wrap}$Wrap"
    }
}

fun DelegateVisitor.visitAssignment(ctx: AssignmentContext): String {
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
    val type = id.type
    if (cannotAssign(expr.type, id.type)) {
        println("the type of assign value '${expr.type.name}' is not confirm '${id.type.name}'")
        throw CompilingCheckException()
    }
    return "${id.name} = ${expr.generateCode()}"
}

fun DelegateVisitor.visitIfStatement(ctx: IfStatementContext): String {
    val cond = visitExpression(ctx.expression())
    if (ctx.pattern() == null) {
        if (cond.type != builtinTypeBool) {
            println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
        val thenBranch = visitBlock(ctx.block(0))
        return if (ctx.ifStatement() != null) {
            "if (${cond.generateCode()}) { $thenBranch } else {${visitIfStatement(ctx.ifStatement())}}"
        } else if (ctx.block().size == 1) {
            "if (${cond.generateCode()}) { $thenBranch }"
        } else {
            "if (${cond.generateCode()}) { $thenBranch } else { ${visitBlock(ctx.block(1))} }"
        }
    } else {
        pushScope()
        val pattern = visitPattern(ctx.pattern())
        if (pattern is IdentifierPattern) {
            val identifier = Identifier(pattern.identifier, cond.type)
            addIdentifier(identifier)
        }
        val thenBranch = visitBlock(ctx.block(0))
        popScope()
        return when (pattern) {
            is TypePattern -> {
                if (cond.type !is InterfaceType) {
                    println("the type of condition is not interface, only interface type can use type pattern")
                    throw CompilingCheckException()
                }
                val matchCode =
                    "val ${pattern.identifier.name} = ${cond.generateCode()}.castOrNull<${
                        pattern.type.generateTypeName()
                    }>();$Wrap"
                val condCode = "${pattern.identifier.name} != null"
                if (ctx.ifStatement() != null) {
                    "$matchCode if (${condCode}) { $thenBranch } else {${visitIfStatement(ctx.ifStatement())}}"
                } else if (ctx.block().size == 1) {
                    "$matchCode if (${condCode}) { $thenBranch }"
                } else {
                    "$matchCode if (${condCode}) { $thenBranch } else { ${visitBlock(ctx.block(1))} }"
                }
            }
            is IdentifierPattern ->
                "run{val ${pattern.identifier} = ${cond.generateCode()};$Wrap ${thenBranch}${Wrap}};Unit;$Wrap"
            is LiteralPattern -> {
                checkCompareExpressionType(cond, pattern.expr)
                if (ctx.ifStatement() != null) {
                    "if (${cond.generateCode()} == ${pattern.expr.generateCode()}) { $thenBranch } else {${
                        visitIfStatement(
                            ctx.ifStatement()
                        )
                    }}"
                } else if (ctx.block().size == 1) {
                    "if (${cond.generateCode()} == ${pattern.expr.generateCode()}) { $thenBranch }"
                } else {
                    "if (${cond.generateCode()} == ${pattern.expr.generateCode()}) { $thenBranch } else { ${
                        visitBlock(
                            ctx.block(1)
                        )
                    } }"
                }
            }
            is WildcardPattern ->
                "run{${cond.generateCode()};$Wrap ${thenBranch}${Wrap}};Unit;$Wrap"
        }
    }
}

fun DelegateVisitor.visitWhileStatement(ctx: WhileStatementContext): String {
    val cond = visitExpression(ctx.expression())
    if (cond.type != builtinTypeBool) {
        println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
        throw CompilingCheckException()
    }
    val block = visitBlock(ctx.block())
    return "while (${cond.generateCode()}) { $block }"
}

fun DelegateVisitor.visitBlock(ctx: BlockContext): String {
    pushScope()
    val code = ctx.statement().fold(StringBuilder()) { acc, v -> acc.append("${visitStatement(v)};") }.toString()
    popScope()
    return code
}