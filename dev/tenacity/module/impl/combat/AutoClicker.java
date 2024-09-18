package dev.tenacity.module.impl.combat;

import dev.tenacity.Tenacity;
import dev.tenacity.commands.impl.FriendCommand;
import dev.tenacity.event.impl.network.PacketSendEvent;
import dev.tenacity.event.impl.player.*;
import dev.tenacity.event.impl.render.Render3DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.movement.Scaffold;
import dev.tenacity.module.impl.render.HUDMod;
import dev.tenacity.module.settings.impl.*;
import dev.tenacity.utils.animations.Animation;
import dev.tenacity.utils.animations.Direction;
import dev.tenacity.utils.animations.impl.DecelerateAnimation;
import dev.tenacity.utils.misc.MathUtils;
import dev.tenacity.utils.player.ChatUtil;
import dev.tenacity.utils.player.InventoryUtils;
import dev.tenacity.utils.player.RotationUtils;
import dev.tenacity.utils.render.RenderUtil;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AutoClicker extends Module {
    private final TimerUtil cpsTimer = new TimerUtil();
    private final TimerUtil randomTimer = new TimerUtil();
    private int randomizedDurationMS = 1;

    private int targetCPS = 0;
    private int actualCPS = 0;
    private int stageInteger = 0; // 1 = NORMAL | 2 = DROP | 3 = SPIKE
    private int stageIntegerIncrement = 0; // 1 = NA | 2 = DECREASE | 3 = INCREASE

    private final NumberSetting minCPS = new NumberSetting("CPS (Minimum)", 16, 20, 1, 1);
    private final NumberSetting maxCPS = new NumberSetting("CPS (Maximum)", 18, 20, 1, 1);

    private final NumberSetting stdDevMin = new NumberSetting("Standard Deviation (Minimum)", 50, 500, 1, 1);
    private final NumberSetting stdDevMax = new NumberSetting("Standard Deviation (Maximum)", 100, 500, 1, 1);

    private final BooleanSetting allowCPSDrops = new BooleanSetting("Allow CPS Drops", true);
    private final NumberSetting cpsDropMultMin = new NumberSetting("CPS Drop Multiplier (Minimum)", 0.75, 1.0, 0.05, 0.05);
    private final NumberSetting cpsDropMultMax = new NumberSetting("CPS Drop Multiplier (Maximum)", 1.0, 1.0, 0.05, 0.05);


    private final BooleanSetting allowCPSSpikes = new BooleanSetting("Allow CPS Spikes", true);
    private final NumberSetting cpsSpikeMultMin = new NumberSetting("CPS Spike Multiplier (Minimum)", 1.0, 2.0, 1.0, 0.05);
    private final NumberSetting cpsSpikeMultMax = new NumberSetting("CPS Spike Multiplier (Maximum)", 1.25, 2.0, 1.0, 0.05);

    private final NumberSetting dropSpikeDurationMin = new NumberSetting("CPS Drop/Spike Duration (Minimum)", 1500, 5000, 500, 25);
    private final NumberSetting dropSpikeDurationMax = new NumberSetting("CPS Drop/Spike Duration (Maximum)", 3500, 5000, 500, 25);

    private final ModeSetting dropSpikeIncMode = new ModeSetting("CPS Drop/Spike Increment Mode", "Addition", "Addition", "None");
    private final NumberSetting dropSpikeIncMin = new NumberSetting("CPS Drop/Spike Increment Value (Minimum)", 1.0, 10.0, 1.0, 1.0);
    private final NumberSetting dropSpikeIncMax = new NumberSetting("CPS Drop/Spike Increment Value (Maximum)", 3.0, 10.0, 1.0, 1.0);

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT, "Clicks for you");
        this.addSettings(minCPS, maxCPS, stdDevMin, stdDevMax, allowCPSDrops, cpsDropMultMin, cpsDropMultMax, allowCPSSpikes, cpsSpikeMultMin, cpsSpikeMultMax, dropSpikeDurationMin, dropSpikeDurationMax, dropSpikeIncMode, dropSpikeIncMin, dropSpikeIncMax);
    }

    @Override
    public void onEnable() {
        stageInteger = MathUtils.getRandomInRange(1, 4);
        stageIntegerIncrement = 1;

        cpsTimer.reset();
        randomTimer.reset();

        if (stageInteger == 1 || stageInteger == 4) {
            targetCPS = (int) MathUtils.getRandomInRange(minCPS.getValue(), maxCPS.getValue());
            actualCPS = (int) MathUtils.getRandomInRange(minCPS.getValue(), maxCPS.getValue());
        }
        if (stageInteger == 2) {
            double randomMult = MathUtils.getRandomInRange(cpsDropMultMin.getValue(), cpsDropMultMax.getValue());
            targetCPS = (int) MathUtils.getRandomInRange(minCPS.getValue() * randomMult, maxCPS.getValue() * randomMult);
            actualCPS = (int) MathUtils.getRandomInRange(minCPS.getValue() * randomMult, maxCPS.getValue() * randomMult);
        }
        if (stageInteger == 3) {
            double randomMult = MathUtils.getRandomInRange(cpsSpikeMultMin.getValue(), cpsSpikeMultMax.getValue());
            targetCPS = (int) MathUtils.getRandomInRange(minCPS.getValue() * randomMult, maxCPS.getValue() * randomMult);
            actualCPS = (int) MathUtils.getRandomInRange(minCPS.getValue() * randomMult, maxCPS.getValue() * randomMult);
        }

        randomizedDurationMS = (int) MathUtils.getRandomInRange(dropSpikeDurationMin.getValue(), dropSpikeDurationMax.getValue());

        super.onEnable();
    }

    CopyOnWriteArrayList<String> packetNames = new CopyOnWriteArrayList<>();

    @Override
    public void onPacketSendEvent(PacketSendEvent event) {
        if (event.getPacket() instanceof C0BPacketEntityAction) {

        }

        super.onPacketSendEvent(event);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (event.isPre()) {
            String randomType = "NORMAL";

            if (stageInteger == 1) {
                randomType = "NORMAL";
            }
            if (stageInteger == 2) {
                randomType = "DROP";
            }
            if (stageInteger == 3) {
                randomType = "SPIKE";
            }

            ChatUtil.print("Target: " + targetCPS + ", Actual: " + actualCPS + " | Type: " + randomType);
            if (cpsTimer.hasTimeElapsed(1000 / actualCPS))
                cpsTimer.reset();

                mc.thePlayer.swingItem();

                if (mc.objectMouseOver != null) {
                    switch (mc.objectMouseOver.typeOfHit) {
                        case ENTITY:
                            mc.playerController.attackEntity(mc.thePlayer, mc.objectMouseOver.entityHit);
                            break;
                    }
                }

                if (stageIntegerIncrement == 1) {
                    if (stageInteger == 1 || stageInteger == 4) {
                        targetCPS = (int) MathUtils.getRandomInRange(minCPS.getValue(), maxCPS.getValue());
                    }
                    if (stageInteger == 2) {
                        double randomMult = MathUtils.getRandomInRange(cpsDropMultMin.getValue(), cpsDropMultMax.getValue());
                        targetCPS = (int) MathUtils.getRandomInRange(minCPS.getValue() * randomMult, maxCPS.getValue() * randomMult);
                    }
                    if (stageInteger == 3) {
                        double randomMult = MathUtils.getRandomInRange(cpsSpikeMultMin.getValue(), cpsSpikeMultMax.getValue());
                        targetCPS = (int) MathUtils.getRandomInRange(minCPS.getValue() * randomMult, maxCPS.getValue() * randomMult);
                    }
                }

                if (targetCPS > actualCPS) {
                    actualCPS += MathUtils.getRandomInRange(dropSpikeIncMin.getValue(), dropSpikeIncMax.getValue());
                }
                if (targetCPS < actualCPS) {
                    actualCPS -= MathUtils.getRandomInRange(dropSpikeIncMin.getValue(), dropSpikeIncMax.getValue());
                }
            if (randomTimer.hasTimeElapsed(randomizedDurationMS)) {
                randomizedDurationMS = (int) MathUtils.getRandomInRange(dropSpikeDurationMin.getValue(), dropSpikeDurationMax.getValue());
                stageInteger = MathUtils.getRandomInRange(1, 4);
            }
            }
        super.onMotionEvent(event);
        }

}
