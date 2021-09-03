package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

open class LogicVisitor() : FunctionVisitor() {
    // logic -----------------------------

    override fun visitIteratorStatement(context: IteratorStatementContext) = Iterator().apply {
        if (context.Dot_Dot_Dot() != null || context.Dot_Dot_Greater() != null) {
            order = F
        }
        if (context.Dot_Dot_Less() != null || context.Dot_Dot_Greater() != null) {
            close = F
        }
        if (context.expression().size == 2) {
            begin = visit(context.expression(0)) as Result
            end = visit(context.expression(1)) as Result
            step = Result().apply {
                data = I32
                text = "1"
            }
        } else {
            begin = visit(context.expression(0)) as Result
            end = visit(context.expression(1)) as Result
            step = visit(context.expression(2)) as Result
        }
    }

    override fun visitLoopStatement(context: LoopStatementContext) = run {
        var obj = ""
        val id = visit(context.id()).to<Result>().text

        val it = visit(context.iteratorStatement()) as Iterator
        val target = if (it.order == "true") {
            when {
                it.close == "true" -> "${it.begin.text}..${it.end.text} step ${it.step.text}"
                else -> "${it.begin.text} until ${it.end.text} step ${it.step.text}"
            }
        } else {
            when {
                it.close == "true" -> "${it.begin.text} downTo ${it.end.text} step ${it.step.text}"
                else -> "${it.begin.text} downTo ${it.end.text} step ${it.step.text}"
            }
        }

        obj += "for ($id in $target)"

        obj += BlockLeft + Wrap
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight + Wrap
        if (context.loopElseStatement() != null) {
            val elseContent = visit(context.loopElseStatement()) as str
            obj = "if (!can_range( $target ))  $elseContent else" + BlockLeft +
                    Wrap + obj + BlockRight + Wrap
        }
        obj
    }

    override fun visitLoopEachStatement(context: LoopEachStatementContext) = run {
        var obj = ""
        val arr = visit(context.expression()) as Result
        val target = arr.text
        val id = when (context.id().size) {
            2 -> "(${visit(context.id(0)).to<Result>().text}, ${visit(context.id(1)).to<Result>().text})"
            else -> visit(context.id(0)).to<Result>().text
        }

        obj += "for ($id in $target)"
        obj += BlockLeft + Wrap
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight + Wrap
        if (context.loopElseStatement() != null) {
            val elseContent = visit(context.loopElseStatement()) as str
            obj = "if (!can_range( $target ))  $elseContent else" + BlockLeft +
                    Wrap + obj + BlockRight + Wrap
        }
        obj
    }

    override fun visitLoopCaseStatement(context: LoopCaseStatementContext) = run {
        var obj = ""
        val expr = visit(context.expression()) as Result
        obj += "while (${expr.text})"
        obj += BlockLeft + Wrap
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight + Wrap
        if (context.loopElseStatement() != null) {
            val elseContent = visit(context.loopElseStatement()) as str
            obj = "if (! ${expr.text} )  $elseContent else" + BlockLeft +
                    Wrap + obj + BlockRight + Wrap
        }
        obj
    }

    override fun visitLoopElseStatement(context: LoopElseStatementContext) = run {
        var obj = BlockLeft + Wrap
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight + Wrap
        obj
    }

    override fun visitLoopJumpStatement(context: LoopJumpStatementContext) = "break$Wrap"

    override fun visitLoopContinueStatement(context: LoopContinueStatementContext) = "continue$Wrap"

    override fun visitJudgeCaseStatement(context: JudgeCaseStatementContext) = run {
        var obj = ""
        val expr = visit(context.expression()) as Result
        obj += "when (${expr.text}) $BlockLeft$Wrap"
        for (item in context.caseStatement()) {
            val r = visit(item) as str
            obj += r + Wrap
        }
        obj += BlockRight + Wrap
        obj
    }

