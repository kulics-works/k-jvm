package com.kulics.k.visitor

import com.kulics.k.grammar.KParser.*
import com.kulics.k.node.*
import org.antlr.v4.runtime.tree.ParseTree

fun DelegateVisitor.visitExpressionWithTerminator(ctx: ExpressionWithTerminatorContext): ExpressionNode {
    return if (ctx.expression() != null) {
        visitExpression(ctx.expression())
    } else {
        visitExpressionWithBlock(ctx.expressionWithBlock())
    }
}

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
        is IfThenElseExpressionContext -> visitIfThenElseExpression(expr)
        is IfDoExpressionContext -> visitIfDoExpression(expr)
        is WhileDoExpressionContext -> visitWhileDoExpression(expr)
        is LambdaExpressionContext -> visitLambdaExpression(expr)
        is ExpressionWithBlockContext -> visitExpressionWithBlock(expr)
        is AssignmentExpressionContext -> visitAssignmentExpression(expr)
        else -> throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitExpressionWithBlock(ctx: ExpressionWithBlockContext): ExpressionNode {
    return when (val expr = ctx.getChild(0)) {
        is BlockExpressionContext -> visitBlockExpression(expr)
        is IfDoExpressionWithBlockContext -> visitIfDoExpressionWithBlock(expr)
        is IfThenElseExpressionWithBlockContext -> visitIfThenElseExpressionWithBlock(expr)
        is WhileDoExpressionWithBlockContext -> visitWhileDoExpressionWithBlock(expr)
        is AssignmentExpressionWithBlockContext -> visitAssignmentExpressionWithBlock(expr)
        else -> throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitMemberAccessFunctionCallExpression(
    exprCtx: ExpressionContext, callCtx: MemberAccessCallSuffixContext
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
        is FunctionType -> processFunctionCall(memberAccessExpr, args, memberType)
        is GenericsType -> processGenericsFunctionCall(memberAccessExpr, typeArgs to args, memberType)
        else -> {
            println("the type of expression is not a function")
            throw CompilingCheckException()
        }
    }
    return callExprNode
}

fun DelegateVisitor.visitFunctionCallExpression(
    exprCtx: ExpressionContext, callCtx: CallSuffixContext
): ExpressionNode {
    val expr = visitExpression(exprCtx)
    return when (val type = expr.type) {
        is FunctionType -> {
            processFunctionCall(expr, (callCtx.expression().map { visitExpression(it) }), type)
        }

        else -> {
            println("the type of expression is not a function")
            throw CompilingCheckException()
        }
    }
}

private fun DelegateVisitor.processFunctionCall(
    expr: ExpressionNode, callArgs: List<ExpressionNode>, type: FunctionType
): FunctionCallExpressionNode {
    if (type.parameterTypes.size != callArgs.size) {
        println("the size of args is ${callArgs.size}, but need ${type.parameterTypes.size}")
        throw CompilingCheckException()
    }
    val argList = mutableListOf<ExpressionNode>()
    for ((i, v) in type.parameterTypes.withIndex()) {
        if (cannotAssign(callArgs[i].type, v)) {
            println("the type of args${i}: '${callArgs[i].type.name}' is not '${v.name}'")
            throw CompilingCheckException()
        }
        argList.add(callArgs[i])
    }
    return FunctionCallExpressionNode(expr, emptyList(), argList, type.returnType)
}

private fun DelegateVisitor.processGenericsFunctionCall(
    expr: ExpressionNode, callArgs: Pair<List<Type>, List<ExpressionNode>>, type: GenericsType
): FunctionCallExpressionNode {
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
                instanceType.returnType, if (it is GenericsType) it.typeConstructor(callArgs.first) else it
            )
        }
    }
    return FunctionCallExpressionNode(expr, callArgs.first, argList, instanceType.returnType)
}

