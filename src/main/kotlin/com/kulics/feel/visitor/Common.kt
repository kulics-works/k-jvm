package com.kulics.feel.visitor

import kotlin.collections.ArrayList

const val Wrap = "\r\n"

const val BlockLeft = "{"
const val BlockRight = "}"

val keywords = arrayOf(
    "abstract", "as", "base", "bool", "break", "byte", "case", "catch",
    "char", "checked", "class", "const", "continue", "decimal", "default", "delegate", "do", "double", "_",
    "enum", "event", "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach", "goto",
    "?", "implicit", "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null",
    "object", "operator", "out", "override", "params", "private", "protected", "public", "readonly", "ref",
    "return", "sbyte", "sealed", "short", "sizeof", "stackalloc", "static", "string", "struct", "switch",
    "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using",
    "virtual", "void", "volatile", "while"
)

class CompilingCheckException : Exception()

class ArrayStack<T> {
    private var data = ArrayList<T>()

    fun peek(): T {
        return data[data.lastIndex]
    }

    fun push(element: T) {
        data.add(element)
    }

    fun pop() {
        data.removeAt(data.lastIndex)
    }

    fun find(predicate: T.() -> Boolean): Boolean {
        for (i in data.lastIndex downTo 0) {
            if (predicate(data[i])) {
                return true
            }
        }
        return false
    }
}

val builtinTypeInt = Type("Int", "Int")
val builtinTypeFloat = Type("Float", "Double")