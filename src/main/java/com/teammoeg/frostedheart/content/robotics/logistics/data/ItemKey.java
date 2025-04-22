package com.teammoeg.frostedheart.content.robotics.logistics.data;

import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemKey {
	public static final Codec<ItemKey> CODEC=RecordCodecBuilder.create(t->t.group(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(o->o.item),
		CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(o->Optional.ofNullable(o.nbt))).apply(t, ItemKey::new));
	public final Item item;
	public final CompoundTag nbt;
	ItemStack stackCache;
	int hash;
	boolean hasHash;
	public ItemKey(Item item, CompoundTag nbt) {
		super();
		this.item = item;
		this.nbt = nbt;
	}
	public ItemKey(Item item,Optional<CompoundTag> tag) {
		this.item=item;
		this.nbt=tag.orElse(null);
	}

	public ItemKey(ItemStack stack) {
		this(stack.getItem(),stack.getTag());
	}
	public ItemStack getStack() {
		if(stackCache==null) {
			stackCache=new ItemStack(item,1,nbt);
		}
		return stackCache;
	}
	public int getMaxStackSize() {
		return this.getStack().getMaxStackSize();
	}
	public boolean isSameItem(ItemStack stack) {
		if(stack.getItem()!=this.item)return false;
		CompoundTag stackTag=stack.getTag();
		if(stackTag!=null&&this.nbt!=null)return this.nbt.equals(stackTag);
		return stackTag==this.nbt;
	}
	public ItemStack createStackWithSize(int size) {
		if(size==0)return ItemStack.EMPTY;
		return new ItemStack(item,size,nbt);
	}
	@Override
	public int hashCode() {
		if(!hasHash) {
			hash=Objects.hash(item, nbt);
			hasHash=true;
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ItemKey other = (ItemKey) obj;
		return Objects.equals(item, other.item) && Objects.equals(nbt, other.nbt);
	}

	@Override
	public String toString() {
		return "ItemKey [item=" + getStack() + "]";
	}

}
