
package com.yottabyte.bukkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * Admin plugin for Bukkit.
 *
 * @author yottabyte
 */

public class KiwiAdmin extends JavaPlugin {

	public static final Logger log = Logger.getLogger("Minecraft");

	static Permissions CurrentPermissions = null;
	static Database db;
	static String maindir = "plugins/KiwiAdmin/";
	static File Settings = new File(maindir + "config.properties");
	static HashSet<String> bannedPlayers = new HashSet<String>();
	static Map<String,Long> tempBans = new HashMap<String,Long>();
	private final KiwiAdminPlayerListener playerListener = new KiwiAdminPlayerListener(this);

	public static boolean useMysql;
	public static String mysqlDatabase;
	public static String mysqlUser;
	public static String mysqlPassword;
	public static String mysqlTable;
	public static boolean autoComplete;

	public Configuration properties = new Configuration(new File("plugins/KiwiAdmin/config.yml"));

	// NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled

	public void setupPermissions() {
		Plugin plugin = this.getServer().getPluginManager().getPlugin("Permissions");

		if (KiwiAdmin.CurrentPermissions == null) {
			// Permission plugin already registered
			return;
		}

		if (plugin != null) {
			KiwiAdmin.CurrentPermissions = (Permissions) plugin;
		} else {
			log.log(Level.CONFIG, "Permissions plugin is required for this plugin to work. Disabling plugin");
			this.getServer().getPluginManager().disablePlugin(this);
		}
	}

	public void onDisable() {
		tempBans.clear();
		bannedPlayers.clear();
		System.out.println("KiwiAdmin disabled.");
	}

