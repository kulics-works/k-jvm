package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.*
import org.antlr.v4.runtime.tree.ParseTree

fun DelegateVisitor.visitExpression(ctx: ExpressionContext): ExpressionNode = when (ctx.childCount) {
    1 -> visitSingleExpression(ctx.getChild(0))
    2 -> if (ctx.memberAccessCallSuffix() != null) {
        visitMemberAccessFunctionCallExpression(ctx.expression(0), ctx.memberAccessCallSuffix())
    } else if (ctx.callSuffix() != null) {
        visitFunctionCallExpression(ctx.expression(0), ctx.callSuffix())
    } else {
        visitMemberAccessExpression(ctx.expression(0), ctx.memberAccess())
    }
    3 -> visitBinaryExpression(ctx.expression(0), ctx.getChild(1), ctx.expression(1))
    else -> throw CompilingCheckException()
}

fun DelegateVisitor.visitSingleExpression(expr: ParseTree): ExpressionNode {
    return when (expr) {
        is PrimaryExpressionContext -> visitPrimaryExpression(expr)
        is BlockExpressionContext -> visitBlockExpression(expr)
        is IfExpressionContext -> visitIfExpression(expr)
        is LambdaExpressionContext -> visitLambdaExpression(expr)
        else -> throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitMemberAccessFunctionCallExpression(
    exprCtx: ExpressionContext,
    callCtx: MemberAccessCallSuffixContext
): ExpressionNode {
    val expr = visitExpression(exprCtx)
    val type = when (val ty = expr.type) {
        is TypeParameter -> when (val constraintType = ty.constraint) {
            is GenericsType -> constraintType.typeConstructor(listOf(ty))
            is InterfaceType -> constraintType
        }
        else -> ty
    }
    val (memberIdentifier, typeArgs, args) = visitMemberAccessCallSuffix(callCtx)
    val member = type.getMember(memberIdentifier)
    if (member == null) {
        println("the type '${type.name}' have not member '${memberIdentifier}'")
        throw CompilingCheckException()
    }
    val memberAccessExpr = if (expr.type is TypeParameter && expr.type.isFromExtension) {
        MemberExpressionNode(CastExpressionNode(expr, type), member)
    } else {
        MemberExpressionNode(expr, member)
    }
    val callExprNode = when (val memberType = memberAccessExpr.type) {
        is FunctionType -> processFunctionCall(memberAccessExpr, typeArgs to args, memberType)
        is GenericsType -> processGenericsFunctionCall(memberAccessExpr, typeArgs to args, memberType)
        else -> {
            println("the type of expression is not a function")
            throw CompilingCheckException()
        }
    }
    return callExprNode
}

fun DelegateVisitor.visitFunctionCallExpression(
    exprCtx: ExpressionContext,
    callCtx: CallSuffixContext
): ExpressionNode {
    val expr = visitExpression(exprCtx)
    val callArgs = visitCallSuffix(callCtx)
    return when (val type = expr.type) {
        is FunctionType -> processFunctionCall(expr, callArgs, type)
        is GenericsType -> processGenericsFunctionCall(expr, callArgs, type)
        else -> {
            println("the type of expression is not a function")
            throw CompilingCheckException()
        }
    }
}

private fun DelegateVisitor.processFunctionCall(
    expr: ExpressionNode,
    callArgs: Pair<List<Type>, List<ExpressionNode>>,
    type: FunctionType
): CallExpressionNode {
    if (callArgs.first.isNotEmpty()) {
        println("the type of expression is not a generics function")
        throw CompilingCheckException()
    }
    if (type.parameterTypes.size != callArgs.second.size) {
        println("the size of args is ${callArgs.second.size}, but need ${type.parameterTypes.size}")
        throw CompilingCheckException()
    }
    val argList = mutableListOf<ExpressionNode>()
    for ((i, v) in type.parameterTypes.withIndex()) {
        if (cannotAssign(callArgs.second[i].type, v)) {
            println("the type of args${i}: '${callArgs.second[i].type.name}' is not '${v.name}'")
            throw CompilingCheckException()
        }
        argList.add(callArgs.second[i])
    }
    return CallExpressionNode(expr, argList, type.returnType)
}

private fun DelegateVisitor.processGenericsFunctionCall(
    expr: ExpressionNode,
    callArgs: Pair<List<Type>, List<ExpressionNode>>,
    type: GenericsType
): GenericsCallExpressionNode {
    if (type.typeParameter.size != callArgs.first.size) {
        println("the type args size need '${type.typeParameter.size}', but found '${callArgs.first.size}'")
        throw CompilingCheckException()
    }
    for ((i, v) in callArgs.first.withIndex()) {
        if (v is GenericsType) {
            println("the generics type '${v.name}' can not be type args")
            throw CompilingCheckException()
        }
        val typeParam = type.typeParameter[i]
        val constraintType = when (val ty = typeParam.constraint) {
            is GenericsType -> ty.typeConstructor(listOf(v)) as InterfaceType
            is InterfaceType -> ty
        }
        if (cannotAssign(v, constraintType)) {
            println("the type '${v.name}' can not confirm the type parameter '${constraintType.name}'")
            throw CompilingCheckException()
        }
    }
    val instanceType = type.typeConstructor(callArgs.first)
    if (instanceType !is FunctionType) {
        println("the type of expression is not a generics function")
        throw CompilingCheckException()
    }
    val argList = mutableListOf<ExpressionNode>()
    for ((i, v) in instanceType.parameterTypes.withIndex()) {
        if (cannotAssign(callArgs.second[i].type, v)) {
            println("the type of args${i}: '${callArgs.second[i].type.name}' is not '${v.name}'")
            throw CompilingCheckException()
        }
        argList.add(callArgs.second[i])
    }
    if (hasType(type.name)) {
        getImplementType(type)?.forEach {
            addImplementType(
                instanceType.returnType,
                if (it is GenericsType) it.typeConstructor(callArgs.first) else it
            )
        }
    }
    return GenericsCallExpressionNode(expr, callArgs.first, argList, instanceType.returnType)
}

fun DelegateVisitor.visitMemberAccessExpression(
    exprCtx: ExpressionContext,
    memberCtx: MemberAccessContext
): ExpressionNode {
    val expr = visitExpression(exprCtx)
    val type = expr.type
    val memberIdentifier = visitMemberAccess(memberCtx)
    val member = type.getMember(memberIdentifier)
    if (member == null) {
        println("the type '${expr.type.name}' have not member '${memberIdentifier}'")
        throw CompilingCheckException()
    }
    return MemberExpressionNode(expr, member)
}

fun DelegateVisitor.visitBinaryExpression(
    lhs: ExpressionContext,
    op: ParseTree,
    rhs: ExpressionContext
): ExpressionNode {
    val lhsExpr = visitExpression(lhs)
    val rhsExpr = visitExpression(rhs)
    return when (op) {
        is AdditiveOperatorContext -> {
            checkCalculateExpressionType(lhsExpr, rhsExpr)
            val symbol = if (op.Add() != null) CalculativeOperator.Add
            else CalculativeOperator.Sub
            CalculativeExpressionNode(lhsExpr, rhsExpr, symbol, lhsExpr.type)
        }
        is MultiplicativeOperatorContext -> {
            checkCalculateExpressionType(lhsExpr, rhsExpr)
            val symbol = if (op.Mul() != null) CalculativeOperator.Mul
            else if (op.Div() != null) CalculativeOperator.Div
            else CalculativeOperator.Mod
            CalculativeExpressionNode(lhsExpr, rhsExpr, symbol, lhsExpr.type)
        }
        is CompareOperatorContext -> {
            checkCompareExpressionType(lhsExpr, rhsExpr)
            val symbol = if (op.EqualEqual() != null) CompareOperator.Equal
            else if (op.NotEqual() != null) CompareOperator.NotEqual
            else if (op.Less() != null) CompareOperator.Less
            else if (op.LessEqual() != null) CompareOperator.LessEqual
            else if (op.Greater() != null) CompareOperator.Greater
            else CompareOperator.GreaterEqual
            CompareExpressionNode(lhsExpr, rhsExpr, symbol)
        }
        is LogicOperatorContext -> {
            checkLogicExpressionType(lhsExpr, rhsExpr)
            val symbol = if (op.AndAnd() != null) LogicOperator.And
            else LogicOperator.Or
            LogicExpressionNode(lhsExpr, rhsExpr, symbol)
        }
        else -> throw CompilingCheckException()
    }
}

fun checkCalculateExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeInt -> if (rhs.type != builtinTypeInt) {
            println("the type of right value is not '${builtinTypeInt.name}'")
            throw CompilingCheckException()
        }
        builtinTypeFloat -> if (rhs.type != builtinTypeFloat) {
            println("the type of right value is not '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeInt.name}' or '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
    }
}

fun checkCompareExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeInt -> if (rhs.type != builtinTypeInt) {
            println("the type of right value is not '${builtinTypeInt.name}'")
            throw CompilingCheckException()
        }
        builtinTypeFloat -> if (rhs.type != builtinTypeFloat) {
            println("the type of right value is not '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeInt.name}' or '${builtinTypeFloat.name}'")
            throw CompilingCheckException()
        }
    }
}

