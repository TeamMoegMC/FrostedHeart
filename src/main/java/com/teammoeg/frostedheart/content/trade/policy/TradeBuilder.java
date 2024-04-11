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

package com.teammoeg.frostedheart.content.trade.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.trade.policy.actions.AddFlagValueAction;
import com.teammoeg.frostedheart.content.trade.policy.actions.SetFlagAction;
import com.teammoeg.frostedheart.content.trade.policy.actions.SetFlagValueAction;
import com.teammoeg.frostedheart.content.trade.policy.actions.SetLevelAction;
import com.teammoeg.frostedheart.content.trade.policy.conditions.FlagValueCondition;
import com.teammoeg.frostedheart.content.trade.policy.conditions.GreaterFlagCondition;
import com.teammoeg.frostedheart.content.trade.policy.conditions.LevelCondition;
import com.teammoeg.frostedheart.content.trade.policy.conditions.NotCondition;
import com.teammoeg.frostedheart.content.trade.policy.conditions.TotalTradeCondition;
import com.teammoeg.frostedheart.content.trade.policy.conditions.WithFlagCondition;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.data.IFinishedRecipe;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

/**
 * Class TradeBuilder.
 * Builder class to build trade policy
 *
 * @author khjxiaogu
 */
public class TradeBuilder implements IFinishedRecipe {
    /**
     * Class ActionBuilder.
     * Builder class to build action list
     *
     * @param <T> parent type
     * @author khjxiaogu
     */
    public static class ActionBuilder<T> {

        private Consumer<PolicyAction> consumer;
        private T parent;

        private ActionBuilder(Consumer<PolicyAction> consumer, T parent) {
            super();
            this.consumer = consumer;
            this.parent = parent;
        }

        /**
         * Apply action .<br>
         *
         * @param act the action<br>
         * @return returns self
         */
        public ActionBuilder<T> action(PolicyAction act) {
            consumer.accept(act);
            return this;
        }

        /**
         * Action to add value to the flag.<br>
         *
         * @param name the flag name
         * @param val  the value, negative to minus;
         * @return returns self
         */
        public ActionBuilder<T> addFlag(String name, int val) {
            action(new AddFlagValueAction(name, val));
            return this;
        }

        /**
         * Finish to build actions.<br>
         *
         * @return returns parent
         */
        public T finish() {
            return parent;
        }

        /**
         * Action to set flag to 1 if not exists.<br>
         *
         * @param name the flag name
         * @return returns self
         */
        public ActionBuilder<T> setFlag(String name) {
            action(new SetFlagAction(name));
            return this;
        }

        /**
         * Action to set flag to.<br>
         *
         * @param name the name<br>
         * @param val  the value<br>
         * @return returns self
         */
        public ActionBuilder<T> setFlag(String name, int val) {
            action(new SetFlagValueAction(name, val));
            return this;
        }

        /**
         * Action to set trade level to.<br>
         *
         * @param val the value<br>
         * @return returns self
         */
        public ActionBuilder<T> setLevel(int val) {
            action(new SetLevelAction(val));
            return this;
        }

        /**
         * Action to remove flag and its value.<br>
         *
         * @param name the flag name
         * @return returns self
         */
        public ActionBuilder<T> unsetFlag(String name) {
            action(new SetFlagValueAction(name, 0));
            return this;
        }
    }
    /**
     * Class ConditionBuilder.
     * Builder class to build condition list
     *
     * @param <T> Parent type
     * @author khjxiaogu
     */
    public static class ConditionBuilder<T> {
        private Consumer<PolicyCondition> consumer;
        private T parent;

        private ConditionBuilder(Consumer<PolicyCondition> consumer, T parent) {
            super();
            this.consumer = consumer;
            this.parent = parent;
        }

        /**
         * Require condition.<br>
         *
         * @param pc the condition<br>
         * @return returns self
         */
        public ConditionBuilder<T> condition(PolicyCondition pc) {
            consumer.accept(pc);
            return this;
        }

        /**
         * Finish build conditions.<br>
         *
         * @return returns parent
         */
        public T finish() {
            return parent;
        }

