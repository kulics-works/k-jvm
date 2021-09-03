package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

open class ExpressionVisitor() : BaseVisitor() {
    // expression ---------------------
    override fun visitVarStatement(context: VarStatementContext): any {
        var obj = ""
        for ((i, v) in context.varId().withIndex()) {
            if (i != 0) {
                obj += "," + visit(v)
            } else {
                obj += visit(v)
            }
        }
        if (context.varId().count() > 1) {
            obj = "(" + obj + ")"
        }
        var r2 = visit(context.tupleExpression()) as Result
        obj += " = ${r2.text + Wrap}"
        return obj
    }

    override fun visitBindStatement(context: BindStatementContext): any {
        var obj = ""
        for ((i, v) in context.constId().withIndex()) {
            if (i != 0) {
                obj += "," + visit(v)
            } else {
                obj += visit(v)
            }
        }
        if (context.constId().count() > 1) {
            obj = "(" + obj + ")"
        }
        var r2 = visit(context.tupleExpression()) as Result
        obj += " = ${r2.text + Wrap}"
        return obj
    }

    override fun visitVariableDeclaredStatement(context: VariableDeclaredStatementContext) = run {
        val Type = visit(context.typeType()) as str
        val r = visit(context.id()) as Result
        "var ${r.text}:$Type$Wrap"
    }

    override fun visitConstantDeclaredStatement(context: ConstantDeclaredStatementContext) = run {
        val Type = visit(context.typeType()) as str
        val r = visit(context.id()) as Result
        "val ${r.text}:$Type$Wrap"
    }

    override fun visitAssignStatement(context: AssignStatementContext) = run {
        val r1 = visit(context.tupleExpression(0)) as Result
        val r2 = visit(context.tupleExpression(1)) as Result
        r1.text + visit(context.assign()) + r2.text + Wrap
    }

    override fun visitAssign(context: AssignContext) = context.op.text

    override fun visitExpressionStatement(context: ExpressionStatementContext) =
        visit(context.expression()).to<Result>().text + Wrap

    override fun visitExpression(context: ExpressionContext) = Result().apply {
        when (context.childCount) {
            3 -> {
                val e1 = visit(context.getChild(0)) as Result
                val e2 = visit(context.getChild(2))
                var op = visit(context.getChild(1))

                when (context.getChild(1)) {
                    is CompareContext -> {
                        // # todo 如果左右不是bool类型值 ， 报错 #
                        data = Bool
                    }
                    is LogicContext -> {
                        // # todo 如果左右不是bool类型值 ， 报错 #
                        data = Bool
                    }
                    is AddContext -> {
                        // # todo 如果左右不是number或text类型值 ， 报错 #
                        if (e1.data as str == Str || e2.to<Result>().data.to<str>() == Str) {
                            data = Str
                        } else if (e1.data as str == I32 && e2.to<Result>().data.to<str>() == I32) {
                            data = I32
                        } else {
                            data = F64
                        }
                    }
                    is MulContext -> {
                        // # todo 如果左右不是number类型值 ， 报错 #
                        if (e1.data as str == I32 && e2.to<Result>().data.to<str>() == I32) {
                            data = I32
                        } else {
                            data = F64
                        }
                    }
                    is PowContext -> {
                        // # todo 如果左右部署number类型 ， 报错 #
                        data = F64
                        op = when (op) {
                            "**" -> "pow"
                            "//" -> "root"
                            "%%" -> "log"
                            else -> op
                        }
                        text = "$op (${e1.text}, ${e2.to<Result>().text})"
                    }
                }
                text = e1.text + op + e2.to<Result>().text
            }
            2 -> {
                val r = visit(context.getChild(0)) as Result
                when (context.getChild(1)) {
                    is TypeConversionContext -> {
                        val e2 = visit(context.getChild(1)) as str
                        r.data = e2
                        r.text = "(${r.text} as $e2)"
                    }
                    is CallExpressionContext -> {
                        val e2 = visit(context.getChild(1)) as Result
                        r.text = r.text + e2.text
                    }
                    is CallFuncContext -> {
                        val e2 = visit(context.getChild(1)) as Result
                        r.text = r.text + e2.text
                    }
                    is CallElementContext -> {
                        val e2 = visit(context.getChild(1)) as Result
                        r.text = r.text + e2.text
                    }
                    else -> {
                        if (context.op.type == Bang) {
                            text = "ref $text"
                        } else if (context.op.type == Question) {
                            text += "?"
                        }
                        return@apply
                    }
                }
                return r
            }
            1 -> {
                return visit(context.getChild(0)) as Result
            }
            else -> {
            }
        }
    }

