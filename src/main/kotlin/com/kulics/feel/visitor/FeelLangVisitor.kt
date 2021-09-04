package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.grammar.FeelParserBaseVisitor
import com.kulics.feel.node.ExpressionNode

class FeelLangVisitor : FeelParserBaseVisitor<Any>() {
    private val delegate = DelegateVisitor()

    fun generateCode(ctx: ProgramContext): String {
        return visitProgram(ctx)
    }

    override fun visitIdentifier(ctx: IdentifierContext): String {
        return delegate.visitIdentifier(ctx)
    }

    override fun visitVariableIdentifier(ctx: VariableIdentifierContext): String {
        return delegate.visitVariableIdentifier(ctx)
    }

    override fun visitConstantIdentifier(ctx: ConstantIdentifierContext): String {
        return delegate.visitConstantIdentifier(ctx)
    }

    override fun visitModuleDeclaration(ctx: ModuleDeclarationContext): String {
        return delegate.visitModuleDeclaration(ctx)
    }

    override fun visitGlobalVariableDeclaration(ctx: GlobalVariableDeclarationContext): String {
        return delegate.visitGlobalVariableDeclaration(ctx)
    }

    override fun visitGlobalConstantDeclaration(ctx: GlobalConstantDeclarationContext): String {
        return delegate.visitGlobalConstantDeclaration(ctx)
    }

    override fun visitGlobalDeclaration(ctx: GlobalDeclarationContext): String {
        return delegate.visitGlobalDeclaration(ctx)
    }

    override fun visitExpression(ctx: ExpressionContext): ExpressionNode {
        return delegate.visitExpression(ctx)
    }

    override fun visitPrimaryExpression(ctx: PrimaryExpressionContext): ExpressionNode {
        return delegate.visitPrimaryExpression(ctx)
    }

    override fun visitLiteralExpression(ctx: LiteralExpressionContext): ExpressionNode {
        return delegate.visitLiteralExpression(ctx)
    }

    override fun visitType(ctx: TypeContext): String {
        return delegate.visitType(ctx)
    }

    override fun visitProgram(ctx: ProgramContext): String {
        return delegate.visitProgram(ctx)
    }
}

