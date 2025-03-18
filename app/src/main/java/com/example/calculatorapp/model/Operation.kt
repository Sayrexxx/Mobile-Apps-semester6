package com.example.calculatorapp.model

data class Operation(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)