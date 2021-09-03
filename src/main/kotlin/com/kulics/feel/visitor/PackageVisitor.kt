package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

open class PackageVisitor() : NamespaceVisitor() {
    // package -------------------------------

    override fun visitIncludeStatement(context: IncludeStatementContext): any {
        return visit(context.typeType())
    }

    override fun visitPackageStatement(context: PackageStatementContext): any {
        val id = visit(context.id()) as Result
        var obj = ""
        var extend = listOf<str>()

        if (context.packageStaticStatement() != null) {
            val item = context.packageStaticStatement()
            val r = visit(item) as Result
            obj += r.text
        }
        if (context.packageFieldStatement() != null) {
            val item = context.packageFieldStatement()
            val r = visit(item) as Result
            obj += r.text
            extend += r.data as list<str>
        }
        if (context.packageNewStatement() != null) {
            val item = context.packageNewStatement()
            val r = visit(item) as str
            obj += r
        }
        obj += BlockRight + Wrap
        var header = ""
        if (context.annotationSupport() != null) {
            header += visit(context.annotationSupport()) as str
        }
        header += "${id.permission} class ${id.text}"
        // # 泛型 #
        var template = ""
        var templateContract = ""
        if (context.templateDefine() != null) {
            val item = visit(context.templateDefine()) as TemplateItem
            template += item.Template
            templateContract = item.Contract
            header += template
        }

        if (extend.count() > 0) {
            var temp = extend[0]
            for (i in 1..extend.count() - 1) {
                temp += "," + extend[i]
            }
            header += ":" + temp
        }

        header += templateContract + BlockLeft + Wrap
        obj = header + obj
        this.selfID = ""
        return obj
    }

    override fun visitPackageStaticStatement(context: PackageStaticStatementContext) = Result().apply {
        var obj = ""
        for (item in context.packageStaticSupportStatement()) {
            obj += visit(item)
        }
        text = obj
    }

    override fun visitPackageFieldStatement(context: PackageFieldStatementContext) = Result().apply {
        var obj = ""
        var extend = listOf<str>()
        if (context.id(0) != null) {
            val Self = visit(context.id(0)) as Result
            selfID = Self.text
        }
        if (context.id(1) != null) {
            val Super = visit(context.id(1)) as Result
            superID = Super.text
        }
        for (item in context.packageSupportStatement()) {
            val content = when (visit(item)) {
                null -> ""
                else -> visit(item) as str
            }
            when (item.getChild(0)) {
                is IncludeStatementContext -> extend += content
                else -> obj += content
            }
        }
        selfID = ""
        superID = ""
        text = obj
        data = extend
    }

    override fun visitPackageConstantStatement(context: PackageConstantStatementContext): any {
        val r1 = visit(context.id()) as Result
        val isMutable = r1.isVirtual
        var typ = ""
        var r2: Result? = null
        if (context.expression() != null) {
            r2 = visit(context.expression()) as Result
            typ = r2.data as str
        }
        if (context.typeType() != null) {
            typ = visit(context.typeType()) as str
        }
        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }

