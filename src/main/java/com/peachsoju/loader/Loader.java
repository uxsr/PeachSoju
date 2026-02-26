package com.peachsoju.loader;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

public class Loader {

    public static void init() {
        MixinBootstrap.init();
        Mixins.addConfiguration("PeachSoju.mixins.json");
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("searge");
    }

}
