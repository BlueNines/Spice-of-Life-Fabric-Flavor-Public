package com.sol2f.interfaces;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;

/**
 * 用于访问 Mixin 注入到 Entity 中的方法的接口
 * 通过此接口可以安全地访问被 Mixin 扩展的功能，而无需直接依赖 Mixin 类
 */
public interface IEntityDataSaver {
    NbtCompound getPersistentData();
    
    /**
     * 获取饥饿计时器计数
     * @return 当前饥饿计时器的值
     */
    int getHungerTickCounter();
    
    /**
     * 重置饥饿计时器
     */
    void resetHungerTickCounter();
    
    /**
     * 增加饥饿计时器
     */
    void increaseHungerTickCounter();

    /**
     * 获取睡眠开始的 tick
     * @return 睡眠开始的 tick
     */
    int getSleepStartTick();
    /**
     * 获取睡眠开始的 tick
     * @return 睡眠开始的 tick
     */

    void setSleepStartTick(int tick);
    
    /**
     * 工具方法：从 Entity 获取 IEntityDataSaver 实例
     * 这是访问 Mixin 功能的安全方式
     * 
     * @param entity 要获取的实体
     * @return IEntityDataSaver 实例，如果 entity 为 null 则返回 null
     */
    static IEntityDataSaver of(Entity entity) {
        if (entity == null) {
            return null;
        }
        return (IEntityDataSaver) entity;
    }
}
