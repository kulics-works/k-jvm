package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.*

internal fun DelegateVisitor.visitProgram(ctx: ProgramContext): ProgramNode {
    return ProgramNode(visitModuleDeclaration(ctx.moduleDeclaration()),
        ctx.globalDeclaration().map {
            visitGlobalDeclaration(it)
        })
}

internal fun DelegateVisitor.visitModuleDeclaration(ctx: ModuleDeclarationContext): ModuleDeclarationNode {
    return ModuleDeclarationNode(visitIdentifier(ctx.identifier()))
}

internal fun DelegateVisitor.visitGlobalDeclaration(ctx: GlobalDeclarationContext): DeclarationNode {
    return when (val declaration = ctx.getChild(0)) {
        is GlobalVariableDeclarationContext -> visitGlobalVariableDeclaration(declaration)
        is GlobalFunctionDeclarationContext -> visitGlobalFunctionDeclaration(declaration)
        is GlobalRecordDeclarationContext -> visitGlobalRecordDeclaration(declaration)
        is GlobalInterfaceDeclarationContext -> visitGlobalInterfaceDeclaration(declaration)
        is GlobalExtensionDeclarationContext -> visitGlobalExtensionDeclaration(declaration)
        else -> throw CompilingCheckException()
    }
}

internal fun DelegateVisitor.visitGlobalVariableDeclaration(ctx: GlobalVariableDeclarationContext): GlobalVariableDeclarationNode {
    val idName = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpression(ctx.expression())
    val type = checkType(visitType(ctx.type()))
    if (cannotAssign(expr.type, type)) {
        println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
        throw CompilingCheckException()
    }
    val id = Identifier(idName, type, if (ctx.Mut() != null) IdentifierKind.Mutable else IdentifierKind.Immutable)
    addIdentifier(id)
    return GlobalVariableDeclarationNode(id, expr)
}