    override fun visitCallExpression(context: CallExpressionContext) = visit(context.id()).to<Result>().apply {
        text = ".$text"
        if (context.templateCall() != null) {
            text += visit(context.templateCall()) as str
        }
        when {
            context.callFunc() != null -> {
                val e2 = visit(context.callFunc()) as Result
                text += e2.text
            }
            context.callElement() != null -> {
                val e2 = visit(context.callElement()) as Result
                text += e2.text
            }
        }
    }

    override fun visitTypeConversion(context: TypeConversionContext) =
        visit(context.typeType()) as str

    override fun visitCall(context: CallContext) = context.op.text

    override fun visitWave(context: WaveContext) = context.op.text

    override fun visitBitwise(context: BitwiseContext) = visit(context.getChild(0)) as str

    override fun visitBitwiseAnd(context: BitwiseAndContext) = "&"

    override fun visitBitwiseOr(context: BitwiseOrContext) = "|"

    override fun visitBitwiseXor(context: BitwiseXorContext) = "^"

    override fun visitBitwiseLeftShift(context: BitwiseLeftShiftContext) = "<<"

    override fun visitBitwiseRightShift(context: BitwiseRightShiftContext) = ">>"


    override fun visitCompare(context: CompareContext): any {
        return when (context.op.type) {
            Not_Equal -> "!="
            else -> context.op.text
        }
    }

    override fun visitLogic(context: LogicContext): any {
        return when (context.op.type) {
            And -> "&&"
            Or -> "||"
            else -> context.op.text
        }
    }

    override fun visitAdd(context: AddContext) = context.op.text

    override fun visitMul(context: MulContext) = context.op.text

    override fun visitPow(context: PowContext) = "^"

    override fun visitPrimaryExpression(context: PrimaryExpressionContext): any {
        if (context.childCount == 1) {
            val c = context.getChild(0)
            when {
                c is DataStatementContext -> return visit(context.dataStatement())
                c is IdContext -> return visit(context.id())
                context.t.type == Discard -> return Result().apply {
                    text = "_"
                    data = "var"
                }
            }
        } else if (context.childCount == 2) {
            val id = visit(context.id()) as Result
            val template = visit(context.templateCall()) as str
            return Result().apply {
                text = id.text + template
                data = id.text + template
            }
        }
        val r = visit(context.expression()) as Result
        return Result().apply {
            text = "(" + r.text + ")"
            data = r.data
        }
    }

    override fun visitExpressionList(context: ExpressionListContext) = Result().apply {
        var obj = ""
        for (i in 0 until context.expression().size) {
            val temp = visit(context.expression(i)) as Result
            obj += if (i == 0) {
                temp.text
            } else {
                ", ${temp.text}"
            }
        }
        text = obj
        data = "var"
    }

    override fun visitTemplateDefine(context: TemplateDefineContext) = TemplateItem().apply {
        Template += "<"
        for (i in 0 until context.templateDefineItem().size) {
            if (i > 0) {
                Template += ","
                if (Contract.isNotEmpty()) {
                    Contract += ","
                }
            }
            val r = visit(context.templateDefineItem(i)) as TemplateItem
            Template += r.Template
            Contract += r.Contract
        }
        Template += ">"
    }

    override fun visitTemplateDefineItem(context: TemplateDefineItemContext) = TemplateItem().apply {
        if (context.id().size == 1) {
            val id1 = context.id(0).text
            Template = id1
        } else {
            val id1 = context.id(0).text
            Template = id1
            val id2 = context.id(1).text
            Contract = " where $id1 : $id2 "
        }
    }

    override fun visitTemplateCall(context: TemplateCallContext) = run {
        var obj = ""
        obj += "<"
        for (i in 0 until context.typeType().size) {
            if (i > 0) {
                obj += ","
            }
            val r = visit(context.typeType(i))
            obj += r
        }
        obj += ">"
        obj
    }

    override fun visitCallElement(context: CallElementContext) =
        if (context.expression() == null) {
            Result().apply { text = visit(context.slice()) as str }
        } else {
            val r = visit(context.expression()) as Result
            r.text = "[${r.text}]"
            r
        }

    override fun visitSlice(context: SliceContext) = visit(context.getChild(0)) as str

    override fun visitSliceFull(context: SliceFullContext) = run {
        var order = ""
        var attach = ""
        val expr1 = visit(context.expression(0)) as Result
        val expr2 = visit(context.expression(1)) as Result
        ".slice(${expr1.text}..${expr2.text})"
    }

