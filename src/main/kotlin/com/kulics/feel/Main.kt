package com.kulics.feel

import com.kulics.feel.grammar.FeelLexer
import com.kulics.feel.grammar.FeelParser
import com.kulics.feel.visitor.FeelErrorListener
import com.kulics.feel.visitor.FeelLangVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

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
    val result = vt.generateCode(tree)
    println(result)
}