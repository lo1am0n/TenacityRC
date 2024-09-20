package dev.tenacity.module.impl.misc;

import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.utils.player.ChatUtil;
import dev.tenacity.utils.server.PacketUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

@SuppressWarnings("unused")
public final class FlagDetector extends Module {

    private int movementFlagCount = 0;
    private int combatMitigationCount = 0;

    private boolean sentAttack = false;
    private EntityPlayer attackedEntity = null;
    private int attackTicks = 0;

    public FlagDetector() {
        super("FlagDetector", Category.MISC, "Detects different flags and mitigations");
    }

    @Override
    public void onEnable() {
        movementFlagCount = 0;
        combatMitigationCount = 0;

        sentAttack = false;
        attackTicks = 0;
        attackedEntity = null;
        super.onEnable();
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (event.isPre()) {
            if (sentAttack) {
                attackTicks++;

                if (attackTicks >= 3 && attackedEntity != null && attackedEntity.hurtTime <= 0) {
                    sentAttack = false;
                    attackTicks = 0;
                    attackedEntity = null;

                    combatMitigationCount++;
                    ChatUtil.print("Combat Flag Detected (" + combatMitigationCount + ")");
                }
            }
        }
        super.onMotionEvent(event);
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent event) {
        if (event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity) event.getPacket();
            Entity entity = useEntity.getEntityFromWorld(mc.theWorld);

            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK && entity instanceof EntityPlayer) {
                sentAttack = true;
                attackTicks = 0;
                attackedEntity = (EntityPlayer) entity;
            }
        }
        super.onPacketSendEvent(event);
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            movementFlagCount++;
            ChatUtil.print("Movement Flag Detected (" + movementFlagCount + ")");
        }

        super.onPacketReceiveEvent(event);
    }
}
