package com.feb.moregrid.registry;

import com.feb.moregrid.MoreGrid;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreGrid.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoreGrid.MOD_ID);

    public static final DeferredHolder<Item, Item> TRANSFORMER =
            ITEMS.registerSimpleItem("transformer", new Item.Properties());
    public static final DeferredHolder<Item, Item> FUSE =
            ITEMS.registerSimpleItem("fuse", new Item.Properties());
    public static final DeferredHolder<Item, Item> SCR =
            ITEMS.registerSimpleItem("scr", new Item.Properties());
    public static final DeferredHolder<Item, Item> DRY_CELL =
            ITEMS.registerSimpleItem("dry_cell", new Item.Properties().stacksTo(16));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.moregrid"))
                    .icon(() -> TRANSFORMER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(TRANSFORMER.get());
                        output.accept(FUSE.get());
                        output.accept(SCR.get());
                        output.accept(DRY_CELL.get());
                    })
                    .build());

    private ModItems() {
    }
}
