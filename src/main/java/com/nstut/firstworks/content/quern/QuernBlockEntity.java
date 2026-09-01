package com.nstut.firstworks.content.quern;

import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.registry.*;
import com.nstut.firstworks.compat.OptionalIntegrations;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public final class QuernBlockEntity extends BlockEntity {
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;
    private int progress; private long rotationSteps; private double clientPrevRotation, clientRotation, rotationTarget; private boolean clientInitialized;
    private final IItemHandler inputHandler=new QuernItemHandler(true,false),outputHandler=new QuernItemHandler(false,true),combinedHandler=new QuernItemHandler(true,true);
    public QuernBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.QUERN.get(),pos,state);}
    public static void clientTick(Level level,BlockPos pos,BlockState state,QuernBlockEntity q){q.clientPrevRotation=q.clientRotation;if(q.clientRotation<q.rotationTarget){double diff=q.rotationTarget-q.clientRotation;q.clientRotation=diff<1D?q.rotationTarget:q.clientRotation+Math.min(diff,9D);}else if(q.clientRotation>q.rotationTarget){q.clientRotation=q.rotationTarget;q.clientPrevRotation=q.rotationTarget;}}
    private boolean tryBegin(RecipeHolder<QuernGrindingRecipe> h){if(progress!=0)return true;return !(level instanceof ServerLevel s&&OptionalIntegrations.fireQuernGrindingStarting(s,this,h.id(),h.value(),input.copy(),h.value().result()));}
    public boolean work(){Optional<RecipeHolder<QuernGrindingRecipe>> h=recipe();if(h.isEmpty()||!output.isEmpty()||input.getCount()<h.get().value().inputCount()||!tryBegin(h.get()))return false;int multiplier=getBlockState().is(ModBlocks.ROTARY_QUERN.get())?4:1;progress+=FirstworksConfig.QUERN_MANUAL_WORK_PER_CRANK.get()*multiplier;rotationSteps+=multiplier;level.playSound(null,worldPosition,SoundEvents.GRINDSTONE_USE,SoundSource.BLOCKS,.38F,.72F+level.random.nextFloat()*.1F);if(level instanceof ServerLevel sl)sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,input.copyWithCount(1)),worldPosition.getX()+.5,worldPosition.getY()+.55,worldPosition.getZ()+.5,3,.14,.03,.14,.015);if(progress>=h.get().value().work())complete(h.get());else sync();return true;}
    private void complete(RecipeHolder<QuernGrindingRecipe> h){QuernGrindingRecipe r=h.value();ItemStack consumed=input.copyWithCount(r.inputCount());input.shrink(r.inputCount());if(input.isEmpty())input=ItemStack.EMPTY;output=r.result().copy();progress=0;if(level instanceof ServerLevel s)OptionalIntegrations.fireQuernGrindingCompleted(s,this,h.id(),r,consumed,output);level.playSound(null,worldPosition,SoundEvents.PLAYER_LEVELUP,SoundSource.BLOCKS,.25F,1.55F);sync();}
    public Optional<RecipeHolder<QuernGrindingRecipe>> findRecipeForIngredient(ItemStack stack){if(stack.isEmpty()||level==null)return Optional.empty();return level.getRecipeManager().getAllRecipesFor(ModRecipes.QUERN_GRINDING_TYPE.get()).stream().filter(h->h.value().ingredient().test(stack)).findFirst();}
    private Optional<RecipeHolder<QuernGrindingRecipe>> recipe(){if(level==null||input.isEmpty())return Optional.empty();return level.getRecipeManager().getRecipeFor(ModRecipes.QUERN_GRINDING_TYPE.get(),new SingleRecipeInput(input),level);}
    public boolean canInsert(ItemStack stack){if(!output.isEmpty()||stack.isEmpty())return false;Optional<RecipeHolder<QuernGrindingRecipe>> r=findRecipeForIngredient(stack);return r.isPresent()&&(input.isEmpty()||ItemStack.isSameItemSameComponents(input,stack))&&input.getCount()<r.get().value().inputCount();}
    public void insert(ItemStack stack,boolean creative){if(!canInsert(stack))return;if(input.isEmpty())input=stack.copyWithCount(1);else input.grow(1);if(!creative)stack.shrink(1);reset();sync();}
    public boolean takeOutput(Player p){if(output.isEmpty())return false;p.getInventory().placeItemBackInInventory(output.copy());output=ItemStack.EMPTY;sync();return true;}public boolean takeInput(Player p){if(input.isEmpty())return false;p.getInventory().placeItemBackInInventory(input.copy());input=ItemStack.EMPTY;reset();sync();return true;}private void reset(){progress=0;}
    public ItemStack getInput(){return input;}public ItemStack getOutput(){return output;}public int getProgress(){return progress;}public float getRotation(float partial){if(level!=null&&level.isClientSide){double i=clientPrevRotation+(clientRotation-clientPrevRotation)*partial;return(float)(i%360D);}return Math.floorMod(rotationSteps,8L)*45F;}public int requiredWork(){return recipe().map(r->r.value().work()).orElse(0);}
    public IItemHandler getItemHandler(@Nullable Direction side){if(side==Direction.UP)return inputHandler;if(side==Direction.DOWN)return outputHandler;return combinedHandler;}
    private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider regs){super.saveAdditional(tag,regs);if(!input.isEmpty())tag.put("Input",input.save(regs));if(!output.isEmpty())tag.put("Output",output.save(regs));tag.putInt("Progress",progress);tag.putLong("RotationSteps",rotationSteps);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider regs){super.loadAdditional(tag,regs);input=ItemStack.parseOptional(regs,tag.getCompound("Input"));output=ItemStack.parseOptional(regs,tag.getCompound("Output"));progress=tag.getInt("Progress");rotationSteps=tag.contains("RotationSteps")?tag.getLong("RotationSteps"):Math.round(tag.getFloat("Rotation")/45F);if(level!=null&&level.isClientSide){rotationTarget=rotationSteps*45D;if(!clientInitialized){clientRotation=rotationTarget;clientPrevRotation=rotationTarget;clientInitialized=true;}}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider regs){CompoundTag t=new CompoundTag();saveAdditional(t,regs);return t;}@Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    private final class QuernItemHandler implements IItemHandler{private final boolean allowInput,allowOutput;private QuernItemHandler(boolean i,boolean o){allowInput=i;allowOutput=o;}@Override public int getSlots(){return 2;}@Override public ItemStack getStackInSlot(int slot){return slot==0?input.copy():slot==1?output.copy():ItemStack.EMPTY;}@Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(slot!=0||!allowInput||!canInsert(stack))return stack;int required=findRecipeForIngredient(stack).map(h->h.value().inputCount()).orElse(0);int accepted=Math.min(required-input.getCount(),stack.getCount());if(accepted<=0)return stack;if(!simulate){if(input.isEmpty())input=stack.copyWithCount(accepted);else input.grow(accepted);reset();sync();}return stack.copyWithCount(stack.getCount()-accepted);}@Override public ItemStack extractItem(int slot,int amount,boolean simulate){if(slot!=1||!allowOutput||output.isEmpty()||amount<=0)return ItemStack.EMPTY;int extracted=Math.min(amount,output.getCount());ItemStack result=output.copyWithCount(extracted);if(!simulate){output.shrink(extracted);if(output.isEmpty())output=ItemStack.EMPTY;sync();}return result;}@Override public int getSlotLimit(int slot){return slot==0?findRecipeForIngredient(input).map(h->h.value().inputCount()).orElse(64):64;}@Override public boolean isItemValid(int slot,ItemStack stack){return slot==0&&allowInput&&canInsert(stack);}}
}