    override fun visitSliceStart(context: SliceStartContext) = run {
        var order = ""
        var attach = ""
        val expr = visit(context.expression()) as Result
        ".slice(${expr.text}..)"
    }

    override fun visitSliceEnd(context: SliceEndContext) = run {
        var order = ""
        var attach = "false"
        val expr = visit(context.expression()) as Result
        ".slice(..${expr.text})"
    }

    override fun visitCallFunc(context: CallFuncContext) = Result().apply {
        data = "var"
        text += if (context.tuple() != null) {
            visit(context.tuple()).to<Result>().text
        } else {
            "(${visit(context.lambda()).to<Result>().text})"
        }
    }

    override fun visitCallPkg(context: CallPkgContext) = Result().apply {
        data = visit(context.typeType())
        text = "${visit(context.typeType())}()"
        text += when {
            context.pkgAssign() != null -> visit(context.pkgAssign())
            context.listAssign() != null -> visit(context.listAssign())
            context.dictionaryAssign() != null -> visit(context.dictionaryAssign())
            else -> ""
        }
    }

    override fun visitCallNew(context: CallNewContext) = Result().apply {
        data = visit(context.typeType())
        var param = ""
        if (context.expressionList() != null) {
            param = visit(context.expressionList()).to<Result>().text
        }
        text = "${visit(context.typeType())}($param)"
    }

    override fun visitPkgAssign(context: PkgAssignContext) = run {
        var obj = ""
        obj += "{"
        for (i in 0 until context.pkgAssignElement().size) {
            if (i == 0) {
                obj += visit(context.pkgAssignElement(i))
            } else {
                obj += ", ${visit(context.pkgAssignElement(i))}"
            }
        }
        obj += "}"
        obj
    }

    override fun visitListAssign(context: ListAssignContext) = run {
        var obj = ""
        obj += "{"
        for (i in 0 until context.expression().size) {
            val r = visit(context.expression(i)) as Result
            obj += if (i == 0) {
                r.text
            } else {
                ", ${r.text}"
            }
        }
        obj += "}"
        obj
    }

    override fun visitDictionaryAssign(context: DictionaryAssignContext) = run {
        var obj = ""
        obj += "{"
        for (i in 0 until context.dictionaryElement().size) {
            val r = visit(context.dictionaryElement(i)) as DicEle
            obj += if (i == 0) {
                r.text
            } else {
                ", ${r.text}"
            }
        }
        obj += "}"
        obj
    }

    override fun visitPkgAssignElement(context: PkgAssignElementContext) =
        visit(context.name()).to<str>() + " = " + visit(context.expression()).to<Result>().text

    override fun visitPkgAnonymous(context: PkgAnonymousContext) = Result().apply {
        data = "var"
        text = "new" + visit(context.pkgAnonymousAssign()) as str
    }

    override fun visitPkgAnonymousAssign(context: PkgAnonymousAssignContext) = run {
        var obj = ""
        obj += "{"
        for (i in 0 until context.pkgAnonymousAssignElement().size) {
            if (i == 0) {
                obj += visit(context.pkgAnonymousAssignElement(i))
            } else {
                obj += ", ${visit(context.pkgAnonymousAssignElement(i))}"
            }
        }
        obj += "}"
        obj
    }

    override fun visitPkgAnonymousAssignElement(context: PkgAnonymousAssignElementContext) =
        visit(context.name()).to<str>() + " = " + visit(context.expression()).to<Result>().text

    override fun visitCallAwait(context: CallAwaitContext) = Result().apply {
//        val expr = visit(context.expression()) as Result
//        data = "var"
//        text = "await " + expr.text
    }

    override fun visitList(context: ListContext) = Result().apply {
        var type = Any
        for (i in 0 until context.expression().size) {
            val r = visit(context.expression(i)) as Result
            if (i == 0) {
                type = r.data as str
                text += r.text
            } else {
                if (type != r.data as str) {
                    type = Any
                }
                text += "," + r.text
            }
        }
        data = "$Lst<$type>"
        text = "mutableListOf($text)"
    }

    override fun visitDictionary(context: DictionaryContext) = Result().apply {
        var key = Any
        var value = Any
        for (i in 0 until context.dictionaryElement().size) {
            val r = visit(context.dictionaryElement(i)) as DicEle
            if (i == 0) {
                key = r.key
                value = r.value
                text += r.text
            } else {
                if (key != r.key) {
                    key = Any
                }
                if (value != r.value) {
                    value = Any
                }
                text += "," + r.text
            }
        }
        val type = "$key,$value"
        data = "$Dic<$type>"
        text = "mutableMapOf($text)"
    }

