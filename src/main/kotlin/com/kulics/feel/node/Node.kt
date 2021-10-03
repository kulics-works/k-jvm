package com.kulics.feel.node

sealed class Node {
    abstract fun generateCode(): String
}