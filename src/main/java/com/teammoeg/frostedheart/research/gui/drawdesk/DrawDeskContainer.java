package com.teammoeg.frostedheart.research.gui.drawdesk;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import blusunrize.immersiveengineering.common.gui.IESlot;
import com.teammoeg.frostedheart.research.machines.DrawingDeskTileEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class DrawDeskContainer extends IEBaseContainer<DrawingDeskTileEntity> {
    interface Enabled {
        void setEnabled(boolean enabled);
    }

    public static class EnableIESlot extends IESlot implements Enabled {
        public EnableIESlot(Container containerMenu, IInventory inv, int id, int x, int y) {
            super(containerMenu, inv, id, x, y);
        }

        boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class EnableSlot extends Slot implements Enabled {
        public EnableSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public DrawDeskContainer(int id, PlayerInventory inventoryPlayer, DrawingDeskTileEntity tile) {
        super(tile, id);
        this.addSlot(new EnableIESlot(this, this.inv, 0, 114, 93) {// research
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return tile.isStackValid(0, itemStack);
            }

        });
        this.addSlot(new EnableIESlot(this, this.inv, 1, 114, 161) {// paper
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return tile.isStackValid(1, itemStack);
            }
        });
        this.addSlot(new EnableIESlot(this, this.inv, 2, 114, 178) {// pen
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return tile.isStackValid(2, itemStack);
            }
        });

        slotCount = 3;

        for (int i = 0; i < 36; i++) {
            int posi = i;
            if (i < 9)
                posi += 27;
            else
                posi -= 9;
            addSlot(new EnableSlot(inventoryPlayer, i, 10 + (posi % 6) * 17, 93 + (posi / 6) * 17));
        }
        //this.inventorySlots.get(0).set

    }

    public void setEnabled(boolean en) {
        for (Slot s : this.inventorySlots) {
            if (s instanceof Enabled)
                ((Enabled) s).setEnabled(en);
        }
    }
}
