package com.lukasabbe.bucketpickup.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Minecraft.class)
public class PickUpMixin {
    @Shadow public Entity cameraEntity;

    @Shadow public LocalPlayer player;
    @Shadow public ClientLevel level;
    @Unique
    public HitResult crosshairTargetFluid;

    @Inject(method = "tick", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V", shift = At.Shift.AFTER))
    public void updateFluidCrosshair(CallbackInfo ci){
        if(this.cameraEntity == null) return;
        crosshairTargetFluid = this.cameraEntity.pick(20f,0f,true);
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    public void pickBucket(CallbackInfo ci){

        if(!player.isShiftKeyDown()) return;

        if(crosshairTargetFluid == null) return;

        final HitResult.Type type = crosshairTargetFluid.getType();
        if(type == HitResult.Type.MISS) return;

        BlockPos blockPos = ((BlockHitResult)crosshairTargetFluid).getBlockPos();
        FluidState fluidState = level.getFluidState(blockPos);

        final BlockState blockState = fluidState.createLegacyBlock();
        if(!(blockState.is(Blocks.WATER) || blockState.is(Blocks.LAVA))) return;

        Inventory inventory = player.getInventory();
        final ItemStack bucketItem = Items.BUCKET.getDefaultInstance();
        int i = inventory.findSlotMatchingItem(bucketItem);

        if(i == -1) return;

        if(Inventory.isHotbarSlot(i)){
            inventory.setSelectedSlot(i);
        }else{
            inventory.pickSlot(i);
        }
        ci.cancel();
    }
}