internal fun DelegateVisitor.visitGlobalFunctionDeclaration(ctx: GlobalFunctionDeclarationContext): GlobalFunctionDeclarationNode {
    val idName = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        pushScope()
        val typeParameter = visitTypeParameterList(typeParameterList)
        val returnType = checkType(visitType(ctx.type()))
        val params = visitParameterList(ctx.parameterList())
        val type = GenericsType(idName, typeParameter) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter[i].name] = li[i]
            }
            typeSubstitution(FunctionType(params.first.map { it.type }, returnType, true), typeMap)
        }
        popScope()
        val id = Identifier(idName, type, IdentifierKind.Immutable)
        addIdentifier(id)
        pushScope()
        for (v in typeParameter) {
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
        if (cannotAssign(expr.type, returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        GlobalFunctionDeclarationNode(
            id,
            typeParameter,
            params.first.map { ParameterDeclarationNode(it, it.type) },
            returnType,
            expr
        )
    } else {
        val returnType = checkType(visitType(ctx.type()))
        val params = visitParameterList(ctx.parameterList())
        val type = FunctionType(params.first.map { it.type }, returnType)
        val id = Identifier(idName, type, IdentifierKind.Immutable)
        addIdentifier(id)
        pushScope()
        for (v in params.first) {
            if (isRedefineIdentifier(v.name)) {
                println("identifier: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addIdentifier(v)
        }
        val expr = visitExpression(ctx.expression())
        if (cannotAssign(expr.type, returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        GlobalFunctionDeclarationNode(
            id,
            listOf(),
            params.first.map { ParameterDeclarationNode(it, it.type) },
            returnType,
            expr
        )
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

internal fun DelegateVisitor.visitTypeParameterList(ctx: TypeParameterListContext): List<TypeParameter> {
    return ctx.typeParameter().map { visitTypeParameter(it) }
}

internal fun DelegateVisitor.visitTypeParameter(ctx: TypeParameterContext): TypeParameter {
    val idName = visitIdentifier(ctx.identifier())
    val typeParameter = TypeParameter(idName, builtinTypeAny)
    addType(typeParameter)
    val typeNode = visitType(ctx.type())
    val (type, constraintTypeName) = when (val targetType = getType(typeNode.id)) {
        null -> {
            println("type: '${typeNode.id}' is undefined")
            throw CompilingCheckException()
        }
        is GenericsType -> {
            if (typeNode.typeArguments.isEmpty() || targetType.typeParameter.size != typeNode.typeArguments.size) {
                println("the type args size need '${targetType.typeParameter.size}', but found '${typeNode.typeArguments.size}'")
                throw CompilingCheckException()
            }
            val list = mutableListOf<Type>()
            for (v in typeNode.typeArguments) {
                list.add(checkType(v))
            }
            GenericsType(targetType.name, listOf(typeParameter), list) { li ->
                val typeMap = mutableMapOf<String, Type>(typeParameter.name to li[0])
                val instanceType = targetType.typeConstructor(list.map { typeSubstitution(it, typeMap) })
                getImplementType(targetType)?.forEach {
                    addImplementType(instanceType, if (it is GenericsType) it.typeConstructor(list) else it)
                }
                instanceType
            } to "${targetType.name}ConstraintObject<${
                if (list.isEmpty()) idName
                else joinString(listOf(idName).plus(list.map { it.generateTypeName() })) { it }
            }>"
        }
        else -> targetType to "${targetType.name}ConstraintObject<${idName}>"
    }
    return if (type is ConstraintType) {
        typeParameter.constraint = type
        typeParameter.constraintObjectTypeName = constraintTypeName
        typeParameter
    } else {
        println("the constraint of '${idName}' is not interface")
        throw CompilingCheckException()
    }
}

internal fun DelegateVisitor.visitGlobalRecordDeclaration(ctx: GlobalRecordDeclarationContext): GlobalRecordDeclarationNode {
    val idName = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(idName) || isRedefineType(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        pushScope()
        val typeParameter = visitTypeParameterList(typeParameterList)
        val fieldList = visitFieldList(ctx.fieldList())
        val members = mutableMapOf<String, Identifier>()
        fieldList.forEach { members[it.name] = it }
        popScope()
        val type = GenericsType(idName, typeParameter) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter[i].name] = li[i]
            }
            typeSubstitution(
                RecordType(
                    "${idName}[${joinString(li) { it.name }}]",
                    members,
                    "${idName}<${joinString(li) { it.name }}>",
                    generateGenericsUniqueName(idName, li),
                    true,
                ),
                typeMap
            )
        }
        addType(type)
        val constructorType = GenericsType(idName, typeParameter) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter[i].name] = li[i]
            }
            typeSubstitution(FunctionType(fieldList.map { it.type }, type.typeConstructor(li)), typeMap)
        }
        addIdentifier(Identifier(idName, constructorType, IdentifierKind.Immutable))
        pushScope()
        for (v in typeParameter) {
            if (isRedefineType(v.name)) {
                println("type: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addType(v)
        }
        fieldList.forEach { addIdentifier(it) }
        val methods = if (ctx.methodList() != null) {
            val methods = visitMethodList(ctx.methodList())
            for (v in methods) {
                members[v.id.name] = v.id
            }
            methods
        } else listOf()
        val (interfaceType, overrideMembers) = checkImplementInterface(ctx.type(), members, type)
        popScope()
        GlobalRecordDeclarationNode(type, typeParameter, fieldList, methods.map {
            if (overrideMembers.contains(it.id.name)) {
                MethodNode(it.id, it.params, it.returnType, it.body, true)
            } else {
                it
            }
        }, interfaceType)
    } else {
        val fieldList = visitFieldList(ctx.fieldList())
        val members = mutableMapOf<String, Identifier>()
        fieldList.forEach { members[it.name] = it }
        val type = RecordType(idName, members, null)
        addType(type)
        val constructorType = FunctionType(fieldList.map { it.type }, type)
        addIdentifier(Identifier(idName, constructorType, IdentifierKind.Immutable))
        pushScope()
        fieldList.forEach { addIdentifier(it) }
        val methods = if (ctx.methodList() != null) {
            val methods = visitMethodList(ctx.methodList())
            for (v in methods) {
                members[v.id.name] = v.id
            }
            methods
        } else listOf()
        val (interfaceType, overrideMembers) = checkImplementInterface(ctx.type(), members, type)
        popScope()
        GlobalRecordDeclarationNode(type, listOf(), fieldList, methods.map {
            if (overrideMembers.contains(it.id.name)) {
                MethodNode(it.id, it.params, it.returnType, it.body, true)
            } else {
                it
            }
        }, interfaceType)
    }
}

private fun DelegateVisitor.checkImplementInterface(
    interfaceType: TypeContext?,
    members: MutableMap<String, Identifier>,
    implementType: Type
): Pair<Type?, MutableMap<String, Identifier>> {
    if (interfaceType == null) {
        return null to mutableMapOf()
    }
    val typeNode = visitType(interfaceType)
    return when (val targetInterfaceType = getType(typeNode.id)) {
        null -> {
            println("type: '${typeNode.id}' is undefined")
            throw CompilingCheckException()
        }
        is GenericsType -> {
            if (typeNode.typeArguments.isEmpty() ||
                targetInterfaceType.typeParameter.size != typeNode.typeArguments.size
            ) {
                println("the type args size need '${targetInterfaceType.typeParameter.size}', but found '${typeNode.typeArguments.size}'")
                throw CompilingCheckException()
            }
            val list = mutableListOf<Type>()
            for (v in typeNode.typeArguments) {
                list.add(checkType(v))
            }
            val instanceType = targetInterfaceType.typeConstructor(list)
            val mapType = if (implementType is GenericsType) {
                val typeParameter = implementType.typeParameter
                GenericsType(targetInterfaceType.name, typeParameter, list) { li ->
                    val typeMap = mutableMapOf<String, Type>()
                    for (i in li.indices) {
                        typeMap[typeParameter[i].name] = li[i]
                    }
                    targetInterfaceType.typeConstructor(list.map { typeSubstitution(it, typeMap) })
                }
            } else {
                instanceType
            }
            getImplementType(targetInterfaceType)?.forEach {
                addImplementType(instanceType, if (it is GenericsType) it.typeConstructor(list) else it)
            }
            val overrideMember = checkMemberImplement(instanceType, members)
            addImplementType(implementType, mapType)
            instanceType to overrideMember
        }
        else -> {
            val overrideMember = checkMemberImplement(targetInterfaceType, members)
            addImplementType(implementType, targetInterfaceType)
            targetInterfaceType to overrideMember
        }
    }
}

private fun DelegateVisitor.checkMemberImplement(
    implInterface: Type,
    members: MutableMap<String, Identifier>
): MutableMap<String, Identifier> {
    return if (implInterface !is InterfaceType) {
        println("type '${implInterface.name}' is not interface")
        throw CompilingCheckException()
    } else {
        val overrideMember = mutableMapOf<String, Identifier>()
        for (v in implInterface.member) {
            val member = members[v.key]
            if (member == null) {
                if (!v.value.hasImplement) {
                    println("the member '${v.key}' of '${implInterface.name}' is not implement ")
                    throw CompilingCheckException()
                }
            } else if (cannotAssign(v.value.type, member.type)) {
                println("the type of member '${v.key}' is can not to implement '${implInterface.name}'")
                throw CompilingCheckException()
            } else {
                overrideMember[v.key] = member
            }
        }
        overrideMember
    }
}

internal fun DelegateVisitor.visitFieldList(ctx: FieldListContext): List<Identifier> {
    return ctx.field().map { visitField(it) }
}

internal fun DelegateVisitor.visitField(ctx: FieldContext): Identifier {
    return Identifier(
        visitIdentifier(ctx.identifier()),
        checkType(visitType(ctx.type())),
        if (ctx.Mut() == null) IdentifierKind.Immutable else IdentifierKind.Mutable
    )
}

internal fun DelegateVisitor.visitMethodList(ctx: MethodListContext): List<MethodNode> {
    return ctx.method().map { visitMethod(it) }
}

internal fun DelegateVisitor.visitMethod(ctx: MethodContext): MethodNode {
    val idName = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val returnType = checkType(visitType(ctx.type()))
    val params = visitParameterList(ctx.parameterList())
    val type = FunctionType(params.first.map { it.type }, returnType)
    val id = Identifier(idName, type, IdentifierKind.Immutable)
    addIdentifier(id)
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
    return MethodNode(id, params.first, returnType, expr, false)
}

internal fun DelegateVisitor.visitGlobalInterfaceDeclaration(ctx: GlobalInterfaceDeclarationContext): GlobalInterfaceDeclarationNode {
    val idName = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val members = mutableMapOf<String, VirtualIdentifier>()
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        pushScope()
        val typeParameter = visitTypeParameterList(typeParameterList)
        val type = GenericsType(idName, typeParameter) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter[i].name] = li[i]
            }
            typeSubstitution(
                InterfaceType(
                    "${idName}[${joinString(li) { it.name }}]",
                    members,
                    "${idName}<${joinString(li) { it.generateTypeName() }}>",
                    generateGenericsUniqueName(idName, typeParameter),
                    true,
                ), typeMap
            )
        }
        popScope()
        addType(type)
        pushScope()
        for (v in typeParameter) {
            addType(v)
        }
        val methods = if (ctx.virtualMethodList() != null) {
            val methods = visitVirtualMethodList(ctx.virtualMethodList())
            for (v in methods) {
                members[v.id.name] = v.id
            }
            methods
        } else listOf()
        popScope()
        GlobalInterfaceDeclarationNode(type, typeParameter, methods)
    } else {
        val type = InterfaceType(idName, members, null)
        addType(type)
        val methods = if (ctx.virtualMethodList() != null) {
            val methods = visitVirtualMethodList(ctx.virtualMethodList())
            for (v in methods) {
                members[v.id.name] = v.id
            }
            methods
        } else listOf()
        GlobalInterfaceDeclarationNode(type, listOf(), methods)
    }
}

