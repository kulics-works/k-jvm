package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*
import com.kulics.feel.node.BoxExpressionNode
import com.kulics.feel.node.ExpressionNode
import com.kulics.feel.node.generateFunctionSignature

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
        };
        interface RawObject {
            fun getRawObject(): Any
        }
        inline fun Any.getRawObject() = this
        interface AnyConstraintObject<ThisConstraint>
        inline fun<reified T> newArray(constraintObjectT: AnyConstraintObject<Int>, size: Int, initValue: T): Array<T> = Array(size) { initValue };
        inline fun<reified T> emptyArray(constraintObjectT: AnyConstraintObject<Int>): Array<T> = arrayOf();$Wrap
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
        is GlobalFunctionDeclarationContext -> visitGlobalFunctionDeclaration(declaration)
        is GlobalRecordDeclarationContext -> visitGlobalRecordDeclaration(declaration)
        is GlobalInterfaceDeclarationContext -> visitGlobalInterfaceDeclaration(declaration)
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
    if (cannotAssign(expr.type, type)) {
        println("the type of init value '${expr.type.name}' is not confirm '${type.name}'")
        throw CompilingCheckException()
    }
    addIdentifier(Identifier(id, type, if (ctx.Mut() != null) IdentifierKind.Mutable else IdentifierKind.Immutable))
    val exprCode = boxToImplementInterface(type, expr)
    return "var $id: ${type.generateTypeName()} = (${exprCode});$Wrap"
}

internal fun boxToImplementInterface(
    type: Type,
    expr: ExpressionNode
) = if (type.name != builtinTypeAny.name && type is InterfaceType && expr.type !is InterfaceType) BoxExpressionNode(
    expr,
    type
).generateCode()
else expr.generateCode()

internal fun DelegateVisitor.visitGlobalFunctionDeclaration(ctx: GlobalFunctionDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        pushScope()
        val typeParameter = visitTypeParameterList(typeParameterList)
        val returnType = checkType(visitType(ctx.type()))
        val params = visitParameterList(ctx.parameterList())
        val type = GenericsType(id, typeParameter) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter[i].name] = li[i]
            }
            typeSubstitution(FunctionType(params.first.map { it.type }, returnType, true), typeMap)
        }
        popScope()
        addIdentifier(Identifier(id, type, IdentifierKind.Immutable))
        pushScope()
        val constraintObject = mutableListOf<Pair<String, String>>()
        for (v in typeParameter) {
            if (isRedefineType(v.name)) {
                println("type: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addType(v)
            constraintObject.add(
                Pair(
                    "constraintObject_${v.name}_${v.uniqueName}",
                    v.constraintObjectTypeName
                )
            )
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
        val exprCode = boxToImplementInterface(returnType, expr)
        "fun <${
            joinString(typeParameter) { "${it.name}: Any" }
        }> ${id}(${
            joinString(constraintObject) { (name, type) ->
                "$name: $type"
            }
        }, ${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${
            exprCode
        });$Wrap}$Wrap"
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
        if (cannotAssign(expr.type, returnType)) {
            println("the return is '${returnType.name}', but find '${expr.type.name}'")
            throw CompilingCheckException()
        }
        popScope()
        val exprCode = boxToImplementInterface(returnType, expr)
        "fun ${id}(${params.second}): ${returnType.generateTypeName()} {${Wrap}return (${exprCode});$Wrap}$Wrap"
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
    val id = visitIdentifier(ctx.identifier())
    val typeParameter = TypeParameter(id, builtinTypeAny)
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
                if (list.isEmpty()) id
                else joinString(listOf(id).plus(list.map { it.generateTypeName() })) { it }
            }>"
        }
        else -> targetType to "${targetType.name}ConstraintObject<${id}>"
    }
    return if (type is ConstraintType) {
        typeParameter.constraint = type
        typeParameter.constraintObjectTypeName = constraintTypeName
        typeParameter
    } else {
        println("the constraint of '${id}' is not interface")
        throw CompilingCheckException()
    }
}

