package dev.tenacity.module.impl.combat;

import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.event.impl.render.Render3DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.combat.backtrack.BacktrackData;
import dev.tenacity.module.impl.combat.backtrack.BacktrackTimedPacket;
import dev.tenacity.module.impl.render.HUDMod;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ModeSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.misc.MathUtils;
import dev.tenacity.utils.player.ChatUtil;
import dev.tenacity.utils.render.RenderUtil;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

import java.util.concurrent.CopyOnWriteArrayList;

public class Backtrack extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "LagBehind", "Freeze", "LagBehind");
    private final NumberSetting minimumRange = new NumberSetting("Minimum Activation Range (Blocks)", 2.5, 10.0, 0.5, 0.1);
    private final NumberSetting maximumRange = new NumberSetting("Maximum Activation Range (Blocks)", 7.5, 10.0, 0.5, 0.1);

    // private final NumberSetting maximumReach = new NumberSetting("Maximum Reach (Blocks)", 4.0, 6.0, 0.05, 0.05);

    private final NumberSetting minimumLagMS = new NumberSetting("Minimum Lag Delay (Milliseconds)", 125, 5000, 5, 5);
    private final NumberSetting maximumLagMS = new NumberSetting("Maximum Lag Delay (Milliseconds)", 275, 5000, 5, 5);

    private final NumberSetting maximumAllowedPackets = new NumberSetting("Maximum Packet Data", 7, 100, 1, 1);

    private final NumberSetting maximumReach = new NumberSetting("Maximum Reach (Blocks)", 4.0, 6.0, 0.05, 0.05);

    private final BooleanSetting smart = new BooleanSetting("Smart", true);

    // private final ModeSetting lagMode = new ModeSetting("Lag Increment Mode", "Normal", "Normal", "Instant");

    private final MultipleBoolSetting targetPackets = new MultipleBoolSetting("Packets to Lag",
            new BooleanSetting("Movements", true),
            new BooleanSetting("Swings", true),
            new BooleanSetting("Attacks", true),
            new BooleanSetting("Entity Actions", true),
            new BooleanSetting("Digs", true),
            new BooleanSetting("Placements", true),
            new BooleanSetting("Transactions", true),
            new BooleanSetting("Keep Alives", true));


    public Backtrack() {
        super("Backtrack", Category.COMBAT, "Simulates lag for a reach advantage");

        this.addSettings(mode, smart, minimumRange, maximumRange, minimumLagMS, maximumLagMS, maximumAllowedPackets, maximumReach, targetPackets);
    }

    public CopyOnWriteArrayList<BacktrackData> packetData = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<BacktrackTimedPacket> lagPackets = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<BacktrackTimedPacket> lagPackets2 = new CopyOnWriteArrayList<>();

    public int totalLaggedPackets = 0;
    public int laggedPacketsCount = 0;

    public TimerUtil backtrackTimer = new TimerUtil();

    public int randomizedMilliseconds = 0;

    public boolean shouldBacktrackSmart = false;

    @Override
    public void onEnable() {
        backtrackTimer.reset();
        randomizedMilliseconds = MathUtils.getRandomInRange(minimumLagMS.getValue().intValue(), maximumLagMS.getValue().intValue());

        super.onEnable();
    }

    @Override
    public void onDisable() {
        for (BacktrackData data : packetData) {
            for (Packet packet : data.movements) {
                packet.processPacket(mc.getNetHandler());
            }
        }

        packetData.clear();

        for (BacktrackTimedPacket lagPacket : lagPackets) {
            PacketUtils.sendPacketNoEvent(lagPacket.packet);
        }

        lagPackets.clear();

        for (BacktrackTimedPacket packet : lagPackets2) {
            packet.packet.processPacket(mc.getNetHandler());
        }

        lagPackets2.clear();

        super.onDisable();
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        if (e.isPre()) {
            double closestDist = 999999.0;
            for (BacktrackData data : packetData) {
                if (smart.isEnabled() && data.entity.getDistanceToEntity(mc.thePlayer) < closestDist) {
                    closestDist = data.entity.getDistanceToEntity(mc.thePlayer);
                }
            }

            if (smart.isEnabled() && closestDist >= maximumRange.getValue()) {
                ChatUtil.print("Smart Dist: " + closestDist);
                shouldBacktrackSmart = false;
            }

            if (smart.isEnabled() && !shouldBacktrackSmart) {
                for (BacktrackData data : packetData) {
                    for (Packet packet : data.movements) {
                        packet.processPacket(mc.getNetHandler());
                    }
                }

                packetData.clear();

                for (BacktrackTimedPacket lagPacket : lagPackets) {
                    PacketUtils.sendPacketNoEvent(lagPacket.packet);
                }

                lagPackets.clear();

                for (BacktrackTimedPacket packet : lagPackets2) {
                    packet.packet.processPacket(mc.getNetHandler());
                }

                lagPackets2.clear();
                return;
            }

            if (backtrackTimer.hasTimeElapsed(randomizedMilliseconds)) {
                for (BacktrackTimedPacket lagPacket : lagPackets2) {
                    if (System.currentTimeMillis() - lagPacket.mills >= randomizedMilliseconds) {
                        lagPacket.packet.processPacket(mc.getNetHandler());
                        lagPackets2.remove(lagPacket);
                    }
                }

                for (BacktrackTimedPacket lagPacket : lagPackets) {
                    if (System.currentTimeMillis() - lagPacket.mills >= randomizedMilliseconds) {

                        if (lagPacket.packet instanceof C02PacketUseEntity) {
                            C02PacketUseEntity packetWrapper = (C02PacketUseEntity) lagPacket.packet;

                            BacktrackData data2 = retrieveData(packetWrapper.entityId);
                            if (data2.actualPosition.distanceTo(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)) >= maximumReach.getValue().doubleValue()) {
                                ChatUtil.print("Reach: " + data2.actualPosition.distanceTo(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)) + " | CANCELLED");
                                lagPackets.remove(lagPacket);
                            }
                            else {
                                ChatUtil.print("Reach: " + data2.actualPosition.distanceTo(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)));
                                PacketUtils.sendPacketNoEvent(lagPacket.packet);
                                lagPackets.remove(lagPacket);
                            }
                        }
                        else {
                            PacketUtils.sendPacketNoEvent(lagPacket.packet);
                            lagPackets.remove(lagPacket);
                        }
                    }
                }


                if (mode.is("Freeze")) {
                    for (BacktrackData data : packetData) {
                        for (Packet packet : data.movements) {
                            packet.processPacket(mc.getNetHandler());
                            data.movements.remove(packet);
                        }
                    }
                }

                backtrackTimer.reset();
                randomizedMilliseconds = MathUtils.getRandomInRange(minimumLagMS.getValue().intValue(), maximumLagMS.getValue().intValue());
            }

            if (mode.is("LagBehind")) {
                for (BacktrackData data : packetData) {
                    boolean sent = false;

                    if (data.movements.size() >= (randomizedMilliseconds / 50)) {
                        for (Packet packet : data.movements) {
                            if (!sent) {
                                packet.processPacket(mc.getNetHandler());
                                data.movements.remove(packet);

                                sent = true;
                            }
                        }
                    }
                }
            }
        }
        super.onMotionEvent(e);
    }

    public BacktrackData retrieveData(int entityId) {
        BacktrackData data = null;

        for (BacktrackData iData : packetData) {
            if (iData == null || iData.entity == null) return null;

            if (iData.entity.getEntityId() == entityId) {
                data = iData;
            }
        }

        if (data == null) {
            data = new BacktrackData(mc.theWorld.getEntityByID(entityId));
            packetData.add(data);
        }

        return data;
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent event) {
        if (targetPackets.isEnabled("Movements") && event.getPacket() instanceof C03PacketPlayer) {
            lagPackets.add(new BacktrackTimedPacket(event.getPacket()));
            event.cancel();
        }
        if (targetPackets.isEnabled("Swings") && event.getPacket() instanceof C0APacketAnimation) {
            lagPackets.add(new BacktrackTimedPacket(event.getPacket()));
            event.cancel();
        }
        if (targetPackets.isEnabled("Attacks") && event.getPacket() instanceof C02PacketUseEntity) {
            BacktrackData data = retrieveData(((C02PacketUseEntity) event.getPacket()).entityId);

            lagPackets.add(new BacktrackTimedPacket(event.getPacket()));

            if (!shouldBacktrackSmart) {
                shouldBacktrackSmart = true;
            }
            event.cancel();
        }
        if (targetPackets.isEnabled("Entity Actions") && event.getPacket() instanceof C0BPacketEntityAction) {
            lagPackets.add(new BacktrackTimedPacket(event.getPacket()));
            event.cancel();
        }
        if (targetPackets.isEnabled("Digs") && event.getPacket() instanceof C07PacketPlayerDigging) {
            lagPackets.add(new BacktrackTimedPacket(event.getPacket()));
            event.cancel();
        }
        if (targetPackets.isEnabled("Placements") && event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            lagPackets.add(new BacktrackTimedPacket(event.getPacket()));
            event.cancel();
        }
        if (targetPackets.isEnabled("Transactions") && event.getPacket() instanceof C0FPacketConfirmTransaction) {
            lagPackets.add(new BacktrackTimedPacket(event.getPacket()));
            event.cancel();
        }
        if (targetPackets.isEnabled("Keep Alives") && event.getPacket() instanceof C00PacketKeepAlive) {
            lagPackets.add(new BacktrackTimedPacket(event.getPacket()));
            event.cancel();
        }
        super.onPacketSendEvent(event);
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S14PacketEntity) {
            S14PacketEntity packet = (S14PacketEntity) event.getPacket();
            if (packet.entityId == mc.thePlayer.getEntityId()) return;

            BacktrackData data = retrieveData(packet.entityId);

            if (data == null) return;

            double d0 = (double) packet.posX / 32.0D;
            double d1 = (double) packet.posY / 32.0D;
            double d2 = (double) packet.posZ / 32.0D;

            data.actualPosition = new Vec3(d0, d1, d2);

            if (data.movements.size() > maximumAllowedPackets.getValue().intValue()) return;

            if (mc.theWorld.getEntityByID(packet.entityId).getDistanceToEntity(mc.thePlayer) < minimumRange.getValue().doubleValue()) return;
            if (mc.theWorld.getEntityByID(packet.entityId).getDistanceToEntity(mc.thePlayer) > maximumRange.getValue().doubleValue()) return;

            data.movements.add(packet);

            event.cancel();
        }
        if (event.getPacket() instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport packet = (S18PacketEntityTeleport) event.getPacket();

            if (packet.entityId == mc.thePlayer.getEntityId()) return;

            BacktrackData data = retrieveData(packet.entityId);

            if (data == null) return;

            double d0 = (double) packet.posX / 32.0D;
            double d1 = (double) packet.posY / 32.0D;
            double d2 = (double) packet.posZ / 32.0D;

            data.actualPosition = new Vec3(d0, d1, d2);

            if (data.movements.size() > maximumAllowedPackets.getValue().intValue()) return;

            if (mc.theWorld.getEntityByID(packet.entityId).getDistanceToEntity(mc.thePlayer) <= minimumRange.getValue().doubleValue()) return;
            if (mc.theWorld.getEntityByID(packet.entityId).getDistanceToEntity(mc.thePlayer) >= maximumRange.getValue().doubleValue()) return;

            data.movements.add(packet);

            event.cancel();
        }
        super.onPacketReceiveEvent(event);
    }
}
