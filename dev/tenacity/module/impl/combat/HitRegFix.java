package dev.tenacity.module.impl.combat;

import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.player.InventoryUtils;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.potion.Potion;

public class HitRegFix extends Module {

    public HitRegFix() {
        super("HitRegFix", Category.COMBAT, "Fixes that 1.8 stupid hit delay");
    }

    @Override
    public void onMotionEvent(MotionEvent e) {

    }


    // idk why im using this tbh
    @Override
    public void onRender2DEvent(Render2DEvent event) {
        mc.leftClickCounter = 0;
        super.onRender2DEvent(event);
    }
}