fun DelegateVisitor.visitMemberAccessExpression(
    exprCtx: ExpressionContext, memberCtx: MemberAccessContext
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
    lhs: ExpressionContext, op: ParseTree, rhs: ExpressionContext
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

        is LogicAndOperatorContext -> {
            checkLogicExpressionType(lhsExpr, rhsExpr)
            LogicExpressionNode(lhsExpr, rhsExpr, LogicOperator.And)
        }

        is LogicOrOperatorContext -> {
            checkLogicExpressionType(lhsExpr, rhsExpr)
            LogicExpressionNode(lhsExpr, rhsExpr, LogicOperator.Or)
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

        builtinTypeInt -> if (rhs.type != builtinTypeInt) {
            println("the type of right value is not '${builtinTypeInt.name}'")
            throw CompilingCheckException()
        }

        else -> {
            println("the type of left value is not '${builtinTypeBool.name}' or '${builtinTypeInt.name}'")
            throw CompilingCheckException()
        }
    }
}

fun DelegateVisitor.visitMemberAccessCallSuffix(ctx: MemberAccessCallSuffixContext): MemberAccessCallSuffix {
    return MemberAccessCallSuffix(visitIdentifier(ctx.variableIdentifier()), ctx.type().map {
        checkTypeNode(visitType(it))
    }, ctx.expression().map { visitExpression(it) })
}

data class MemberAccessCallSuffix(val memberName: String, val typeArgs: List<Type>, val args: List<ExpressionNode>)

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
    } else if (ctx.functionCallExpression() != null) {
        val callCtx = ctx.functionCallExpression()
        val name = visitIdentifier(callCtx.variableIdentifier())
        val id = getIdentifier(name)
        return if (id == null) {
            println("the identifier '${name}' is not define")
            throw CompilingCheckException()
        } else {
            when (val type = id.type) {
                is FunctionType -> {
                    if (callCtx.type().isNotEmpty()) {
                        println("the type is not a generics type")
                        throw CompilingCheckException()
                    }
                    processFunctionCall(
                        IdentifierExpressionNode(id),
                        (callCtx.expression().map { visitExpression(it) }),
                        type
                    )
                }

                is GenericsType -> {
                    processGenericsFunctionCall(IdentifierExpressionNode(id), (
                            callCtx.type().map {
                                checkTypeNode(visitType(it))
                            } to callCtx.expression().map { visitExpression(it) }
                            ), type)
                }

                else -> {
                    println("the type of expression is not a function")
                    throw CompilingCheckException()
                }
            }
        }
    } else {
        visitConstructExpression(ctx.constructExpression())
    }
}

fun DelegateVisitor.visitConstructExpression(ctx: ConstructExpressionContext): ConstructCallExpressionNode {
    val name = visitIdentifier(ctx.typeIdentifier())
    val id = getIdentifier(name)
    return if (id == null) {
        println("the identifier '${name}' is not define")
        throw CompilingCheckException()
    } else {
        when (val type = id.type) {
            is FunctionType -> {
                if (ctx.type().isNotEmpty()) {
                    println("the type is not a generics type")
                    throw CompilingCheckException()
                }
                processConstructCall(id, ctx.expression().map { visitExpression(it) }, type)
            }

            is GenericsType -> {
                processGenericsConstructCall(id, (ctx.type().map {
                    checkTypeNode(visitType(it))
                }) to (ctx.expression().map { visitExpression(it) }), type)
            }

            else -> {
                println("compiler internal error: construct expression")
                throw CompilingCheckException()
            }
        }
    }
}

private fun DelegateVisitor.processConstructCall(
    identifier: Identifier, callArgs: List<ExpressionNode>, type: FunctionType
): ConstructCallExpressionNode {
    if (type.parameterTypes.size != callArgs.size) {
        println("the size of args is ${callArgs.size}, but need ${type.parameterTypes.size}")
        throw CompilingCheckException()
    }
    val argList = mutableListOf<ExpressionNode>()
    for ((i, v) in type.parameterTypes.withIndex()) {
        if (cannotAssign(callArgs[i].type, v)) {
            println("the type of args${i}: '${callArgs[i].type.name}' is not '${v.name}'")
            throw CompilingCheckException()
        }
        argList.add(callArgs[i])
    }
    return ConstructCallExpressionNode(identifier, emptyList(), argList, type.returnType)
}

