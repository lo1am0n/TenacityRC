package dev.tenacity.module.impl.combat.backtrack;

import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vector3d;

import java.util.concurrent.CopyOnWriteArrayList;

public class BacktrackData {
    public Entity entity;
    public CopyOnWriteArrayList<Packet> movements;

    public Vec3 actualPosition;

    public BacktrackData(Entity entity) {
        this.entity = entity;
        this.movements = new CopyOnWriteArrayList<>();
        this.actualPosition = new Vec3(0, 0, 0);
    }
}
