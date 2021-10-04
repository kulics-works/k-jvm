package com.kulics.feel.visitor

import com.kulics.feel.node.*

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
        val implements = implementMap[subtype]
        if (implements != null) {
            implements.add(type)
        } else {
            implementMap[subtype] = mutableSetOf(type)
        }
    }

    internal fun checkSubtype(subtype: Type, type: Type): Boolean {
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

    internal fun getImplementType(subtype: Type): Set<Type>? {
        return implementMap[subtype]
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

fun DelegateVisitor.codeGenerate(programNode: ProgramNode): String {
    val records = mutableMapOf<Type, RecordDeclaration>()
    val code = StringBuilder().append("${programNode.preloadCode}$Wrap ${
        joinString(programNode.declarations, Wrap) {
            when (it) {
                is GlobalVariableDeclarationNode -> it.generateCode()
                is GlobalFunctionDeclarationNode -> it.generateCode()
                is GlobalInterfaceDeclarationNode -> it.generateCode()
                is GlobalRecordDeclarationNode -> if (it is GlobalGenericsRecordDeclarationNode) {
                    ""
                } else {
                    records[it.type] = RecordDeclaration(
                        it.type, it.fields, it.methods.toMutableList(), if (it.implements != null) {
                            mutableListOf(it.implements)
                        } else mutableListOf()
                    )
                    ""
                }
                is GlobalExtensionDeclarationNode -> if (it is GlobalGenericsExtensionDeclarationNode) {
                    ""
                } else {
                    val record = records[it.type]
                    if (record != null) {
                        record.methods.addAll(it.methods)
                        if (it.implements != null) {
                            record.implements.add(it.implements)
                        }
                    }
                    ""
                }
                else -> throw CompilingCheckException()
            }
        }
    }")
    for ((_, v) in records) {
        code.append(v.generateCode())
    }
    return code.toString()
}

class RecordDeclaration(
    val type: Type,
    val fields: List<Identifier>,
    val methods: MutableList<MethodNode>,
    val implements: MutableList<Type>
) {
    fun generateCode(): String {
        return "class ${type.name}(${
            joinString(fields) {
                "${it.name}: ${it.type.generateTypeName()}"
            }
        }) ${
            if (implements.isEmpty()) "" else ": ${
                joinString(implements, Wrap) {
                    it.generateTypeName()
                }
            }"
        } { $Wrap${
            joinString(methods, Wrap) {
                it.generateCode()
            }
        }$Wrap }$Wrap"
    }
}