private fun DelegateVisitor.processGenericsConstructCall(
    identifier: Identifier, callArgs: Pair<List<Type>, List<ExpressionNode>>, type: GenericsType
): ConstructCallExpressionNode {
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
        println("the type is not a generics type")
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
                instanceType.returnType, if (it is GenericsType) it.typeConstructor(callArgs.first) else it
            )
        }
    }
    return ConstructCallExpressionNode(identifier, callArgs.first, argList, instanceType.returnType)
}

fun DelegateVisitor.visitLiteralExpression(ctx: LiteralExpressionContext): ExpressionNode {
    return if (ctx.integerExpression() != null) {
        LiteralExpressionNode(ctx.integerExpression().text, builtinTypeInt)
    } else if (ctx.floatExpression() != null) {
        LiteralExpressionNode(ctx.floatExpression().text, builtinTypeFloat)
    } else if (ctx.runeExpression() != null) {
        LiteralExpressionNode(ctx.runeExpression().text, builtinTypeRune)
    } else if (ctx.boolExpression() != null) {
        LiteralExpressionNode(ctx.boolExpression().text, builtinTypeBool)
    } else {
        LiteralExpressionNode(ctx.stringExpression().text, builtinTypeString)
    }
}

fun DelegateVisitor.visitIfThenElseExpression(ctx: IfThenElseExpressionContext): ExpressionNode {
    return processIfThenElseExpression(ctx.condition(), ctx.expression(0), Either.Left(ctx.expression(1)))
}

fun DelegateVisitor.visitIfThenElseExpressionWithBlock(ctx: IfThenElseExpressionWithBlockContext): ExpressionNode {
    return processIfThenElseExpression(ctx.condition(), ctx.expression(), Either.Right(ctx.expressionWithBlock()))
}

private fun DelegateVisitor.processIfThenElseExpression(
    cond: ConditionContext,
    thenExpression: ExpressionContext,
    elseExpression: Either<ExpressionContext, ExpressionWithBlockContext>
): IfThenElseExpressionNode {
    pushScope()
    val condition = visitCondition(cond)
    val thenBranch = visitExpression(thenExpression)
    popScope()
    val elseBranch = when (elseExpression) {
        is Either.Left -> visitExpression(elseExpression.value)
        is Either.Right -> visitExpressionWithBlock(elseExpression.value)
    }
    if (thenBranch.type != elseBranch.type) {
        println("the type of then branch is '${thenBranch.type.name}', and the type of else branch is '${elseBranch.type.name}', they are not equal")
        throw CompilingCheckException()
    }
    return IfThenElseExpressionNode(condition, thenBranch, elseBranch, thenBranch.type)
}

fun DelegateVisitor.visitIfDoExpression(ctx: IfDoExpressionContext): ExpressionNode {
    return processIfDoExpression(ctx.condition(), Either.Left(ctx.expression()))
}

fun DelegateVisitor.visitIfDoExpressionWithBlock(ctx: IfDoExpressionWithBlockContext): ExpressionNode {
    return processIfDoExpression(ctx.condition(), Either.Right(ctx.expressionWithBlock()))
}

fun DelegateVisitor.processIfDoExpression(
    cond: ConditionContext,
    branch: Either<ExpressionContext, ExpressionWithBlockContext>
): ExpressionNode {
    pushScope()
    val condition = visitCondition(cond)
    val doBranch = when (branch) {
        is Either.Left -> visitExpression(branch.value)
        is Either.Right -> visitExpressionWithBlock(branch.value)
    }
    popScope()
    return IfDoExpressionNode(condition, doBranch)
}