fun checkLogicExpressionType(lhs: ExpressionNode, rhs: ExpressionNode) {
    when (lhs.type) {
        builtinTypeBool -> if (rhs.type != builtinTypeBool) {
            println("the type of right value is not '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
        else -> {
            println("the type of left value is not '${builtinTypeBool.name}'")
            throw CompilingCheckException()
        }
    }
}

fun DelegateVisitor.visitMemberAccessCallSuffix(ctx: MemberAccessCallSuffixContext): MemberAccessCallSuffix {
    return MemberAccessCallSuffix(
        visitIdentifier(ctx.variableIdentifier()),
        ctx.type().map {
            checkTypeNode(visitType(it))
        },
        ctx.expression().map { visitExpression(it) }
    )
}

data class MemberAccessCallSuffix(val memberName: String, val typeArgs: List<Type>, val args: List<ExpressionNode>)

fun DelegateVisitor.visitCallSuffix(ctx: CallSuffixContext): Pair<List<Type>, List<ExpressionNode>> {
    return (ctx.type().map {
        checkTypeNode(visitType(it))
    }) to (ctx.expression().map { visitExpression(it) })
}

fun DelegateVisitor.visitMemberAccess(ctx: MemberAccessContext): String {
    return visitIdentifier(ctx.variableIdentifier())
}

fun DelegateVisitor.visitPrimaryExpression(ctx: PrimaryExpressionContext): ExpressionNode {
    return if (ctx.literalExpression() != null) {
        visitLiteralExpression(ctx.literalExpression())
    } else if (ctx.variableIdentifier() != null) {
        val name = visitIdentifier(ctx.variableIdentifier())
        val id = getIdentifier(name)
        if (id == null) {
            println("the identifier '${name}' is not define")
            throw CompilingCheckException()
        } else {
            IdentifierExpressionNode(id)
        }
    } else {
        val name = visitIdentifier(ctx.typeIdentifier())
        val id = getIdentifier(name)
        if (id == null) {
            println("the identifier '${name}' is not define")
            throw CompilingCheckException()
        } else {
            IdentifierExpressionNode(id)
        }
    }
}

fun DelegateVisitor.visitLiteralExpression(ctx: LiteralExpressionContext): ExpressionNode {
    return if (ctx.integerExpression() != null) {
        LiteralExpressionNode(ctx.integerExpression().text, builtinTypeInt)
    } else if (ctx.floatExpression() != null) {
        LiteralExpressionNode(ctx.floatExpression().text, builtinTypeFloat)
    } else if (ctx.characterExpression() != null) {
        LiteralExpressionNode(ctx.characterExpression().text, builtinTypeChar)
    } else if (ctx.boolExpression() != null) {
        LiteralExpressionNode(ctx.boolExpression().text, builtinTypeBool)
    } else {
        LiteralExpressionNode(ctx.stringExpression().text, builtinTypeString)
    }
}

fun DelegateVisitor.visitIfExpression(ctx: IfExpressionContext): ExpressionNode {
    val cond = visitExpression(ctx.expression(0))
    return if (ctx.pattern() == null) {
        processIf(ctx, cond)
    } else {
        processIfPattern(ctx, cond)
    }
}

private fun DelegateVisitor.processIf(
    ctx: IfExpressionContext,
    cond: ExpressionNode
): IfExpressionNode {
    if (cond.type != builtinTypeBool) {
        println("the type of if condition is '${cond.type.name}', but want '${builtinTypeBool.name}'")
        throw CompilingCheckException()
    }
    val thenBranch = visitExpression(ctx.expression(1))
    val elseBranch = visitExpression(ctx.expression(2))
    if (thenBranch.type != elseBranch.type) {
        println("the type of then branch is '${thenBranch.type.name}', and the type of else branch is '${elseBranch.type.name}', they are not equal")
        throw CompilingCheckException()
    }
    return IfExpressionNode(cond, thenBranch, elseBranch, thenBranch.type)
}

private fun DelegateVisitor.processIfPattern(
    ctx: IfExpressionContext,
    cond: ExpressionNode
): IfPatternExpressionNode {
    pushScope()
    val pattern = visitPattern(ctx.pattern())
    if (pattern is IdentifierPattern) {
        val identifier = Identifier(pattern.identifier, cond.type)
        addIdentifier(identifier)
    }
    val thenBranch = visitExpression(ctx.expression(1))
    popScope()
    val elseBranch = visitExpression(ctx.expression(2))
    if (thenBranch.type != elseBranch.type) {
        println("the type of then branch is '${thenBranch.type.name}', and the type of else branch is '${elseBranch.type.name}', they are not equal")
        throw CompilingCheckException()
    }
    when (pattern) {
        is TypePattern -> if (cond.type !is InterfaceType) {
            println("the type of condition is not interface, only interface type can use type pattern")
            throw CompilingCheckException()
        }
        is LiteralPattern -> checkCompareExpressionType(cond, pattern.expr)
        else -> Unit
    }
    return IfPatternExpressionNode(cond, pattern, thenBranch, elseBranch, thenBranch.type)
}

fun DelegateVisitor.visitIdentifierPattern(ctx: IdentifierPatternContext): String {
    return visitIdentifier(ctx.variableIdentifier())
}

fun DelegateVisitor.visitBlockExpression(ctx: BlockExpressionContext): BlockExpressionNode {
    pushScope()
    val stats = mutableListOf<StatementNode>()
    for (v in ctx.statement()) {
        stats.add(visitStatement(v))
    }
    val node = BlockExpressionNode(
        stats,
        when (val expr = ctx.expression()) {
            null -> null
            else -> visitExpression(expr)
        }
    )
    popScope()
    return node
}

fun DelegateVisitor.visitLambdaExpression(ctx: LambdaExpressionContext): LambdaExpressionNode {
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
        LambdaExpressionNode(params.map {
            ParameterDeclarationNode(it, it.type)
        }, returnType, expr)
    } else {
        val returnType = checkTypeNode(visitType(ctx.type()))
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
        if (cannotAssign(expr.type, returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        LambdaExpressionNode(params.map {
            ParameterDeclarationNode(it, it.type)
        }, returnType, expr)
    }
}