    override fun visitCaseExprStatement(context: CaseExprStatementContext) =
        when {
            context.expression() != null -> {
                val expr = visit(context.expression()) as Result
                "${expr.text} -> $Wrap"
            }
            context.typeType() != null -> {
                var id = "it"
                if (context.id() != null) {
                    id = visit(context.id()).to<Result>().text
                }
                add_id(id)
                val type = visit(context.typeType()) as str
                "is $type ->$Wrap"
            }
            else -> {
                "else ->$Wrap"
            }
        }


    override fun visitCaseStatement(context: CaseStatementContext) = run {
        var obj = ""
        for (item in context.caseExprStatement()) {
            val r = visit(item) as str
            add_current_set()
            val process =
                "$BlockLeft${ProcessFunctionSupport(context.functionSupportStatement())}$BlockRight"
            delete_current_set()
            obj += r + process
        }
        obj
    }

    override fun visitJudgeStatement(context: JudgeStatementContext) = run {
        var obj = ""
        obj += visit(context.judgeIfStatement())
        for (it in context.judgeElseIfStatement()) {
            obj += visit(it)
        }
        if (context.judgeElseStatement() != null) {
            obj += visit(context.judgeElseStatement())
        }
        obj
    }

    override fun visitJudgeIfStatement(context: JudgeIfStatementContext) = run {
        val b = visit(context.expression()) as Result
        var obj = "if (${b.text}) $BlockLeft$Wrap"
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += "$BlockRight$Wrap"
        obj
    }

    override fun visitJudgeElseIfStatement(context: JudgeElseIfStatementContext) = run {
        val b = visit(context.expression()) as Result
        var obj = "else if (${b.text}) $BlockLeft$Wrap"
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += "$BlockRight$Wrap"
        obj
    }

    override fun visitJudgeElseStatement(context: JudgeElseStatementContext) = run {
        var obj = "else $BlockLeft$Wrap"
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += "$BlockRight$Wrap"
        obj
    }

    override fun visitCheckStatement(context: CheckStatementContext) = run {
        var obj = "try $BlockLeft$Wrap"
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += "$BlockRight$Wrap"
        for (item in context.checkErrorStatement()) {
            obj += "${visit(item)}$Wrap"
        }
        if (context.checkFinallyStatment() != null) {
            obj += visit(context.checkFinallyStatment())
        }
        obj
    }

    override fun visitCheckErrorStatement(context: CheckErrorStatementContext) = run {
        add_current_set()
        var obj = ""
        val id = visit(context.id()).to<Result>().text
        add_id(id)
        val Type = if (context.typeType() != null) {
            visit(context.typeType()) as str
        } else {
            "Exception"
        }
        obj += "catch($id:$Type)$BlockLeft$Wrap"
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight
        obj
    }

    override fun visitCheckFinallyStatment(context: CheckFinallyStatmentContext) = run {
        var obj = "finally $BlockLeft$Wrap"
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += "$BlockRight$Wrap"
        obj
    }

    override fun visitUsingStatement(context: UsingStatementContext) :any {
        var obj = ""
        var r1 = ""
        for (v in context.constId()) {
            r1 = visit(v) as str
        }
        val r2 = visit(context.tupleExpression()) as Result
        obj = "${r2.text}.use$BlockLeft ${r1} -> "
        add_current_set()
        obj += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        obj += BlockRight
        return obj
    }

    override fun visitLinq(context: LinqContext) = Result().apply {
        data = "var"
        text += "from ${visit(context.linqHeadItem()) as str} "
        for (item in context.linqItem()) {
            text += " ${visit(item)}"
        }
        text += "${(visit(context.id()) as Result).text} ${visit(context.expression()).to<Result>().text}"
    }

    override fun visitLinqItem(context: LinqItemContext) :any {
        if (context.linqHeadItem() != null) {
            return visit(context.linqHeadItem()) as str
        }
        var obj = (visit(context.id() )as Result).text
        if (context.expression() != null) {
            obj += " ${visit(context.expression()).to<Result>().text}"
        }
        return obj
    }

    override fun visitLinqHeadItem(context: LinqHeadItemContext): any {
        var obj = ""
        var id = visit(context.id()) as Result
        obj += "from ${id.text} in ${(visit(context.expression()) as Result).text} "
        return obj
    }
}