fun DelegateVisitor.visitCondition(
    cond: ConditionContext
): ConditionNode {
    val pat = cond.pattern()
    val expr = cond.expression()
    return if (expr != null) {
        val exprNode = visitExpression(expr)
        if (pat != null) {
            val pattern = visitPattern(pat)
            when (pattern) {
                is IdentifierPattern -> {
                    val identifier = Identifier(pattern.identifier, exprNode.type)
                    addIdentifier(identifier)
                }

                is TypePattern -> if (exprNode.type !is InterfaceType) {
                    println("the type of condition is not interface, only interface type can use type pattern")
                    throw CompilingCheckException()
                }

                is LiteralPattern -> checkCompareExpressionType(exprNode, pattern.expr)
                else -> Unit
            }
            PatternMatchConditionNode(exprNode, pattern)
        } else {
            if (exprNode.type != builtinTypeBool) {
                println("the type of if condition is '${exprNode.type.name}', but want '${builtinTypeBool.name}'")
                throw CompilingCheckException()
            }
            ExpressionConditionNode(exprNode)
        }
    } else if (cond.AndAnd() != null) {
        val l = visitCondition(cond.condition(0))
        val r = visitCondition(cond.condition(1))
        LogicalConditionNode(l, r, LogicOperator.And)
    } else if (cond.OrOr() != null) {
        val l = visitCondition(cond.condition(0))
        val r = visitCondition(cond.condition(1))
        if (l.hasPattern || r.hasPattern) {
            println("the or condition can not has pattern")
            throw CompilingCheckException()
        }
        LogicalConditionNode(l, r, LogicOperator.Or)
    } else {
        visitCondition(cond.condition(0))
    }
}

fun DelegateVisitor.visitWhileDoExpression(ctx: WhileDoExpressionContext): ExpressionNode {
    return processWhileDoExpression(ctx.condition(), Either.Left(ctx.expression()))
}

fun DelegateVisitor.visitWhileDoExpressionWithBlock(ctx: WhileDoExpressionWithBlockContext): ExpressionNode {
    return processWhileDoExpression(ctx.condition(), Either.Right(ctx.expressionWithBlock()))
}

fun DelegateVisitor.processWhileDoExpression(
    cond: ConditionContext,
    branch: Either<ExpressionContext, ExpressionWithBlockContext>
): ExpressionNode {
    pushScope()
    val condition = visitCondition(cond)
    val doBranch = when (branch) {
        is Either.Left -> visitExpression(branch.value)
        is Either.Right -> visitExpressionWithBlock(branch.value)
    }
    popScope()
    return WhileDoExpressionNode(condition, doBranch)
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
        stats, when (val expr = ctx.expression()) {
            null -> null
            else -> visitExpression(expr)
        }
    )
    popScope()
    return node
}

fun DelegateVisitor.visitAssignmentExpression(ctx: AssignmentExpressionContext): AssignmentExpressionNode {
    return processAssignmentExpression(ctx.variableIdentifier(), Either.Left(ctx.expression()))
}

fun DelegateVisitor.visitAssignmentExpressionWithBlock(ctx: AssignmentExpressionWithBlockContext): AssignmentExpressionNode {
    return processAssignmentExpression(ctx.variableIdentifier(), Either.Right(ctx.expressionWithBlock()))
}

fun DelegateVisitor.processAssignmentExpression(
    leftValue: VariableIdentifierContext,
    rightValue: Either<ExpressionContext, ExpressionWithBlockContext>
): AssignmentExpressionNode {
    val idName = visitIdentifier(leftValue)
    val id = getIdentifier(idName)
    if (id == null) {
        println("the identifier '${idName}' is not defined")
        throw CompilingCheckException()
    }
    if (id.kind == IdentifierKind.Immutable) {
        println("the identifier '${idName}' is not mutable")
        throw CompilingCheckException()
    }
    val expr = when (rightValue) {
        is Either.Left -> visitExpression(rightValue.value)
        is Either.Right -> visitExpressionWithBlock(rightValue.value)
    }
    if (cannotAssign(expr.type, id.type)) {
        println("the type of assign value '${expr.type.name}' is not confirm '${id.type.name}'")
        throw CompilingCheckException()
    }
    return AssignmentExpressionNode(id, expr)
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