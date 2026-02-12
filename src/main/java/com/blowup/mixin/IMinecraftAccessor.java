package com.blowup.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMinecraftAccessor {

    @Accessor("fontManager")
    FontManager getFontManager();

    @Invoker("startAttack")
    boolean callStartAttack();

    @Invoker("startUseItem")
    void callStartUseItem();
}