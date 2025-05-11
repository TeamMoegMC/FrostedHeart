package com.teammoeg.frostedheart.content.town.resource;

/**
 * 直接储存于城镇资源中的TownResourceKey
 * @param <T> 部分资源需要进行再次封装才能在城镇资源中储存，T为分装之前的类。若未进行二次封装，则为类本身。
 */
public interface ITownResourceKey<T> {
    T getThing();
}
