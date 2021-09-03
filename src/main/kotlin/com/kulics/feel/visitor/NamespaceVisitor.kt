package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

open class NamespaceVisitor() : LogicVisitor() {
    // namespace ----------------------------

    override fun visitStatement(context: StatementContext) = run {
        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }
        val ns = visit(context.exportStatement()) as Namespace
        obj += "package ${ns.name + Wrap}"

        var content = ""
        add_current_set()
        for (item in context.namespaceSupportStatement()) {
            content += when (val v = visit(item)) {
                is str -> v
                else -> ""
            }
        }
        obj += content
        delete_current_set()
        obj
    }

    override fun visitExportStatement(context: ExportStatementContext) = run {
        var name = visit(context.nameSpaceItem()) as str
        val obj = Namespace().apply {
            this.name = name
        }
        obj
    }

    override fun visitImportStatement(context: ImportStatementContext) : any {
        var obj = ""
        for (item in context.importSubStatement()) {
            obj += visit(item) as str
        }
        return obj
    }

    override fun visitImportSubStatement(context: ImportSubStatementContext) = run {
        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }
        var ns = visit(context.nameSpaceItem()) as str
        obj += when {
            context.Discard() != null -> "import ${visit(context.id()).to<Result>().text}.$ns"
            context.id() != null -> "import ${visit(context.id()).to<Result>().text} as $ns"
            else -> "import $ns"
        }
        obj += Wrap
        obj
    }

    override fun visitNameSpaceItem(context: NameSpaceItemContext) = run {
        var obj = ""
        for (i in 0 until context.id().size) {
            val id = visit(context.id(i)) as Result
            obj += if (i == 0) {
                id.text
            } else {
                ".${id.text}"
            }
        }
        obj
    }

    override fun visitName(context: NameContext) = run {
        var obj = ""
        for (i in 0 until context.id().size) {
            val id = visit(context.id(i)) as Result
            obj += if (i == 0) {
                id.text
            } else {
                ".${id.text}"
            }
        }
        obj
    }

    override fun visitEnumStatement(context: EnumStatementContext) = run {
        var obj = ""
        val id = visit(context.id()) as Result
        var header = ""
//        val typ = visit(context.typeType()) as str
        val typ = "Int"
        if (context.annotationSupport() != null) {
            header += visit(context.annotationSupport())
        }
        header += id.permission + " enum " + id.text + ":" + typ
        header += Wrap + BlockLeft + Wrap
        for (i in 0 until context.enumSupportStatement().size) {
            obj += visit(context.enumSupportStatement(i))
        }
        obj += BlockRight + Wrap
        obj = header + obj
        obj
    }

    override fun visitEnumSupportStatement(context: EnumSupportStatementContext) = run {
        val id = visit(context.id()) as Result
        if (context.integerExpr() != null) {
            var op = ""
            if (context.add() != null) {
                op = visit(context.add()) as str
            }
            id.text += " = $op ${visit(context.integerExpr())}"
        }
        id.text + ","
    }

    override fun visitNamespaceFunctionStatement(context: NamespaceFunctionStatementContext) = run {
        val id = visit(context.id()) as Result
        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }
        //# 泛型 #
        var templateContract = ""
        if (context.templateDefine() != null) {
            val template = visit(context.templateDefine()) as TemplateItem
            obj += template.Template
            templateContract = template.Contract
        }
        //# 异步 #
        var pout = visit(context.parameterClauseOut()) as str
        if (context.t.type == Right_Flow) {
            pout = if (pout != "Unit") {
                pout
            } else {
                "Unit"
            }
            obj += " ${id.permission} fun $templateContract async ${id.text} "
        } else {
//            ? context.y > < nil {
//                ? pout > < "void" {
//                pout = "" IEnum "<" pout ">"
//            }
//            }
            obj += " ${id.permission} fun $templateContract ${id.text} "
        }
        add_current_set()
        obj += visit(context.parameterClauseIn()).to<str>() + ":" + pout + BlockLeft + Wrap
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight + Wrap
        obj
    }

    override fun visitNamespaceConstantStatement(context: NamespaceConstantStatementContext) = run {
        val id = visit(context.id()) as Result
        val expr = visit(context.expression()) as Result
        var typ = ""
        typ = if (context.typeType() != null) {
            visit(context.typeType()) as str
        } else {
            expr.data as str
        }

        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }

        obj += "${id.permission} const val ${id.text}: $typ  =  ${expr.text} $Wrap"
        obj
    }

    override fun visitNamespaceVariableStatement(context: NamespaceVariableStatementContext) = run {
        val r1 = visit(context.id()) as Result
        add_id(r1.text)
        var isMutable = r1.isVirtual
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

        obj += "${r1.permission} var ${r1.text}:$typ "
        obj += if (r2 != null) {
            " = " + r2.text + Wrap
        } else {
            Wrap
        }
        obj
    }
}