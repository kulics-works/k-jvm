package com.kulics.feel.codeGenerator

import com.kulics.feel.node.*
import com.kulics.feel.visitor.*
import javassist.*

class JavaByteCodeGenerator : CodeGenerator<Any> {

    private val pool = ClassPool(true)

    // root class
    private val cc = pool.makeClass("example")

    init {
        builtinTypeGenerate("Int", "int")
        builtinTypeGenerate("Float", "double")
        builtinBoolTypeGenerate()
        builtinCharTypeGenerate()
        builtinStringTypeGenerate()
        builtinVoidTypeGenerate()
    }

    private fun builtinTypeGenerate(name: String, targetName: String) {
        val feelType = pool.makeClass("com.feel.${name}")
        feelType.addField(CtField.make("private $targetName value;", feelType))
        val cons = CtConstructor(arrayOf(pool.get(targetName)), feelType)
        cons.setBody("{ $0.value = $1; }");
        feelType.addConstructor(cons)
        feelType.addMethod(
            CtMethod.make(
                "public com.feel.${name} add(com.feel.${name} v) { return new com.feel.${name}($0.value + $1.value); }",
                feelType
            )
        )
        feelType.addMethod(
            CtMethod.make(
                "public com.feel.${name} sub(com.feel.${name} v) { return new com.feel.${name}($0.value - $1.value); }",
                feelType
            )
        )
        feelType.addMethod(
            CtMethod.make(
                "public com.feel.${name} mul(com.feel.${name} v) { return new com.feel.${name}($0.value * $1.value); }",
                feelType
            )
        )
        feelType.addMethod(
            CtMethod.make(
                "public com.feel.${name} div(com.feel.${name} v) { return new com.feel.${name}($0.value / $1.value); }",
                feelType
            )
        )
        feelType.addMethod(
            CtMethod.make(
                "public com.feel.${name} mod(com.feel.${name} v) { return new com.feel.${name}($0.value % $1.value); }",
                feelType
            )
        )
        feelType.writeFile("./src/test/build/example")
    }

    private fun builtinBoolTypeGenerate() {
        val feelType = pool.makeClass("com.feel.Bool")
        feelType.addField(CtField.make("private boolean value;", feelType))
        val cons = CtConstructor(arrayOf(pool.get("boolean")), feelType)
        cons.setBody("{ $0.value = $1; }");
        feelType.addConstructor(cons)
        feelType.writeFile("./src/test/build/example")
    }

    private fun builtinCharTypeGenerate() {
        val feelType = pool.makeClass("com.feel.Char")
        feelType.addField(CtField.make("private char value;", feelType))
        val cons = CtConstructor(arrayOf(pool.get("char")), feelType)
        cons.setBody("{ $0.value = $1; }");
        feelType.addConstructor(cons)
        feelType.writeFile("./src/test/build/example")
    }

    private fun builtinStringTypeGenerate() {
        val feelType = pool.makeClass("com.feel.String")
        feelType.addField(CtField.make("private java.lang.String value;", feelType))
        val cons = CtConstructor(arrayOf(pool.get("java.lang.String")), feelType)
        cons.setBody("{ $0.value = $1; }");
        feelType.addConstructor(cons)
        feelType.writeFile("./src/test/build/example")
    }

    private fun builtinVoidTypeGenerate() {
        val feelType = pool.makeClass("com.feel.Void")
        val cons = CtConstructor(arrayOf(), feelType)
        cons.setBody("{}");
        feelType.addConstructor(cons)
        feelType.writeFile("./src/test/build/example")
    }

    override fun generateCode(filePath: String) {
        cc.writeFile(filePath)
    }

    override fun visit(node: ProgramNode) {
        visit(node.module)
        node.declarations.forEach {
            when (it) {
                is GlobalVariableDeclarationNode -> visit(it)
                is GlobalFunctionDeclarationNode -> visit(it)
                is GlobalInterfaceDeclarationNode -> visit(it)
                is GlobalRecordDeclarationNode -> visit(it)
                is GlobalExtensionDeclarationNode -> visit(it)
            }
        }
    }

    override fun visit(node: ModuleDeclarationNode) {
    }

