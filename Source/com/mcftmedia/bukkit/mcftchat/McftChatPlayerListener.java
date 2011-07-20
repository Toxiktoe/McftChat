package com.mcftmedia.bukkit.mcftchat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerListener;

/**
 * Handles player events
 * 
 * @author Jon
 */
public class McftChatPlayerListener extends PlayerListener {

    private final McftChat plugin;

    public McftChatPlayerListener(McftChat instance) {
        plugin = instance;
    }

    @Override
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

        // when someone uses /help command, change it to /me is stupid!
        //System.out.println("[MC] preprocessing "+event.getMessage());
        // split the command
        String[] args = event.getMessage().split(" ", 2);

        if (args.length > 1) {
            String cmd = args[0];
            //System.out.println("[MC] cmd = "+cmd);
            for (String channel : plugin.settings.keySet()) {
                //System.out.println("[MC] channel = "+channel);
                if (cmd.equalsIgnoreCase("/" + channel)) {
                    //System.out.println("[MC] matched");
                    event.setCancelled(true);
                    Player p = event.getPlayer();
                    if (p != null) {
                        p.chat("/mcftchat " + channel + " " + args[1]);
                    }
                    break;
                }
            }
        }

    }
}