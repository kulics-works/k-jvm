package com.kulics.k.visitor

import com.kulics.k.grammar.KParser.*
import com.kulics.k.node.ExpressionNode

fun DelegateVisitor.visitPattern(ctx: PatternContext): Pattern {
    return when (val pattern = ctx.getChild(0)) {
        is TypePatternContext -> {
            val castType = checkTypeNode(visitType(pattern.type()))
            if (pattern.identifierPattern() == null) {
                println("Unsupported pattern")
                throw CompilingCheckException()
            }
            val castIdentifier = Identifier(visitIdentifierPattern(pattern.identifierPattern()), castType)
            addIdentifier(castIdentifier)
            TypePattern(castType, castIdentifier)
        }
        is LiteralPatternContext ->
            LiteralPattern(visitLiteralExpression(pattern.literalExpression()))
        is IdentifierPatternContext ->
            IdentifierPattern(visitIdentifier(pattern.variableIdentifier()))
        is WildcardPatternContext ->
            WildcardPattern
        else -> {
            println("Unsupported pattern")
            throw CompilingCheckException()
        }
    }
}

sealed class Pattern

class TypePattern(val type: Type, val identifier: Identifier) : Pattern()
class LiteralPattern(val expr: ExpressionNode) : Pattern()
class IdentifierPattern(val identifier: String) : Pattern()
object WildcardPattern : Pattern()