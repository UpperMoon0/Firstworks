package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.charcoal.CharcoalMoundData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum CharcoalMoundProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("charcoal_mound");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getLevel() instanceof ServerLevel serverLevel)) return;
        CharcoalMoundData.get(serverLevel).getStatusAt(accessor.getPosition(), serverLevel.getGameTime())
                .ifPresent(status -> {
                    data.putString("MoundPhase", status.phase().name());
                    data.putInt("MoundLogs", status.logCount());
                    data.putLong("MoundRemainingTicks", status.remainingTicks());
                    data.putInt("MoundExpectedYield", status.expectedYield());
                    int totalTicks = status.phase() == CharcoalMoundData.Phase.CARBONIZING
                            ? com.nstut.firstworks.FirstworksConfig.CHARCOAL_CARBONIZE_DURATION.get()
                            : com.nstut.firstworks.FirstworksConfig.CHARCOAL_SEAL_WINDOW.get();
                    data.putInt("MoundTotalTicks", totalTicks);
                });
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("MoundPhase")) return;

        String phaseName = data.getString("MoundPhase");
        int logs = data.getInt("MoundLogs");
        long remainingTicks = data.getLong("MoundRemainingTicks");
        int totalTicks = Math.max(1, data.getInt("MoundTotalTicks"));

        tooltip.add(Component.translatable("jade.firstworks.charcoal_mound.title")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("jade.firstworks.charcoal_mound.logs", logs)
                .withStyle(ChatFormatting.GRAY));

        int secondsLeft = Math.max(0, (int) ((remainingTicks + 19) / 20));
        String timeFormatted = "%d:%02d".formatted(secondsLeft / 60, secondsLeft % 60);

        if ("WAITING_FOR_SEAL".equals(phaseName)) {
            tooltip.add(Component.translatable("jade.firstworks.charcoal_mound.waiting_seal")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("jade.firstworks.charcoal_mound.seal_within", timeFormatted)
                    .withStyle(ChatFormatting.WHITE));
        } else if ("CARBONIZING".equals(phaseName)) {
            int expectedYield = data.getInt("MoundExpectedYield");
            float progress = Mth.clamp(1.0F - (float) remainingTicks / totalTicks, 0.0F, 1.0F);

            tooltip.add(Component.translatable("jade.firstworks.charcoal_mound.carbonizing")
                    .withStyle(ChatFormatting.WHITE));
            tooltip.add(IElementHelper.get().progress(
                    progress,
                    Component.translatable("jade.firstworks.charcoal_mound.remaining", timeFormatted)
                            .withStyle(ChatFormatting.WHITE),
                    IElementHelper.get().progressStyle().color(0xFF302B27, 0xFF51463C).textColor(0xFFFFFFFF),
                    BoxStyle.getTransparent(), false).size(new Vec2(140, 12)));
            tooltip.add(Component.translatable("jade.firstworks.charcoal_mound.expected_yield", expectedYield)
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
