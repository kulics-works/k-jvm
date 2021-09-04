package com.kulics.feel.visitor

class DelegateVisitor {
    private val scopes = ArrayStack<Scope>().apply {
        push(Scope().apply {
            addType(Type("Int"))
            addType(Type("Float"))
        })
    }

    internal fun hasIdentifier(id: String): Boolean {
        return scopes.find { hasIdentifier(id) }
    }

    internal fun isRedefineIdentifier(id: String): Boolean {
        return scopes.peek().hasIdentifier(id)
    }

    internal fun hasType(ty: Type): Boolean {
        return scopes.find { hasType(ty) }
    }

    internal fun isRedefineType(ty: Type): Boolean {
        return scopes.peek().hasType(ty)
    }

    internal fun addIdentifier(id: String) {
        scopes.peek().addIdentifier(id)
    }

    internal fun pushScope() {
        scopes.push(Scope())
    }

    internal fun popScope() {
        scopes.pop()
    }
}

class Scope {
    private val identifiers = HashSet<String>()
    private val types = HashSet<Type>()

    internal fun addIdentifier(id: String) {
        identifiers.add(id)
    }

    internal fun hasIdentifier(id: String): Boolean {
        return identifiers.contains(id)
    }

    internal fun addType(ty: Type) {
        types.add(ty)
    }

    internal fun hasType(ty: Type): Boolean {
        return types.contains(ty)
    }
}

