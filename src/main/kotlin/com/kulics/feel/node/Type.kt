package com.kulics.feel.node

sealed class TypeNode : Node()

class NominalTypeNode(val id: String, val typeArguments: List<TypeNode>) : TypeNode()

class FunctionTypeNode(val parameterTypes: List<TypeNode>, val returnType: TypeNode) : TypeNode()