internal fun DelegateVisitor.visitVirtualMethodList(ctx: VirtualMethodListContext): List<VirtualMethodNode> {
    return ctx.virtualMethod().map { visitVirtualMethod(it) }
}

internal fun DelegateVisitor.visitVirtualMethod(ctx: VirtualMethodContext): VirtualMethodNode {
    val idName = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val returnType = checkType(visitType(ctx.type()))
    val params = visitParameterList(ctx.parameterList())
    val type = FunctionType(params.first.map { it.type }, returnType)
    val id = VirtualIdentifier(idName, type, IdentifierKind.Immutable)
    addIdentifier(id)
    pushScope()
    for (v in params.first) {
        if (isRedefineIdentifier(v.name)) {
            println("identifier: '${v.name}' is redefined")
            throw CompilingCheckException()
        }
        addIdentifier(v)
    }
    val node = if (ctx.expression() != null) {
        val expr = visitExpression(ctx.expression())
        if (expr.type != returnType) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        id.hasImplement = true
        VirtualMethodNode(id, params.first, returnType, expr)
    } else {
        VirtualMethodNode(id, params.first, returnType, null)
    }
    popScope()
    return node
}

internal fun DelegateVisitor.visitGlobalExtensionDeclaration(ctx: GlobalExtensionDeclarationContext): GlobalExtensionDeclarationNode {
    val idName = visitIdentifier(ctx.identifier())
    val type = getType(idName)
    if (type == null) {
        println("identifier: '$idName' is not defined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        throw CompilingCheckException()
    } else {
        if (type !is RecordType) {
            println("the type '${type.name}' is not a record type")
            throw CompilingCheckException()
        }
        pushScope()
        val methods = if (ctx.methodList() != null) {
            val methods = visitMethodList(ctx.methodList())
            for (v in methods) {
                if (type.member.contains(v.id.name)) {
                    println("the member: '${v.id.name}' of type '${type.name}' is redefined")
                    throw CompilingCheckException()
                }
                type.member[v.id.name] = v.id
            }
            methods
        } else listOf()
        val (interfaceType, overrideMembers) = checkImplementInterface(ctx.type(), type.member, type)
        popScope()
        GlobalExtensionDeclarationNode(type, listOf(), methods.map {
            if (overrideMembers.contains(it.id.name)) {
                MethodNode(it.id, it.params, it.returnType, it.body, true)
            } else {
                it
            }
        }, interfaceType)
    }
}
