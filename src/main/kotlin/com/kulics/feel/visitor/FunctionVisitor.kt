package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

open class FunctionVisitor() : ExpressionVisitor() {
// function -----------------------------

    override fun visitFunctionStatement(context: FunctionStatementContext) = run {
        val id = visit(context.id()) as Result
        var obj = ""
        //# 泛型 #
        var templateContract = ""
        if (context.templateDefine() != null) {
            val template = visit(context.templateDefine()) as TemplateItem
            obj += template.Template
            templateContract = template.Contract
        }
        var pout = visit(context.parameterClauseOut()) as str
        // # 异步 #
        if (context.t.type == Right_Flow) {
            pout = if (pout != "Unit") {
                pout
            } else {
                "Unit"
            }
            obj += " ${id.permission} $templateContract async ${id.text} "
        } else {
//            ? context.y > < nil {
//                ? pout > < "void" {
//                pout = "" IEnum "<" pout ">"
//            }
//            }
            obj += " ${id.permission} $templateContract ${id.text} "
        }
        add_current_set()
        obj += visit(context.parameterClauseIn()).to<str>() + ":" + pout + Wrap + BlockLeft + Wrap
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight + Wrap
        obj
    }

    override fun visitReturnStatement(context: ReturnStatementContext) =
        if (context.tupleExpression() != null) {
            "return ${visit(context.tupleExpression()).to<Result>().text}$Wrap"
        } else {
            "return$Wrap"
        }

    override fun visitYieldReturnStatement(context: YieldReturnStatementContext) =
        "yield return ${visit(context.tupleExpression()).to<Result>().text}$Wrap"

    override fun visitYieldBreakStatement(context: YieldBreakStatementContext) =
        "yield break$Wrap"

    override fun visitTuple(context: TupleContext) = Result().apply {
        data = "var"
        text = "("
        for (i in 0 until context.expression().size) {
            val r = visit(context.expression(i)) as Result
            text += if (i == 0) {
                r.text
            } else {
                ", ${r.text}"
            }
        }
        text += ")"
    }

    override fun visitTupleExpression(context: TupleExpressionContext) = Result().apply {
        data = "var"
        for (i in 0 until context.expression().size) {
            val r = visit(context.expression(i)) as Result
            text += if (i == 0) {
                r.text
            } else {
                ", ${r.text}"
            }
        }
        if (context.expression().size > 1) {
            text = "($text)"
        }
    }

    override fun visitParameterClauseIn(context: ParameterClauseInContext) = run {
        var obj = "("
        val temp = mutableListOf<str>()
        for (i in context.parameter().size - 1 downTo 0) {
            val p = visit(context.parameter(i)) as Parameter
            temp.add("${p.annotation} ${p.id}:${p.type} ${p.value}")
            add_id(p.id)
        }
        for (i in temp.size - 1 downTo 0) {
            obj += if (i == temp.size - 1) {
                temp[i]
            } else {
                ",  ${temp[i]}"
            }
        }

        obj += ")"
        obj
    }

    override fun visitParameterClauseOut(context: ParameterClauseOutContext) = run {
        var obj = ""
        if (context.parameter().size == 0) {
            obj += "Unit"
        } else if (context.parameter().size == 1) {
            val p = visit(context.parameter(0)) as Parameter
            obj += p.type
        }
        if (context.parameter().size > 1) {
            obj += "( "
            val temp = mutableListOf<str>()
            for (i in context.parameter().size - 1 downTo 0) {
                val p = visit(context.parameter(i)) as Parameter
                temp.add(p.type)
            }
            for (i in temp.size - 1 downTo 0) {
                obj += if (i == temp.size - 1) {
                    temp[i]
                } else {
                    ", ${temp[i]}"
                }
            }
            obj += " )"
        }
        obj
    }

    override fun visitParameter(context: ParameterContext) = Parameter().apply {
        val id = visit(context.id()) as Result
        this.id = id.text
        permission = id.permission
        if (context.annotationSupport() != null) {
            annotation = visit(context.annotationSupport()) as str
        }
        if (context.expression() != null) {
            value = "=" + visit(context.expression()).to<Result>().text
        }
        type = visit(context.typeType()) as str
    }

}