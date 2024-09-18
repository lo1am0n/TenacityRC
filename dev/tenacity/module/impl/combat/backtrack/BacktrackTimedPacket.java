package dev.tenacity.module.impl.combat.backtrack;

import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.util.Vec3;

import java.util.concurrent.CopyOnWriteArrayList;

public class BacktrackTimedPacket {
    public Packet packet;
    public long mills;

    public BacktrackTimedPacket(Packet packet) {
        this.packet = packet;
        this.mills = System.currentTimeMillis();
    }
}
