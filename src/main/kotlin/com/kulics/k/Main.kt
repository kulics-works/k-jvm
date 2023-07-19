package com.kulics.k

import com.kulics.k.codeGenerator.BackendKind
import com.kulics.k.codeGenerator.codeGenerate
import com.kulics.k.grammar.KLexer
import com.kulics.k.grammar.KParser
import com.kulics.k.visitor.KErrorListener
import com.kulics.k.visitor.KLangVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.file.FileSystems
import java.nio.file.Paths

fun main(arg: Array<String>) {
    val localPath = FileSystems.getDefault().getPath("").toAbsolutePath().toString()
    val path = Paths.get(localPath, "src", "test", "example.k")
    val input = CharStreams.fromFileName(path.toString())
    val lexer = KLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = KParser(tokens)
    parser.buildParseTree = true
    parser.removeErrorListeners()
    parser.addErrorListener(KErrorListener())
    val tree = parser.program() // parse
    val vt = KLangVisitor()
    val backendKind = if (arg.isNotEmpty() && arg[0] == "jvm") {
        BackendKind.JavaByteCode
    } else {
        BackendKind.Kotlin
    }
    codeGenerate(vt.visitProgram(tree), Paths.get(localPath, "src", "test", "kotlin", "example").toString(), backendKind)
    println("k compile completed")
}