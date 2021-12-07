/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.compat;

import com.teammoeg.frostedheart.content.temperature.heatervest.HeaterVestItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.InterModComms;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Iterator;
import java.util.function.Predicate;

public class CuriosCompat {
    public static void sendIMCS() {
        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE,
                () -> SlotTypePreset.BACK.getMessageBuilder().build());
        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE,
                () -> SlotTypePreset.CHARM.getMessageBuilder().build());
    }

    public static ItemStack getHeaterVest(LivingEntity living) {
        return getCuriosIfVisible(living, SlotTypePreset.BACK, stack -> stack.getItem() instanceof HeaterVestItem);
    }

    public static ItemStack getCuriosIfVisible(LivingEntity living, SlotTypePreset slot, Predicate<ItemStack> predicate) {
        LazyOptional<ICuriosItemHandler> optional = CuriosApi.getCuriosHelper().getCuriosHandler(living);
        return optional.resolve()
                .flatMap(handler -> handler.getStacksHandler(slot.getIdentifier()))
                .filter(ICurioStacksHandler::isVisible)
                .map(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getSlots(); i++)
                        if (stacksHandler.getRenders().get(i)) {
                            ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                            if (predicate.test(stack))
                                return stack;
                        }
                    return ItemStack.EMPTY;
                }).orElse(ItemStack.EMPTY);
    }

    public static Iterable<ItemStack> getAllCuriosIfVisible(LivingEntity el) {
        return new Iterable<ItemStack>() {
            @Override
            public Iterator<ItemStack> iterator() {
                return CuriosApi.getCuriosHelper().getCuriosHandler(el).resolve().map(h -> new CuriosIterator(h.getCurios().values().iterator())).orElse(new CuriosIterator(null) {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public ItemStack next() {
                        return null;
                    }

                });
            }
        };
    }

    static class CuriosIterator implements Iterator<ItemStack> {
        ItemIterator cur;
        Iterator<ICurioStacksHandler> it;

        public CuriosIterator(Iterator<ICurioStacksHandler> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return (cur != null && cur.hasNext()) || it.hasNext();
        }

        @Override
        public ItemStack next() {
            if (cur != null && cur.hasNext()) return cur.next();
            ICurioStacksHandler current;
            do {
                current = it.next();
            } while (!current.isVisible() && it.hasNext());
            if (current.isVisible()) {
                cur = new ItemIterator(current.getSlots(), current.getStacks());
                return cur.next();
            }
            return null;
        }

        static class ItemIterator implements Iterator<ItemStack> {
            int i = 0;
            int max;
            IDynamicStackHandler handler;

            @Override
            public boolean hasNext() {
                return i < max;
            }

            public ItemIterator(int max, IDynamicStackHandler handler) {
                this.max = max;
                this.handler = handler;
            }

            @Override
            public ItemStack next() {
                return handler.getStackInSlot(i++);
            }

        }
    }
}
