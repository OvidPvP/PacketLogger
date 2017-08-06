package com.sainttx.packetlogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;

public class PacketLoggerPlugin extends JavaPlugin {
    
    public static String INTERCEPTING_PACKETS_META = "packetlogger-intercepting";
    
    @Override
    public void onEnable() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new ReceivingPacketInterceptor(this));
    }
    
    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(p -> p.removeMetadata(INTERCEPTING_PACKETS_META, this));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args.length < 2) {
            return false;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("\"" + args[1] + "\" is not online");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("log")) {
            if (isBeingTracked(target)) {
                sender.sendMessage(target.getName() + " is already being logged");
            } else {
                target.setMetadata(INTERCEPTING_PACKETS_META, new FixedMetadataValue(this, new ArrayList<String>()));
                sender.sendMessage("Now logging the packets of " + target.getName());
            }
            return true;
        } else if (args[0].equalsIgnoreCase("dump")) {
            if (!isBeingTracked(target)) {
                sender.sendMessage(target.getName() + " is not being logged");
            } else {
                sender.sendMessage("Writing packet dump...");
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    List<String> packets = getPacketLog(target);
                    File output = new File(getDataFolder(), "dump-" + target.getName() + ".txt");
                    try {
                        if (!getDataFolder().exists()) {
                            getDataFolder().mkdirs();
                        }
                        if (!output.exists()) {
                            output.createNewFile();
                        }

                        try (FileWriter writer = new FileWriter(output, false)) {
                            for (String packet : packets) {
                                writer.write(packet);
                            }
                        }

                        sender.sendMessage("Successfully dumped the packet log of " + target.getName() + " to " + output.getAbsolutePath());
                    } catch (IOException e) {
                        sender.sendMessage("An error occured when dumping the packet log of " + target.getName());
                        getLogger().log(Level.SEVERE, "dumping packets", e);
                        return;
                    }

                    target.removeMetadata(INTERCEPTING_PACKETS_META, this);
                    sender.sendMessage("No longer tracking the packets of " + target.getName());
                });
                return true;
            }
        } else if (args[0].equalsIgnoreCase("clear")) {
            if (!isBeingTracked(target)) {
                sender.sendMessage(target.getName() + " is not being logged");
            } else {
                target.removeMetadata(INTERCEPTING_PACKETS_META, this);
                sender.sendMessage("No longer tracking the packets of " + target.getName());
            }
            return true;
        }
            
        return false;
    }

    /**
     * Returns whether a player is having their packets intercepted.
     * 
     * @param player The player to intercept
     * @return True if the plugin should inter
     */
    public boolean isBeingTracked(Player player) {
        return player.hasMetadata(INTERCEPTING_PACKETS_META)
                && player.getMetadata(INTERCEPTING_PACKETS_META).get(0).value() instanceof List;
    }

    /**
     * Returns a current players packet log
     * 
     * @param player Player to fetch log from
     * @return Players log
     * @throws IllegalArgumentException if player is not being tracked
     */
    @SuppressWarnings("unchecked")
    public List<String> getPacketLog(Player player) {
       if (!isBeingTracked(player)) {
           throw new IllegalArgumentException("player not tracked");
       }
       return (List<String>) player.getMetadata(INTERCEPTING_PACKETS_META).get(0).value();
    }
    
}
