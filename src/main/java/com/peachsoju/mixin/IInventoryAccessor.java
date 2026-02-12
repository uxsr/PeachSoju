package com.peachsoju.mixin;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface IInventoryAccessor {

    @Accessor("selected")
    int getSelected();

    @Accessor("selected")
    void setSelected(int slot);
}