/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.ui.keyhint;

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
