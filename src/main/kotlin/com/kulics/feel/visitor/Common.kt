package com.kulics.feel.visitor

import kotlin.collections.ArrayList

const val Wrap = "\r\n"

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

val builtinTypeAny = PrimitiveType("Any", "Any")
val builtinTypeVoid = PrimitiveType("Void", "Unit")
val builtinTypeInt = PrimitiveType("Int", "Int")
val builtinTypeFloat = PrimitiveType("Float", "Double")
val builtinTypeBool = PrimitiveType("Bool", "Boolean")
val builtinLiteralTrue = Identifier("true", builtinTypeBool, IdentifierKind.Immutable)
val builtinLiteralFalse = Identifier("false", builtinTypeBool, IdentifierKind.Immutable)