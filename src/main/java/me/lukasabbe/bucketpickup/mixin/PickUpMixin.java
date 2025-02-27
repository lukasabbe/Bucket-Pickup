package me.lukasabbe.bucketpickup.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.server.command.GiveCommand;
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
public abstract class PickUpMixin {
    @Shadow @Nullable public Entity cameraEntity;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientWorld world;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Shadow @Nullable public abstract ClientPlayNetworkHandler getNetworkHandler();

    @Unique public HitResult crosshairTargetFluid;

    @Inject(method = "tick", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;updateCrosshairTarget(F)V",shift = At.Shift.AFTER))
    public void updateFluidCrosshair(CallbackInfo ci){
        if(cameraEntity == null) return;
        crosshairTargetFluid = this.cameraEntity.raycast(20f,0f,true);
    }

    @Inject(method = "doItemPick", at=@At("HEAD"), cancellable = true)
    public void pickBucket(CallbackInfo ci){
        if(crosshairTargetFluid != null) {

            final HitResult.Type type = crosshairTargetFluid.getType();
            if (type != HitResult.Type.MISS && type == HitResult.Type.BLOCK && player.isSneaking()) {

                BlockPos blockPos = ((BlockHitResult) this.crosshairTargetFluid).getBlockPos();
                FluidState fluidState = world.getFluidState(blockPos);

                if (fluidState != null) {

                    final BlockState blockState = fluidState.getBlockState();
                    if (blockState.isOf(Blocks.WATER) || blockState.isOf(Blocks.LAVA)) {

                        PlayerInventory playerInventory = player.getInventory();
                        final ItemStack bucketItem = Items.BUCKET.getDefaultStack();
                        int i = playerInventory.getSlotWithStack(bucketItem);

                        if (i != -1) {
                            if (PlayerInventory.isValidHotbarIndex(i)) {
                                playerInventory.selectedSlot = i;
                            } else {
                                playerInventory.swapSlotWithHotbar(i);
                            }
                        }
                        ci.cancel();
                    }
                }
            }
        }
    }
}
