package com.blowup.eventbus;

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SubscribeEvent(
    val priority: EventPriority = EventPriority.NORMAL
)