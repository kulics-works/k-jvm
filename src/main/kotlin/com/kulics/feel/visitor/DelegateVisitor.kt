package com.kulics.feel.visitor

class DelegateVisitor {
    private val implementMap = mutableMapOf<String, MutableList<Type>>()

    private val scopes = ArrayStack<Scope>().apply {
        push(Scope().apply {
            addType(builtinTypeAny)
            addType(builtinTypeVoid)
            addType(builtinTypeInt)
            addType(builtinTypeFloat)
            addType(builtinTypeBool)
            addType(builtinTypeChar)
            addType(builtinTypeString)
            addType(builtinTypeArray)
            addIdentifier(builtinIdentifierTrue)
            addIdentifier(builtinIdentifierFalse)
            addIdentifier(builtinIdentifierNewArray)
            addIdentifier(builtinIdentifierEmptyArray)
            addImplementType(builtinTypeVoid, builtinTypeAny)
            addImplementType(builtinTypeInt, builtinTypeAny)
            addImplementType(builtinTypeFloat, builtinTypeAny)
            addImplementType(builtinTypeBool, builtinTypeAny)
            addImplementType(builtinTypeChar, builtinTypeAny)
            addImplementType(builtinTypeString, builtinTypeAny)
            addImplementType(builtinTypeArray, builtinTypeAny)
        })
    }

    internal fun hasIdentifier(id: String): Boolean {
        return scopes.any { it.hasIdentifier(id) }
    }

    internal fun isRedefineIdentifier(id: String): Boolean {
        return scopes.peek().hasIdentifier(id)
    }

    internal fun addIdentifier(id: Identifier) {
        scopes.peek().addIdentifier(id)
    }

    internal fun getIdentifier(id: String): Identifier? {
        return scopes.firstNotNullOfOrNull { it.getIdentifier(id) }
    }

    internal fun hasType(ty: String): Boolean {
        return scopes.any { it.hasType(ty) }
    }

    internal fun addType(ty: Type) {
        scopes.peek().addType(ty)
    }

    internal fun getType(ty: String): Type? {
        return scopes.firstNotNullOfOrNull { it.getType(ty) }
    }

    internal fun isRedefineType(ty: String): Boolean {
        return scopes.peek().hasType(ty)
    }

    internal fun addImplementType(subtype: Type, type: Type) {
        val implements = implementMap[subtype.name]
        if (implements != null) {
            implements.add(type)
        } else {
            implementMap[subtype.name] = mutableListOf(type)
        }
    }

    internal fun checkSubtype(subtype: Type, type: Type): Boolean {
        val implements = implementMap[subtype.name]
        if (implements != null) {
            for (v in implements) {
                if (v.name == type.name) {
                    return true
                }
            }
        }
        return false
    }

    internal fun getImplementType(subtype: Type): List<Type>? {
        return implementMap[subtype.name]
    }

    internal fun pushScope() {
        scopes.push(Scope())
    }

    internal fun popScope() {
        scopes.pop()
    }
}

class Scope {
    private val identifiers = HashMap<String, Identifier>()
    private val types = HashMap<String, Type>()

    internal fun addIdentifier(id: Identifier) {
        identifiers[id.name] = id
    }

    internal fun hasIdentifier(id: String): Boolean {
        return identifiers.contains(id)
    }

    internal fun getIdentifier(id: String): Identifier? {
        return identifiers[id]
    }

    internal fun addType(ty: Type) {
        types[ty.name] = ty
    }

    internal fun hasType(ty: String): Boolean {
        return types.contains(ty)
    }

    internal fun getType(ty: String): Type? {
        return types[ty]
    }
}

