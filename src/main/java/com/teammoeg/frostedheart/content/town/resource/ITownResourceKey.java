package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;

/**
 * 直接储存于城镇资源中的TownResourceKey
 */
public interface ITownResourceKey extends IGettable{
    KeyType getKeyType();


    Codec<ITownResourceKey> CODEC = CodecUtil.enumCodec(KeyType.class).dispatch(
            ITownResourceKey::getKeyType,
            type -> switch (type) {
                case ITEM -> ItemStackResourceKey.CODEC;
                case VIRTUAL -> VirtualResourceAttribute.CODEC;
            }
    );

    /**
     * 资源在程序上所属的类型，用于区分不同资源以实现Codec。
     */
    enum KeyType {
        ITEM, VIRTUAL
    }

}
