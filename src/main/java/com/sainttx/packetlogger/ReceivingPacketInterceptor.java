package com.sainttx.packetlogger;

import static com.comphenix.protocol.PacketType.Play.Client.*;

import java.lang.reflect.Field;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

/**
 * Intercepts all received packets from a player
 */
public class ReceivingPacketInterceptor extends PacketAdapter {

    public ReceivingPacketInterceptor(Plugin plugin) {
        super(plugin, TELEPORT_ACCEPT, AUTO_RECIPE, TAB_COMPLETE, CHAT, 
                CLIENT_COMMAND, SETTINGS, TRANSACTION, ENCHANT_ITEM, 
                WINDOW_CLICK, CLOSE_WINDOW, CUSTOM_PAYLOAD, USE_ENTITY, 
                KEEP_ALIVE, FLYING, POSITION, POSITION_LOOK, LOOK, VEHICLE_MOVE,
                BOAT_MOVE, ABILITIES, BLOCK_DIG, ENTITY_ACTION, STEER_VEHICLE, 
                RECIPE_DISPLAYED, RESOURCE_PACK_STATUS, ADVANCEMENTS, 
                HELD_ITEM_SLOT, SET_CREATIVE_SLOT, UPDATE_SIGN, ARM_ANIMATION, 
                SPECTATE, USE_ITEM, BLOCK_PLACE);
    }
    
    @Override
    public PacketLoggerPlugin getPlugin() {
        return (PacketLoggerPlugin) super.getPlugin();
    }
    
    @Override
    public void onPacketReceiving(PacketEvent event) {
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
        getPlugin().getPacketLog(player).add(output);
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