internal fun DelegateVisitor.visitGlobalRecordDeclaration(ctx: GlobalRecordDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id) || isRedefineType(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        pushScope()
        val typeParameter = visitTypeParameterList(typeParameterList)
        val fieldList = visitFieldList(ctx.fieldList())
        val members = mutableMapOf<String, Identifier>()
        fieldList.first.forEach { members[it.name] = it }
        popScope()
        val type = GenericsType(id, typeParameter) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter[i].name] = li[i]
            }
            typeSubstitution(
                RecordType(
                    "${id}[${joinString(li) { it.name }}]",
                    members,
                    "${id}<${joinString(li) { it.name }}>",
                    generateGenericsUniqueName(id, li),
                    true,
                ),
                typeMap
            )
        }
        addType(type)
        val constructorType = GenericsType(id, typeParameter) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter[i].name] = li[i]
            }
            typeSubstitution(FunctionType(fieldList.first.map { it.type }, type.typeConstructor(li)), typeMap)
        }
        addIdentifier(Identifier(id, constructorType, IdentifierKind.Immutable))
        pushScope()
        val constraintObjects = mutableListOf<Pair<String, String>>()
        for (v in typeParameter) {
            if (isRedefineType(v.name)) {
                println("type: '${v.name}' is redefined")
                throw CompilingCheckException()
            }
            addType(v)
            constraintObjects.add(
                Pair(
                    "constraintObject_${v.name}_${v.uniqueName}",
                    v.constraintObjectTypeName
                )
            )
        }
        fieldList.first.forEach { addIdentifier(it) }
        val typeParameterCode = joinString(typeParameter) {
            "${it.name}: ${builtinTypeAny.generateTypeName()}"
        }
        val typeArgumentCode = joinString(typeParameter) { p -> p.name }
        val methodCode = if (ctx.methodList() == null) {
            ""
        } else {
            val methods = visitMethodList(ctx.methodList())
            for (v in methods) {
                members[v.id.name] = v.id
            }
            joinString(methods, Wrap) {
                "fun <${
                    typeParameterCode
                }> ${id}<${typeArgumentCode}>.${
                    generateGenericsMethod(
                        it.id,
                        constraintObjects,
                        it.params,
                        it.returnType,
                        it.body
                    )
                }"
            }
        }
        val generateWrapperCode = checkImplementInterface(ctx.type(), members, type, constraintObjects)
        popScope()
        "class ${id}<${
            typeParameterCode
        }>(${fieldList.second})$Wrap${methodCode}$Wrap${generateWrapperCode?.invoke(id, typeParameterCode)}$Wrap"
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
            for (v in methods) {
                members[v.id.name] = v.id
            }
            joinString(methods, Wrap) {
                "fun ${id}.${
                    generateMethod(
                        it.id,
                        it.params,
                        it.returnType,
                        it.body
                    )
                }"
            }
        }
        val generateWrapperCode = checkImplementInterface(ctx.type(), members, type, listOf())
        popScope()
        "class ${id}(${fieldList.second})${methodCode};$Wrap${generateWrapperCode?.invoke(id, null)}$Wrap"
    }
}

private fun DelegateVisitor.checkImplementInterface(
    interfaceType: TypeContext?,
    members: MutableMap<String, Identifier>,
    implementType: Type,
    constraintObjects: List<Pair<String, String>>
): ((String, String?) -> String)? {
    if (interfaceType == null) {
        return null
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
            val (mapType, ThisType) = if (implementType is GenericsType) {
                val typeParameter = implementType.typeParameter
                GenericsType(targetInterfaceType.name, typeParameter, list) { li ->
                    val typeMap = mutableMapOf<String, Type>()
                    for (i in li.indices) {
                        typeMap[typeParameter[i].name] = li[i]
                    }
                    targetInterfaceType.typeConstructor(list.map { typeSubstitution(it, typeMap) })
                } to implementType.typeConstructor(typeParameter)
            } else {
                instanceType to implementType
            }
            getImplementType(targetInterfaceType)?.forEach {
                addImplementType(instanceType, if (it is GenericsType) it.typeConstructor(list) else it)
            }
            checkMemberImplement(instanceType, members)
            addImplementType(implementType, mapType)
            generateImplementConstraintCode(
                ThisType,
                targetInterfaceType.name,
                instanceType as InterfaceType,
                joinString(list) { it.generateTypeName() },
                constraintObjects
            )
        }
        else -> {
            checkMemberImplement(targetInterfaceType, members)
            addImplementType(implementType, targetInterfaceType)
            generateImplementConstraintCode(
                implementType,
                targetInterfaceType.name,
                targetInterfaceType as InterfaceType,
                "",
                constraintObjects
            )
        }
    }
}

