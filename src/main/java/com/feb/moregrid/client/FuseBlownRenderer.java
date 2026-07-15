package com.feb.moregrid.client;

import com.feb.moregrid.MoreGrid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Client-side persistent and transient visuals for an opened cartridge fuse. */
public final class FuseBlownRenderer {
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MoreGrid.MOD_ID,
            "textures/misc/fuse_blown_overlay.png"
    );

    private static final float MARKER_SIZE = 2.35F / 16.0F;
    private static final float MARKER_Y = 1.085F / 16.0F;
    private static final int FULL_BRIGHT = 0x00F000F0;

    /**
     * A weak client-only state cache makes the smoke/pop happen once per
     * intact -> blown transition while the persistent marker renders every frame.
     */
    private static final Map<PlacedComponent, Boolean> LAST_STATE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private FuseBlownRenderer() {
    }

    public static void markIntact(PlacedComponent placed) {
        LAST_STATE.put(placed, false);
    }

    public static void render(
            CircuitBoardBlockEntity board,
            PlacedComponent placed,
            float centerX,
            float centerZ,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int overlay
    ) {
        playTransitionEffect(board, placed);

        float half = MARKER_SIZE * 0.5F;
        float x0 = centerX - half;
        float x1 = centerX + half;
        float z0 = centerZ - half;
        float z1 = centerZ + half;

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(OVERLAY_TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        vertex(pose, consumer, x0, MARKER_Y, z0, 0.0F, 0.0F, overlay);
        vertex(pose, consumer, x0, MARKER_Y, z1, 0.0F, 1.0F, overlay);
        vertex(pose, consumer, x1, MARKER_Y, z1, 1.0F, 1.0F, overlay);
        vertex(pose, consumer, x1, MARKER_Y, z0, 1.0F, 0.0F, overlay);
    }

    private static void playTransitionEffect(CircuitBoardBlockEntity board, PlacedComponent placed) {
        Boolean previous = LAST_STATE.put(placed, true);
        // A fuse that was already blown when its chunk loaded only receives the
        // persistent marker. Effects are reserved for a visible false -> true transition.
        if (previous == null || previous) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockPos pos = board.getBlockPos();
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.03D;
        double z = pos.getZ() + 0.5D;

        level.playLocalSound(
                x,
                y,
                z,
                SoundEvents.FIRECHARGE_USE,
                SoundSource.BLOCKS,
                0.42F,
                1.75F,
                false
        );

        for (int i = 0; i < 7; ++i) {
            double ox = (level.random.nextDouble() - 0.5D) * 0.22D;
            double oz = (level.random.nextDouble() - 0.5D) * 0.22D;
            level.addParticle(
                    ParticleTypes.SMOKE,
                    x + ox,
                    y + level.random.nextDouble() * 0.08D,
                    z + oz,
                    ox * 0.12D,
                    0.015D + level.random.nextDouble() * 0.025D,
                    oz * 0.12D
            );
        }

        for (int i = 0; i < 5; ++i) {
            level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    x,
                    y + 0.015D,
                    z,
                    (level.random.nextDouble() - 0.5D) * 0.10D,
                    level.random.nextDouble() * 0.06D,
                    (level.random.nextDouble() - 0.5D) * 0.10D
            );
        }
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int overlay
    ) {
        consumer.addVertex(pose.pose(), x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setUv2(FULL_BRIGHT & 65535, FULL_BRIGHT >> 16 & 65535)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
