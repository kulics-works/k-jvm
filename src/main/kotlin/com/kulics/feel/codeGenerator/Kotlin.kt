package com.kulics.feel.codeGenerator

import com.kulics.feel.node.*
import com.kulics.feel.visitor.*

class KotlinCodeGenerator : CodeGenerator {
    private val codeBuilder = StringBuilder()
    private val records = mutableMapOf<Type, RecordDeclaration>()

    private fun append(code: String) {
        codeBuilder.append(code)
    }

    override fun generateCode(): String {
        records.forEach {
            append(it.value.generateCode())
        }
        return codeBuilder.toString()
    }

    override fun visit(node: ProgramNode) {
        visit(node.module)
        append(
            """
        inline fun <reified T> Any.castOrThrow(): T = this as T
        inline fun <reified T> Any.castOrNull(): T? = this as? T
        inline fun<reified T> newArray(size: Int, initValue: T): Array<T> = Array(size) { initValue };
        inline fun<reified T> emptyArray(): Array<T> = arrayOf();$Wrap
    """.trimIndent()
        )
        node.declarations.forEach {
            when (it) {
                is GlobalVariableDeclarationNode -> visit(it)
                is GlobalFunctionDeclarationNode -> visit(it)
                is GlobalInterfaceDeclarationNode -> visit(it)
                is GlobalGenericsRecordDeclarationNode -> visit(it)
                is GlobalRecordDeclarationNode -> visit(it)
                is GlobalExtensionDeclarationNode -> visit(it)
                else -> throw CompilingCheckException()
            }
        }
    }

    override fun visit(node: ModuleDeclarationNode) {
        append("package ${node.name}$Wrap")
    }

    override fun visit(node: GlobalRecordDeclarationNode) {
        records[node.type] = RecordDeclaration(
            node.type, node.fields, node.methods.toMutableList(), if (node.implements != null) {
                mutableListOf(node.implements)
            } else mutableListOf()
        )
    }

    override fun visit(node: GlobalGenericsRecordDeclarationNode) {
        TODO("Not yet implemented")
    }

    override fun visit(node: GlobalFunctionDeclarationNode) {
        if (node.typeParameter.isEmpty()) {
            append("fun ${node.id.name}(")
        } else {
            append("fun <${
                joinString(node.typeParameter) {
                    "${it.name}: ${
                        when (val constraintType = it.constraint) {
                            is GenericsType -> constraintType.typeConstructor(listOf(it)).generateTypeName()
                            is InterfaceType -> constraintType.generateTypeName()
                        }
                    }"
                }
            }> ${node.id.name}(")
        }
        for ((i, v) in node.parameterTypes.withIndex()) {
            if (i == 0) {
                visit(v)
            } else {
                append(",")
                visit(v)
            }
        }
        append("): ${node.returnType.generateTypeName()} {${Wrap}return (${node.body.generateCode()});$Wrap}$Wrap")
    }

    override fun visit(node: ParameterDeclarationNode) {
        append("${node.id.name}: ${node.paramType.generateTypeName()}")
    }

    override fun visit(node: GlobalVariableDeclarationNode) {
        append(
            if (node.id.kind == IdentifierKind.Immutable) {
                "val ${node.id.name}: ${node.id.type.generateTypeName()} = ${node.initValue.generateCode()}$Wrap"
            } else {
                "var ${node.id.name}: ${node.id.type.generateTypeName()} = ${node.initValue.generateCode()}$Wrap"
            }
        )
    }

    override fun visit(node: GlobalInterfaceDeclarationNode) {
        if (node.typeParameter.isEmpty()) {
            append("interface ${node.type.generateTypeName()} {${
                joinString(node.methods, Wrap) {
                    it.generateCode()
                }
            }}$Wrap")
        } else {
            append("interface ${node.type.name}<${
                joinString(node.typeParameter) {
                    when (val constraintType = it.constraint) {
                        is GenericsType -> {
                            val ty = constraintType.typeConstructor(listOf(it))
                            "${it.name}: ${ty.generateTypeName()}"
                        }
                        is InterfaceType -> "${it.name}: ${constraintType.generateTypeName()}"
                    }
                }
            }> {${
                joinString(node.methods, Wrap) {
                    it.generateCode()
                }
            }}$Wrap")
        }
    }

    override fun visit(node: GlobalExtensionDeclarationNode) {
        val record = records[node.type]
        if (record != null) {
            record.methods.addAll(node.methods)
            if (node.implements != null) {
                record.implements.add(node.implements)
            }
        }
    }
}