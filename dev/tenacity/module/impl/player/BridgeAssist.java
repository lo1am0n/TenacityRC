package dev.tenacity.module.impl.player;

import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.event.impl.render.Render3DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.utils.player.ChatUtil;
import dev.tenacity.utils.player.MovementUtils;
import dev.tenacity.utils.server.PacketUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.concurrent.CopyOnWriteArrayList;

public final class BridgeAssist extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "0 CPS (Cha-Cha)", "0 CPS (Cha-Cha)", "N");


    public float rotationPitch = -420f;
    public boolean placedBlockSide = false;

    @Override
    public void onEnable() {
        rotationPitch = mc.thePlayer.rotationPitch;
        placedBlockSide = false;

        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.thePlayer.rotationPitch = rotationPitch;
        rotationPitch = -420f;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);

        super.onDisable();
    }

    @Override
    public void onRender2DEvent(Render2DEvent event) {
        if (!placedBlockSide && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.objectMouseOver.sideHit != EnumFacing.UP && mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock().getMaterial() != Material.air) {
            placedBlockSide = true;
        }

        if (placedBlockSide) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
        }

        if (MovementUtils.getSpeed() >= 0.1575) {
            MovementUtils.setSpeed(0.15);
        }

    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (event.isPre()) {
            ChatUtil.print("MotionH: " + MovementUtils.getSpeed());
            if (rotationPitch > -90f) {
                mc.thePlayer.rotationPitch = 68.6f;
            }
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
        }
        super.onMotionEvent(event);
    }

    public BridgeAssist() {
        super("BridgeAssist", Category.PLAYER, "Helps you bridge");
        this.addSettings(mode);
    }

}