private fun DelegateVisitor.checkMemberImplement(
    implInterface: Type,
    members: MutableMap<String, Identifier>
) {
    if (implInterface !is InterfaceType) {
        println("type '${implInterface.name}' is not interface")
        throw CompilingCheckException()
    } else {
        for (v in implInterface.member) {
            val member = members[v.key]
            if (member == null) {
                println("the member '${v.key}' of '${implInterface.name}' is not implement ")
                throw CompilingCheckException()
            } else if (cannotAssign(v.value.type, member.type)) {
                println("the type of member '${v.key}' is can not to implement '${implInterface.name}'")
                throw CompilingCheckException()
            }
        }
    }
}

fun generateImplementConstraintCode(
    targetType: Type,
    interfaceName: String,
    interfaceType: InterfaceType,
    interfaceTypeArgs: String,
    constraintObjects: List<Pair<String, String>>
): (String, String?) -> String {
    return { id, typeParameterCode ->
        val constraintTypeName =
            "${interfaceName}ConstraintObject<${targetType.generateTypeName()}, ${interfaceTypeArgs}>"
        val members = interfaceType.member.asSequence().fold(StringBuilder()) { acc, entry ->
            val member = targetType.getMember(entry.key)
            if (member != null) {
                val funcSig = generateFunctionSignature(member.type as FunctionType)
                acc.append(
                    """
                    override fun ${member.name}(thisConstraint: ${targetType.generateTypeName()}, ${funcSig.second} {
                        return thisConstraint.${member.name}(${joinString(funcSig.first) { it }});
                    }
                """.trimIndent()
                )
            }
            acc
        }
        """
            class ${interfaceName}ConstraintObjectFor${id}${
            if (typeParameterCode == null) ""
            else "<${typeParameterCode}>"
        }(${joinString(constraintObjects) { "val ${it.first}: ${it.second}" }}): $constraintTypeName {
                $members
            }
        """.trimIndent()
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

internal fun DelegateVisitor.visitMethodList(ctx: MethodListContext): List<Method> {
    return ctx.method().map {
        visitMethod(it)
    }
}

internal fun DelegateVisitor.visitMethod(ctx: MethodContext): Method {
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
    return Method(identifier, params.first, returnType, expr)
}

class Method(val id: Identifier, val params: List<Identifier>, val returnType: Type, val body: ExpressionNode)

fun generateMethod(id: Identifier, params: List<Identifier>, returnType: Type, body: ExpressionNode): String {
    return "${id.name}(${
        joinString(params) { "${it.name}: ${it.type.generateTypeName()}" }
    }): ${returnType.generateTypeName()} { return run{ ${body.generateCode()} } }"
}

fun generateGenericsMethod(
    id: Identifier,
    constraintObject: List<Pair<String, String>>,
    params: List<Identifier>,
    returnType: Type,
    body: ExpressionNode
): String {
    return "${id.name}(${
        joinString(constraintObject) { (name, type) ->
            "$name: $type"
        }
    },${
        joinString(params) { "${it.name}: ${it.type.generateTypeName()}" }
    }): ${returnType.generateTypeName()} { return run{ ${body.generateCode()} } }"
}

