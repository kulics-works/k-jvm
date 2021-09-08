package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

internal fun DelegateVisitor.visitModuleDeclaration(ctx: ModuleDeclarationContext): String {
    return "package ${visitIdentifier(ctx.identifier())}$Wrap"
}

internal fun DelegateVisitor.visitGlobalVariableDeclaration(ctx: GlobalVariableDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val typeName = visitType(ctx.type())
    val type = getType(typeName)
    if (type == null) {
        println("type: '${typeName}' is undefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    if (expr.type != type) {
        println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
        throw CompilingCheckException()
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Mutable))
    return "var $id: ${type.generateTypeName()} = (${expr.generateCode()});$Wrap"
}

internal fun DelegateVisitor.visitGlobalConstantDeclaration(ctx: GlobalConstantDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val typeName = visitType(ctx.type())
    val type = getType(typeName)
    if (type == null) {
        println("type: '${typeName}' is undefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    if (expr.type != type) {
        println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
        throw CompilingCheckException()
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
    return "val $id: ${type.generateTypeName()} = (${expr.generateCode()});$Wrap"
}

internal fun DelegateVisitor.visitGlobalFunctionDeclaration(ctx: GlobalFunctionDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
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
    return "fun ${id}(${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${expr.generateCode()});$Wrap}$Wrap"
}

internal fun DelegateVisitor.visitParameterList(ctx: ParameterListContext): Pair<ArrayList<Identifier>, String> {
    val params = ctx.parameter()
    val buf = StringBuilder()
    val ids = ArrayList<Identifier>()
    if (params.size > 0) {
        val first = visitParameter(params[0])
        fun genParam(id: Identifier): String {
            return "${id.name}: ${id.type.generateTypeName()}"
        }
        buf.append(genParam(first))
        ids.add(first)
        for (i in 1 until params.size) {
            val id = visitParameter(params[i])
            ids.add(id)
            buf.append(", ${genParam(id)}")
        }
    }
    return ids to buf.toString()
}

internal fun DelegateVisitor.visitParameter(ctx: ParameterContext): Identifier {
    val id = visitIdentifier(ctx.identifier())
    val typeName = visitType(ctx.type())
    val type = getType(typeName)
    if (type == null) {
        println("type: '${typeName}' is undefined")
        throw CompilingCheckException()
    }
    return Identifier(id, type, IdentifierKind.Immutable)
}

internal fun DelegateVisitor.visitGlobalDeclaration(ctx: GlobalDeclarationContext): String {
    return if (ctx.globalVariableDeclaration() != null) {
        visitGlobalVariableDeclaration(ctx.globalVariableDeclaration())
    } else if (ctx.globalConstantDeclaration() != null) {
        visitGlobalConstantDeclaration(ctx.globalConstantDeclaration())
    } else if (ctx.globalFunctionDeclaration() != null) {
        visitGlobalFunctionDeclaration(ctx.globalFunctionDeclaration())
    } else {
        throw CompilingCheckException()
    }
}

internal fun DelegateVisitor.visitProgram(ctx: ProgramContext): String {
    val result = StringBuilder()
    result.append(visitModuleDeclaration(ctx.moduleDeclaration()))
    for (item in ctx.globalDeclaration()) {
        result.append(visitGlobalDeclaration(item))
    }
    return result.toString()
}


