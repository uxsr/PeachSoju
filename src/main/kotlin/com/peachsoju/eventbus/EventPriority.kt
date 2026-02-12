package com.peachsoju.eventbus;

enum class EventPriority(val level: Int) {
    HIGHEST(3),
    HIGH(2),
    NORMAL(1),
    LOW(0),
    LOWEST(-1)
}
