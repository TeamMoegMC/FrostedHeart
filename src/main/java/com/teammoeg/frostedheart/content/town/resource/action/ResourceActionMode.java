package com.teammoeg.frostedheart.content.town.resource.action;

/**
 * 当无法全部完成操作时，执行的方案
 */
public enum ResourceActionMode {
    //想不出啥好名字。。如果你们想出比较好的名字，顺便改掉吧
    /**
     * 尝试执行操作，若无法全部完成，则取消操作。
     * 当添加时，若剩余容量<应添加量，则不添加。
     * 当消耗时，若剩余资源<应消耗量，则不消耗。
     */
    ATTEMPT,
    /**
     * 尝试执行操作，若无法全部完成，则尽可能操作更多。
     * 当添加时，若剩余容量<应添加量，则添加相当于剩余容量的资源。
     * 当消耗时，若剩余资源<应消耗量，则消耗剩余的所有资源。
     */
    MAXIMIZE
}
