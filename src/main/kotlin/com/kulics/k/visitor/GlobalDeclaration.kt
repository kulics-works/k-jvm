package com.kulics.k.visitor

import com.kulics.k.grammar.KParser.*
import com.kulics.k.node.*

fun DelegateVisitor.visitProgram(ctx: ProgramContext): ProgramNode {
    return ProgramNode(visitModuleDeclaration(ctx.moduleDeclaration()),
        ctx.globalDeclaration().map {
            visitGlobalDeclaration(it)
        })
}

fun DelegateVisitor.visitModuleDeclaration(ctx: ModuleDeclarationContext): ModuleDeclarationNode {
    return ModuleDeclarationNode(visitIdentifier(ctx.variableIdentifier()))
}

fun DelegateVisitor.visitGlobalDeclaration(ctx: GlobalDeclarationContext): DeclarationNode {
    return when (val declaration = ctx.getChild(0)) {
        is GlobalVariableDeclarationContext -> visitGlobalVariableDeclaration(declaration)
        is GlobalFunctionDeclarationContext -> visitGlobalFunctionDeclaration(declaration)
        is GlobalRecordDeclarationContext -> visitGlobalRecordDeclaration(declaration)
        is GlobalInterfaceDeclarationContext -> visitGlobalInterfaceDeclaration(declaration)
        is GlobalExtensionDeclarationContext -> visitGlobalExtensionDeclaration(declaration)
        is GlobalSumTypeDeclarationContext -> visitGlobalSumTypeDeclaration(declaration)
        else -> throw CompilingCheckException()
    }
}

