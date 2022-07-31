package com.kulics.feel.codeGenerator

import com.kulics.feel.node.*
import com.kulics.feel.visitor.*
import java.io.File

class KotlinCodeGenerator : CodeGenerator<String> {
    private val codeBuilder = StringBuilder()
    private val records = mutableMapOf<Type, RecordDeclaration>()

    private fun append(code: String) {
        codeBuilder.append(code)
    }

    private class RecordDeclaration(
        val type: Type,
        val typeParameter: List<TypeParameter>,
        val fields: List<Identifier>,
        val methods: MutableList<MethodNode>,
        val implements: MutableList<Type>
    )

    override fun generateCode(filePath: String) {
        records.forEach {
            append(generate(it.value))
        }
        val output = File("${filePath}.kt")
        if (!output.exists()) {
            output.createNewFile()
        }
        output.bufferedWriter().use {
            it.write(codeBuilder.toString())
        }
    }

    override fun visit(node: ProgramNode): String {
        append(visit(node.module))
        append(
            """
        inline fun <reified T> Any.castOrThrow(): T = this as T
        inline fun <reified T> Any.castOrNull(): T? = this as? T
        inline fun <reified T> newArray(size: Int, initValue: T): Array<T> = Array(size) { initValue };
        inline fun <reified T> emptyArray(): Array<T> = arrayOf();$Wrap
    """.trimIndent()
        )
        node.declarations.forEach {
            append(
                when (it) {
                    is GlobalVariableDeclarationNode -> visit(it)
                    is GlobalFunctionDeclarationNode -> visit(it)
                    is GlobalInterfaceDeclarationNode -> visit(it)
                    is GlobalRecordDeclarationNode -> visit(it)
                    is GlobalExtensionDeclarationNode -> visit(it)
                    is GlobalSumTypeDeclarationNode -> visit(it)
                }
            )
        }
        return ""
    }

    override fun visit(node: ModuleDeclarationNode): String {
        return "package ${node.name}$Wrap"
    }

    override fun visit(node: GlobalRecordDeclarationNode): String {
        records[node.type] = RecordDeclaration(
            node.type,
            node.typeParameter,
            node.fields,
            node.methods.toMutableList(),
            if (node.implements != null) mutableListOf(node.implements)
            else mutableListOf()
        )
        return ""
    }

