package com.kulics.feel.visitor

class DelegateVisitor {
    private val scopes = ArrayStack<Scope>().apply {
        push(Scope().apply {
            addType(builtinTypeInt)
            addType(builtinTypeFloat)
        })
    }

    internal fun hasIdentifier(id: String): Boolean {
        return scopes.find { hasIdentifier(id) }
    }

    internal fun isRedefineIdentifier(id: String): Boolean {
        return scopes.peek().hasIdentifier(id)
    }

    internal fun addIdentifier(id: String) {
        scopes.peek().addIdentifier(id)
    }

    internal fun hasType(ty: String): Boolean {
        return scopes.find { hasType(ty) }
    }

    internal fun getType(ty: String): Type? {
        return scopes.peek().getType(ty)
    }

    internal fun isRedefineType(ty: String): Boolean {
        return scopes.peek().hasType(ty)
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
    private val types = HashMap<String, Type>()

    internal fun addIdentifier(id: String) {
        identifiers.add(id)
    }

    internal fun hasIdentifier(id: String): Boolean {
        return identifiers.contains(id)
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

