package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.BellowsBlock;
import com.nstut.firstworks.content.BellowsBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum BellowsAirProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("bellows_air");
    private static final String CONNECTED = "FirstworksBellowsConnected";
    private static final String AIR = "FirstworksBellowsAir";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BellowsBlockEntity)) {
            return;
        }
        Direction facing = accessor.getBlockState().getValue(BellowsBlock.FACING);
        var furnace = accessor.getLevel().getBlockEntity(accessor.getPosition().relative(facing));
        if (furnace instanceof WorkshopBlockEntity workshop
                && WorkshopRecipe.CRUCIBLE_FURNACE.equals(workshop.station())) {
            data.putBoolean(CONNECTED, true);
            data.putInt(AIR, workshop.getStokeTicks());
        } else {
            data.putBoolean(CONNECTED, false);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean(CONNECTED)) {
            tooltip.add(Component.translatable("jade.firstworks.bellows.disconnected")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        tooltip.add(Component.translatable("jade.firstworks.bellows.connected"));
        tooltip.add(Component.translatable("jade.firstworks.bellows.air_reserve",
                (data.getInt(AIR) + 19) / 20));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