    private fun generate(record: RecordDeclaration): String {
        return with(record) {
            if (typeParameter.isEmpty()) {
                "class ${type.name}(${
                    joinString(fields) {
                        "${if (it.kind == IdentifierKind.Immutable) "val" else "var"} ${it.name}: ${it.type.generateName()}"
                    }
                }) ${
                    if (implements.isEmpty()) "" else ": ${
                        joinString(implements) {
                            it.generateName()
                        }
                    }"
                } { $Wrap${
                    joinString(methods, Wrap) {
                        generate(it)
                    }
                }$Wrap }$Wrap"
            } else {
                "class ${type.name}<${
                    joinString(typeParameter) {
                        "${it.name}: ${
                            when (val constraintType = it.constraint) {
                                is GenericsType -> constraintType.typeConstructor(listOf(it)).generateName()
                                is InterfaceType -> constraintType.generateName()
                            }
                        }"
                    }
                }>(${
                    joinString(fields) {
                        "${if (it.kind == IdentifierKind.Immutable) "val" else "var"} ${it.name}: ${it.type.generateName()}"
                    }
                }) ${
                    if (implements.isEmpty()) "" else ": ${
                        joinString(implements) {
                            when (val interfaceType = it) {
                                is GenericsType -> interfaceType.typeConstructor(listOf(it)).generateName()
                                is InterfaceType -> interfaceType.generateName()
                                else -> throw CompilingCheckException()
                            }
                        }
                    }"
                } {$Wrap${
                    joinString(methods, Wrap) {
                        generate(it)
                    }
                } }$Wrap"
            }
        }
    }

    override fun visit(node: GlobalFunctionDeclarationNode): String {
        return "${
            if (node.typeParameter.isEmpty()) {
                "fun ${node.id.name}("
            } else {
                "fun <${
                    joinString(node.typeParameter) {
                        "${it.name}: ${
                            when (val constraintType = it.constraint) {
                                is GenericsType -> constraintType.typeConstructor(listOf(it)).generateName()
                                is InterfaceType -> constraintType.generateName()
                            }
                        }"
                    }
                }> ${node.id.name}("
            }
        } ${
            joinString(node.parameterTypes) { visit(it) }
        }): ${
            node.returnType.generateName()
        } {${Wrap}return (${
            visit(node.body)
        });$Wrap}$Wrap"
    }

    override fun visit(node: ParameterDeclarationNode): String {
        return "${node.id.name}: ${node.paramType.generateName()}"
    }

    override fun visit(node: GlobalVariableDeclarationNode): String {
        return "${
            if (node.id.kind == IdentifierKind.Immutable) "val" else "var"
        } ${node.id.name}: ${node.id.type.generateName()} = ${
            visit(
                node.initValue
            )
        }$Wrap"
    }

    override fun visit(node: GlobalInterfaceDeclarationNode): String {
        return if (node.typeParameter.isEmpty()) {
            "interface ${node.type.generateName()} {${
                joinString(node.methods, Wrap) {
                    generate(it)
                }
            }}$Wrap"
        } else {
            "interface ${node.type.name}<${
                joinString(node.typeParameter) {
                    when (val constraintType = it.constraint) {
                        is GenericsType -> {
                            val ty = constraintType.typeConstructor(listOf(it))
                            "${it.name}: ${ty.generateName()}"
                        }
                        is InterfaceType -> "${it.name}: ${constraintType.generateName()}"
                    }
                }
            }> {${
                joinString(node.methods, Wrap) {
                    generate(it)
                }
            }}$Wrap"
        }
    }

    override fun visit(node: GlobalExtensionDeclarationNode): String {
        val record = records[node.type]
        if (record != null) {
            record.methods.addAll(node.methods)
            if (node.implements != null) {
                record.implements.add(node.implements)
            }
        }
        return ""
    }

    override fun visit(node: GlobalSumTypeDeclarationNode): String {
        return if (node.typeParameter.isEmpty()) {
            "sealed class ${node.type.generateName()} {}$Wrap ${
                joinString(node.valueConstructor, Wrap) { it ->
                    "class ${it.type.generateName()} (${
                        joinString(it.fields) {field ->
                            "${if (field.kind == IdentifierKind.Immutable) "val" else "var"} ${field.name}: ${field.type.generateName()}"
                        }
                    }): ${it.implements.generateName()}()"
            }}"
        } else {
            TODO()
        }
    }

    private fun generate(node: MethodNode): String {
        return "${if (node.isOverride) "override" else ""} fun ${node.id.name}(${
            joinString(node.params) { "${it.name}: ${it.type.generateName()}" }
        }): ${node.returnType.generateName()} { return run{ ${visit(node.body)} } }"
    }

    private fun generate(node: VirtualMethodNode): String {
        return "fun ${node.id.name}(${
            joinString(node.params) { "${it.name}: ${it.type.generateName()}" }
        }): ${node.returnType.generateName()} ${
            if (node.body != null) {
                "{ return run{ ${visit(node.body)} } }"
            } else {
                ""
            }
        } "
    }

    override fun visit(node: StatementNode): String {
        return when (node) {
            is VariableStatementNode -> visit(node)
            is ExpressionStatementNode -> visit(node)
            is AssignmentStatementNode -> visit(node)
            is FunctionStatementNode -> visit(node)
            is WhileStatementNode -> visit(node)
        }
    }

    override fun visit(node: VariableStatementNode): String {
        return "var ${node.id.name}: ${node.id.type.generateName()} = ${visit(node.initValue)}"
    }

    override fun visit(node: ExpressionStatementNode): String {
        return visit(node.expr)
    }

    override fun visit(node: AssignmentStatementNode): String {
        return "${node.id.name} = ${visit(node.newValue)}"
    }

    override fun visit(node: FunctionStatementNode): String {
        return "fun ${node.id.name}(${
            joinString(node.parameterTypes) {
                "${it.id.name}: ${it.paramType.generateName()}"
            }
        }): ${node.returnType.generateName()} {${Wrap}return (${visit(node.body)});$Wrap}$Wrap"
    }

    private fun generateStatements(branch: List<StatementNode>): String {
        return joinString(branch, ";$Wrap") { visit(it) }
    }

    override fun visit(node: WhileStatementNode): String {
        return "while (${visit(node.cond)}) { ${
            joinString(node.stats, Wrap) {
                visit(it)
            }
        } }"
    }

    override fun visit(node: ExpressionNode): String {
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
            is IfThenElseExpressionNode -> visit(node)
            is IfThenElsePatternExpressionNode -> visit(node)
            is IfDoExpressionNode -> visit(node)
            is IfDoPatternExpressionNode -> visit(node)
            is CastExpressionNode -> visit(node)
        }
    }

    override fun visit(node: IdentifierExpressionNode): String {
        return node.id.name
    }

    override fun visit(node: LiteralExpressionNode): String {
        return node.text
    }

    override fun visit(node: CalculativeExpressionNode): String {
        return when (node.operator) {
            CalculativeOperator.Add -> "(${visit(node.lhs)} + ${visit(node.rhs)})"
            CalculativeOperator.Sub -> "(${visit(node.lhs)} - ${visit(node.rhs)})"
            CalculativeOperator.Mul -> "(${visit(node.lhs)} * ${visit(node.rhs)})"
            CalculativeOperator.Div -> "(${visit(node.lhs)} / ${visit(node.rhs)})"
            CalculativeOperator.Mod -> "(${visit(node.lhs)} % ${visit(node.rhs)})"
        }
    }

    override fun visit(node: CompareExpressionNode): String {
        return when (node.operator) {
            CompareOperator.Equal -> "(${visit(node.lhs)} == ${visit(node.rhs)})"
            CompareOperator.NotEqual -> "(${visit(node.lhs)} != ${visit(node.rhs)})"
            CompareOperator.Less -> "(${visit(node.lhs)} < ${visit(node.rhs)})"
            CompareOperator.LessEqual -> "(${visit(node.lhs)} <= ${visit(node.rhs)})"
            CompareOperator.Greater -> "(${visit(node.lhs)} > ${visit(node.rhs)})"
            CompareOperator.GreaterEqual -> "(${visit(node.lhs)} >= ${visit(node.rhs)})"
        }
    }

    override fun visit(node: LogicExpressionNode): String {
        return when (node.operator) {
            LogicOperator.And -> "(${visit(node.lhs)} && ${visit(node.rhs)})"
            LogicOperator.Or -> "(${visit(node.lhs)} || ${visit(node.rhs)})"
        }
    }

    override fun visit(node: BlockExpressionNode): String {
        return "run{${
            generateStatements(node.stats)
        };${node.expr?.let { visit(it) } ?: "Unit"}}"
    }

    override fun visit(node: LambdaExpressionNode): String {
        return "fun (${
            joinString(node.parameterTypes) { "${it.id.name}: ${it.paramType.generateName()}" }
        }): ${node.returnType.generateName()} {${Wrap}return (${visit(node.body)});$Wrap}$Wrap "
    }

    override fun visit(node: CallExpressionNode): String {
        return "${visit(node.expr)}(${
            joinString(node.args) { visit(it) }
        })"
    }

    override fun visit(node: GenericsCallExpressionNode): String {
        return "${visit(node.expr)}<${
            joinString(node.types) { it.generateName() }
        }> (${
            joinString(node.args) { visit(it) }
        })"
    }

    override fun visit(node: MemberExpressionNode): String {
        return "${visit(node.expr)}.${node.member.name}"
    }

    override fun visit(node: IfThenElseExpressionNode): String {
        return "if (${visit(node.condExpr)}) { ${visit(node.thenExpr)} } else { ${visit(node.elseExpr)} }"
    }

    override fun visit(node: IfThenElsePatternExpressionNode): String {
        return when (node.pattern) {
            is TypePattern -> {
                val matchCode =
                    "val ${node.pattern.identifier.name} = ${visit(node.condExpr)}.castOrNull<${
                        node.pattern.type.generateName()
                    }>();$Wrap"
                "run{${matchCode}if (${
                    node.pattern.identifier.name
                } != null) { ${
                    visit(node.thenExpr)
                } } else { ${
                    visit(node.elseExpr)
                } }}"
            }
            is IdentifierPattern -> {
                "run{val ${node.pattern.identifier} = ${visit(node.condExpr)};$Wrap${visit(node.thenExpr)}}"
            }
            is LiteralPattern -> {
                "if (${visit(node.condExpr)}==${visit(node.pattern.expr)}) { ${
                    visit(node.thenExpr)
                } } else { ${
                    visit(node.elseExpr)
                } }"
            }
            is WildcardPattern -> {
                "run{${visit(node.condExpr)};$Wrap${visit(node.thenExpr)}}"
            }
        }
    }

    override fun visit(node: IfDoExpressionNode): String {
        return "if (${visit(node.condExpr)}) { ${visit(node.doExpr)} } else { }"
    }

    override fun visit(node: IfDoPatternExpressionNode): String {
        return when (node.pattern) {
            is TypePattern -> {
                val matchCode =
                    "val ${node.pattern.identifier.name} = ${visit(node.condExpr)}.castOrNull<${
                        node.pattern.type.generateName()
                    }>();$Wrap"
                "run{${matchCode}if (${
                    node.pattern.identifier.name
                } != null) { ${
                    visit(node.doExpr)
                } } else { }}"
            }
            is IdentifierPattern -> {
                "run{val ${node.pattern.identifier} = ${visit(node.condExpr)};$Wrap${visit(node.doExpr)}}"
            }
            is LiteralPattern -> {
                "if (${visit(node.condExpr)}==${visit(node.pattern.expr)}) { ${
                    visit(node.doExpr)
                } } else { }"
            }
            is WildcardPattern -> {
                "run{${visit(node.condExpr)};$Wrap${visit(node.doExpr)}}"
            }
        }
    }

    override fun visit(node: CastExpressionNode): String {
        return "${visit(node.expr)}.castOrThrow<${node.targetType.generateName()}>()"
    }

    private fun Type.generateName(): String {
        return when (this) {
            is FunctionType -> "(${joinString(parameterTypes) { it.generateName() }})->${returnType.generateName()}"
            is RecordType -> if (rawGenericsType != null) {
                "${rawGenericsType.first}<${joinString(rawGenericsType.second) { it.generateName() }}>"
            } else {
                when (this) {
                    builtinTypeVoid -> "Unit"
                    builtinTypeInt -> "Int"
                    builtinTypeFloat -> "Double"
                    builtinTypeBool -> "Boolean"
                    builtinTypeChar -> "Char"
                    builtinTypeString -> "String"
                    else -> name
                }
            }
            is InterfaceType -> if (rawGenericsType != null) {
                "${rawGenericsType.first}<${joinString(rawGenericsType.second) { it.generateName() }}>"
            } else {
                name
            }
            else -> name
        }
    }
}