	/**
	 * Create a default configuration file from the .jar.
	 * 
	 * @param name
	 */
	protected void createDefaultConfiguration(String name) {
		File actual = new File(getDataFolder(), name);
		if (!actual.exists()) {

			InputStream input =
				this.getClass().getResourceAsStream("/defaults/" + name);
			if (input != null) {
				FileOutputStream output = null;

				try {
					output = new FileOutputStream(actual);
					byte[] buf = new byte[8192];
					int length = 0;
					while ((length = input.read(buf)) > 0) {
						output.write(buf, 0, length);
					}

					System.out.println(getDescription().getName()
							+ ": Default configuration file written: " + name);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (input != null)
							input.close();
					} catch (IOException e) {}

					try {
						if (output != null)
							output.close();
					} catch (IOException e) {}
				}
			}
		}
	}

	public void onEnable() {
		new File(maindir).mkdir();

		createDefaultConfiguration("config.yml");
		Database.updateFlatFile();
		
		properties.load();
		useMysql = properties.getBoolean("mysql",false);
		mysqlTable = properties.getString("mysql-table","banlist");
		autoComplete = properties.getBoolean("auto-complete",true);

		// Create the database
		db = new Database(this);

		// Register our events   	
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Highest, this);

		PluginDescriptionFile pdfFile = this.getDescription();
		log.log(Level.INFO,pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}

	public static String combineSplit(int startIndex, String[] string, String seperator) {
		StringBuilder builder = new StringBuilder();

		for (int i = startIndex; i < string.length; i++) {
			builder.append(string[i]);
			builder.append(seperator);
		}

		builder.deleteCharAt(builder.length() - seperator.length()); // remove
		return builder.toString();
	}

	static long parseTimeSpec(String time, String unit) {
		long sec;
		try {
			sec = Integer.parseInt(time)*60;
		} catch (NumberFormatException ex) {
			return 0;
		}
		if (unit.startsWith("hour"))
			sec *= 60;
		else if (unit.startsWith("day"))
			sec *= (60*24);
		else if (unit.startsWith("week"))
			sec *= (7*60*24);
		else if (unit.startsWith("month"))
			sec *= (30*60*24);
		else if (unit.startsWith("min"))
			sec *= 1;
		else if (unit.startsWith("sec"))
			sec /= 60;
		return sec*1000;
	}

	public String expandName(String Name) {
		int m = 0;
		String Result = "";
		for (int n = 0; n < getServer().getOnlinePlayers().length; n++) {
			String str = getServer().getOnlinePlayers()[n].getName();
			if (str.matches("(?i).*" + Name + ".*")) {
				m++;
				Result = str;
				if(m==2) {
					return null;
				}
			}
			if (str.equalsIgnoreCase(Name))
				return str;
		}
		if (m == 1)
			return Result;
		if (m > 1) {
			return null;
		}
		if (m < 1) {
			return Name;
		}
		return Name;
	}

	public String formatMessage(String str){
		String funnyChar = new Character((char) 167).toString();
		str = str.replaceAll("&", funnyChar);
		return str;
	}



	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		String commandName = command.getName().toLowerCase();
		String[] trimmedArgs = args;

		//sender.sendMessage(ChatColor.GREEN + trimmedArgs[0]);
		if(commandName.equals("reloadka")){
			return reloadKA(sender);
		}
		if(commandName.equals("unban")){
			return unBanPlayer(sender,trimmedArgs);
		}
		if(commandName.equals("ban")){
			return banPlayer(sender,trimmedArgs);
		}
		if(commandName.equals("kick")){
			return kickPlayer(sender,trimmedArgs);
		}
		if(commandName.equals("tempban")){
			return tempbanPlayer(sender,trimmedArgs);
		}
		if(commandName.equals("checkban")){
			return checkBan(sender,trimmedArgs);
		}
		return false;
	}

	private boolean unBanPlayer(CommandSender sender, String[] args){
		boolean auth = false;
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.unban")) auth=true;
			kicker = player.getName();
		}else{
			auth = true;
		}
		// Has permission?
		if (!auth) return false;

		// Has enough arguments?
		if (args.length < 1) return false;

		String p = args[0];

		if(Database.removeFromBanlist(p)){
			// Now lets remove him from the array
			bannedPlayers.remove(p.toLowerCase());
			// Remove him from temporary bans if he's there
			if(tempBans.containsKey(p.toLowerCase()))
				tempBans.remove(p.toLowerCase());
			//Log in console
			log.log(Level.INFO, "[KiwiAdmin] " + kicker + " unbanned player " + p + ".");

			properties.load();
			String kickerMsg = properties.getNode("messages").getString("unbanMsg");
			String globalMsg = properties.getNode("messages").getString("unbanMsgGlobal");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			globalMsg = globalMsg.replaceAll("%victim%", p);
			globalMsg = globalMsg.replaceAll("%player%", kicker);
			//Send a message to unbanner!
			sender.sendMessage(formatMessage(kickerMsg));
			//send a message to everyone!
			this.getServer().broadcastMessage(formatMessage(globalMsg));
			return true;
		}else{
			//Unban failed
			properties.load();
			String kickerMsg = properties.getNode("messages").getString("unbanMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}
	}
	private boolean kickPlayer(CommandSender sender, String[] args){
		boolean auth = false;
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.kick")) auth=true;
			kicker = player.getName();
		}else{
			auth = true;
		}
		// Has permission?
		if (!auth) return false;

		// Has enough arguments?
		if (args.length < 1) return false;

		String p = args[0].toLowerCase();
		// Reason stuff
		String reason = "undefined";
		if(args.length > 1) reason = combineSplit(1, args, " ");

		if(p.equals("*") && Permissions.Security.permission(player, "kiwiadmin.kick.all")){
			properties.load();
			String kickerMsg = properties.getNode("messages").getString("kickAll");
			kickerMsg = kickerMsg.replaceAll("%player%", kicker);
			kickerMsg = kickerMsg.replaceAll("%reason%", reason);
			log.log(Level.INFO, "[KiwiAdmin] " + formatMessage(kickerMsg));

			// Kick everyone on server
			for (Player pl : this.getServer().getOnlinePlayers()) {
				pl.kickPlayer(formatMessage(kickerMsg));
				return true;
			}
		}
		if(autoComplete)
			p = expandName(p);
		Player victim = this.getServer().getPlayer(p);
		if(victim == null){
			properties.load();
			String kickerMsg = properties.getNode("messages").getString("kickMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}

		//Log in console
		log.log(Level.INFO, "[KiwiAdmin] " + kicker + " kicked player " + p + ". Reason: " + reason);

		//Send message to victim
		String kickerMsg = properties.getNode("messages").getString("kickMsgVictim");
		kickerMsg = kickerMsg.replaceAll("%player%", kicker);
		kickerMsg = kickerMsg.replaceAll("%reason%", reason);
		victim.kickPlayer(formatMessage(kickerMsg));

		//Send message to all players
		String kickerMsgAll = properties.getNode("messages").getString("kickMsgBroadcast");
		kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
		kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
		kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
		this.getServer().broadcastMessage(formatMessage(kickerMsgAll));
		return true;
	}
	private boolean banPlayer(CommandSender sender, String[] args){
		boolean auth = false;
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.ban")) auth=true;
			kicker = player.getName();
		}else{
			auth = true;
		}
		// Has permission?
		if (!auth) return false;

		// Has enough arguments?
		if (args.length < 1) return false;

		String p = args[0]; // Get the victim's name
		if(autoComplete)
			p = expandName(p); //If the admin has chosen to do so, autocomplete the name!
		Player victim = this.getServer().getPlayer(p); // What player is really the victim?
		// Reason stuff
		String reason = "undefined";
		if(args.length > 1) reason = combineSplit(1, args, " ");

		if(KiwiAdmin.bannedPlayers.contains(p.toLowerCase())){
			properties.load();
			String kickerMsg = properties.getNode("messages").getString("banMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}

		KiwiAdmin.bannedPlayers.add(p.toLowerCase()); // Add name to RAM

		// Add player to database
		db.addPlayer(p, reason, kicker);

		//Log in console
		log.log(Level.INFO, "[KiwiAdmin] " + kicker + " banned player " + p + ".");

		if(victim != null){ // If he is online, kick him with a nice message :)

			//Send message to victim
			String kickerMsg = properties.getNode("messages").getString("banMsgVictim");
			kickerMsg = kickerMsg.replaceAll("%player%", kicker);
			kickerMsg = kickerMsg.replaceAll("%reason%", reason);
			victim.kickPlayer(formatMessage(kickerMsg));
		}
		//Send message to all players
		String kickerMsgAll = properties.getNode("messages").getString("banMsgBroadcast");
		kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
		kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
		kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
		this.getServer().broadcastMessage(formatMessage(kickerMsgAll));

		return true;
	}

	private boolean tempbanPlayer(CommandSender sender, String[] args){
		boolean auth = false;
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.tempban")) auth=true;
			kicker = player.getName();
		}else{
			auth = true;
		}
		if (!auth) return false;

		if (args.length < 3) return false;

		String p = args[0]; // Get the victim's name
		if(autoComplete)
			p = expandName(p); //If the admin has chosen to do so, autocomplete the name!
		Player victim = this.getServer().getPlayer(p); // What player is really the victim?
		// Reason stuff
		String reason = "undefined";
		if(args.length > 3) reason = combineSplit(3, args, " ");

		if(KiwiAdmin.bannedPlayers.contains(p.toLowerCase())){
			properties.load();
			String kickerMsg = properties.getNode("messages").getString("banMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}

		KiwiAdmin.bannedPlayers.add(p.toLowerCase()); // Add name to RAM
		long tempTime = parseTimeSpec(args[1],args[2]); //parse the time and do other crap below
		tempTime = System.currentTimeMillis()+tempTime;
		KiwiAdmin.tempBans.put(p.toLowerCase(), tempTime); //put him in the temporary bans

		// Add to database
		db.addPlayer(p, reason, kicker, tempTime);

		//Log in console
		log.log(Level.INFO, "[KiwiAdmin] " + kicker + " tempbanned player " + p + ".");

		if(victim != null){ // If he is online, kick him with a nice message :)

			//Send message to victim
			String kickerMsg = properties.getNode("messages").getString("tempbanMsgVictim");
			kickerMsg = kickerMsg.replaceAll("%player%", kicker);
			kickerMsg = kickerMsg.replaceAll("%reason%", reason);
			victim.kickPlayer(formatMessage(kickerMsg));
		}
		//Send message to all players
		String kickerMsgAll = properties.getNode("messages").getString("tempbanMsgBroadcast");
		kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
		kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
		kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
		this.getServer().broadcastMessage(formatMessage(kickerMsgAll));

		return true;
	}
	
	private boolean checkBan(CommandSender sender, String[] args){
		String p = args[0];
		if(bannedPlayers.contains(p.toLowerCase()))
			sender.sendMessage(ChatColor.RED + "Player " + p + " is banned.");
		else
			sender.sendMessage(ChatColor.GREEN + "Player " + p + " is not banned.");
		return true;
	}
	
	private boolean reloadKA(CommandSender sender){
		boolean auth = false;
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.reload")) 
				auth=true;
			kicker = player.getName();
		}else{
			auth = true;
		}
		if (auth) {

			bannedPlayers.clear(); // Clear the arraylist
			tempBans.clear();

			db = new Database(this);

			log.log(Level.INFO, "[KiwiAdmin] " + kicker + " reloaded the banlist.");
			sender.sendMessage("§2Reloaded banlist.");
			return true;
		}
		return false;
	}

}
