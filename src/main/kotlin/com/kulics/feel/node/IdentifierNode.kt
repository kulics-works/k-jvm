package com.kulics.feel.node

class IdentifierNode(val name: String, val type: String, val kind: VariableKind) : Node()

enum class VariableKind {
    variable, constant
}