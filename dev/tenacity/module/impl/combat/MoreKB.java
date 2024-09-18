package dev.tenacity.module.impl.combat;

import dev.tenacity.event.impl.player.AttackEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.misc.MathUtils;
import dev.tenacity.utils.player.MovementUtils;
import dev.tenacity.utils.server.PacketUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public final class MoreKB extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "LegitFast", "LegitFast", "Packet");
    private final NumberSetting chance = new NumberSetting("Chance", 100, 100, 0, 1);

    public MoreKB() {
        super("MoreKB", Category.COMBAT, "Makes the player your attacking take extra knockback");
        this.addSettings(mode, chance);
    }

    public boolean doingLegitTick = false;

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (event.isPre()) {
            if (mode.is("LegitFast") && doingLegitTick) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                doingLegitTick = false;
            }
        }
        super.onMotionEvent(event);
    }

    @Override
    public void onRender2DEvent(Render2DEvent event) {
        this.setSuffix(mode.getMode() + " / " + chance.getValue());
        super.onRender2DEvent(event);
    }

    @Override
    public void onAttackEvent(AttackEvent event) {
        if(event.getTargetEntity() != null) {
            if (chance.getValue() != 100 && MathUtils.getRandomInRange(0, 100) > chance.getValue())
                return;

            if (mode.is("Packet")) {
                if (mc.thePlayer.isSprinting())
                    PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));

                PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                PacketUtils.sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
            }
            if (mode.is("LegitFast")) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
                doingLegitTick = true;
            }
        }
    }
}
