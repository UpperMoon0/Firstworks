package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import com.nstut.firstworks.content.loom.LoomBlock;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class FirstworksJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BarrelProgressProvider.INSTANCE, BarrelBlockEntity.class);
        registration.registerBlockDataProvider(LoomProgressProvider.INSTANCE, LoomBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BarrelProgressProvider.INSTANCE, BarrelBlock.class);
        registration.registerBlockComponent(LoomProgressProvider.INSTANCE, LoomBlock.class);
    }
}