fun DelegateVisitor.visitGlobalVariableDeclaration(ctx: GlobalVariableDeclarationContext): GlobalVariableDeclarationNode {
    val idName = visitIdentifier(ctx.variableIdentifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
    val type = if (ctx.type() == null) {
        expr.type
    } else {
        val type = checkTypeNode(visitType(ctx.type()))
        if (cannotAssign(expr.type, type)) {
            println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
            throw CompilingCheckException()
        }
        type
    }
    val id = Identifier(idName, type, if (ctx.Mut() != null) IdentifierKind.Mutable else IdentifierKind.Immutable)
    addIdentifier(id)
    return GlobalVariableDeclarationNode(id, expr)
}

fun DelegateVisitor.visitGlobalFunctionDeclaration(ctx: GlobalFunctionDeclarationContext): GlobalFunctionDeclarationNode {
    val idName = visitIdentifier(ctx.variableIdentifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        if (ctx.type() == null) {
            pushScope()
            val typeParameter = visitTypeParameterList(typeParameterList)
            val params = visitParameterList(ctx.parameterList())
            popScope()
            pushScope()
            for (v in typeParameter) {
                if (isRedefineType(v.name)) {
                    println("type: '${v.name}' is redefined")
                    throw CompilingCheckException()
                }
                addType(v)
            }
            for (v in params) {
                if (isRedefineIdentifier(v.name)) {
                    println("identifier: '${v.name}' is redefined")
                    throw CompilingCheckException()
                }
                addIdentifier(v)
            }
            val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
            val returnType = expr.type
            popScope()
            val type = GenericsType(idName, typeParameter) { li ->
                val typeMap = mutableMapOf<String, Type>()
                for (i in li.indices) {
                    typeMap[typeParameter[i].name] = li[i]
                }
                typeSubstitution(FunctionType(params.map { it.type }, returnType), typeMap)
            }
            val id = Identifier(idName, type, IdentifierKind.Immutable)
            addIdentifier(id)
            GlobalFunctionDeclarationNode(
                id,
                typeParameter,
                params.map { ParameterDeclarationNode(it, it.type) },
                returnType,
                expr
            )
        } else {
            pushScope()
            val typeParameter = visitTypeParameterList(typeParameterList)
            val returnType = checkTypeNode(visitType(ctx.type()))
            val params = visitParameterList(ctx.parameterList())
            val type = GenericsType(idName, typeParameter) { li ->
                val typeMap = mutableMapOf<String, Type>()
                for (i in li.indices) {
                    typeMap[typeParameter[i].name] = li[i]
                }
                typeSubstitution(FunctionType(params.map { it.type }, returnType), typeMap)
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
            for (v in params) {
                if (isRedefineIdentifier(v.name)) {
                    println("identifier: '${v.name}' is redefined")
                    throw CompilingCheckException()
                }
                addIdentifier(v)
            }
            val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
            if (cannotAssign(expr.type, returnType)) {
                println("the return is '${returnType.name}', but find '${expr.type.name}'")
                throw CompilingCheckException()
            }
            popScope()
            GlobalFunctionDeclarationNode(
                id,
                typeParameter,
                params.map { ParameterDeclarationNode(it, it.type) },
                returnType,
                expr
            )
        }
    } else {
        if (ctx.type() == null) {
            val params = visitParameterList(ctx.parameterList())
            pushScope()
            for (v in params) {
                if (isRedefineIdentifier(v.name)) {
                    println("identifier: '${v.name}' is redefined")
                    throw CompilingCheckException()
                }
                addIdentifier(v)
            }
            val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
            val returnType = expr.type
            popScope()
            val type = FunctionType(params.map { it.type }, returnType)
            val id = Identifier(idName, type, IdentifierKind.Immutable)
            addIdentifier(id)
            GlobalFunctionDeclarationNode(
                id,
                listOf(),
                params.map { ParameterDeclarationNode(it, it.type) },
                returnType,
                expr
            )
        } else {
            val returnType = checkTypeNode(visitType(ctx.type()))
            val params = visitParameterList(ctx.parameterList())
            val type = FunctionType(params.map { it.type }, returnType)
            val id = Identifier(idName, type, IdentifierKind.Immutable)
            addIdentifier(id)
            pushScope()
            for (v in params) {
                if (isRedefineIdentifier(v.name)) {
                    println("identifier: '${v.name}' is redefined")
                    throw CompilingCheckException()
                }
                addIdentifier(v)
            }
            val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
            if (cannotAssign(expr.type, returnType)) {
                println("the return is '${returnType.name}', but find '${expr.type.name}'")
                throw CompilingCheckException()
            }
            popScope()
            GlobalFunctionDeclarationNode(
                id,
                listOf(),
                params.map { ParameterDeclarationNode(it, it.type) },
                returnType,
                expr
            )
        }
    }
}

fun DelegateVisitor.visitParameterList(ctx: ParameterListContext): List<Identifier> {
    return ctx.parameter().map { visitParameter(it) }
}

fun DelegateVisitor.visitParameter(ctx: ParameterContext): Identifier {
    val id = visitIdentifier(ctx.variableIdentifier())
    val type = checkTypeNode(visitType(ctx.type()))
    return Identifier(id, type, IdentifierKind.Immutable)
}

fun DelegateVisitor.visitTypeParameterList(ctx: TypeParameterListContext): List<TypeParameter> {
    return ctx.typeParameter().map { visitTypeParameter(it) }
}

fun DelegateVisitor.visitTypeParameter(ctx: TypeParameterContext): TypeParameter {
    val idName = visitIdentifier(ctx.typeIdentifier())
    val typeParameter = TypeParameter(idName, builtinTypeAny)
    addType(typeParameter)
    if (ctx.type() == null) {
        return typeParameter
    }
    return when (val typeNode = visitType(ctx.type())) {
        is FunctionTypeNode -> {
            println("the constraint type is not interface")
            throw CompilingCheckException()
        }

        is NominalTypeNode -> {
            val type = when (val targetType = getType(typeNode.id)) {
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
                    for ((i, v) in typeNode.typeArguments.withIndex()) {
                        val typeArg = checkTypeNode(v)
                        if (cannotAssign(targetType.typeParameter[i], typeArg)) {
                            println("the type '${typeArg.name}' not confirm ${targetType.typeParameter[i].name}")
                            throw CompilingCheckException()
                        }
                        list.add(typeArg)
                    }
                    GenericsType(targetType.name, listOf(typeParameter), list) { li ->
                        val typeMap = mutableMapOf(typeParameter.name to li[0])
                        val instanceType = targetType.typeConstructor(list.map { typeSubstitution(it, typeMap) })
                        getImplementType(targetType)?.forEach {
                            addImplementType(instanceType, if (it is GenericsType) it.typeConstructor(list) else it)
                        }
                        instanceType
                    }
                }

                else -> if (typeParameter == targetType) {
                    println("the constraint of '${idName}' can not be it self")
                    throw CompilingCheckException()
                } else targetType
            }
            if (type is ConstraintType) {
                typeParameter.constraint = type
                typeParameter
            } else {
                println("the constraint of '${idName}' is not interface")
                throw CompilingCheckException()
            }
        }
    }
}

fun DelegateVisitor.visitGlobalRecordDeclaration(ctx: GlobalRecordDeclarationContext): GlobalRecordDeclarationNode {
    val idName = visitIdentifier(ctx.typeIdentifier())
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
                    generateGenericsUniqueName(idName, li),
                    idName to li,
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
        val type = RecordType(idName, members)
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
    return when (val typeNode = visitType(interfaceType)) {
        is FunctionTypeNode -> {
            println("the implements type is not interface")
            throw CompilingCheckException()
        }

        is NominalTypeNode -> when (val targetInterfaceType = getType(typeNode.id)) {
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
                    list.add(checkTypeNode(v))
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

fun DelegateVisitor.visitFieldList(ctx: FieldListContext): List<Identifier> {
    return ctx.field().map { visitField(it) }
}

fun DelegateVisitor.visitField(ctx: FieldContext): Identifier {
    return Identifier(
        visitIdentifier(ctx.variableIdentifier()),
        checkTypeNode(visitType(ctx.type())),
        if (ctx.Mut() == null) IdentifierKind.Immutable else IdentifierKind.Mutable
    )
}

fun DelegateVisitor.visitMethodList(ctx: MethodListContext): List<MethodNode> {
    return ctx.method().map { visitMethod(it) }
}

fun DelegateVisitor.visitMethod(ctx: MethodContext): MethodNode {
    val idName = visitIdentifier(ctx.variableIdentifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val returnType = checkTypeNode(visitType(ctx.type()))
    val params = visitParameterList(ctx.parameterList())
    val type = FunctionType(params.map { it.type }, returnType)
    val id = Identifier(idName, type, IdentifierKind.Immutable)
    addIdentifier(id)
    pushScope()
    for (v in params) {
        if (isRedefineIdentifier(v.name)) {
            println("identifier: '${v.name}' is redefined")
            throw CompilingCheckException()
        }
        addIdentifier(v)
    }
    val expr = visitExpressionWithTerminator(ctx.expressionWithTerminator())
    if (expr.type != returnType) {
        println("the return is '${returnType.name}', but find '${expr.type.name}'")
        throw CompilingCheckException()
    }
    popScope()
    return MethodNode(id, params, returnType, expr, false)
}

fun DelegateVisitor.visitGlobalInterfaceDeclaration(ctx: GlobalInterfaceDeclarationContext): GlobalInterfaceDeclarationNode {
    val idName = visitIdentifier(ctx.typeIdentifier())
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
                    generateGenericsUniqueName(idName, typeParameter),
                    idName to li,
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
        val type = InterfaceType(idName, members)
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

fun DelegateVisitor.visitVirtualMethodList(ctx: VirtualMethodListContext): List<VirtualMethodNode> {
    return ctx.virtualMethod().map { visitVirtualMethod(it) }
}

fun DelegateVisitor.visitVirtualMethod(ctx: VirtualMethodContext): VirtualMethodNode {
    val idName = visitIdentifier(ctx.variableIdentifier())
    if (isRedefineIdentifier(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val returnType = checkTypeNode(visitType(ctx.type()))
    val params = visitParameterList(ctx.parameterList())
    val type = FunctionType(params.map { it.type }, returnType)
    val id = VirtualIdentifier(idName, type, IdentifierKind.Immutable)
    addIdentifier(id)
    pushScope()
    for (v in params) {
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
        VirtualMethodNode(id, params, returnType, expr)
    } else {
        VirtualMethodNode(id, params, returnType, null)
    }
    popScope()
    return node
}

fun DelegateVisitor.visitGlobalExtensionDeclaration(ctx: GlobalExtensionDeclarationContext): GlobalExtensionDeclarationNode {
    val idName = visitIdentifier(ctx.typeIdentifier())
    val type = getType(idName)
    if (type == null) {
        println("identifier: '$idName' is not defined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        if (type !is GenericsType) {
            println("the type '${type.name}' is not a generics type")
            throw CompilingCheckException()
        }
        val typeParameters = visitTypeParameterList(typeParameterList).map {
            TypeParameter(it.name, it.constraint, true)
        }
        val instanceType = type.typeConstructor(typeParameters)
        if (instanceType !is RecordType) {
            println("the type '${type.name}' is not a record type")
            throw CompilingCheckException()
        }
        pushScope()
        for ((_, id) in instanceType.member) {
            addIdentifier(id)
        }
        val methods = if (ctx.methodList() != null) {
            val methods = visitMethodList(ctx.methodList())
            for (v in methods) {
                if (instanceType.member.contains(v.id.name)) {
                    println("the member: '${v.id.name}' of type '${type.name}' is redefined")
                    throw CompilingCheckException()
                }
                instanceType.member[v.id.name] = v.id
            }
            methods
        } else listOf()
        val (interfaceType, overrideMembers) = checkImplementInterface(ctx.type(), instanceType.member, type)
        popScope()
        GlobalExtensionDeclarationNode(type, typeParameters, methods.map {
            if (overrideMembers.contains(it.id.name)) {
                MethodNode(it.id, it.params, it.returnType, it.body, true)
            } else {
                it
            }
        }, interfaceType)
    } else {
        if (type !is RecordType) {
            println("the type '${type.name}' is not a record type")
            throw CompilingCheckException()
        }
        pushScope()
        for ((_, id) in type.member) {
            addIdentifier(id)
        }
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

fun DelegateVisitor.visitGlobalSumTypeDeclaration(ctx: GlobalSumTypeDeclarationContext): GlobalSumTypeDeclarationNode {
    val idName = visitIdentifier(ctx.typeIdentifier())
    if (isRedefineIdentifier(idName) || isRedefineType(idName)) {
        println("identifier: '$idName' is redefined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        TODO("sum type generics")
    } else {
        val members = mutableMapOf<String, Identifier>()
        val type = SumType(idName, members)
        addType(type)
        val constructors = visitValueConstructorList(ctx.recordConstructor(), type)
        GlobalSumTypeDeclarationNode(type, listOf(), constructors)
    }
}

fun DelegateVisitor.visitValueConstructorList(ctx: List<RecordConstructorContext>, targetInterfaceType: Type): List<ValueConstructorNode> {
    return ctx.map { ItemCtx ->
        val idName = visitIdentifier(ItemCtx.typeIdentifier())
        if (isRedefineIdentifier(idName) || isRedefineType(idName)) {
            println("identifier: '$idName' is redefined")
            throw CompilingCheckException()
        }
        val fieldList = visitFieldList(ItemCtx.fieldList())
        val members = mutableMapOf<String, Identifier>()
        fieldList.forEach { members[it.name] = it }
        val type = RecordType(idName, members)
        addType(type)
        val constructorType = FunctionType(fieldList.map { it.type }, type)
        addIdentifier(Identifier(idName, constructorType, IdentifierKind.Immutable))
        pushScope()
        fieldList.forEach { addIdentifier(it) }
        addImplementType(type, targetInterfaceType)
        popScope()
        ValueConstructorNode(type, listOf(), fieldList, targetInterfaceType)
    }
}