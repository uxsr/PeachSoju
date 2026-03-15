package com.peachsoju.modules.impl.misc.customitems

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object ItemCustomize {

    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        ItemCustomizeManager.load()
        ItemCustomizeManager.loadAnimatedHeadsFromResources()  // Make sure this is called!
        ItemCustomizeCommand.register()

        // Register tick event to increment the animation counter
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            ItemCustomizeManager.incrementTick()
        }
    }
}