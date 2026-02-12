package com.blowup.eventbus.events;

import com.blowup.eventbus.Event
import net.minecraft.network.protocol.Packet

abstract class PacketEvent(val packet: Packet<*>) : Event() {

    class Receive(packet: Packet<*>) : PacketEvent(packet)

    class Send(packet: Packet<*>) : PacketEvent(packet)
}