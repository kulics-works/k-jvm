package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

internal fun DelegateVisitor.visitModuleDeclaration(ctx: ModuleDeclarationContext): String {
    return "package ${visitIdentifier(ctx.identifier())}$Wrap"
}

internal fun DelegateVisitor.visitGlobalVariableDeclaration(ctx: GlobalVariableDeclarationContext): String {
    val id = visitVariableIdentifier(ctx.variableIdentifier())
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
    return "var $id: ${type.generateTypeName()} = ${expr.generateCode()}$Wrap"
}

internal fun DelegateVisitor.visitGlobalConstantDeclaration(ctx: GlobalConstantDeclarationContext): String {
    val id = visitConstantIdentifier(ctx.constantIdentifier())
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
    return "val $id: ${type.generateTypeName()} = ${expr.generateCode()}$Wrap"
}

internal fun DelegateVisitor.visitGlobalFunctionDeclaration(ctx: GlobalFunctionDeclarationContext): String {
    val id = visitConstantIdentifier(ctx.constantIdentifier())
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
    val type = FunctionType(returnType, returnType)
    val blockCode = if (ctx.blockExpression() != null) {
        val blockExpr = visitBlockExpression(ctx.blockExpression())
        if (blockExpr.type != returnType) {
            println("the return is '${returnTypeName}', but find '${blockExpr.type.name}'")
            throw CompilingCheckException()
        }
        "${blockExpr.generateCode()}${Wrap}return ${blockExpr.expr.generateCode()}$Wrap"
    } else {
        ""
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
    return "fun ${id}(): ${returnType.generateTypeName()} {$blockCode}$Wrap"
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


