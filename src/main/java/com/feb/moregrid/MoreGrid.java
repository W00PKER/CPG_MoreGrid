package com.feb.moregrid;

import com.feb.moregrid.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MoreGrid.MOD_ID)
public final class MoreGrid {
    public static final String MOD_ID = "moregrid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MoreGrid(IEventBus modBus) {
        ModItems.ITEMS.register(modBus);
        ModItems.CREATIVE_TABS.register(modBus);
    }

}