    override fun visit(node: GlobalRecordDeclarationNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: GlobalFunctionDeclarationNode) {
        val method = CtMethod.make(
            "public static ${
                node.returnType.generateName()
            } ${node.id.name}(${joinString(node.parameterTypes) { visit(it) }}) { return ${visit(node.body)};}", cc
        )
        cc.addMethod(method)
    }

    override fun visit(node: ParameterDeclarationNode): String {
        return "${node.paramType.generateName()} ${node.id.name}"
    }

    override fun visit(node: GlobalVariableDeclarationNode) {
        val initValue = visit(node.initValue)
        val field = CtField.make(
            "public static ${
                if (node.id.kind == IdentifierKind.Immutable) "final" else ""
            } ${node.id.type.generateName()} ${node.id.name} = ${initValue};",
            cc
        )
        cc.addField(field)
    }

    override fun visit(node: GlobalInterfaceDeclarationNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: GlobalExtensionDeclarationNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: StatementNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: VariableStatementNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: ExpressionStatementNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: AssignmentStatementNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: FunctionStatementNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: IfStatementNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: WhileStatementNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: ExpressionNode): Any {
        return when (node) {
            is IdentifierExpressionNode -> visit(node)
            is LiteralExpressionNode -> visit(node)
            is CalculativeExpressionNode -> visit(node)
            is CompareExpressionNode -> visit(node)
            is LogicExpressionNode -> visit(node)
            is BlockExpressionNode -> visit(node)
            is LambdaExpressionNode -> visit(node)
            is CallExpressionNode -> visit(node)
            is GenericsCallExpressionNode -> visit(node)
            is MemberExpressionNode -> visit(node)
            is IfExpressionNode -> visit(node)
            is IfPatternExpressionNode -> visit(node)
            is CastExpressionNode -> visit(node)
        }
    }

    override fun visit(node: IdentifierExpressionNode): Any {
        return node.id.name
    }

    override fun visit(node: LiteralExpressionNode): Any {
        return when (node.type) {
            builtinTypeInt -> "(new com.feel.Int(${node.text}))"
            builtinTypeFloat -> "(new com.feel.Float(${node.text}))"
            builtinTypeChar -> "(new com.feel.Char(${node.text}))"
            builtinTypeString -> "(new com.feel.String(${node.text}))"
            builtinTypeBool -> "(new com.feel.Bool(${node.text}))"
            else -> ""
        }
    }

    override fun visit(node: CalculativeExpressionNode): Any {
        return when (node.operator) {
            CalculativeOperator.Add -> "${visit(node.lhs)}.add(${visit(node.rhs)})"
            CalculativeOperator.Sub -> "${visit(node.lhs)}.sub(${visit(node.rhs)})"
            CalculativeOperator.Mul -> "${visit(node.lhs)}.mul(${visit(node.rhs)})"
            CalculativeOperator.Div -> "${visit(node.lhs)}.div(${visit(node.rhs)})"
            CalculativeOperator.Mod -> "${visit(node.lhs)}.mod(${visit(node.rhs)})"
        }
    }

    override fun visit(node: CompareExpressionNode): Any {
        return when (node.operator) {
            CompareOperator.Equal -> "(${visit(node.lhs)} == ${visit(node.rhs)})"
            CompareOperator.NotEqual -> "(${visit(node.lhs)} != ${visit(node.rhs)})"
            CompareOperator.Less -> "(${visit(node.lhs)} < ${visit(node.rhs)})"
            CompareOperator.LessEqual -> "(${visit(node.lhs)} <= ${visit(node.rhs)})"
            CompareOperator.Greater -> "(${visit(node.lhs)} > ${visit(node.rhs)})"
            CompareOperator.GreaterEqual -> "(${visit(node.lhs)} >= ${visit(node.rhs)})"
        }
    }

    override fun visit(node: LogicExpressionNode): Any {
        return when (node.operator) {
            LogicOperator.And -> "(${visit(node.lhs)} && ${visit(node.rhs)})"
            LogicOperator.Or -> "(${visit(node.lhs)} || ${visit(node.rhs)})"
        }
    }

    override fun visit(node: BlockExpressionNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: LambdaExpressionNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: CallExpressionNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: GenericsCallExpressionNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: MemberExpressionNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: IfExpressionNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: IfPatternExpressionNode): Any {
        TODO("Not yet implemented")
    }

    override fun visit(node: CastExpressionNode): Any {
        TODO("Not yet implemented")
    }

    private fun Type.generateName(): String {
        return when (this) {
            is FunctionType -> "(${joinString(parameterTypes) { it.generateName() }})->${returnType.generateName()}"
            is RecordType -> when (this) {
                builtinTypeVoid -> "com.feel.Void"
                builtinTypeInt -> "com.feel.Int"
                builtinTypeFloat -> "com.feel.Float"
                builtinTypeBool -> "com.feel.Bool"
                builtinTypeChar -> "com.feel.Char"
                builtinTypeString -> "com.feel.String"
                else -> name
            }
            is InterfaceType -> name
            else -> name
        }
    }
}