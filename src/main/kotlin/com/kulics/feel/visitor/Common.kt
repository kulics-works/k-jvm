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

class ArrayStack<T> : Collection<T> {
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

    override val size: Int get() = data.size

    override fun iterator(): ArrayStackIterator<T> {
        return ArrayStackIterator(data)
    }

    class ArrayStackIterator<T>(private val data: ArrayList<T>) : Iterator<T> {
        var index = data.size
        override fun hasNext(): Boolean {
            return index > 0
        }

        override fun next(): T {
            index -= 1
            return data[index]
        }
    }

    override fun contains(element: T): Boolean {
        return data.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return data.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }
}

val builtinTypeInt = PrimitiveType("Int", "Int")
val builtinTypeFloat = PrimitiveType("Float", "Double")
val builtinTypeBool = PrimitiveType("Bool", "Boolean")
val builtinLiteralTrue = Identifier("true", builtinTypeBool, IdentifierKind.Immutable)
val builtinLiteralFalse = Identifier("false", builtinTypeBool, IdentifierKind.Immutable)