        /**
         * Require flag value greater or equals.<br>
         *
         * @param flag flag name
         * @param val  the value
         * @return returns self
         */
        public ConditionBuilder<T> greaterFlag(String flag, int val) {
            return condition(new GreaterFlagCondition(flag, val));
        }

        /**
         * Require flag exist.<br>
         *
         * @param flag flag name
         * @return returns self
         */
        public ConditionBuilder<T> hasFlag(String flag) {
            return condition(new WithFlagCondition(flag));
        }

        /**
         * Require flag value equals.<br>
         *
         * @param flag flag name
         * @param val  the value
         * @return returns self
         */
        public ConditionBuilder<T> hasFlag(String flag, int val) {
            return condition(new FlagValueCondition(flag, val));
        }

        /**
         * Require flag non-existence.<br>
         *
         * @param flag flag name
         * @return returns self
         */
        public ConditionBuilder<T> hasNoFlag(String flag) {
            return not(new WithFlagCondition(flag));
        }

        /**
         * Require flag value not equal.<br>
         *
         * @param flag flag name
         * @param val  the value
         * @return returns self
         */
        public ConditionBuilder<T> hasNoFlag(String flag, int val) {
            return not(new FlagValueCondition(flag, val));
        }

        /**
         * Require flag value lesser than.<br>
         *
         * @param flag flag name
         * @param val  the value
         * @return returns self
         */
        public ConditionBuilder<T> lesserFlag(String flag, int val) {
            return not(new GreaterFlagCondition(flag, val));
        }

        /**
         * Require level greater or equals.<br>
         *
         * @param lvl the level
         * @return returns self
         */
        public ConditionBuilder<T> level(int lvl) {
            return condition(new LevelCondition(lvl));
        }

        /**
         * Require level lesser than.<br>
         *
         * @param lvl the level
         * @return returns self
         */
        public ConditionBuilder<T> lowerLevel(int lvl) {
            return not(new LevelCondition(lvl));
        }

        /**
         * Require total trade lesser than.<br>
         *
         * @param val the value
         * @return returns self
         */
        public ConditionBuilder<T> lowerTotal(int val) {
            return not(new TotalTradeCondition(val));
        }

        /**
         * Inverse condition.<br>
         *
         * @param pc the condition
         * @return returns self
         */
        public ConditionBuilder<T> not(PolicyCondition pc) {
            return condition(new NotCondition(pc));
        }

        /**
         * Require total trade value greater or equals.<br>
         *
         * @param val the value<br>
         * @return returns self
         */
        public ConditionBuilder<T> total(int val) {
            return condition(new TotalTradeCondition(val));
        }
    }
    /**
     * Class GroupBuilder.
     * Builder class to build a policy group
     *
     * @author khjxiaogu
     */
    public static class GroupBuilder {
        private List<PolicyCondition> conditions = new ArrayList<>();
        private List<BaseData> bdata = new ArrayList<>();
        private TradeBuilder parent;
        private BaseData lastAction;

        private GroupBuilder(TradeBuilder parent) {
            super();
            this.parent = parent;
        }

        /**
         * Build a policy group with given stock and conditions, then add to policy<br>
         *
         * @return returns self
         */
        public GroupBuilder basic() {
            parent.groups.add(new BasicPolicyGroup(conditions, bdata));
            return this;
        }

        /**
         * Buy.<br>
         *
         * @param maxstore the maxstore<br>
         * @param recover  the recover<br>
         * @param price    the price<br>
         * @param item     the item<br>
         * @return returns buy
         */
        public GroupBuilder buy(int maxstore, float recover, int price, Item item) {
            return this.buy(RegistryUtils.getRegistryName(item).toString(), maxstore, recover, price, Ingredient.fromItems(item));
        }

        /**
         * Buy.<br>
         *
         * @param maxstore the maxstore<br>
         * @param recover  the recover<br>
         * @param price    the price<br>
         * @param item     the item<br>
         * @return returns buy
         */
        public GroupBuilder buy(int maxstore, float recover, int price, ItemStack item) {
            return this.buy(RegistryUtils.getRegistryName(item.getItem()).toString(), maxstore, recover, price, Ingredient.fromStacks(item));
        }

