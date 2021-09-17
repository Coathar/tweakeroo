package fi.dy.masa.tweakeroo.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.tweakeroo.Reference;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.config.Hotkeys;
import fi.dy.masa.tweakeroo.mixin.IMixinAxeItem;
import fi.dy.masa.tweakeroo.mixin.IMixinClientWorld;
import fi.dy.masa.tweakeroo.mixin.IMixinCommandBlockExecutor;
import fi.dy.masa.tweakeroo.renderer.RenderUtils;

public class MiscUtils
{
    // name;blocks;biome;options;iconitem
    public static final Pattern PATTERN_WORLD_PRESET = Pattern.compile("^(?<name>[a-zA-Z0-9_/&*#!=()\\[\\]{} -]+);(?<blocks>[a-z0-9_:.*,-]+);(?<biome>[a-z0-9_:.-]+);(?<options>[a-z0-9_, ()=]*);(?<icon>[a-z0-9_:.-]+)$");

    private static net.minecraft.text.Text[] previousSignText;
    private static String previousChatText = "";
    private static final Date DATE = new Date();
    private static double lastRealPitch;
    private static double lastRealYaw;
    private static double mouseSensitivity = -1.0F;
    private static boolean zoomActive;

    public static boolean isZoomActive()
    {
        return FeatureToggle.TWEAK_ZOOM.getBooleanValue() &&
               Hotkeys.ZOOM_ACTIVATE.getKeybind().isKeybindHeld();
    }

    public static void checkZoomStatus()
    {
        if (zoomActive && isZoomActive() == false)
        {
            onZoomDeactivated();
        }
    }

    public static void onZoomActivated()
    {
        if (Configs.Generic.ZOOM_ADJUST_MOUSE_SENSITIVITY.getBooleanValue())
        {
            setMouseSensitivityForZoom();
        }

        zoomActive = true;
    }

    public static void onZoomDeactivated()
    {
        if (zoomActive)
        {
            resetMouseSensitivityForZoom();

            // Refresh the rendered chunks when exiting zoom mode
            MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate();

            zoomActive = false;
        }
    }

    public static void setMouseSensitivityForZoom()
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        double fov = Configs.Generic.ZOOM_FOV.getDoubleValue();
        double origFov = mc.options.fov;

