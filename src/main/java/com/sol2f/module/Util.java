package com.sol2f.module;

import java.util.HashMap;
import java.util.Map;

import com.sol2f.SpiceOfLifeFabricFlavor;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

public class Util {
    /**
     * 读取玩家的持久化 NBT 数据
     * 
     * @param player 玩家
     * @return 持久化 NBT 数据
     */
    static NbtCompound readPersistentCompound(ServerPlayerEntity player) {
        if (player instanceof com.sol2f.interfaces.IEntityDataSaver saver) {
            return saver.getPersistentData();
        }

        SpiceOfLifeFabricFlavor.LOGGER.error("Failed to read persistent compound");
        return new NbtCompound();
    }

    /**
     * 写入玩家的持久化 NBT 数据
     * 
     * @param player 玩家
     * @param data 持久化 NBT 数据
     */
    static void writePersistentCompound(ServerPlayerEntity player, NbtCompound data) {
        if (player instanceof com.sol2f.interfaces.IEntityDataSaver saver) {
            NbtCompound persistent = saver.getPersistentData();
            for (String key : data.getKeys()) {
                persistent.put(key, data.get(key));
            }
            return;
        }
        SpiceOfLifeFabricFlavor.LOGGER.error("Failed to write persistent compound. Player is not IEntityDataSaver!");
    }

    /**
     * 检查物品是否是食物
     * 
     * @param stack item stack
     * @return 如果是食物返回 true
     */
    public static boolean isFoodItem(ItemStack stack) {
        return stack != null && stack.getItem().getFoodComponent() != null;
    }

    /**
     * 检查物品是否是食物
     * 
     * @param item item
     * @return 如果是食物返回 true
     */
    public static boolean isFoodItem(Item item) {
        return item != null && item.getFoodComponent() != null;
    }

    // 函数计算器部分
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

    public static final Function IF = new Function("if", 3) {// 定义条件判断函数
        @Override
        public double apply(double... args) {
            return args[0] > 0 ? args[1] : args[2];
        }
    };

    public static final Function OR = new Function("or", 2) {// 定义或函数
        @Override
        public double apply(double... args) {
            return args[0] > 0 || args[1] > 0 ? 1 : 0;
        }
    };

    public static final Function AND = new Function("and", 2) {// 定义与函数
        @Override
        public double apply(double... args) {
            return args[0] > 0 && args[1] > 0 ? 1 : 0;
        }
    };

    public static final Function NOT = new Function("not", 1) {// 定义非函数
        @Override
        public double apply(double... args) {
            return args[0] > 0 ? 0 : 1;
        }
    };

    public static final Function LT = new Function("lt", 2) {// 定义lessthan函数，返回0或1
    @Override
        public double apply(double... args) {
            return args[0] < args[1] ? 1 : 0;
        }
    };

    public static final Function GT = new Function("gt", 2) {// 定义greaterthan函数，返回0或1
        @Override
        public double apply(double... args) {
            return args[0] > args[1] ? 1 : 0;
        }
    };

    public static final Function EQ = new Function("eq", 2) {// 定义equals函数，返回0或1
        @Override
        public double apply(double... args) {
            return args[0] == args[1] ? 1 : 0;
        }
    };

    public static final Function NEQ = new Function("neq", 2) {// 定义not equals函数，返回0或1
        @Override
        public double apply(double... args) {
            return args[0] != args[1] ? 1 : 0;
        }
    };

    public static final Function[] CUSTOM_FUNCTIONS = { FLOOR, CEIL, ROUND, MIN, MAX, POW, LOG, IF, OR, AND, NOT, LT, GT, EQ, NEQ };

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
        if (expressionStr == null || expressionStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression string cannot be null or empty");
        }
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
    }
}
