package com.sol2f.util;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.HashMap;
import java.util.Map;

public class FunctionCaculator {
    public static final Function FLOOR = new Function("floor", 1) {// 定义向下取整函数
        @Override
        public double apply(double... args) {
            return Math.floor(args[0]);
        };
    };

    public static final Function CEIL = new Function("ceil", 1) {// 定义向上取整函数
        @Override
        public double apply(double... args) {
            return Math.ceil(args[0]);
        }
    };

    public static final Function ROUND = new Function("round", 1) {// 定义四舍五入函数
        @Override
        public double apply(double... args) {
            return Math.round(args[0]);
        }
    };

    public static final Function MIN = new Function("min", 2) {// 定义取最小值函数
        @Override
        public double apply(double... args) {
            return Math.min(args[0], args[1]);
        }
    };

    public static final Function MAX = new Function("max", 2) {// 定义取最大值函数
        @Override
        public double apply(double... args) {
            return Math.max(args[0], args[1]);
        }
    };

    public static final Function POW = new Function("pow", 2) {// 定义幂函数
        @Override
        public double apply(double... args) {
            return Math.pow(args[0], args[1]);
        }
    };

    public static final Function LOG = new Function("log", 2) {// 定义对数函数
        @Override
        public double apply(double... args) {
            return Math.log(args[1]) / Math.log(args[0]);
        }
    };

    public static final Function[] CUSTOM_FUNCTIONS = { FLOOR, CEIL, ROUND, MIN, MAX, POW };// 自定义函数数组

    public static Expression buildExpression(String exprStr, String... variables) {
        if (exprStr == null || exprStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression string cannot be null or empty");
        }

        try {
            ExpressionBuilder builder = new ExpressionBuilder(exprStr).variables(variables).functions(CUSTOM_FUNCTIONS);
            Expression expr = builder.build();
            return expr;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to build expression: " + e.getMessage(), e);
        }
    }

    public static double evaluate(String expressionStr, Map<String, Double> variables) {
        if (variables == null) {
            throw new IllegalArgumentException("Variables map cannot be null");
        }

        Map<String, Double> allVars = new HashMap<>(variables);
        allVars.put("e", Math.E);
        allVars.put("pi", Math.PI);
        String[] varNames = allVars.keySet().toArray(new String[0]);
        Expression expr = buildExpression(expressionStr, varNames);
        for (Map.Entry<String, Double> entry : allVars.entrySet()) {
            expr.setVariable(entry.getKey(), entry.getValue());
        }

        try {
            return expr.evaluate();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Error evaluating expression: " + e.getMessage(), e);
        }
    };
}
