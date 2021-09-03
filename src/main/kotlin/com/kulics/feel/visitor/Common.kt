package com.kulics.feel.visitor

class Result {
    var data: any = ""
    var text = ""
    var permission = ""
    var isVirtual = false
    var isDefine = false
    var rootID = ""
}

class Namespace {
    var name = ""
    var imports = ""
}

class TemplateItem {
    var Template = ""
    var Contract = ""
}

class Parameter {
    var id = ""
    var type = ""
    var value = ""
    var annotation = ""
    var permission = ""
}

class DicEle {
    var key = ""
    var value = ""
    var text = ""
}

class Iterator {
    var begin = Result()
    var end = Result()
    var step = Result()
    var order = T
    var close = T
}

fun GetControlSub(id: str) =
    when (id) {
        "get" -> Pair(" get ", "get")
        "set" -> Pair(" set ", "set")
        "_get" -> Pair(" protected get ", "get")
        "_set" -> Pair(" protected set ", "set")
        else -> Pair("", "")
    }

fun <T> list<T>.peek(): T {
    return this[this.size - 1]
}

fun <T> list<T>.push(new: T) {
    this.add(new)
}

fun <T> list<T>.pop() {
    this.removeAt(this.size - 1)
}

fun <T> Any.to() = this as T

const val Wrap = "\r\n"

const val Any = "Any"
const val Int = "Int"
const val Num = "Double"
const val I8 = "Byte"
const val I16 = "Short"
const val I32 = "Int"
const val I64 = "Long"

const val U8 = "byte"
const val U16 = "ushort"
const val U32 = "uint"
const val U64 = "ulong"

const val F32 = "Float"
const val F64 = "Double"

const val Bool = "Boolean"
const val T = "true"
const val F = "false"

const val Chr = "Char"
const val Str = "String"
const val Arr = "Array"
const val Lst = "MutableList"
const val Set = "MutableSet"
const val Dic = "MutableMap"
const val Stk = "Stack"

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

typealias any = Any
typealias str = String
typealias int = Int
typealias bool = Boolean
typealias list<T> = MutableList<T>
typealias map<K, V> = MutableMap<K, V>
typealias set<T> = MutableSet<T>