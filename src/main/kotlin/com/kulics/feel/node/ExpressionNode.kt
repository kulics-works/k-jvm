package com.kulics.feel.node

import com.kulics.feel.visitor.*

sealed class ExpressionNode(val type: Type) : Node()

class ParenExpressionNode(private val expr: ExpressionNode) : ExpressionNode(expr.type) {
    override fun generateCode(): String {
        return "(${expr.generateCode()})"
    }
}

class IdentifierExpressionNode(private val id: Identifier) : ExpressionNode(id.type) {
    override fun generateCode(): String {
        return id.name
    }
}

class LiteralExpressionNode(private val text: String, ty: Type) : ExpressionNode(ty) {
    override fun generateCode(): String {
        return text
    }
}

enum class CalculativeOperator {
    Add, Sub, Mul, Div, Mod
}

class MultiplicativeExpressionNode(
    private val lhs: ExpressionNode,
    private val rhs: ExpressionNode,
    private val op: CalculativeOperator,
    ty: Type
) : ExpressionNode(ty) {
    override fun generateCode(): String {
        return when (op) {
            CalculativeOperator.Add -> "(${lhs.generateCode()} + ${rhs.generateCode()})"
            CalculativeOperator.Sub -> "(${lhs.generateCode()} - ${rhs.generateCode()})"
            CalculativeOperator.Mul -> "(${lhs.generateCode()} * ${rhs.generateCode()})"
            CalculativeOperator.Div -> "(${lhs.generateCode()} / ${rhs.generateCode()})"
            CalculativeOperator.Mod -> "(${lhs.generateCode()} % ${rhs.generateCode()})"
        }
    }
}

enum class CompareOperator {
    Equal, NotEqual, Less, LessEqual, Greater, GreaterEqual
}

class CompareExpressionNode(
    private val lhs: ExpressionNode,
    private val rhs: ExpressionNode,
    private val op: CompareOperator
) : ExpressionNode(builtinTypeBool) {
    override fun generateCode(): String {
        return when (op) {
            CompareOperator.Equal -> "(${lhs.generateCode()} == ${rhs.generateCode()})"
            CompareOperator.NotEqual -> "(${lhs.generateCode()} != ${rhs.generateCode()})"
            CompareOperator.Less -> "(${lhs.generateCode()} < ${rhs.generateCode()})"
            CompareOperator.LessEqual -> "(${lhs.generateCode()} <= ${rhs.generateCode()})"
            CompareOperator.Greater -> "(${lhs.generateCode()} > ${rhs.generateCode()})"
            CompareOperator.GreaterEqual -> "(${lhs.generateCode()} >= ${rhs.generateCode()})"
        }
    }
}

enum class LogicOperator {
    And, Or
}

class LogicExpressionNode(
    private val lhs: ExpressionNode,
    private val rhs: ExpressionNode,
    private val op: LogicOperator
) : ExpressionNode(builtinTypeBool) {
    override fun generateCode(): String {
        return when (op) {
            LogicOperator.And -> "(${lhs.generateCode()} && ${rhs.generateCode()})"
            LogicOperator.Or -> "(${lhs.generateCode()} || ${rhs.generateCode()})"
        }
    }
}

class BlockExpressionNode(val code: String, val expr: ExpressionNode?) : ExpressionNode(expr?.type ?: builtinTypeVoid) {
    override fun generateCode(): String {
        return "run{${code}${expr?.generateCode() ?: "Unit"}}"
    }
}

class CallExpressionNode(val expr: ExpressionNode, val args: List<ExpressionNode>, type: Type) : ExpressionNode(type) {
    override fun generateCode(): String {
        return "${expr.generateCode()}(${
            joinString(args) { it.generateCode() }
        })"
    }
}

class GenericsCallExpressionNode(
    val expr: ExpressionNode,
    val types: List<Type>,
    val args: List<ExpressionNode>,
    type: Type
) : ExpressionNode(type) {
    override fun generateCode(): String {
        return "${expr.generateCode()}<${
            joinString(types) { it.generateTypeName() }
        }> (${
            joinString(args) { it.generateCode() }
        })"
    }
}

class MemberExpressionNode(val expr: ExpressionNode, val member: Identifier) : ExpressionNode(member.type) {
    override fun generateCode(): String {
        return "${expr.generateCode()}.${member.name}"
    }
}

class ConditionExpressionNode(
    val condExpr: ExpressionNode,
    val thenExpr: ExpressionNode,
    val elseExpr: ExpressionNode,
    ty: Type
) : ExpressionNode(ty) {
    override fun generateCode(): String {
        return "if (${condExpr.generateCode()}) { ${thenExpr.generateCode()} } else { ${elseExpr.generateCode()} }"
    }
}

class ConstraintObjectNode(val argType: Type, val constraintType: ConstraintType) : ExpressionNode(argType) {
    override fun generateCode(): String {
        return when (constraintType) {
            is GenericsType ->
                "${constraintType.name}ConstraintObjectFor${argType.generateTypeName()}()"
            is InterfaceType ->
                "${constraintType.name}ConstraintObjectFor${argType.generateTypeName()}()"
        }
    }
}

fun generateFunctionSignature(type: FunctionType): Pair<List<String>, String> {
    var startName = 'a'
    val paramList = mutableListOf<String>()
    return paramList to "${
        joinString(type.parameterTypes) {
            val paramName = startName
            startName = startName.inc()
            paramList.add(paramName.toString())
            "${paramName}: ${it.generateTypeName()}"
        }
    }): ${type.returnType.generateTypeName()}"
}