internal fun DelegateVisitor.visitGlobalInterfaceDeclaration(ctx: GlobalInterfaceDeclarationContext): String {
    val id = visitIdentifier(ctx.identifier())
    if (isRedefineIdentifier(id)) {
        println("identifier: '$id' is redefined")
        throw CompilingCheckException()
    }
    val members = mutableMapOf<String, Identifier>()
    val typeParameterList = ctx.typeParameterList()
    return if (typeParameterList != null) {
        pushScope()
        val typeParameter = visitTypeParameterList(typeParameterList)
        val type = GenericsType(id, typeParameter) { li ->
            val typeMap = mutableMapOf<String, Type>()
            for (i in li.indices) {
                typeMap[typeParameter[i].name] = li[i]
            }
            typeSubstitution(
                InterfaceType(
                    "${id}[${joinString(li) { it.name }}]",
                    members,
                    "${id}<${joinString(li) { it.generateTypeName() }}>",
                    generateGenericsUniqueName(id, typeParameter),
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
        val methods = ctx.virtualMethodList()?.let {
            visitVirtualMethodList(it).apply {
                for (v in this) {
                    members[v.id.name] = v.id
                }
            }
        }
        val methodCode = methods?.let { list ->
            "{ ${
                list.fold(StringBuilder()) { acc, s ->
                    acc.append(
                        "fun ${s.id.name}(${
                            joinString(s.params) {
                                "${it.name}: ${it.type.generateTypeName()}"
                            }
                        }): ${s.returnType.generateTypeName()}$Wrap"
                    )
                }
            } }"
        } ?: ""
        val constraintMethodCode = methods?.let { list ->
            "{ ${
                list.fold(StringBuilder()) { acc, s ->
                    acc.append("fun ${s.id.name}(thisConstraint: ThisConstraint, ${
                        joinString(s.params) {
                            "${it.name}: ${it.type.generateTypeName()}"
                        }
                    }): ${s.returnType.generateTypeName()}$Wrap")
                }
            } }"
        } ?: ""
        popScope()
        fun generateConstraintTypeName(p: TypeParameter): String {
            return when (val constraintType = p.constraint) {
                is GenericsType -> {
                    val ty = constraintType.typeConstructor(listOf(p))
                    "${p.name}: ${ty.generateTypeName()}"
                }
                is InterfaceType -> "${p.name}: ${constraintType.generateTypeName()}"
            }
        }
        """
            interface ${id}<${
            joinString(typeParameter) {
                generateConstraintTypeName(it)
            }
        }>: RawObject${methodCode};
            interface ${id}ConstraintObject<ThisConstraint: ${builtinTypeAny.generateTypeName()}, ${
            joinString(typeParameter) {
                generateConstraintTypeName(it)
            }
        }>${constraintMethodCode};$Wrap
        """.trimIndent()
    } else {
        val type = InterfaceType(id, members, null)
        addType(type)
        pushScope()
        val methods = ctx.virtualMethodList()?.let { virtualMethodListContext ->
            visitVirtualMethodList(virtualMethodListContext).onEach {
                members[it.id.name] = it.id
            }
        }
        val methodCode = methods?.let { list ->
            "{ ${
                list.fold(StringBuilder()) { acc, s ->
                    acc.append("fun ${s.id.name}(${
                        joinString(s.params) {
                            "${it.name}: ${it.type.generateTypeName()}"
                        }
                    }): ${s.returnType.generateTypeName()}$Wrap")
                }
            } }"
        } ?: ""
        val constraintMethodCode = methods?.let { list ->
            "{ ${
                list.fold(StringBuilder()) { acc, s ->
                    acc.append("fun ${s.id.name}(thisConstraint: ThisConstraint, ${
                        joinString(s.params) {
                            "${it.name}: ${it.type.generateTypeName()}"
                        }
                    }): ${s.returnType.generateTypeName()}$Wrap")
                }
            } }"
        } ?: ""
        popScope()
        """
            interface ${id}: RawObject${methodCode};
            interface ${id}ConstraintObject<ThisConstraint>${constraintMethodCode};$Wrap
        """.trimIndent()
    }
}

internal fun DelegateVisitor.visitVirtualMethodList(ctx: VirtualMethodListContext): List<VirtualMethod> {
    return ctx.virtualMethod().map {
        visitVirtualMethod(it)
    }
}

internal fun DelegateVisitor.visitVirtualMethod(ctx: VirtualMethodContext): VirtualMethod {
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
    for (v in params.first) {
        if (isRedefineIdentifier(v.name)) {
            println("identifier: '${v.name}' is redefined")
            throw CompilingCheckException()
        }
        addIdentifier(v)
    }
    return VirtualMethod(identifier, params.first, returnType)
}

data class VirtualMethod(val id: Identifier, val params: ArrayList<Identifier>, val returnType: Type)