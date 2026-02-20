package com.teammoeg.frostedheart.content.town.provider;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.Town;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface ITownProviderSerializable<T extends Town> extends ITownProvider<T>, INBTSerializable<Tag> {

    /**
     * @return provider提供的城镇类型
     */
    Class<T> getTownType();

    /**
     * Key:对应城镇的的类名(Class.getSimpleName())
     * <br>
     * Value:一个生成Provider的构造函数
     */
    public static final Map<String, Supplier<? extends ITownProviderSerializable<? extends Town>>> PROVIDERS = new HashMap<>();

    static <T extends Town> void register(Class<T> townType
            , Supplier<? extends ITownProviderSerializable<T>> constructor){
        PROVIDERS.put(townType.getSimpleName(), constructor);
    }

    public static void registerAll(){
        register(TeamTown.class, TeamTownProvider::new);
    }

    default CompoundTag toNBT(){
        CompoundTag tag = new CompoundTag();
        Class<?> clazz = this.getClass();
        tag.putString("townType", this.getTownType().getSimpleName());
        tag.put("data", serializeNBT());
        return tag;
    }

    static @Nullable ITownProviderSerializable<? extends Town> fromNBT(CompoundTag tag){
        if(tag.contains("townType")){
            String townType = tag.getString("townType");
            Supplier<? extends ITownProviderSerializable<? extends Town>> constructor = PROVIDERS.get(townType);
            if(constructor != null && tag.contains("data")){
                ITownProviderSerializable<? extends Town> provider = constructor.get();
                provider.deserializeNBT(tag.get("data"));
                return provider;
            }
        }
        return null;
    }
}
