package com.example.stockstudy.aop

import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

class CustomSpringELParser {
    companion object {
        private val parser = SpelExpressionParser()

        fun getDynamicKey(parameterNames: Array<String>, args: Array<Any>, key: String): Any {
            val context = StandardEvaluationContext().apply {
                for ((index, paramName) in parameterNames.withIndex()) {
                    setVariable(paramName, args[index])
                }
            }

            return parser.parseExpression(key).getValue(context, Any::class.java)!!
        }
    }
}
