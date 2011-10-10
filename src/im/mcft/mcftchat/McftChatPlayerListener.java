package im.mcft.mcftchat;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerListener;

/**
 * McftChat player listener for command processing
 * 
 * @author      Jon la Cour
 * @version     1.3.4
 */
public class McftChatPlayerListener extends PlayerListener {

    private final McftChat plugin;

    public McftChatPlayerListener(McftChat instance) {
        plugin = instance;
    }

    @Override
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ", 2);
        if (args.length > 1) {
            String cmd = args[0];
            for (String channel : plugin.settings.keySet()) {
                if (cmd.equalsIgnoreCase("/" + channel)) {
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