package com.blowup.eventbus;

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

class EventBus {

    private val listeners = mutableMapOf<Class<*>, MutableList<ListenerMethod>>()

    init {
        EventDispatcher.initialize()
    }

    fun register(obj: Any) {
        val kClass = obj::class

        for (method in kClass.declaredFunctions) {
            val annotation = method.findAnnotation<SubscribeEvent>() ?: continue
            val params = method.parameters

            if (params.size != 2) continue

            val eventType = (params[1].type.classifier as? KClass<*>)?.java ?: continue

            method.isAccessible = true

            listeners.computeIfAbsent(eventType) { mutableListOf() }
                .add(
                    ListenerMethod(
                        instance = obj,
                        function = method,
                        priority = annotation.priority
                    )
                )
        }

        listeners.values.forEach { list ->
            list.sortByDescending { it.priority.level }
        }
    }

    fun unregister(obj: Any) {
        val removeKeys = mutableListOf<Class<*>>()

        for ((eventType, list) in listeners) {
            list.removeIf { it.instance == obj }
            if (list.isEmpty()) removeKeys.add(eventType)
        }

        for (key in removeKeys) listeners.remove(key)
    }

    fun post(event: Event): Boolean {
        val eventClass = event::class.java
        val methods = listeners[eventClass] ?: emptyList()

        for (listener in methods) {
            listener.function.call(listener.instance, event)
        }

        return event.cancelled
    }

    private data class ListenerMethod(
        val instance: Any,
        val function: kotlin.reflect.KFunction<*>,
        val priority: EventPriority
    )
}
