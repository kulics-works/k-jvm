package com.kulics.feel.visitor

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class FeelErrorListener : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        println("------Syntax Error------")
        // println("File: ${me.FileDir}")
        println("Line: $line  Column: $charPositionInLine")
        println("OffendingSymbol: $offendingSymbol")
        println("Message: $msg")
    }
}