package com.teammoeg.frostedheart.infrastructure.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider.LootType;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.generators.BlockStateProvider.ConfiguredModelList;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FHBlockBuilder<T extends Block, P extends AbstractRegistrate> extends AbstractBuilder<Block, T, P, FHBlockBuilder<T, P>> {

	protected FHBlockBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
			NonNullFunction<Properties, T> factory, NonNullSupplier<Properties> initialProperties) {
        super(owner, parent, name, callback, ForgeRegistries.Keys.BLOCKS);
        this.factory = factory;
        this.initialProperties = initialProperties;
	}


    private final NonNullFunction<BlockBehaviour.Properties, T> factory;
    
    private NonNullSupplier<BlockBehaviour.Properties> initialProperties;
    private NonNullFunction<BlockBehaviour.Properties, BlockBehaviour.Properties> propertiesCallback = NonNullUnaryOperator.identity();
    private List<Supplier<Supplier<RenderType>>> renderLayers = new ArrayList<>(1);

    @Nullable
    private NonNullSupplier<Supplier<BlockColor>> colorHandler;

    /**
     * Modify the properties of the block. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
     * different operations.
     * <p>
     * If a different properties instance is returned, it will replace the existing one entirely.
     * 
     * @param func
     *            The action to perform on the properties
     * @return this {@link BlockBuilder}
     */
    public FHBlockBuilder<T, P> properties(NonNullUnaryOperator<BlockBehaviour.Properties> func) {
        propertiesCallback = propertiesCallback.andThen(func);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     * 
     * @param block
     *            The block to create the initial properties from (via {@link Block.Properties#copy(BlockBehaviour)})
     * @return this {@link BlockBuilder}
     */
    public FHBlockBuilder<T, P> initialProperties(NonNullSupplier<? extends Block> block) {
        initialProperties = () -> BlockBehaviour.Properties.copy(block.get());
        return this;
    }

    /**
     * @deprecated Set your render type in your model's JSON ({@link net.minecraftforge.client.model.generators.ModelBuilder#renderType(ResourceLocation)}) or override {@link net.minecraft.client.resources.model.BakedModel#getRenderTypes(BlockState, net.minecraft.util.RandomSource, net.minecraftforge.client.model.data.ModelData)}
     */
    @Deprecated(forRemoval = true)
    public FHBlockBuilder<T, P> addLayer(Supplier<Supplier<RenderType>> layer) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            Preconditions.checkArgument(RenderType.chunkBufferLayers().contains(layer.get().get()), "Invalid block layer: " + layer);
        });
        if (this.renderLayers.isEmpty()) {
            onRegister(this::registerLayers);
        }
        this.renderLayers.add(layer);
        return this;
    }

    @SuppressWarnings("deprecation")
    protected void registerLayers(T entry) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            OneTimeEventReceiver.addModListener(getOwner(), FMLClientSetupEvent.class, $ -> {
                if (renderLayers.size() == 1) {
                    final RenderType layer = renderLayers.get(0).get().get();
                    ItemBlockRenderTypes.setRenderLayer(entry, layer);
                } else if (renderLayers.size() > 1) {
                    final Set<RenderType> layers = renderLayers.stream()
                            .map(s -> s.get().get())
                            .collect(Collectors.toSet());
                    ItemBlockRenderTypes.setRenderLayer(entry, layers::contains);
                }
            });
        });
    }

    /**
     * Create a standard {@link BlockItem} for this block, building it immediately, and not allowing for further configuration.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     *
     * @return this {@link BlockBuilder}
     * @see #item()
     */
    public FHBlockBuilder<T, P> simpleItem() {
        return item().build();
    }

    /**
     * Create a standard {@link BlockItem} for this block, and return the builder for it so that further customization can be done.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     * 
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public ItemBuilder<BlockItem, FHBlockBuilder<T, P>> item() {
        return item(BlockItem::new);
    }

    /**
     * Create a {@link BlockItem} for this block, which is created by the given factory, and return the builder for it so that further customization can be done.
     * <p>
     * By default, the item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     * 
     * @param <I>
     *            The type of the item
     * @param factory
     *            A factory for the item, which accepts the block object and properties and returns a new item
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public <I extends Item> ItemBuilder<I, FHBlockBuilder<T, P>> item(NonNullBiFunction<? super T, Item.Properties, ? extends I> factory) {
        return getOwner().<I, FHBlockBuilder<T, P>> item(this, getName(), p -> factory.apply(getEntry(), p))
                .setData(ProviderType.LANG, NonNullBiConsumer.noop()) // FIXME Need a beetter API for "unsetting" providers
                .model((ctx, prov) -> {
                    Optional<String> model = getOwner().getDataProvider(ProviderType.BLOCKSTATE)
                            .flatMap(p -> p.getExistingVariantBuilder(getEntry()))
                            .map(b -> b.getModels().get(b.partialState()))
                            .map(ConfiguredModelList::toJSON)
                            .filter(JsonElement::isJsonObject)
                            .map(j -> j.getAsJsonObject().get("model"))
                            .map(JsonElement::getAsString);
                    if (model.isPresent()) {
                        prov.withExistingParent(ctx.getName(), model.get());
                    } else {
                        prov.blockItem(asSupplier());
                    }
                });
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * 
     * @param <BE>
     *            The type of the block entity
     * @param factory
     *            A factory for the block entity
     * @return this {@link FHBlockBuilder}
     */
    public <BE extends BlockEntity> FHBlockBuilder<T, P> simpleBlockEntity(BlockEntityFactory<BE> factory) {
        return blockEntity(factory).build();
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * <p>
     * The created {@link BlockEntityBuilder} is returned for further configuration.
     * 
     * @param <BE>
     *            The type of the block entity
     * @param factory
     *            A factory for the block entity
     * @return the {@link BlockEntityBuilder}
     */
    public <BE extends BlockEntity> BlockEntityBuilder<BE, FHBlockBuilder<T, P>> blockEntity(BlockEntityFactory<BE> factory) {
        return getOwner().<BE, FHBlockBuilder<T, P>>blockEntity(this, getName(), factory).validBlock(asSupplier());
    }
    
    /**
     * Register a block color handler for this block. The {@link BlockColor} instance can be shared across many blocks.
     * 
     * @param colorHandler
     *            The color handler to register for this block
     * @return this {@link FHBlockBuilder}
     */
    // TODO it might be worthwhile to abstract this more and add the capability to automatically copy to the item
    public FHBlockBuilder<T, P> color(NonNullSupplier<Supplier<BlockColor>> colorHandler) {
        if (this.colorHandler == null) {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> this::registerBlockColor);
        }
        this.colorHandler = colorHandler;
        return this;
    }
    
    protected void registerBlockColor() {
        OneTimeEventReceiver.addModListener(getOwner(), RegisterColorHandlersEvent.Block.class, e -> {
            NonNullSupplier<Supplier<BlockColor>> colorHandler = this.colorHandler;
            if (colorHandler != null) {
                e.register(colorHandler.get().get(), getEntry());
            }
        });
    }

    /**
     * Assign the default blockstate, which maps all states to a single model file (via {@link RegistrateBlockstateProvider#simpleBlock(Block)}). This is the default, so it is generally not necessary
     * to call, unless for undoing previous changes.
     * 
     * @return this {@link FHBlockBuilder}
     */
    public FHBlockBuilder<T, P> defaultBlockstate() {
        return blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry()));
    }

    /**
     * Configure the blockstate/models for this block.
     * 
     * @param cons
     *            The callback which will be invoked during data generation.
     * @return this {@link FHBlockBuilder}
     * @see #setData(ProviderType, NonNullBiConsumer)
     */
    public FHBlockBuilder<T, P> blockstate(NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> cons) {
        return setData(ProviderType.BLOCKSTATE, cons);
    }

    /**
     * Assign the default translation, as specified by {@link RegistrateLangProvider#getAutomaticName(NonNullSupplier, net.minecraft.resources.ResourceKey)}. This is the default, so it is generally
     * not necessary to call, unless for undoing previous changes.
     * 
     * @return this {@link FHBlockBuilder}
     */
    public FHBlockBuilder<T, P> defaultLang() {
        return lang(Block::getDescriptionId);
    }

    /**
     * Set the translation for this block.
     * 
     * @param name
     *            A localized English name
     * @return this {@link FHBlockBuilder}
     */
    public FHBlockBuilder<T, P> lang(String name) {
        return lang(Block::getDescriptionId, name);
    }

    /**
     * Assign the default loot table, as specified by {@link RegistrateBlockLootTables#dropSelf(Block)}. This is the default, so it is generally not necessary to call, unless for
     * undoing previous changes.
     * 
     * @return this {@link FHBlockBuilder}
     */
    public FHBlockBuilder<T, P> defaultLoot() {
        return loot(RegistrateBlockLootTables::dropSelf);
    }

    /**
     * Configure the loot table for this block. This is different than most data gen callbacks as the callback does not accept a {@link DataGenContext}, but instead a
     * {@link RegistrateBlockLootTables}, for creating specifically block loot tables.
     * <p>
     * If the block does not have a loot table (i.e. {@link Block.Properties#noLootTable()} is called) this action will be <em>skipped</em>.
     * 
     * @param cons
     *            The callback which will be invoked during block loot table creation.
     * @return this {@link FHBlockBuilder}
     */
    public FHBlockBuilder<T, P> loot(NonNullBiConsumer<RegistrateBlockLootTables, T> cons) {
        return setData(ProviderType.LOOT, (ctx, prov) -> prov.addLootAction(LootType.BLOCK, tb -> {
            if (!ctx.getEntry().getLootTable().equals(BuiltInLootTables.EMPTY)) {
                cons.accept(tb, ctx.getEntry());
            }
        }));
    }

    /**
     * Configure the recipe(s) for this block.
     * 
     * @param cons
     *            The callback which will be invoked during data generation.
     * @return this {@link FHBlockBuilder}
     * @see #setData(ProviderType, NonNullBiConsumer)
     */
    public FHBlockBuilder<T, P> recipe(NonNullBiConsumer<DataGenContext<Block, T>, RegistrateRecipeProvider> cons) {
        return setData(ProviderType.RECIPE, cons);
    }

    /**
     * Assign {@link TagKey}{@code s} to this block. Multiple calls will add additional tags.
     * 
     * @param tags
     *            The tags to assign
     * @return this {@link FHBlockBuilder}
     */
    @SafeVarargs
    public final FHBlockBuilder<T, P> tag(TagKey<Block>... tags) {
        return tag(ProviderType.BLOCK_TAGS, tags);
    }

    @Override
    protected T createEntry() {
        @Nonnull BlockBehaviour.Properties properties = this.initialProperties.get();
        properties = propertiesCallback.apply(properties);
        return factory.apply(properties);
    }
    
    @Override
    protected RegistryEntry<T> createEntryWrapper(RegistryObject<T> delegate) {
        return new com.tterrag.registrate.util.entry.BlockEntry<>(getOwner(), delegate);
    }
	Consumer<com.tterrag.registrate.util.entry.BlockEntry<T>> shaderRegistry;

    public static <T extends Block> FHBlockBuilder<T, FHRegistrate> create(AbstractRegistrate<?> owner, FHRegistrate parent, String name, BuilderCallback callback, NonNullFunction<BlockBehaviour.Properties, T> factory) {
        return new FHBlockBuilder<>(owner, parent, name, callback, factory, () -> BlockBehaviour.Properties.of())
                .defaultBlockstate().defaultLoot().defaultLang();
    }
	@Override
	public com.tterrag.registrate.util.entry.BlockEntry<T> register() {
		com.tterrag.registrate.util.entry.BlockEntry<T> be= (com.tterrag.registrate.util.entry.BlockEntry<T>) super.register();
		if(shaderRegistry!=null)
			shaderRegistry.accept(be);
		return be;
	}
	/*public FHBlockBuilder<T,P> shaderLike(Supplier<Block> vanilla){
		shaderLikeState(()->vanilla.get().defaultBlockState());
		return this;
	}
	public FHBlockBuilder<T,P> shaderLikeState(Supplier<BlockState> vanilla){
		DistExecutor.safeRunWhenOn(Dist.CLIENT, ()->()->{
			shaderRegistry=r->ShaderHelper.initializers.add(()->ShaderHelper.modBlockState2VanillaBlockMap.put(BlockEntry.parse(r.getId().toString()), vanilla.get()));
		});

		return this;
	}
	public FHBlockBuilder<T,P> shaderLooksLike(Function<BlockState,BlockState> blockState2Vanilla){
		DistExecutor.safeRunWhenOn(Dist.CLIENT, ()->()->{
			shaderRegistry=r->{
				for(BlockState bs:r.get().getStateDefinition().getPossibleStates()) {
					Map<String,String> properties=new HashMap<>();
					bs.getProperties().forEach(t->properties.put(t.getName(), ((Property)t).getName(bs.getValue(t))));
					ShaderHelper.modBlockState2VanillaBlockMap.put(new BlockEntry(new NamespacedId(super.getParent().getModid(),super.getName()),properties), blockState2Vanilla.apply(bs));
				}
			};
		});
		return this;
	}*/
	
}
