package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.registry.ModItems;
import com.feb.moregrid.util.MoreGridMath;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.VerticallyOrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** A non-rechargeable zinc-carbon dry-cell battery pack. */
public final class DryCellComponent extends VerticallyOrientableComponent
        implements IComponentGoggleInformation, IInteractableComponent {
    private static final double TICK_SECONDS = 1.0D / 20.0D;
    private static final double BATTERY_TIME_SCALE = 60.0D;
    private static final double REVERSE_DAMAGE_MULTIPLIER = 5.0D;

    /**
     * Runtime handles are rebuilt whenever the circuit board is baked. Weak
     * keys prevent removed/rebuilt boards from being retained by the component.
     */
    private static final Map<PlacedComponent, VoltageSourceCoupling> SOURCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final ComponentFootprint VERTICAL_FOOTPRINT =
            new ComponentFootprint.Builder(
                    3, 3,
                    "component." + MoreGrid.MOD_ID + ".dry_cell",
                    null
            )
                    .addPad(2, 1, 0, "Positive", "+")
                    .addPad(0, 1, 1, "Negative", "-")
                    .withItem()
                    .withOutline()
                    .build();

    public static final IntProperty CELL_COUNT = new IntProperty(
            MoreGrid.MOD_ID,
            "dry_cell_count",
            6,
            1,
            12
    );

    public static final FloatProperty CAPACITY_AH = new FloatProperty(
            MoreGrid.MOD_ID,
            "dry_cell_capacity",
            2.0F,
            0.1F,
            2.0F
    );

    public static final FloatProperty STATE_OF_CHARGE = (FloatProperty) new FloatProperty(
            MoreGrid.MOD_ID,
            "dry_cell_soc",
            1.0F,
            0.0F,
            1.0F
    ).hidden().cast();

    public static final CalculatedProperty<Float> OPEN_VOLTAGE = new CalculatedProperty<>(
            MoreGrid.MOD_ID,
            "dry_cell_open_voltage",
            placed -> (float) MoreGridMath.dryCellOpenVoltage(
                    placed.get(CELL_COUNT),
                    placed.get(STATE_OF_CHARGE)
            ),
            value -> String.format(Locale.ROOT, "%.2f V", value)
    );

    public static final CalculatedProperty<Float> INTERNAL_RESISTANCE = new CalculatedProperty<>(
            MoreGrid.MOD_ID,
            "dry_cell_internal_resistance",
            placed -> (float) MoreGridMath.dryCellInternalResistance(
                    placed.get(CELL_COUNT),
                    placed.get(STATE_OF_CHARGE)
            ),
            value -> String.format(Locale.ROOT, "%.3f Ω", value)
    );

    public DryCellComponent(ComponentFootprint footprint) {
        super(footprint, VERTICAL_FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(CELL_COUNT);
        properties.add(CAPACITY_AH);
        properties.add(OPEN_VOLTAGE);
        properties.add(INTERNAL_RESISTANCE);
        properties.add(STATE_OF_CHARGE);
        properties.add(power(5.0F));
    }

    @Override
    public void bake(
            @NotNull PlacedComponent placed,
            @NotNull ComponentCircuitBuilder builder,
            ThermalBuilder.@NotNull IEmitter thermals
    ) {
        int cells = placed.get(CELL_COUNT);
        double soc = MoreGridMath.clamp01(placed.get(STATE_OF_CHARGE));
        float resistance = (float) MoreGridMath.dryCellInternalResistance(cells, soc);

        /*
         * Use Power Grid's native voltage-source node. The previous Norton
         * wire implemented ISolverHook but did not report isSource() == true.
         * Power Grid skips solving and zeroes the state vector when a network's
         * source count is zero. current() still subtracted the Norton current,
         * so attaching a wire produced 0 V output together with phantom current,
         * rapid discharge and I²R self-heating. VoltageSourceCoupling is a real
         * source node and is handled by the solver's normal source path.
         */
        VoltageSourceCoupling source = builder.addInternalNode(
                VoltageSourceCoupling.class,
                builder.terminalNode(0),
                builder.terminalNode(1),
                resistance
        );
        source.setVoltage((float) MoreGridMath.dryCellOpenVoltage(cells, soc));
        source.setResistance(resistance);
        SOURCES.put(placed, source);

        // Battery internal loss is deliberately not registered with the board's
        // destructive thermal system. A short circuit may drain the cell, but an
        // open lead can no longer generate phantom I²R heat and destroy it.
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        VoltageSourceCoupling source = SOURCES.get(placed);
        if (source == null || !source.isConverged()) {
            return true;
        }

        // Power Grid's VoltageSourceCoupling reports discharge as negative
        // current (the same convention used by its built-in battery).
        double current = source.getCurrent();
        if (!Double.isFinite(current)) {
            return true;
        }

        double dischargeCurrent = Math.max(0.0D, -current);
        double reverseCurrent = Math.max(0.0D, current);
        double effectiveDrain = dischargeCurrent + REVERSE_DAMAGE_MULTIPLIER * reverseCurrent;

        double capacity = Math.max(0.1D, placed.get(CAPACITY_AH));
        double soc = MoreGridMath.clamp01(placed.get(STATE_OF_CHARGE));
        double consumedAh = effectiveDrain * TICK_SECONDS * BATTERY_TIME_SCALE / 3600.0D;
        soc = MoreGridMath.clamp01(soc - consumedAh / capacity);
        placed.set(STATE_OF_CHARGE, (float) soc);

        updateSource(placed, soc);
        return true;
    }


    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        boolean vertical = placed.footprint().getWidth()== 3 && placed.footprint().getHeight() == 3;
        return IInteractableComponent.extrudedFootprint(placed, vertical ? 6.0F / 16.0F : 2.5F / 16.0F);
    }

    /** Replace the installed pack with a fresh MoreGrid dry-cell item. */
    @Override
    public InteractionResult use(
            CircuitBoardBlockEntity be,
            PlacedComponent placed,
            Player player
    ) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.DRY_CELL.get())) {
            if (placed.get(STATE_OF_CHARGE) < 0.999F) {
                player.displayClientMessage(
                        Component.translatable("moregrid.message.dry_cell.replace_required"),
                        true
                );
            }
            return InteractionResult.PASS;
        }

        if (placed.get(STATE_OF_CHARGE) >= 0.999F) {
            return InteractionResult.PASS;
        }

        if (be.getLevel() != null && be.getLevel().isClientSide) {
            org.patryk3211.powergrid.circuits.components.Component.modelChanged(be.getBlockPos());
            return InteractionResult.SUCCESS;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        placed.set(STATE_OF_CHARGE, 1.0F);
        placed.notifyClients(STATE_OF_CHARGE);
        updateSource(placed, 1.0D);
        be.setChanged();

        if (be.getLevel() != null) {
            be.getLevel().playSound(
                    null,
                    be.getBlockPos(),
                    SoundEvents.ANVIL_USE,
                    SoundSource.BLOCKS,
                    0.30F,
                    1.65F
            );
        }
        player.displayClientMessage(Component.translatable("moregrid.message.dry_cell.replaced"), true);
        return InteractionResult.SUCCESS;
    }

    private static void updateSource(PlacedComponent placed, double soc) {
        VoltageSourceCoupling source = SOURCES.get(placed);
        if (source == null) {
            return;
        }
        int cells = placed.get(CELL_COUNT);
        source.setVoltage((float) MoreGridMath.dryCellOpenVoltage(cells, soc));
        source.setResistance((float) MoreGridMath.dryCellInternalResistance(cells, soc));
    }

    @Override
    public boolean addToGoggleTooltip(
            @NotNull PlacedComponent placed,
            @NotNull List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        double soc = MoreGridMath.clamp01(placed.get(STATE_OF_CHARGE));
        double voltage = MoreGridMath.dryCellOpenVoltage(placed.get(CELL_COUNT), soc);
        tooltip.add(Component.translatable("moregrid.tooltip.dry_cell.soc", Math.round(soc * 100.0D)));
        tooltip.add(Component.translatable(
                "moregrid.tooltip.dry_cell.voltage",
                String.format(Locale.ROOT, "%.2f", voltage)
        ));
        if (soc < 0.999D) {
            tooltip.add(Component.translatable("moregrid.tooltip.dry_cell.replace"));
        }
        return true;
    }
}
