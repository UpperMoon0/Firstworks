package com.nstut.firstworks.gametest;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
/** Keep unrelated recipe/fuel regression fixtures preheated; thermal behavior has dedicated tests. */
final class ThermalTestSupport {
    static void tickHot(ServerLevel level,BlockPos pos,WorkshopBlockEntity station,int ticks) {
        boolean needsHeat=station.isHot() && station.activeRecipe().map(h->h.value().requiredTemperature()>0).orElse(false);
        if(needsHeat) {
            var saved=station.saveWithoutMetadata(level.registryAccess());
            saved.putDouble("TemperatureCelsius",1150);
            saved.putInt("StokeTicks",900);
            station.loadWithComponents(saved,level.registryAccess());
        }
        for(int i=0;i<ticks;i++) {
            if(needsHeat && i%100==0) station.stoke(900);
            WorkshopBlockEntity.serverTick(level,pos,station.getBlockState(),station);
        }
    }
}
