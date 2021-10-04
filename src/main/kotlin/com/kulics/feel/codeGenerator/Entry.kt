package com.kulics.feel.codeGenerator

import com.kulics.feel.node.*
import com.kulics.feel.visitor.*
import com.kulics.feel.visitor.joinString

interface CodeGenerator : NodeVisitor {
    fun generateCode(): String
}

enum class BackendKind {
    Kotlin
}

fun codeGenerate(programNode: ProgramNode, backendKind: BackendKind = BackendKind.Kotlin): String {
    val visitor: CodeGenerator = when (backendKind) {
        BackendKind.Kotlin -> KotlinCodeGenerator()
    }
    visitor.visit(programNode)
    return visitor.generateCode()
}

class RecordDeclaration(
    val type: Type,
    val fields: List<Identifier>,
    val methods: MutableList<MethodNode>,
    val implements: MutableList<Type>
) {
    fun generateCode(): String {
        return "class ${type.name}(${
            joinString(fields) {
                "${it.name}: ${it.type.generateTypeName()}"
            }
        }) ${
            if (implements.isEmpty()) "" else ": ${
                joinString(implements) {
                    it.generateTypeName()
                }
            }"
        } { $Wrap${
            joinString(methods, Wrap) {
                it.generateCode()
            }
        }$Wrap }$Wrap"
    }
}