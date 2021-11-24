package fi.dy.masa.tweakeroo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import fi.dy.masa.tweakeroo.config.Configs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen.class)
public abstract class MixinAbstractInventoryScreen<T extends net.minecraft.screen.ScreenHandler>
       extends net.minecraft.client.gui.screen.ingame.HandledScreen<T>
{
    public MixinAbstractInventoryScreen(
            T container,
            net.minecraft.entity.player.PlayerInventory playerInventory,
            net.minecraft.text.Text textComponent)
    {
        super(container, playerInventory, textComponent);
    }

    @Inject(method = "hideStatusEffectHud", at = @At("RETURN"))
    private void disableStatusEffectRendering(CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Disable.DISABLE_INVENTORY_EFFECTS.getBooleanValue())
        {
            cir.setReturnValue(true);
        }
    }
}
