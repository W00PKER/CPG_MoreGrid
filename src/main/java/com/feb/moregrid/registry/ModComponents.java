package com.feb.moregrid.registry;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.component.DryCellComponent;
import com.feb.moregrid.component.FuseComponent;
import com.feb.moregrid.component.SCRComponent;
import com.feb.moregrid.component.TransformerComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;

/** Registers MoreGrid components in Power Grid's custom component registry. */
@EventBusSubscriber(modid = MoreGrid.MOD_ID)
public final class ModComponents {

    private ModComponents() {
    }

    private static TransformerComponent buildTransformer() {
        ComponentFootprint footprint = new ComponentFootprint.Builder(
                5,
                4,
                "component." + MoreGrid.MOD_ID + ".transformer",
                null
        )
                .addPad(0, 0, 0, "Primary 1", "P1")
                .addPad(0, 3, 1, "Primary 2", "P2")
                .addPad(4, 0, 2, "Secondary 1", "S1")
                .addPad(4, 3, 3, "Secondary 2", "S2")
                .withItem()
                .withOutline()
                .build();
        return new TransformerComponent(footprint);
    }

    private static FuseComponent buildFuse() {
        ComponentFootprint footprint = new ComponentFootprint.Builder(
                5,
                3,
                "component." + MoreGrid.MOD_ID + ".fuse",
                null
        )
                .addPad(0, 1, 0)
                .addPad(4, 1, 1)
                .withItem()
                .withOutline()
                .build();
        return new FuseComponent(footprint);
    }

    private static SCRComponent buildScr() {
        ComponentFootprint footprint = new ComponentFootprint.Builder(
                3,
                3,
                "component." + MoreGrid.MOD_ID + ".scr",
                null
        )
                .addPad(0, 1, 0, "Anode", "+")
                .addPad(2, 1, 1, "Cathode", "-")
                .addPad(1, 0, 2, "Gate", "G")
                .withItem()
                .withOutline()
                .build();
        return new SCRComponent(footprint);
    }

    private static DryCellComponent buildDryCell() {
        ComponentFootprint footprint = new ComponentFootprint.Builder(
                5,
                3,
                "component." + MoreGrid.MOD_ID + ".dry_cell",
                null
        )
                .addPad(0, 1, 0, "Positive", "+")
                .addPad(4, 1, 1, "Negative", "-")
                .withItem()
                .withOutline()
                .build();
        return new DryCellComponent(footprint);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(ComponentRegistry.REGISTRY_KEY)) {
            return;
        }
        register(event, "transformer", buildTransformer());
        register(event, "fuse", buildFuse());
        register(event, "scr", buildScr());
        register(event, "dry_cell", buildDryCell());
    }

    private static void register(RegisterEvent event, String id, org.patryk3211.powergrid.circuits.components.Component component) {
        event.register(
                ComponentRegistry.REGISTRY_KEY,
                ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, id),
                () -> component
        );
    }
}
