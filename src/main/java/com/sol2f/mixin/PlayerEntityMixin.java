package com.sol2f.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.sol2f.interfaces.IEntityDataSaver;

@Mixin(Entity.class)
public abstract class PlayerEntityMixin implements IEntityDataSaver {
    private NbtCompound persistentData;

    private transient int HungerTickCounter = 0;// 用于给Hunger功能的tick计数
    private int SleepStartTick = 0;

    @Override
    public NbtCompound getPersistentData() {
        if (this.persistentData == null) {
            this.persistentData = new NbtCompound();
        }
        return persistentData;
    }

    public int getHungerTickCounter() {
        return HungerTickCounter;
    }

    public void resetHungerTickCounter() {
        HungerTickCounter = 0;
    }

    public void increaseHungerTickCounter() {
        HungerTickCounter++;
    }

    public void setSleepStartTick(int tick) {
        SleepStartTick = tick;
    }

    public int getSleepStartTick() {
        return SleepStartTick;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    protected void injectWriteMethod(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> info) {
        if (persistentData != null) {
            this.persistentData.putInt("HungerTickCounter", HungerTickCounter);// 将计数器值保存到 persistentData 中
            nbt.put("sol2f", persistentData);// 将 persistentData 保存到实体的 NBT 数据中
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    protected void injectReadMethod(NbtCompound nbt, CallbackInfo info) {
        if (nbt.contains("sol2f", 10)) {
            persistentData = nbt.getCompound("sol2f");
            if (persistentData.contains("HungerTickCounter")) {
                HungerTickCounter = persistentData.getInt("HungerTickCounter");
            } else {
                HungerTickCounter = 0;
            }
        }
    }
}
