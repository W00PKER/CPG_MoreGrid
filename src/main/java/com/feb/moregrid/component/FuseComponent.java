package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.client.FuseBlownRenderer;
import com.feb.moregrid.registry.ModItems;
import com.feb.moregrid.util.MoreGridMath;
import com.google.common.collect.ImmutableCollection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

/** A replaceable cartridge fuse with an I²t-style trip curve. */
public final class FuseComponent extends OrientableComponent
        implements IComponentGoggleInformation, IInteractableComponent, IRenderedComponent {
    private static final double TICK_SECONDS = 1.0D / 20.0D;

    public static final FloatProperty RATED_CURRENT = new FloatProperty(
            MoreGrid.MOD_ID,
            "fuse_rated_current",
            2.0F,
            0.25F,
            32.0F
    );

    /** Approximate opening time at exactly twice the rated current. */
    public static final FloatProperty TRIP_TIME_2X = new FloatProperty(
            MoreGrid.MOD_ID,
            "fuse_trip_time_2x",
            1.5F,
            0.05F,
            30.0F
    );

    public static final FloatProperty DAMAGE = new FloatProperty(
            MoreGrid.MOD_ID,
            "fuse_damage",
            0.0F,
            0.0F,
            1.0F
    ).hidden().cast();

    public static final BooleanProperty BLOWN = new BooleanProperty(
            MoreGrid.MOD_ID,
            "fuse_blown"
    ).hidden().cast();

    public FuseComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RATED_CURRENT);
        properties.add(TRIP_TIME_2X);
        properties.add(DAMAGE);
        properties.add(BLOWN);
    }

    @Override
    public void bake(
            @NotNull PlacedComponent placed,
            @NotNull ComponentCircuitBuilder builder,
            ThermalBuilder.@NotNull IEmitter thermals
    ) {
        float resistance = MoreGridMath.fuseResistance(placed.get(RATED_CURRENT));
        SwitchedWire fuseWire = builder.connectSwitch(
                resistance,
                builder.terminalNode(0),
                builder.terminalNode(1),
                !placed.get(BLOWN)
        );
        placed.add(fuseWire);

        /*
         * Do not register the cartridge with the destructive thermal system.
         * Overcurrent is handled by the I²t state below, leaving a blown/open
         * component on the board that can be replaced instead of deleting or
         * exploding the whole part through generic thermal damage.
         */
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if (placed.wires.isEmpty()) {
            return true;
        }

        SwitchedWire wire = (SwitchedWire) placed.wires.getFirst();
        if (placed.get(BLOWN)) {
            if (wire.getState()) {
                wire.setState(false);
            }
            return true;
        }
        if (!wire.isConverged()) {
            return true;
        }

        double current = Math.abs(wire.current());
        if (!Double.isFinite(current)) {
            return true;
        }

        double rated = Math.max(0.25D, placed.get(RATED_CURRENT));
        double tripTime = Math.max(0.05D, placed.get(TRIP_TIME_2X));
        double damage = placed.get(DAMAGE);

        double increment = MoreGridMath.fuseDamageIncrement(current, rated, tripTime, TICK_SECONDS);
        if (increment > 0.0D) {
            damage += increment;
        } else {
            damage -= MoreGridMath.fuseCooling(tripTime, TICK_SECONDS);
        }
        damage = MoreGridMath.clamp01(damage);

        if (damage >= 1.0D) {
            wire.setState(false);
            placed.set(BLOWN, true);
            placed.set(DAMAGE, 1.0F);
            placed.notifyClients(BLOWN);
            placed.notifyClients(DAMAGE);
            return true;
        }

        placed.set(DAMAGE, (float) damage);
        return true;
    }

    /**
     * Power Grid calls this after rendering the normal component model. A
     * full-bright scorch/broken-filament marker remains visible until repair.
     */
    @Override
    public void render(
            CircuitBoardBlockEntity be,
            PlacedComponent placed,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        if (!placed.get(BLOWN)) {
            FuseBlownRenderer.markIntact(placed);
            return;
        }

        ComponentFootprint currentFootprint = footprint(placed);
        FuseBlownRenderer.render(
                be,
                placed,
                currentFootprint.getWidth() / 32.0F,
                currentFootprint.getHeight() / 32.0F,
                poseStack,
                bufferSource,
                overlay
        );
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 1.0F / 16.0F);
    }

    /**
     * Replace a blown cartridge by right-clicking it while holding a fresh
     * MoreGrid fuse. One item is consumed outside creative mode.
     */
    @Override
    public InteractionResult use(
            CircuitBoardBlockEntity be,
            PlacedComponent placed,
            Player player
    ) {
        if (!placed.get(BLOWN)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.FUSE.get())) {
            player.displayClientMessage(
                    Component.translatable("moregrid.message.fuse.repair_required"),
                    true
            );
            return InteractionResult.FAIL;
        }

        if (be.getLevel() != null && be.getLevel().isClientSide) {
            org.patryk3211.powergrid.circuits.components.Component.modelChanged(be.getBlockPos());
            return InteractionResult.SUCCESS;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        placed.set(BLOWN, false);
        placed.set(DAMAGE, 0.0F);
        placed.notifyClients(BLOWN);
        placed.notifyClients(DAMAGE);
        updateWireState(placed);
        be.setChanged();

        if (be.getLevel() != null) {
            ModdedSoundEvents.FUSE_INSTALL.playOnServer(be.getLevel(), be.getBlockPos());
        }
        player.displayClientMessage(Component.translatable("moregrid.message.fuse.repaired"), true);
        return InteractionResult.SUCCESS;
    }

    private static void updateWireState(PlacedComponent placed) {
        if (!placed.wires.isEmpty() && placed.wires.getFirst() instanceof SwitchedWire wire) {
            wire.setState(!placed.get(BLOWN));
        }
    }

    @Override
    public boolean addToGoggleTooltip(
            @NotNull PlacedComponent placed,
            @NotNull List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        Lang.text("Fuse Information:")
                .forGoggles(tooltip);

        if (placed.get(BLOWN)) {
            Lang.text("BLOWN")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip, 1);
            Lang.text("Right-click with fuse to replace")
                    .style(ChatFormatting.YELLOW)
                    .forGoggles(tooltip, 1);
        } else {
            Lang.text("I²t Damage:")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
            Lang.number(Math.round(placed.get(DAMAGE) * 100.0F))
                    .style(ChatFormatting.AQUA)
                    .add(Lang.text("%"))
                    .forGoggles(tooltip, 1);
        }
        return true;
    }
}
