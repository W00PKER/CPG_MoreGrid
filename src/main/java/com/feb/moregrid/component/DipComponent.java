package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.Collection;
import java.util.List;

public final class DipComponent extends OrientableComponent
        implements IInteractableComponent {

    public static final BooleanProperty STATE = new BooleanProperty(
            MoreGrid.MOD_ID, "dip_state");

    public DipComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(STATE);
        properties.add(current(1));
    }

    @Override
    public void bake(
            @NotNull PlacedComponent placed,
            @NotNull ComponentCircuitBuilder builder,
            ThermalBuilder.@NotNull IEmitter thermals
    ) {
        var wire = builder.connectSwitch(
                0.1f,
                builder.terminalNode(0),
                builder.terminalNode(1),
                placed.get(STATE)
        );
        placed.add(wire);
        thermals.builder()
                .setMaxCurrent(1, 0.1f, 150)
                .setThermalMass(0.005f)
                .addHeatSource(wire);
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 0.4f / 16f);
    }

    @Override
    public InteractionResult use(
            CircuitBoardBlockEntity be,
            PlacedComponent placed,
            Player player
    ) {
        var newState = !placed.get(STATE);
        placed.set(STATE, newState);

        assert be.getLevel() != null;
        if (be.getLevel().isClientSide) {
            org.patryk3211.powergrid.circuits.components.Component.modelChanged(be.getBlockPos());
        } else {
            if (newState) {
                ModdedSoundEvents.MICROSWITCH_ON.playOnServer(be.getLevel(), be.getBlockPos());
            } else {
                ModdedSoundEvents.MICROSWITCH_OFF.playOnServer(be.getLevel(), be.getBlockPos());
            }
            placed.notifyClients(STATE);
            stateUpdated(placed);
        }
        be.setChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        super.stateUpdated(placed);
        if (placed.wires.isEmpty()) return;
        ((SwitchedWire) placed.wires.getFirst()).setState(placed.get(STATE));
        placed.onClientWorld(() -> world -> modelChanged(placed.getPos()));
    }

    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
        return component.get(STATE)
                ? ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "dip_on")
                : ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "dip");
    }

    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        return List.of(
                ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "dip"),
                ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "dip_on")
        );
    }
}