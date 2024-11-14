/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class WanderingRefugee extends PathfinderMob implements Npc {
//    private static final EntityDataAccessor<Boolean> HIRED = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.BOOLEAN);
//    private static final EntityDataAccessor<Integer> AMOUNT_NEEDED = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.INT);
//
//    // first and last names
//    private static final EntityDataAccessor<String> FIRST_NAME = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.STRING);
//    private static final EntityDataAccessor<String> LAST_NAME = SynchedEntityData.defineId(WanderingRefugee.class, EntityDataSerializers.STRING);

    // Random pool of last names
    public static final String[] LAST_NAMES = new String[] {
        "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor",
        "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson",
        "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen", "Young", "Hernandez", "King", "Wright",
        "Lopez", "Hill", "Scott", "Green", "Adams", "Baker", "Gonzalez", "Nelson", "Carter", "Mitchell", "Perez",
        "Roberts", "Turner", "Phillips", "Campbell", "Parker", "Evans", "Edwards", "Collins", "Stewart", "Sanchez",
        "Morris", "Rogers", "Reed", "Cook", "Morgan", "Bell", "Murphy", "Bailey", "Rivera", "Cooper", "Richardson",
        "Cox", "Howard", "Ward", "Torres", "Peterson", "Gray", "Ramirez", "James", "Watson", "Brooks", "Kelly",
        "Sanders", "Price", "Bennett", "Wood", "Barnes", "Ross", "Henderson", "Coleman", "Jenkins", "Perry", "Powell",
        "Long", "Patterson", "Hughes", "Flores", "Washington", "Butler", "Simmons", "Foster", "Gonzales", "Bryant",
        "Alexander", "Russell", "Griffin", "Diaz", "Hayes"
    };

    // Random pool of first names
    public static final String[] FIRST_NAMES = new String[] {
        "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles",
        "Christopher", "Daniel", "Matthew", "Anthony", "Mark", "Donald", "Steven", "Paul", "Andrew", "Joshua",
        "Kenneth", "Kevin", "Brian", "George", "Edward", "Ronald", "Timothy", "Jason", "Jeffrey", "Ryan", "Jacob",
        "Gary", "Nicholas", "Eric", "Stephen", "Jonathan", "Larry", "Justin", "Scott", "Brandon", "Frank", "Benjamin",
        "Gregory", "Samuel", "Raymond", "Patrick", "Alexander", "Jack", "Dennis", "Jerry", "Tyler", "Aaron", "Jose",
        "Henry", "Adam", "Douglas", "Nathan", "Peter", "Zachary", "Kyle", "Walter", "Harold", "Jeremy", "Ethan",
        "Carl", "Keith", "Roger", "Gerald", "Christian", "Terry", "Sean", "Arthur", "Austin", "Noah", "Lawrence",
        "Jesse", "Joe", "Bryan", "Billy", "Jordan", "Albert", "Dylan", "Bruce", "Willie", "Gabriel", "Alan", "Juan",
    };

    private boolean hired = false;
    private int amountNeeded = 3 + (int) (getRandom().nextFloat() * 5);
    private String lastName = LAST_NAMES[(int) (Math.random() * LAST_NAMES.length)];
    private String firstName = FIRST_NAMES[(int) (Math.random() * FIRST_NAMES.length)];

    public WanderingRefugee(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack itemStack = player.getItemInHand(hand);
            if (hired) {
                player.displayClientMessage(TranslateUtils.translateMessage("refugee.hired", player.getName().getString()), false);
                return InteractionResult.CONSUME;
            } else if (itemStack.is(FHTags.Items.REFUGEE_NEEDS)) {
                // try shrink stack and decrement amount needed
                int amountNeeded = this.amountNeeded;
                if (amountNeeded > 0) {
                    amountNeeded--;
                    this.amountNeeded = amountNeeded;
                    itemStack.shrink(1);
                    if (amountNeeded > 0) {
                        player.displayClientMessage(TranslateUtils.translateMessage("refugee.unsatisfied"), false);
                        return InteractionResult.CONSUME;
                    } else {
                        player.displayClientMessage(TranslateUtils.translateMessage("refugee.satisfied"), false);
                        // Get town of player
                        TeamTown town = TeamTown.from(player);
                        // Add resident
                        Resident resident = new Resident(firstName, lastName);
                        town.addResident(resident);
                        // hire
                        hired = true;
                        return InteractionResult.CONSUME;
                    }
                }
            }
            // tell player they need to give the refugee something
            player.displayClientMessage(TranslateUtils.translateMessage("refugee.needs", firstName, lastName), false);
            return InteractionResult.CONSUME;
        }
    }

//    @Override
//    protected void defineSynchedData() {
//        super.defineSynchedData();
//        this.entityData.define(HIRED, false);
//        this.entityData.define(AMOUNT_NEEDED,  3 + (int) (getRandom().nextFloat() * 5));
//        this.entityData.define(LAST_NAME, LAST_NAMES[(int) (Math.random() * LAST_NAMES.length)]);
//        this.entityData.define(FIRST_NAME, FIRST_NAMES[(int) (Math.random() * FIRST_NAMES.length)]);
//    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("hired", this.hired);
        pCompound.putInt("amountNeeded", this.amountNeeded);
        pCompound.putString("lastName", this.lastName);
        pCompound.putString("firstName", this.firstName);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if ((pCompound.contains("amountNeeded", Tag.TAG_INT))) {
            amountNeeded = pCompound.getInt("amountNeeded");
        }
        if ((pCompound.contains("lastName", Tag.TAG_STRING))) {
            lastName = pCompound.getString("lastName");
        }
        if ((pCompound.contains("firstName", Tag.TAG_STRING))) {
            firstName = pCompound.getString("firstName");
        }
        if ((pCompound.contains("hired", Tag.TAG_BYTE))) {
            hired = pCompound.getBoolean("hired");
        }
    }
}
