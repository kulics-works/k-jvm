package com.kulics.feel

import com.kulics.feel.codeGenerator.codeGenerate
import com.kulics.feel.grammar.FeelLexer
import com.kulics.feel.grammar.FeelParser
import com.kulics.feel.visitor.FeelErrorListener
import com.kulics.feel.visitor.FeelLangVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

fun main(arg: Array<String>) {
    val input = CharStreams.fromFileName("./src/test/example.feel")
    val lexer = FeelLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = FeelParser(tokens)
    parser.buildParseTree = true
    parser.removeErrorListeners()
    parser.addErrorListener(FeelErrorListener())
    val tree = parser.program() // parse
    val vt = FeelLangVisitor()
    val result = codeGenerate(vt.visitProgram(tree))
    val output = File("./src/test/kotlin/example.kt")
    output.bufferedWriter().use {
        it.write(result)
    }

    println("feel compile completed")
}