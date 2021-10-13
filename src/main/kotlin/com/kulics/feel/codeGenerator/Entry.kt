package com.kulics.feel.codeGenerator

import com.kulics.feel.node.*

interface CodeGenerator<T> : NodeVisitor<T> {
    fun generateCode(filePath: String)
}

enum class BackendKind {
    Kotlin,
    JavaByteCode
}

fun codeGenerate(programNode: ProgramNode, filePath: String, backendKind: BackendKind) {
    val visitor: CodeGenerator<*> = when (backendKind) {
        BackendKind.Kotlin -> KotlinCodeGenerator()
        BackendKind.JavaByteCode -> JavaByteCodeGenerator()
    }
    visitor.visit(programNode)
    visitor.generateCode(filePath)
}
