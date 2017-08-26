package com.sainttx.packetlogger;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.logging.Level;


public class OutgoingPacketInterceptor extends PacketAdapter {

    public OutgoingPacketInterceptor(Plugin plugin) { // Primarily using this for debugging scoreboards atm...
        super(plugin, PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE,
                PacketType.Play.Server.SCOREBOARD_OBJECTIVE,
                PacketType.Play.Server.SCOREBOARD_SCORE,
                PacketType.Play.Server.SCOREBOARD_TEAM);
    }

    @Override
    public PacketLoggerPlugin getPlugin() {
        return (PacketLoggerPlugin) super.getPlugin();
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();

        if (!getPlugin().isBeingTracked(player)) {
            return;
        }

        Object packet = event.getPacket().getHandle();
        Class<?> packetClass = packet.getClass();
        StringBuilder packetInformation = new StringBuilder();

        try {
            dumpPacket(packetClass, packet, packetInformation, "");
        } catch (Exception e) {
            getPlugin().getLogger().log(Level.SEVERE, "failed to save packet information", e);
            return;
        }

        String output = packetInformation.toString();
        getPlugin().getPacketLog(player).add("[SERVER -> CLIENT] " + output);
    }

    // Recursively prints packet fields and any superclass fields
    private void dumpPacket(Class<?> packetClass, Object packet, StringBuilder packetInformation, String indent) {
        if (packetClass == Object.class) {
            return;
        }

        packetInformation.append(indent).append(packetClass.getSimpleName()).append('\n');
        Field[] fields = packetClass.getDeclaredFields();

        try {
            for (Field declaredField : fields) {
                declaredField.setAccessible(true);
                Object value = declaredField.get(packet);
                packetInformation.append(indent).append("  ")
                        .append(declaredField.getName()).append('=').append(value)
                        .append('\n');
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        if (packetClass.getSuperclass() != null) {
            dumpPacket(packetClass.getSuperclass(), packet, packetInformation, indent + "  ");
        }
    }
}
