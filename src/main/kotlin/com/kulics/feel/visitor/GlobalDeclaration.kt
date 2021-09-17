package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

internal fun DelegateVisitor.visitModuleDeclaration(ctx: ModuleDeclarationContext): String {
    return "package ${visitIdentifier(ctx.identifier())}$Wrap"
}

internal fun DelegateVisitor.visitProgram(ctx: ProgramContext): String {
    val result = StringBuilder()
    result.append(visitModuleDeclaration(ctx.moduleDeclaration()))
    result.append(
        """
        object BuiltinTool {
            inline fun <reified T> cast(obj: Any): T? = obj as? T
        };$Wrap
    """.trimIndent()
    )
    for (item in ctx.globalDeclaration()) {
        result.append(visitGlobalDeclaration(item))
    }
    return result.toString()
}

internal fun DelegateVisitor.visitGlobalDeclaration(ctx: GlobalDeclarationContext): String {
    return when (val declaration = ctx.getChild(0)) {
        is GlobalVariableDeclarationContext -> visitGlobalVariableDeclaration(declaration)
        is GlobalConstantDeclarationContext -> visitGlobalConstantDeclaration(declaration)
        is GlobalFunctionDeclarationContext -> visitGlobalFunctionDeclaration(declaration)
        is GlobalRecordDeclarationContext -> visitGlobalRecordDeclaration(declaration)
        is GlobalEnumDeclarationContext -> visitGlobalEnumDeclaration(declaration)
        else -> throw CompilingCheckException()
    }
}

internal fun DelegateVisitor.visitGlobalVariableDeclaration(ctx: GlobalVariableDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    val type = checkType(visitType(ctx.type()))
    if (expr.type.cannotAssignTo(type)) {
        println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
        throw CompilingCheckException()
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Mutable))
    return "var $id: ${type.generateTypeName()} = (${expr.generateCode()});$Wrap"
}

internal fun DelegateVisitor.visitGlobalConstantDeclaration(ctx: GlobalConstantDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    val type = checkType(visitType(ctx.type()))
    if (expr.type.cannotAssignTo(type)) {
        println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
        throw CompilingCheckException()
    }
    addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
    return "val $id: ${type.generateTypeName()} = (${expr.generateCode()});$Wrap"
}

