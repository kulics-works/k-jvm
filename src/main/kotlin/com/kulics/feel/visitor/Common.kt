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
        return data.size == 0
    }
}

sealed class Either<out L, out R> {
    data class Left<L>(val value: L): Either<L, Nothing>()
    data class Right<R>(val value: R): Either<Nothing, R>()
}


val builtinIdentifierNewArray = run {
    val typeParameter = TypeParameter("T", builtinTypeAny)
    val funcType = GenericsType("NewArray", listOf(typeParameter)) { li ->
        val typeMap = mutableMapOf<String, Type>()
        for (i in li.indices) {
            typeMap[typeParameter.name] = li[i]
        }
        typeSubstitution(FunctionType(listOf(builtinTypeInt, li[0]), builtinTypeArray.typeConstructor(li)), typeMap)
    }
    Identifier("newArray", funcType)
}
val builtinIdentifierEmptyArray = run {
    val typeParameter = TypeParameter("T", builtinTypeAny)
    val funcType = GenericsType("EmptyArray", listOf(typeParameter)) { li ->
        val typeMap = mutableMapOf<String, Type>()
        for (i in li.indices) {
            typeMap[typeParameter.name] = li[i]
        }
        typeSubstitution(FunctionType(listOf(), builtinTypeArray.typeConstructor(li)), typeMap)
    }
    Identifier("emptyArray", funcType)
}