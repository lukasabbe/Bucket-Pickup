package me.lukasabbe.bucketpickup.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class PickUpMixin {
    @Shadow @Nullable public Entity cameraEntity;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Unique
    public HitResult crosshairTargetFluid;
    @Inject(method = "tick", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;updateCrosshairTarget(F)V",shift = At.Shift.AFTER))
    public void updateFluidCrosshair(CallbackInfo ci){
        if(cameraEntity == null) return;
        crosshairTargetFluid = this.cameraEntity.raycast(20f,0f,true);
    }

    @Inject(method = "doItemPick", at=@At("HEAD"), cancellable = true)
    public void pickBucket(CallbackInfo ci){
        final MinecraftClient client = (MinecraftClient) (Object) this;
        if(crosshairTargetFluid != null && crosshairTargetFluid.getType() != HitResult.Type.MISS && crosshairTargetFluid.getType() == HitResult.Type.BLOCK && client.player.isSneaking()){
            BlockPos blockPos = ((BlockHitResult)this.crosshairTargetFluid).getBlockPos();
            FluidState fluidState = client.world.getFluidState(blockPos);
            if(fluidState != null && (fluidState.getBlockState().isOf(Blocks.WATER) || fluidState.getBlockState().isOf(Blocks.LAVA))){
                PlayerInventory playerInventory = client.player.getInventory();
                final ItemStack bucketItem = Items.BUCKET.getDefaultStack();
                int i = playerInventory.getSlotWithStack(bucketItem);
                if(client.player.getAbilities().creativeMode){
                    playerInventory.addPickBlock(bucketItem);
                    client.interactionManager.clickCreativeStack(client.player.getStackInHand(Hand.MAIN_HAND), 36 + playerInventory.selectedSlot);
                }
                else if(i!=-1){
                    if (PlayerInventory.isValidHotbarIndex(i)) {
                        playerInventory.selectedSlot = i;
                    } else {
                        client.interactionManager.pickFromInventory(i);
                    }
                }
                ci.cancel();
            }
        }
    }
}
