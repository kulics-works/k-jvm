package com.kulics.feel.visitor

class DelegateVisitor {
    private val implementMap = mutableMapOf<Type, MutableSet<Type>>()

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

    fun hasIdentifier(id: String): Boolean {
        return scopes.any { it.hasIdentifier(id) }
    }

    fun isRedefineIdentifier(id: String): Boolean {
        return scopes.peek().hasIdentifier(id)
    }

    fun addIdentifier(id: Identifier) {
        scopes.peek().addIdentifier(id)
    }

    fun getIdentifier(id: String): Identifier? {
        return scopes.firstNotNullOfOrNull { it.getIdentifier(id) }
    }

    fun hasType(ty: String): Boolean {
        return scopes.any { it.hasType(ty) }
    }

    fun addType(ty: Type) {
        scopes.peek().addType(ty)
    }

    fun getType(ty: String): Type? {
        return scopes.firstNotNullOfOrNull { it.getType(ty) }
    }

    fun isRedefineType(ty: String): Boolean {
        return scopes.peek().hasType(ty)
    }

    fun addImplementType(subtype: Type, type: Type) {
        val implements = implementMap[subtype]
        if (implements != null) {
            implements.add(type)
        } else {
            implementMap[subtype] = mutableSetOf(type)
        }
    }

    fun checkSubtype(subtype: Type, type: Type): Boolean {
        val implements = implementMap[subtype]
        if (implements != null) {
            for (v in implements) {
                if (v.uniqueName == type.uniqueName) {
                    return true
                }
            }
        }
        return false
    }

    fun getImplementType(subtype: Type): Set<Type>? {
        return implementMap[subtype]
    }

    fun pushScope() {
        scopes.push(Scope())
    }

    fun popScope() {
        scopes.pop()
    }
}

class Scope {
    private val identifiers = HashMap<String, Identifier>()
    private val types = HashMap<String, Type>()

    fun addIdentifier(id: Identifier) {
        identifiers[id.name] = id
    }

    fun hasIdentifier(id: String): Boolean {
        return identifiers.contains(id)
    }

    fun getIdentifier(id: String): Identifier? {
        return identifiers[id]
    }

    fun addType(ty: Type) {
        types[ty.name] = ty
    }

    fun hasType(ty: String): Boolean {
        return types.contains(ty)
    }

    fun getType(ty: String): Type? {
        return types[ty]
    }
}
