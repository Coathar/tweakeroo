package fi.dy.masa.tweakeroo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.WorldRenderer;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Redirect(method = "setUpTerrain", at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/render/ChunkRenderDispatcher;updateCameraPosition(DD)V"))
    private void preventRenderChunkPositionUpdates(ChunkRenderDispatcher dispatcher, double viewEntityX, double viewEntityZ)
    {
        // Don't update the RenderChunk positions when moving around in the Free Camera mode.
        // Otherwise the chunks would become empty when they are outside the render range
        // from the camera entity, ie. on the other side of the actual player.
        if (FeatureToggle.TWEAK_FREE_CAMERA.getBooleanValue() == false)
        {
            dispatcher.updateCameraPosition(viewEntityX, viewEntityZ);
        }
    }
}