        /**
         * Buy.<br>
         *
         * @param id       the id<br>
         * @param maxstore the maxstore<br>
         * @param recover  the recover<br>
         * @param price    the price<br>
         * @param item     the item<br>
         * @return returns buy
         */
        public GroupBuilder buy(String id, int maxstore, float recover, int price, Ingredient item) {
            bdata.add(lastAction = new DemandData(id, maxstore, recover, price, item));
            return this;
        }

        /**
         * Extends a policy and add to policy.<br>
         * This will omit stock settings.
         *
         * @param rl the rl<br>
         * @return returns self
         */
        public GroupBuilder extend(ResourceLocation rl) {
            parent.groups.add(new ExtendPolicyGroup(conditions, rl));
            return this;
        }

        /**
         * Extends a policy and add to policy.<br>
         * This will omit stock settings.
         *
         * @param name the name,would register as frostedheart:name<br>
         * @return returns self
         */
        public GroupBuilder extend(String name) {
            return this.extend(new ResourceLocation(FHMain.MODID, name));
        }

        /**
         * Extends a policy and add to policy.<br>
         * This will omit stock settings.
         *
         * @param b the policy builder<br>
         * @return returns self
         */
        public GroupBuilder extend(TradeBuilder b) {
            return this.extend(b.id);
        }

        /**
         * Extends a policy and add to policy.<br>
         * This will omit stock settings.
         *
         * @param name the policy<br>
         * @return returns self
         */
        public GroupBuilder extend(TradePolicy name) {
            return this.extend(name.getId());
        }

        /**
         * Finish building policy group, this won't add building policy group to policy.<br>
         *
         * @return returns parent
         */
        public TradeBuilder finish() {
            return parent;
        }

        /**
         * Build conditions for this group.<br>
         *
         * @return returns group condition builder
         */
        public ConditionBuilder<GroupBuilder> groupCondition() {
            return new ConditionBuilder<>(conditions::add, this);
        }

        /**
         * Hide last stock when stock out.<br>
         * Must add stock before doing this operation.
         *
         * @return returns hide
         */
        public GroupBuilder hide() {
            lastAction.hideStockout = true;
            return this;
        }

        /**
         * Nop.<br>
         *
         * @param id the id<br>
         * @return returns nop
         */
        public GroupBuilder nop(String id) {
            return nop(id, 1, 0, 0);
        }

        /**
         * Nop.<br>
         *
         * @param id       the id<br>
         * @param maxstore the maxstore<br>
         * @param recover  the recover<br>
         * @param price    the price<br>
         * @return returns nop
         */
        public GroupBuilder nop(String id, int maxstore, float recover, int price) {
            bdata.add(lastAction = new NopData(id, maxstore, recover, price));
            return this;
        }

        /**
         * Build action to execute when restock on last stock.<br>
         * Must add stock before doing this operation.
         *
         * @return returns restock action builder
         */
        public ActionBuilder<GroupBuilder> restockAction() {
            return new ActionBuilder<>(lastAction.actions::add, this);
        }

        /**
         * Build conditions for restock on last stock.<br>
         * Must add stock before doing this operation.
         *
         * @return returns restock condition builder
         */
        public ConditionBuilder<GroupBuilder> restocksBy() {
            return new ConditionBuilder<>(lastAction.restockconditions::add, this);
        }

        /**
         * Sell.<br>
         *
         * @param maxstore the maxstore<br>
         * @param recover  the recover<br>
         * @param price    the price<br>
         * @param item     the item<br>
         * @return returns sell
         */
        public GroupBuilder sell(int maxstore, float recover, int price, Item item) {
            return this.sell(RegistryUtils.getRegistryName(item).toString(), maxstore, recover, price, new ItemStack(item));
        }

        /**
         * Sell.<br>
         *
         * @param maxstore the maxstore<br>
         * @param recover  the recover<br>
         * @param price    the price<br>
         * @param item     the item<br>
         * @param count    the count<br>
         * @return returns sell
         */
        public GroupBuilder sell(int maxstore, float recover, int price, Item item, int count) {
            return this.sell(RegistryUtils.getRegistryName(item).toString(), maxstore, recover, price, new ItemStack(item, count));
        }

