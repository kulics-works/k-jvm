package com.kulics.feel.visitor

import com.kulics.feel.grammar.FeelParser.*

open class TypeVisitor() : ImplementVisitor() {
    // type -------------------------------

    override fun visitTypeType(context: TypeTypeContext) =
        visit(context.getChild(0)) as str

    override fun visitTypeNullable(context: TypeNullableContext) =
        visit(context.typeNotNull()) as str + "?"

    override fun visitTypeArray(context: TypeArrayContext) =
        "$Arr<${visit(context.typeType())}>"

    override fun visitTypeList(context: TypeListContext) =
        "$Lst<${visit(context.typeType())}>"

    override fun visitTypeSet(context: TypeSetContext) =
        "$Set<${visit(context.typeType())}>"

    override fun visitTypeDictionary(context: TypeDictionaryContext) =
        "$Dic<${visit(context.typeType(0))},${visit(context.typeType(1))}>"

    override fun visitTypeStack(context: TypeStackContext) =
        "$Stk<${visit(context.typeType())}>"

    override fun visitTypePackage(context: TypePackageContext) = run {
        var obj = ""
        obj += visit(context.nameSpaceItem())
        if (context.templateCall() != null) {
            obj += visit(context.templateCall())
        }
        obj
    }

    override fun visitTypeFunction(context: TypeFunctionContext) = run {
        var obj = ""
        val `in` = visit(context.typeFunctionParameterClause(0)) as str
        var out = visit(context.typeFunctionParameterClause(1)) as str
        if (context.t.type == Right_Arrow) {
            if (out.isEmpty()) {
                obj = "($`in`)->Unit"
            } else {
                if (out.indexOfFirst { it == ',' } >= 0) {
                    out = "( $out )"
                }
//                if (context.y != null) {
//                    out = "" IEnum "<" out ">"
//                }
                obj = "($`in`)->$out"
            }
        } else {
            if (out.isEmpty()) {
                obj = "($`in`)->Unit"
            } else {
                if (out.indexOfFirst { it == ',' } >= 0) {
                    out = "( $out )"
                }
//                if (context.y != null) {
//                    out = "" IEnum "<" out ">"
//                }
                obj = "($`in`)->$out"
            }
        }
        obj
    }

    override fun visitTypeAny(context: TypeAnyContext) = Any

    override fun visitTypeFunctionParameterClause(context: TypeFunctionParameterClauseContext) = run {
        var obj = ""
        for (i in 0 until context.typeType().size) {
            val p = visit(context.typeType(i)) as str
            obj += if (i == 0) {
                p
            } else {
                ", $p"
            }
        }
        obj
    }

    override fun visitTypeBasic(context: TypeBasicContext) = when (context.t.type) {
        TypeI8 -> I8
        TypeU8 -> U8
        TypeI16 -> I16
        TypeU16 -> U16
        TypeI32 -> I32
        TypeU32 -> U32
        TypeI64 -> I64
        TypeU64 -> U64
        TypeF32 -> F32
        TypeF64 -> F64
        TypeChr -> Chr
        TypeStr -> Str
        TypeBool -> Bool
        TypeInt -> Int
        TypeNum -> Num
        TypeByte -> U8
        else -> Any
    }
}