package com.teammoeg.frostedheart.content.keyhint;

import lombok.Getter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collection;
import java.util.List;


@Getter
public class KeyHintCollectEvent extends Event {
    final LocalPlayer player;
    final List<KeyHintOverlay.KeyHint> collected;

    public KeyHintCollectEvent(LocalPlayer player, List<KeyHintOverlay.KeyHint> collected) {
        super();
        this.player = player;
        this.collected = collected;
    }

    public void add(KeyHintOverlay.KeyHint key) {
        collected.add(key);
    }

    public void addAll(Collection<KeyHintOverlay.KeyHint> keys) {
        collected.addAll(keys);
    }
}