        if (fov < origFov)
        {
            // Only store it once
            if (mouseSensitivity <= 0.0 || mouseSensitivity > 1.0)
            {
                mouseSensitivity = mc.options.mouseSensitivity;
            }

            double min = 0.04;
            double sens = min + (0.5 - min) * (1.0 - (origFov - fov) / origFov);
            mc.options.mouseSensitivity = Math.min(mouseSensitivity, sens);
        }
    }

    public static void resetMouseSensitivityForZoom()
    {
        if (mouseSensitivity > 0.0)
        {
            MinecraftClient.getInstance().options.mouseSensitivity = mouseSensitivity;
            mouseSensitivity = -1.0;
        }
    }

    public static boolean isStrippableLog(World world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        return IMixinAxeItem.tweakeroo_getStrippedBlocks().containsKey(state.getBlock());
    }

    public static boolean getUpdateExec(CommandBlockBlockEntity te)
    {
        return ((IMixinCommandBlockExecutor) te.getCommandExecutor()).getUpdateLastExecution();
    }

    public static void setUpdateExec(CommandBlockBlockEntity te, boolean value)
    {
        ((IMixinCommandBlockExecutor) te.getCommandExecutor()).setUpdateLastExecution(value);
    }

    public static String getChatTimestamp()
    {
        SimpleDateFormat sdf = new SimpleDateFormat(Configs.Generic.CHAT_TIME_FORMAT.getStringValue());
        DATE.setTime(System.currentTimeMillis());
        return sdf.format(DATE);
    }

    public static void setLastChatText(String text)
    {
        previousChatText = text;
    }

    public static String getLastChatText()
    {
        return previousChatText;
    }

    public static int getChatBackgroundColor(int colorOrig)
    {
        int newColor = Configs.Generic.CHAT_BACKGROUND_COLOR.getIntegerValue();
        return (newColor & 0x00FFFFFF) | ((int) (((newColor >>> 24) / 255.0) * ((colorOrig >>> 24) / 255.0) / 0.5 * 255) << 24);
    }

    public static void copyTextFromSign(SignBlockEntity te)
    {
        net.minecraft.text.Text[] text = ((ISignTextAccess) te).getText();
        final int size = text.length;
        previousSignText = new net.minecraft.text.Text[size];

        for (int i = 0; i < size; ++i)
        {
            previousSignText[i] = text[i];
        }
    }

    public static void applyPreviousTextToSign(SignBlockEntity te, @Nullable String[] guiLines)
    {
        if (previousSignText != null)
        {
            final int size = previousSignText.length;

            for (int i = 0; i < size; ++i)
            {
                net.minecraft.text.Text text = previousSignText[i];
                te.setTextOnRow(i, text);

                if (guiLines != null)
                {
                    guiLines[i] = text.asString();
                }
            }
        }
    }

    public static double getLastRealPitch()
    {
        return lastRealPitch;
    }

    public static double getLastRealYaw()
    {
        return lastRealYaw;
    }

    public static void setEntityRotations(Entity entity, float yaw, float pitch)
    {
        entity.yaw = yaw;
        entity.pitch = pitch;
        entity.prevYaw = yaw;
        entity.prevPitch = pitch;

        if (entity instanceof LivingEntity)
        {
            LivingEntity living = (LivingEntity) entity;
            living.headYaw = yaw;
            living.prevHeadYaw = yaw;
        }
    }

    public static float getSnappedPitch(double realPitch)
    {
        if (Configs.Generic.SNAP_AIM_MODE.getOptionListValue() == SnapAimMode.YAW)
        {
            return (float) realPitch;
        }

        if (lastRealPitch != realPitch)
        {
            lastRealPitch = realPitch;
            RenderUtils.notifyRotationChanged();
        }

        if (FeatureToggle.TWEAK_SNAP_AIM_LOCK.getBooleanValue())
        {
            return (float) Configs.Internal.SNAP_AIM_LAST_PITCH.getDoubleValue();
        }

        double step = Configs.Generic.SNAP_AIM_PITCH_STEP.getDoubleValue();
        int limit = Configs.Generic.SNAP_AIM_PITCH_OVERSHOOT.getBooleanValue() ? 180 : 90;
        double snappedPitch;

        //realPitch = MathHelper.clamp(realPitch, -limit, limit);

        if (realPitch < 0)
        {
            snappedPitch = -calculateSnappedAngle(-realPitch, step);
        }
        else
        {
            snappedPitch = calculateSnappedAngle(realPitch, step);
        }

        snappedPitch = MathHelper.clamp(MathHelper.wrapDegrees(snappedPitch), -limit, limit);

        if (Configs.Internal.SNAP_AIM_LAST_PITCH.getDoubleValue() != snappedPitch)
        {
            String g = GuiBase.TXT_GREEN;
            String r = GuiBase.TXT_RST;
            String str = String.format("%s%s%s (step %s%s%s)", g, String.valueOf(MathHelper.wrapDegrees(snappedPitch)), r, g, String.valueOf(step), r);

            InfoUtils.printActionbarMessage("tweakeroo.message.snapped_to_pitch", str);

            Configs.Internal.SNAP_AIM_LAST_PITCH.setDoubleValue(snappedPitch);
        }

        return MathHelper.wrapDegrees((float) snappedPitch);
    }

    public static float getSnappedYaw(double realYaw)
    {
        if (Configs.Generic.SNAP_AIM_MODE.getOptionListValue() == SnapAimMode.PITCH)
        {
            return (float) realYaw;
        }

        if (lastRealYaw != realYaw)
        {
            lastRealYaw = realYaw;
            RenderUtils.notifyRotationChanged();
        }

        if (FeatureToggle.TWEAK_SNAP_AIM_LOCK.getBooleanValue())
        {
            return (float) Configs.Internal.SNAP_AIM_LAST_YAW.getDoubleValue();
        }

        double step = Configs.Generic.SNAP_AIM_YAW_STEP.getDoubleValue();
        double snappedYaw = calculateSnappedAngle(realYaw, step);

        if (Configs.Internal.SNAP_AIM_LAST_YAW.getDoubleValue() != snappedYaw)
        {
            String g = GuiBase.TXT_GREEN;
            String r = GuiBase.TXT_RST;
            String str = String.format("%s%s%s (step %s%s%s)", g, String.valueOf(MathHelper.wrapDegrees(snappedYaw)), r, g, String.valueOf(step), r);

            InfoUtils.printActionbarMessage("tweakeroo.message.snapped_to_yaw", str);

            Configs.Internal.SNAP_AIM_LAST_YAW.setDoubleValue(snappedYaw);
        }

        return MathHelper.wrapDegrees((float) snappedYaw);
    }

    public static double calculateSnappedAngle(double realRotation, double step)
    {
        double offsetRealRotation = MathHelper.floorMod(realRotation, 360.0D) + (step / 2.0);
        return MathHelper.floorMod(((int) (offsetRealRotation / step)) * step, 360.0D);
    }

    public static boolean writeAllMapsAsImages()
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null)
        {
            return true;
        }

        Map<String, MapState> data = ((IMixinClientWorld) mc.world).tweakeroo_getMapStates();
        String worldName = StringUtils.getWorldOrServerName();

        if (worldName == null)
        {
            worldName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date(System.currentTimeMillis()));
        }

        File dir = FileUtils.getConfigDirectory().toPath().resolve(Reference.MOD_ID).resolve("map_images").resolve(worldName).toFile();

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR, "Failed to create directory: " + dir.getAbsolutePath());
            return true;
        }

        int count = 0;

        for (Map.Entry<String, MapState> entry : data.entrySet())
        {
            File file = new File(dir, entry.getKey() + ".png");
            writeMapAsImage(file, entry.getValue());
            ++count;
        }

        InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, String.format("Wrote %d maps to image files", count));

        return true;
    }

    private static void writeMapAsImage(File fileOut, MapState state)
    {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < 128; ++y)
        {
            for (int x = 0; x < 128; ++x)
            {
                int index = x + y * 128;
                int color = state.colors[index] & 255;
                int colorIndex = color / 4;
                int col = colorIndex == 0 ? 0 : MapColor.COLORS[colorIndex].getRenderColor(color & 0x3);
                // Swap the color channels from ABGR to ARGB
                int outputColor = (col & 0xFF00FF00) | (col & 0xFF0000) >> 16 | (col & 0xFF) << 16;

                image.setRGB(x, y, outputColor);
            }
        }

        try
        {
            ImageIO.write(image, "png", fileOut);
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR, "Failed to write image to file: " + fileOut.getAbsolutePath());
        }
    }
}
