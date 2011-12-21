package im.mcft.mcftchat;

import static im.mcft.mcftchat.McftChat.logger;
import static im.mcft.mcftchat.McftChat.toggled;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerListener;

/**
 * McftProfiler handler for all player related events.
 * 
 * @author Jon la Cour
 * @version 1.3.7
 */
public class McftChatPlayerListener extends PlayerListener {

	private final McftChat plugin;

	/**
	 * This just allows the main class to use us.
	 * 
	 * @param instance
	 *            The main class
	 */
	public McftChatPlayerListener(McftChat instance) {
		plugin = instance;
	}

	@Override
	public final void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
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

	public void onPlayerChat(PlayerChatEvent event) {
		String name = event.getPlayer().getName();
		Player p = event.getPlayer();
		String message = event.getMessage();
		if (toggled.containsKey(name)) {
			if (message.contains("-off")) {
				String command = toggled.get(name);
				String channel = plugin.settings.get(command);
				toggled.remove(name);
				p.sendMessage(ChatColor.GOLD + "The " + channel + " channel has been toggled off.");
				logger.info(name + "->" + channel + " [Toggled off chat]");
				event.setCancelled(true);
				event.setMessage("");
			} else {
				String command = toggled.get(name);
				p.chat("/mcftchat " + command + " " + message);
				event.setCancelled(true);
			}
		}
	}
}