        /**
         * Sell.<br>
         *
         * @param maxstore the maxstore<br>
         * @param recover  the recover<br>
         * @param price    the price<br>
         * @param item     the item<br>
         * @return returns sell
         */
        public GroupBuilder sell(int maxstore, float recover, int price, ItemStack item) {
            return this.sell(RegistryUtils.getRegistryName(item.getItem()).toString(), maxstore, recover, price, item);
        }

        /**
         * Sell.<br>
         *
         * @param id       the id<br>
         * @param maxstore the maxstore<br>
         * @param recover  the recover<br>
         * @param price    the price<br>
         * @param item     the item<br>
         * @return returns sell
         */
        public GroupBuilder sell(String id, int maxstore, float recover, int price, ItemStack item) {
            bdata.add(lastAction = new ProductionData(id, maxstore, recover, price, item));
            return this;
        }

        /**
         * Build action to use when trade on last stock.<br>
         * Must add stock before doing this operation.
         *
         * @return returns trade action builder
         */
        public ActionBuilder<GroupBuilder> useAction() {
            return new ActionBuilder<>(lastAction.soldactions::add, this);
        }
    }
    private List<PolicyGroup> groups = new ArrayList<>();
    private ResourceLocation name;
    private ResourceLocation id;

    private int weight;

    private VillagerProfession prof;

    private int[] exp;

    /**
     * Finish building and output.
     *
     * @param out the out<br>
     */
    public void finish(Consumer<IFinishedRecipe> out) {
        out.accept(this);
    }

    @Override
    public ResourceLocation getAdvancementID() {
        return null;
    }

    @Override
    public JsonObject getAdvancementJson() {
        return null;
    }

    @Override
    public ResourceLocation getID() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return TradePolicy.SERIALIZER.get();
    }

    /**
     * Start building a new group.<br>
     *
     * @return returns group builder
     */
    public GroupBuilder group() {
        return new GroupBuilder(this);
    }

    /**
     * Set id for this policy.<br>
     *
     * @param id the id<br>
     * @return returns self
     */
    public TradeBuilder id(ResourceLocation id) {
        this.id = id;
        return this;
    }

    /**
     * Set id for this policy, short for id(frostedheart:id).<br>
     *
     * @param id the id<br>
     * @return returns self
     */
    public TradeBuilder id(String id) {
        return this.id(new ResourceLocation(FHMain.MODID, id));
    }

    public TradeBuilder levelExp(int... exps) {
        exp = exps;
        return this;
    }

    /**
     * Set name for this policy.<br>
     * Name is used to index policy and assign to villager, default value is id.
     *
     * @param name the name<br>
     * @return returns self
     */
    public TradeBuilder name(ResourceLocation name) {
        this.name = name;
        return this;
    }

    /**
     * Set name for this policy, short for name(frostedheart:name).<br>
     * Name is used to index policy and assign to villager, default value is id.
     *
     * @param name the name<br>
     * @return returns self
     */
    public TradeBuilder name(String name) {
        return this.name(new ResourceLocation(FHMain.MODID, name));
    }

    /**
     * Profession, used to set texture and model.<br>
     *
     * @param prof the profession<br>
     * @return returns self
     */
    public TradeBuilder profession(VillagerProfession prof) {
        this.prof = prof;
        return this;
    }

    @Override
    public void serialize(JsonObject arg0) {
        if (name != null)
            arg0.addProperty("name", name.toString());
        arg0.add("policies", SerializeUtil.toJsonList(groups, PolicyGroup::serialize));
        if (weight > 0)
            arg0.addProperty("weight", weight);
        if (prof != null && prof != VillagerProfession.NONE)
            arg0.addProperty("profession", RegistryUtils.getRegistryName(prof).toString());
        if (exp != null)
            arg0.add("exps", SerializeUtil.toJsonList(Arrays.stream(exp).boxed().collect(Collectors.toList()), JsonPrimitive::new));
    }

    /**
     * Weight for job roll on init.<br>
     *
     * @param weight the weight<br>
     * @return returns self
     */
    public TradeBuilder weight(int weight) {
        this.weight = weight;
        return this;
    }

}
