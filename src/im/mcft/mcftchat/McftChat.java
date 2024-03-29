package im.mcft.mcftchat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * McftChat allows you to have clean and easy to set up chat channels for your
 * Bukkit server.
 * 
 * @author Jon la Cour
 * @version 1.3.7
 */
public class McftChat extends JavaPlugin {

	private final McftChatPlayerListener playerListener = new McftChatPlayerListener(this);
	private final ConcurrentHashMap<Player, Boolean> debugees = new ConcurrentHashMap<Player, Boolean>();
	public final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<String, String>();
	public final ConcurrentHashMap<String, String> colorconfig = new ConcurrentHashMap<String, String>();
	public final ConcurrentHashMap<String, String> tagconfig = new ConcurrentHashMap<String, String>();
	public static HashMap<String, String> toggled = new HashMap<String, String>();
	public static PermissionHandler permissionHandler;
	public static final Logger logger = Logger.getLogger("Minecraft.McftChat");
	public boolean log = true;
	public String baseDir = "plugins/McftChat";
	public String configFile = "channels.txt";
	public String tagFile = "tags.txt";
	public String colorconfigFile = "colors.txt";

	@Override
	public final void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		logger.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!");
	}

	@Override
	public final void onEnable() {
		// Settings
		checkSettings();
		loadSettings();

		// Permissions
		setupPermissions();

		Plugin commandsLogging = this.getServer().getPluginManager().getPlugin("Commands Logging");
		if (commandsLogging != null) {
			this.log = false;
		}

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Lowest, this);
		pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.High, this);

		PluginDescriptionFile pdfFile = this.getDescription();
		logger.info("[McftChat] Loaded " + settings.size() + " chat channels.");
		logger.info("[McftChat] Version " + pdfFile.getVersion() + " enabled");
	}

	public boolean isDebugging(final Player player) {
		if (debugees.containsKey(player)) {
			return debugees.get(player);
		} else {
			return false;
		}
	}

	public void setDebugging(final Player player, final boolean value) {
		debugees.put(player, value);
	}

	/**
	 * This makes sure that a permissions plugin is available. If not it will
	 * disable the plugin.
	 * 
	 * @since 1.0.0
	 */
	public void setupPermissions() {
		if (permissionHandler != null) {
			return;
		}

		Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");

		if (permissionsPlugin == null) {
			logger.info("[McftChat] Permissions system not enabled. Disabling plugin.");
			this.getServer().getPluginManager().disablePlugin(this);
		}

		permissionHandler = ((Permissions) permissionsPlugin).getHandler();
		String plugin = ((Permissions) permissionsPlugin).getDescription().getFullName();
		if (plugin.equals("Permissions v2.7.7")) {
			plugin = "PermissionsEx";
		}
		logger.info("[McftChat] Hooked into " + plugin + ".");
	}

	@Override
	public final boolean onCommand(final CommandSender sender, final Command cmd, final String commandLabel, String[] args) {
		String cmdname = cmd.getName().toLowerCase();
		Player player = null;
		String pname = ChatColor.DARK_RED + "[Console]";
		if (sender instanceof Player) {
			player = (Player) sender;
			pname = player.getName();
		}

		for (String command : settings.keySet()) {
			if (cmdname.equalsIgnoreCase(command)) {
				args = unshift(cmdname, args);
				cmdname = "mcftchat";
			}
		}

		if (cmdname.equalsIgnoreCase("mcftchat") && args.length >= 2) {
			String message = "";
			for (Integer index = 1; index < args.length; index++) {
				message = message.concat(" " + args[index]);
			}
			for (String command : settings.keySet()) {
				if (args[0].equalsIgnoreCase(command)) {
					if (player == null || permissionHandler.permission(player, "mcftchat." + command + ".send")) {
						String channel = settings.get(command);
						boolean usetag = true;
						String tag = "[" + tagconfig.get(channel) + "] ";
						if (tag.equals("[null] ") || tag.equals("[off] ")) {
							usetag = false;
						}
						ChatColor color = ChatColor.valueOf(colorconfig.get(channel));
						String channeltag = color + tag;
						String sendername = pname;
						if (player != null) {
							String worldname = player.getLocation().getWorld().getName();
							String group = permissionHandler.getGroup(worldname, pname);
							String prefix = groupPrefix(group, worldname);
							String prefixcolor = prefix.replace("&", "");
							if (prefixcolor.length() == 1) {
								int prefixid = Integer.parseInt(getColor(prefixcolor));
								ChatColor usercolor = ChatColor.getByCode(prefixid);
								sendername = usercolor + "[" + pname + "]";
							}
						}
						if (message.contains("-on")) {
							if (toggled.containsKey(pname)) {
								player.sendMessage(ChatColor.GOLD + "The " + channel + " channel is already toggled on! Say '-off' to toggle off.");
							} else {
								toggled.put(pname, command);
								player.sendMessage(ChatColor.GOLD + "The " + channel + " channel is now toggled on! Say '-off' to toggle off.");
							}
						} else {
							Player[] players = getServer().getOnlinePlayers();
							for (Player p : players) {
								if (permissionHandler.permission(p, "mcftchat." + command + ".receive")) {
									if (usetag) {
										p.sendMessage(channeltag + sendername + color + message);
									} else {
										p.sendMessage(sendername + color + message);
									}
								}
							}
						}
						if (log) {
							if (message.contains("-on")) {
								logger.info(pname + "->" + channel + " [Toggled on chat]");
							} else {
								logger.info(pname + "->" + channel + ":" + message);
							}
						}
						return true;
					} else {
						logger.info("[McftChat] Permission denied for '" + command + "': " + pname);
					}
				}
			}

		}
		return false;
	}

	/**
	 * This makes sure that the settings directory exists and creates the
	 * default settings file if one is not present.
	 * 
	 * @since 1.0.0
	 */
	private void checkSettings() {
		// Creates base directory for config files
		File folder = new File(baseDir);
		if (!folder.exists()) {
			if (folder.mkdir()) {
				logger.info("[McftChat] Created directory '" + baseDir + "'");
			}
		}

		// Creates base config file
		String config = baseDir + "/" + configFile;
		File configfile = new File(config);
		if (!configfile.exists()) {
			BufferedWriter output;
			String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(config));
				output.write("# Command = Channel" + newline);
				output.write("a = Admins" + newline);
				output.write("d = Donators" + newline);
				output.close();
				logger.info("[McftChat] Created config file '" + config + "'");
			} catch (Exception e) {
				Logger.getLogger(McftChat.class.getName()).log(Level.SEVERE, null, e);
			}
		}

		// Creates colors config file
		String colors = baseDir + "/" + colorconfigFile;
		File colorsfile = new File(colors);
		if (!colorsfile.exists()) {
			BufferedWriter output;
			String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(colors));
				output.write("# Channel = CHAT_COLOR" + newline);
				output.write("# Available colors: https://minepedia.net/colorguide.php (Bukkit Plugin section)" + newline);
				output.write("Admins = LIGHT_PURPLE" + newline);
				output.write("Donators = DARK_AQUA" + newline);
				output.close();
				logger.info("[McftChat] Created colors config file '" + colors + "'");
			} catch (Exception e) {
				Logger.getLogger(McftChat.class.getName()).log(Level.SEVERE, null, e);
			}
		}

		// Creates tag config file
		String tag = baseDir + "/" + tagFile;
		File tagfile = new File(tag);
		if (!tagfile.exists()) {
			BufferedWriter output;
			String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(tag));
				output.write("# Channel = Tag" + newline);
				output.write("# This adds a tag before channel messages (i.e. [Admins] [laCour] Hello!)" + newline);
				output.write("# Put off if you do not want a channel tag for a channel. (i.e. Admins = off)" + newline);
				output.write("Admins = Staff" + newline);
				output.write("Donators = off" + newline);
				output.close();
				logger.info("[McftChat] Created tag config file '" + tag + "'");
			} catch (Exception e) {
				Logger.getLogger(McftChat.class.getName()).log(Level.SEVERE, null, e);
			}
		}
	}

	/**
	 * This loads all settings into a HashMap.
	 * 
	 * @since 1.0.0
	 */
	private void loadSettings() {
		String config = baseDir + "/" + configFile;
		String colors = baseDir + "/" + colorconfigFile;
		String tag = baseDir + "/" + tagFile;
		String line = null;

		try {
			BufferedReader configuration = new BufferedReader(new FileReader(config));
			while ((line = configuration.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#") && line.contains(" = ")) {
					String[] pair = line.split(" = ", 2);
					settings.put(pair[0], pair[1]);
				}
			}
			BufferedReader colorconfiguration = new BufferedReader(new FileReader(colors));
			while ((line = colorconfiguration.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#") && line.contains(" = ")) {
					String[] pair = line.split(" = ", 2);
					colorconfig.put(pair[0], pair[1]);
				}
			}
			BufferedReader tagconfiguration = new BufferedReader(new FileReader(tag));
			while ((line = tagconfiguration.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#") && line.contains(" = ")) {
					String[] pair = line.split(" = ", 2);
					tagconfig.put(pair[0], pair[1]);
				}
			}
		} catch (FileNotFoundException e) {
			// Oh man you're screwed, don't worry I'll save you... using default
			// chat channels.
			logger.warning("[McftChat] Error reading " + e.getLocalizedMessage() + ", using defaults");
		} catch (Exception e) {
			// If you thought you were screwed before, boy have I news for you.
			Logger.getLogger(McftChat.class.getName()).log(Level.SEVERE, null, e);
		}
	}

	private String[] unshift(final String str, final String[] array) {
		String[] newarray = new String[array.length + 1];
		newarray[0] = str;
		for (Integer i = 0; i < array.length; i++) {
			newarray[i + 1] = array[i];
		}
		return newarray;
	}

	private String groupPrefix(final String groupname, final String worldname) {
		String prefix = permissionHandler.getGroupPrefix(worldname, groupname);
		if (prefix == null) {
			prefix = "";
		}
		return prefix;
	}

	private String getColor(final String color) {
		if (isInt(color)) {
			return color;
		} else {
			if (color.equalsIgnoreCase("a")) {
				return "10";
			} else if (color.equalsIgnoreCase("b")) {
				return "11";
			} else if (color.equalsIgnoreCase("c")) {
				return "12";
			} else if (color.equalsIgnoreCase("d")) {
				return "13";
			} else if (color.equalsIgnoreCase("e")) {
				return "14";
			} else if (color.equalsIgnoreCase("f")) {
				return "15";
			}
			return null;
		}
	}

	private boolean isInt(final String i) {
		try {
			Integer.parseInt(i);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}
}