        obj += "${r1.permission} val ${r1.text}:$typ"
        obj += if (r2 != null) {
            " = ${r2.text}$Wrap"
        } else {
            Wrap
        }
        return obj
    }

    override fun visitPackageVariableStatement(context: PackageVariableStatementContext): any {
        val r1 = visit(context.id()) as Result
        val isMutable = r1.isVirtual
        var typ = ""
        var r2: Result? = null
        if (context.expression() != null) {
            r2 = visit(context.expression()) as Result
            typ = r2.data as str
        }
        if (context.typeType() != null) {
            typ = visit(context.typeType()) as str
        }
        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }

        obj += "${r1.permission} var ${r1.text}:$typ"
        obj += if (r2 != null) {
            " = ${r2.text}$Wrap"
        } else {
            Wrap
        }
        return obj
    }

    override fun visitPackageFunctionStatement(context: PackageFunctionStatementContext): any {
        val id = visit(context.id()) as Result
        val isVirtual = ""
        var obj = ""
        // # 泛型 #
        var templateContract = ""
        if (context.templateDefine() != null) {
            val template = visit(context.templateDefine()) as TemplateItem
            obj += template.Template
            templateContract = template.Contract
        }
        // # 异步 #
        var pout = visit(context.parameterClauseOut()) as str
        if (context.t.type == Right_Flow) {
            pout = if (pout != "Unit") {
                pout
            } else {
                "Unit"
            }
            obj += "${id.permission} $isVirtual fun $templateContract async ${id.text} "
        } else {
//            if (context.y != null) {
//                if (pout != "Unit") {
//                    pout = "" IEnum "<" pout ">"
//                }
//            }
            obj += " ${id.permission} $isVirtual fun $templateContract ${id.text} "
        }

        add_current_set()
        obj += visit(context.parameterClauseIn()).to<str>() + ":" + pout + BlockLeft + Wrap
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight + Wrap
        this.superID = ""
        return obj
    }

    override fun visitPackageNewStatement(context: PackageNewStatementContext) = run {
        var obj = ""
        obj += "public constructor"
        // # 获取构造数据 #
        add_current_set()
        obj += visit(context.parameterClauseIn()) as str
        if (context.expressionList() != null) {
            obj += ":this(${visit(context.expressionList()).to<Result>().text})"
        }
        obj += BlockLeft + ProcessFunctionSupport(context.functionSupportStatement()) + BlockRight + Wrap
        delete_current_set()
        obj
    }

    override fun visitPackageEventStatement(context: PackageEventStatementContext) = run {
        var obj = ""
        val id = visit(context.id()) as Result
        val nameSpace = visit(context.nameSpaceItem())
        obj += "public event $nameSpace ${id.text} $Wrap"
        obj
    }

    override fun visitProtocolStatement(context: ProtocolStatementContext) = run {
        val id = visit(context.id()) as Result
        var obj = ""
        val extend = mutableListOf<str>()
        var interfaceProtocol = ""
        var ptclName = id.text
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }
        if (context.protocolSubStatement() != null) {
            val item = context.protocolSubStatement()
            val r = visit(item) as str
            interfaceProtocol += r
        }
        obj += "public interface $ptclName"
        // # 泛型 #
        var templateContract = ""
        if (context.templateDefine() != null) {
            val template = visit(context.templateDefine()) as TemplateItem
            obj += template.Template
            templateContract = template.Contract
        }
        if (extend.count() > 0) {
            var temp = extend[0]
            for (i in 1..extend.count() - 1) {
                temp += "," + extend[i]
            }
            obj += ":" + temp
        }
        obj += templateContract + BlockLeft + Wrap
        obj += interfaceProtocol
        obj += BlockRight + Wrap
        obj
    }

    override fun visitProtocolSubStatement(context: ProtocolSubStatementContext): any {
        var obj = ""
        for (item in context.protocolSupportStatement()) {
            obj += (visit(item) as Result).text
        }
        return obj
    }

    override fun visitProtocolVariableStatement(context: ProtocolVariableStatementContext): any {
        var id = visit(context.id()) as Result
        var isMutable = id.isVirtual
        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }
        val type = visit(context.typeType()) as str
        if (context.Bang() == null) {
            obj += "val "
        } else {
            obj += "var "
        }
        obj += id.text + ": " + type + Wrap
        return Result().apply { text = obj }
    }

    override fun visitProtocolFunctionStatement(context: ProtocolFunctionStatementContext) = Result().apply {
        val id = visit(context.id()) as Result
        if (context.annotationSupport() != null) {
            text += visit(context.annotationSupport())
        }
        permission = "public"
        // # 异步 #
        var pout = visit(context.parameterClauseOut()) as str
//        if (context.t.type == Right_Flow) {
//            if (pout != "Unit") {
//                pout = "" Task "<" pout ">"
//            } else {
//                pout = Task
//            }
//        } else {
//            if (context.y != null) {
//                if (pout != "Unit") {
//                    pout = "" IEnum "<" pout ">"
//                }
//            }
//        }
        text += "fun "
        // # 泛型 #
        var templateContract = ""
        if (context.templateDefine() != null) {
            val template = visit(context.templateDefine()) as TemplateItem
            text += template.Template
            templateContract = template.Contract
        }
        text += id.text + visit(context.parameterClauseIn()).to<str>() + ":" + pout + templateContract + Wrap
    }

}