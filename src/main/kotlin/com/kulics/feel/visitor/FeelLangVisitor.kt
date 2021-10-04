package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.grammar.FeelParserBaseVisitor
import com.kulics.feel.node.*

class FeelLangVisitor : FeelParserBaseVisitor<Any>() {
    private val delegate = DelegateVisitor()

    fun generateCode(ctx: ProgramContext): String {
        return delegate.codeGenerate(visitProgram(ctx))
    }

    override fun visitIdentifier(ctx: IdentifierContext): String {
        return visitIdentifier(ctx)
    }

    override fun visitModuleDeclaration(ctx: ModuleDeclarationContext): String {
        return delegate.visitModuleDeclaration(ctx)
    }

    override fun visitGlobalVariableDeclaration(ctx: GlobalVariableDeclarationContext): GlobalVariableDeclarationNode {
        return delegate.visitGlobalVariableDeclaration(ctx)
    }

    override fun visitGlobalFunctionDeclaration(ctx: GlobalFunctionDeclarationContext): GlobalFunctionDeclarationNode {
        return delegate.visitGlobalFunctionDeclaration(ctx)
    }

    override fun visitGlobalExtensionDeclaration(ctx: GlobalExtensionDeclarationContext): GlobalExtensionDeclarationNode {
        return delegate.visitGlobalExtensionDeclaration(ctx)
    }

    override fun visitParameterList(ctx: ParameterListContext): Pair<ArrayList<Identifier>, String> {
        return delegate.visitParameterList(ctx)
    }

    override fun visitParameter(ctx: ParameterContext): Identifier {
        return delegate.visitParameter(ctx)
    }

    override fun visitTypeParameterList(ctx: TypeParameterListContext): Any {
        return delegate.visitTypeParameterList(ctx)
    }

    override fun visitTypeParameter(ctx: TypeParameterContext): TypeParameter {
        return delegate.visitTypeParameter(ctx)
    }

    override fun visitGlobalRecordDeclaration(ctx: GlobalRecordDeclarationContext): GlobalRecordDeclarationNode {
        return delegate.visitGlobalRecordDeclaration(ctx)
    }

    override fun visitFieldList(ctx: FieldListContext): Pair<ArrayList<Identifier>, String> {
        return delegate.visitFieldList(ctx)
    }

    override fun visitField(ctx: FieldContext): Identifier {
        return delegate.visitField(ctx)
    }

    override fun visitMethodList(ctx: MethodListContext): List<MethodNode> {
        return delegate.visitMethodList(ctx)
    }

    override fun visitMethod(ctx: MethodContext): MethodNode {
        return delegate.visitMethod(ctx)
    }

    override fun visitGlobalInterfaceDeclaration(ctx: GlobalInterfaceDeclarationContext): GlobalInterfaceDeclarationNode {
        return delegate.visitGlobalInterfaceDeclaration(ctx)
    }

    override fun visitVirtualMethodList(ctx: VirtualMethodListContext): List<VirtualMethodNode> {
        return delegate.visitVirtualMethodList(ctx)
    }

    override fun visitVirtualMethod(ctx: VirtualMethodContext): VirtualMethodNode {
        return delegate.visitVirtualMethod(ctx)
    }

    override fun visitBlock(ctx: BlockContext): String {
        return delegate.visitBlock(ctx)
    }

    override fun visitBlockExpression(ctx: BlockExpressionContext): BlockExpressionNode {
        return delegate.visitBlockExpression(ctx)
    }

    override fun visitGlobalDeclaration(ctx: GlobalDeclarationContext): DeclarationNode {
        return delegate.visitGlobalDeclaration(ctx)
    }

    override fun visitStatement(ctx: StatementContext): String {
        return delegate.visitStatement(ctx)
    }

    override fun visitVariableDeclaration(ctx: VariableDeclarationContext): String {
        return delegate.visitVariableDeclaration(ctx)
    }

    override fun visitFunctionDeclaration(ctx: FunctionDeclarationContext): String {
        return delegate.visitFunctionDeclaration(ctx)
    }

    override fun visitAssignment(ctx: AssignmentContext): String {
        return delegate.visitAssignment(ctx)
    }

    override fun visitIfStatement(ctx: IfStatementContext): String {
        return delegate.visitIfStatement(ctx)
    }

    override fun visitIfExpression(ctx: IfExpressionContext): ExpressionNode {
        return delegate.visitIfExpression(ctx)
    }

    override fun visitWhileStatement(ctx: WhileStatementContext): String {
        return delegate.visitWhileStatement(ctx)
    }

    override fun visitExpression(ctx: ExpressionContext): ExpressionNode {
        return delegate.visitExpression(ctx)
    }

    override fun visitMemberAccessCallSuffix(ctx: MemberAccessCallSuffixContext): MemberAccessCallSuffix {
        return delegate.visitMemberAccessCallSuffix(ctx)
    }

    override fun visitCallSuffix(ctx: CallSuffixContext): Pair<List<Type>, List<ExpressionNode>> {
        return delegate.visitCallSuffix(ctx)
    }

    override fun visitMemberAccess(ctx: MemberAccessContext): String {
        return delegate.visitMemberAccess(ctx)
    }

    override fun visitPrimaryExpression(ctx: PrimaryExpressionContext): ExpressionNode {
        return delegate.visitPrimaryExpression(ctx)
    }

    override fun visitParenExpression(ctx: ParenExpressionContext): ExpressionNode {
        return delegate.visitParenExpression(ctx)
    }

    override fun visitLiteralExpression(ctx: LiteralExpressionContext): ExpressionNode {
        return delegate.visitLiteralExpression(ctx)
    }

    override fun visitIdentifierPattern(ctx: IdentifierPatternContext): String {
        return delegate.visitIdentifierPattern(ctx)
    }

    override fun visitType(ctx: TypeContext): TypeNode {
        return delegate.visitType(ctx)
    }

    override fun visitProgram(ctx: ProgramContext): ProgramNode {
        return delegate.visitProgram(ctx)
    }
}