    override fun visitDictionaryElement(context: DictionaryElementContext) = DicEle().apply {
        val r1 = visit(context.expression(0)) as Result
        val r2 = visit(context.expression(1)) as Result
        key = r1.data as str
        value = r2.data as str
        text = "${r1.text} to ${r2.text}"
    }

    override fun visitStringExpr(context: StringExprContext): any {
        var text = ""
        if (context.stringTemplate().count() == 0) {
            for (v in context.stringContent()) {
                text += visit(v) as str
            }
            return "\"${text}\""
        } else {
            text = "(new System.Text.StringBuilder()"
            // 去除前后一个元素
            for (i in 1..context.childCount - 2) {
                var v = context.getChild(i)
                var r = visit(context.getChild(i)) as str
                if (v is StringContentContext) {
                    text += ".Append(\"${r}\")"
                } else {
                    text += r
                }
            }
            text += ").to_str()"
            return text
        }
    }

    override fun visitStringContent(context: StringContentContext): any {
        if (context.TextLiteral().text == "\\$") {
            return "$"
        }
        return context.TextLiteral().text
    }

    override fun visitStringTemplate(context: StringTemplateContext): any {
        var text =  ""
        for (v in  context.expression()) {
            var r = visit(v) as Result
            text += ".Append(${r.text})"
        }
        return text
    }

    override fun visitDataStatement(context: DataStatementContext) = Result().apply {
        when {
            context.nilExpr() != null -> {
                data = Any
                text = "null"
            }
            context.floatExpr() != null -> {
                data = F64
                text = visit(context.floatExpr()) as str
            }
            context.integerExpr() != null -> {
                data = I32
                text = visit(context.integerExpr()) as str
            }
            context.stringExpr() != null -> {
                data = Str
                text = visit(context.stringExpr()) as str
            }
            context.t.type == CharLiteral -> {
                data = Chr
                text = context.CharLiteral().text
            }
            context.t.type == TrueLiteral -> {
                data = Bool
                text = T
            }
            context.t.type == FalseLiteral -> {
                data = Bool
                text = F
            }
        }
    }

    override fun visitFloatExpr(context: FloatExprContext):any {
        var number = context.FloatLiteral().text
        return number
    }

    override fun visitIntegerExpr(context: IntegerExprContext) = context.getChild(0).text

    override fun visitFunctionExpression(context: FunctionExpressionContext) = Result().apply {
        // # 异步 #
        if (context.t.type == Right_Flow) {
            text += " async "
        }
        text += "fun ${visit(context.parameterClauseIn())}:${visit(context.parameterClauseOut())} $BlockLeft$Wrap"
        add_current_set()
        text += ProcessFunctionSupport(context.functionSupportStatement())
        delete_current_set()
        text += BlockRight + Wrap
        data = "var"
    }

    override fun visitLambda(context: LambdaContext) = Result().apply {
        data = "var"
        // # 异步 #
        if (context.t.type == Right_Flow) {
            text += "async "
        }
        text += BlockLeft
        if (context.lambdaIn() != null) {
            text += visit(context.lambdaIn())
        }
        text += "->"

        if (context.tupleExpression() != null) {
            text += visit(context.tupleExpression()).to<Result>().text
        } else {
            add_current_set()
            text += "{" + ProcessFunctionSupport(context.functionSupportStatement()) + "}"
            delete_current_set()
        }
        text += BlockRight
    }

    override fun visitLambdaIn(context: LambdaInContext) = run {
        var obj = ""
        for (i in 0 until context.id().size) {
            val r = visit(context.id(i)) as Result
            obj += if (i == 0) {
                r.text
            } else {
                ", ${r.text}"
            }
            add_id(r.text)
        }
        obj
    }

    override fun visitPlusMinus(context: PlusMinusContext) = Result().apply {
        val expr = visit(context.expression()) as Result
        val op = visit(context.add()) as str
        data = expr.data
        text = op + expr.text
    }

    override fun visitNegate(context: NegateContext) = Result().apply {
        val expr = visit(context.expression()) as Result
        data = expr.data
        text = "!" + expr.text
    }

    override fun visitBitwiseNotExpression(context: BitwiseNotExpressionContext) = Result().apply {
        val expr = visit(context.expression()) as Result
        data = expr.data
        text = "~" + expr.text
    }
}

