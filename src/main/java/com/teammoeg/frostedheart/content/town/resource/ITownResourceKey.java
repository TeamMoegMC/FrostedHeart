package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;

/**
 * 直接储存于城镇资源中的TownResourceKey
 */
public interface ITownResourceKey extends IGettable{


    Codec<ITownResourceKey> CODEC = CodecUtil.dispatch(ITownResourceKey.class)
            .type("item", ItemStackResourceKey.class, ItemStackResourceKey.CODEC)
            .type("virtual", VirtualResourceAttribute.class, VirtualResourceAttribute.CODEC)
            .buildByInt();

}
