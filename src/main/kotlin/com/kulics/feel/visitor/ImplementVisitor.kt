package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

open class ImplementVisitor() : PackageVisitor() {
    // implement ---------------------------

//    override fun visitImplementStatement(context: ImplementStatementContext) = run {
//        val Self = visit(context.parameterClauseSelf()) as Parameter
//        selfID = Self.id
//        val isVirtual = ""
//        var obj = ""
//        var extends = ""
//        if (context.typeType() != null) {
//            extends += ":" + visit(context.typeType())
//        }
//        obj += "${Self.permission} class ${Self.type} $extends$BlockLeft$Wrap"
//        for (item in context.implementSupportStatement()) {
//            obj += visit(item)
//        }
//        obj += BlockRight + Wrap
//        selfID = ""
//        obj
//    }

    override fun visitOverrideFunctionStatement(context: OverrideFunctionStatementContext) = run {
        val id = visit(context.id()) as Result
        val isVirtual = "override"
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
        obj
    }

    override fun visitOverrideVariableStatement(context: OverrideVariableStatementContext) :any {
        val r1 = visit(context.id()) as Result
        val isMutable = true // # r1.isVirtual #
        val isVirtual = "override"
        var typ = ""
        typ = visit(context.typeType()) as str
        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }
        obj += "${r1.permission} $isVirtual var ${r1.text}:$typ$Wrap"
        if (context.expression() != null) {
            val expr = visit(context.expression()) as Result
            obj += " = ${expr.text}"
        }
        obj += Wrap
        this.superID = ""
        return obj
    }

    override fun visitOverrideConstantStatement(context: OverrideConstantStatementContext) : any {
        val r1 = visit(context.id()) as Result
        val isMutable = true // # r1.isVirtual #
        val isVirtual = "override"
        var typ = ""
        typ = visit(context.typeType()) as str
        var obj = ""
        if (context.annotationSupport() != null) {
            obj += visit(context.annotationSupport())
        }
        obj += "${r1.permission} $isVirtual val ${r1.text}:$typ$Wrap"
        if (context.expression() != null) {
            val expr = visit(context.expression()) as Result
            obj += " = ${expr.text}"
        }
        obj += Wrap
        this.superID = ""
        return obj
    }
}