internal fun DelegateVisitor.visitGlobalFunctionDeclaration(ctx: GlobalFunctionDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        val typeParameter = visitTypeParameterList(typeParameterList)
        pushScope()
        for (v in typeParameter.first) {
            addType(v)
        }
        val returnType = checkType(visitType(ctx.type()))
        val params = visitParameterList(ctx.parameterList())
        val type = GenericsType(id, typeParameter.first) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter.first[i].name] = li[i]
            }
            typeSubstitution(FunctionType(params.first.map { it.type }, returnType), typeMap)
        }
        popScope()
        addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
        pushScope()
        for (v in typeParameter.first) {
            if (isRedefineType(v.name)) {
                println("type: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addType(v)
        }
        for (v in params.first) {
            if (isRedefineIdentifier(v.name)) {
                println("identifier: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addIdentifier(v)
        }
        val expr = visitExpression(ctx.expression())
        if (expr.type.cannotAssignTo(returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        "fun <${typeParameter.second}> ${id}(${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${expr.generateCode()});$Wrap}$Wrap"
    } else {
        val returnType = checkType(visitType(ctx.type()))
        val params = visitParameterList(ctx.parameterList())
        val type = FunctionType(params.first.map { it.type }, returnType)
        addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
        pushScope()
        for (v in params.first) {
            if (isRedefineIdentifier(v.name)) {
                println("identifier: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addIdentifier(v)
        }
        val expr = visitExpression(ctx.expression())
        if (expr.type.cannotAssignTo(returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        "fun ${id}(${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${expr.generateCode()});$Wrap}$Wrap"
    }
}

internal fun DelegateVisitor.visitParameterList(ctx: ParameterListContext): Pair<ArrayList<Identifier>, String> {
    val params = ctx.parameter()
    val buf = StringBuilder()
    val ids = ArrayList<Identifier>()
    if (params.size > 0) {
        val first = visitParameter(params[0])
        fun genParam(id: Identifier): String {
            return "${id.name}: ${id.type.generateTypeName()}"
        }
        buf.append(genParam(first))
        ids.add(first)
        for (i in 1 until params.size) {
            val id = visitParameter(params[i])
            ids.add(id)
            buf.append(", ${genParam(id)}")
        }
    }
    return ids to buf.toString()
}

internal fun DelegateVisitor.visitParameter(ctx: ParameterContext): Identifier {
    val id = visitIdentifier(ctx.identifier())
    val type = checkType(visitType(ctx.type()))
    return Identifier(id, type, IdentifierKind.Immutable)
}

internal fun DelegateVisitor.visitTypeParameterList(ctx: TypeParameterListContext): Pair<ArrayList<TypeParameter>, String> {
    val params = ctx.typeParameter()
    val buf = StringBuilder()
    val ids = ArrayList<TypeParameter>()
    val first = visitTypeParameter(params[0])
    fun genParam(id: TypeParameter): String {
        return "${id.name}: ${id.constraint.generateTypeName()}"
    }
    buf.append(genParam(first))
    ids.add(first)
    for (i in 1 until params.size) {
        val id = visitTypeParameter(params[i])
        ids.add(id)
        buf.append(", ${genParam(id)}")
    }
    return ids to buf.toString()
}

internal fun DelegateVisitor.visitTypeParameter(ctx: TypeParameterContext): TypeParameter {
    val id = visitIdentifier(ctx.identifier())
    val type = checkType(visitType(ctx.type()))
    return TypeParameter(id, type)
}

internal fun DelegateVisitor.visitGlobalRecordDeclaration(ctx: GlobalRecordDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id) || isRedefineType(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        val typeParameter = visitTypeParameterList(typeParameterList)
        pushScope()
        for (v in typeParameter.first) {
            addType(v)
        }
        val fieldList = visitFieldList(ctx.fieldList())
        val members = mutableMapOf<String, Identifier>()
        fieldList.first.forEach { members[it.name] = it }
        popScope()
        val type = GenericsType(id, typeParameter.first) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter.first[i].name] = li[i]
            }
            typeSubstitution(RecordType("${id}[${
                li.foldIndexed("") { index, acc, type ->
                    if (index == 0) type.name
                    else "${acc}, ${type.name}"
                }
            }]", members, "${id}<${
                li.foldIndexed("") { index, acc, type ->
                    if (index == 0) type.generateTypeName()
                    else "${acc}, ${type.generateTypeName()}"
                }
            }>"), typeMap)
        }
        addType(type)
        val constructorType = GenericsType(id, typeParameter.first) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter.first[i].name] = li[i]
            }
            typeSubstitution(FunctionType(fieldList.first.map { it.type }, type.typeConstructor(li)), typeMap)
        }
        addIdentifier(Identifier(id, constructorType, IdentifierKind.Immutable))
        pushScope()
        fieldList.first.forEach { addIdentifier(it) }
        val methodCode = if (ctx.methodList() == null) {
            ""
        } else {
            val methods = visitMethodList(ctx.methodList())
            for (v in methods.first) {
                members[v.name] = v
            }
            " {${methods.second}}"
        }
        popScope()
        "class ${id}<${typeParameter.second}>(${fieldList.second})${methodCode};$Wrap"
    } else {
        val fieldList = visitFieldList(ctx.fieldList())
        val members = mutableMapOf<String, Identifier>()
        fieldList.first.forEach { members[it.name] = it }
        val type = RecordType(id, members, null)
        addType(type)
        val constructorType = FunctionType(fieldList.first.map { it.type }, type)
        addIdentifier(Identifier(id, constructorType, IdentifierKind.Immutable))
        pushScope()
        fieldList.first.forEach { addIdentifier(it) }
        val methodCode = if (ctx.methodList() == null) {
            ""
        } else {
            val methods = visitMethodList(ctx.methodList())
            for (v in methods.first) {
                members[v.name] = v
            }
            " {${methods.second}}"
        }
        popScope()
        "class ${id}(${fieldList.second})${methodCode};$Wrap"
    }
}

internal fun DelegateVisitor.visitFieldList(ctx: FieldListContext): Pair<ArrayList<Identifier>, String> {
    val fields = ctx.field()
    val buf = StringBuilder()
    val ids = ArrayList<Identifier>()
    if (fields.size > 0) {
        val first = visitField(fields[0])
        fun genParam(id: Identifier): String {
            return "${
                if (id.kind == IdentifierKind.Immutable) "val"
                else "var"
            } ${id.name}: ${id.type.generateTypeName()}"
        }
        buf.append(genParam(first))
        ids.add(first)
        for (i in 1 until fields.size) {
            val id = visitField(fields[i])
            ids.add(id)
            buf.append(", ${genParam(id)}")
        }
    }
    return ids to buf.toString()
}

