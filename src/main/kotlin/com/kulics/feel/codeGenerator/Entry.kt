package com.kulics.feel.codeGenerator

import com.kulics.feel.node.*

interface CodeGenerator<T> : NodeVisitor<T> {
    fun generateCode(): String
}

enum class BackendKind {
    Kotlin
}

fun codeGenerate(programNode: ProgramNode, backendKind: BackendKind = BackendKind.Kotlin): String {
    val visitor: CodeGenerator<String> = when (backendKind) {
        BackendKind.Kotlin -> KotlinCodeGenerator()
    }
    visitor.visit(programNode)
    return visitor.generateCode()
}