internal fun DelegateVisitor.visitField(ctx: FieldContext): Identifier {
    val id = visitIdentifier(ctx.identifier())
    val type = checkType(visitType(ctx.type()))
    return Identifier(id, type, if (ctx.Mut() == null) IdentifierKind.Immutable else IdentifierKind.Mutable)
}

internal fun DelegateVisitor.visitMethodList(ctx: MethodListContext): Pair<ArrayList<Identifier>, String> {
    val list = arrayListOf<Identifier>()
    val buf = StringBuilder()
    ctx.method().forEach {
        val (id, code) = visitMethod(it)
        list.add(id)
        buf.append(code)
    }
    return Pair(list, buf.toString())
}

internal fun DelegateVisitor.visitMethod(ctx: MethodContext): Pair<Identifier, String> {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val returnType = checkType(visitType(ctx.type()))
    val params = visitParameterList(ctx.parameterList())
    val type = FunctionType(params.first.map { it.type }, returnType)
    val identifier = Identifier(id, type, IdentifierKind.Immutable)
    addIdentifier(identifier)
    pushScope()
    for (v in params.first) {
        if (isRedefineIdentifier(v.name)) {
            println("identifier: '${v.name}' is redefined")
            throw CompilingCheckException()
        }
        addIdentifier(v)
    }
    val expr = visitExpression(ctx.expression())
    if (expr.type != returnType) {
        println("the return is '${returnType.name}', but find '${expr.type.name}'")
        throw CompilingCheckException()
    }
    popScope()
    return identifier to "fun ${id}(${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${expr.generateCode()});$Wrap}$Wrap"
}

internal fun DelegateVisitor.visitGlobalEnumDeclaration(ctx: GlobalEnumDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val members = mutableMapOf<String, Identifier>()
    val permitsTypes = mutableSetOf<Type>()
    val type = InterfaceType(id, members, permitsTypes)
    addType(type)
    val constructors = visitConstructorList(ctx.constructorList())
    val buf = StringBuilder()
    for ((name, info) in constructors) {
        val (fields, code) = info
        val constructorMembers = mutableMapOf<String, Identifier>()
        val constructorInitParamList = mutableListOf<Type>()
        for (v in fields) {
            constructorMembers[v.name] = v
            constructorInitParamList.add(v.type)
        }
        val constructorType = RecordType(name, constructorMembers, null)
        val constructorInitType = FunctionType(constructorInitParamList, constructorType)
        val constructor = Identifier(name, constructorInitType, IdentifierKind.Immutable)
        addType(constructorType)
        addIdentifier(constructor)
        permitsTypes.add(constructorType)
        buf.append("${code}: ${id}();$Wrap")
    }
    pushScope()
    val methodCode = if (ctx.methodList() == null) {
        ""
    } else {
        val methods = visitMethodList(ctx.methodList())
        for (v in methods.first) {
            members[v.name] = v
        }
        "{ ${methods.second} }"
    }
    popScope()
    return "sealed class ${id}${methodCode};$Wrap${buf}"
}

internal fun DelegateVisitor.visitConstructorList(ctx: ConstructorListContext): Map<String, Pair<ArrayList<Identifier>, String>> {
    val map = HashMap<String, Pair<ArrayList<Identifier>, String>>()
    for (v in ctx.constructor()) {
        val (name, fields, code) = visitConstructor(v)
        if (map.contains(name)) {
            println("type: '${name}' is redefined")
            throw CompilingCheckException()
        } else {
            map[name] = fields to code
        }
    }
    return map
}

internal fun DelegateVisitor.visitConstructor(ctx: ConstructorContext): Triple<String, ArrayList<Identifier>, String> {
    val id = visitIdentifier(ctx.identifier())
    pushScope()
    val fields = visitFieldList(ctx.fieldList())
    popScope()
    return Triple(id, fields.first, "class ${id}(${fields.second})")
}