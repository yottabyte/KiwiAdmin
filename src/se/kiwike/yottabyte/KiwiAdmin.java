
package se.kiwike.yottabyte;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * Admin plugin for Bukkit.
 *
 * @author yottabyte
 */

public class KiwiAdmin extends JavaPlugin {

	public static final Logger log = Logger.getLogger("Minecraft");

	Permissions CurrentPermissions = null;
	MySQLDatabase db;
	String maindir = "plugins/KiwiAdmin/";
	File Settings = new File(maindir + "config.properties");
	ArrayList<String> bannedPlayers = new ArrayList<String>();
	ArrayList<String> bannedIPs = new ArrayList<String>();
	Map<String,Long> tempBans = new HashMap<String,Long>();
	Map<String, EditBan> banEditors = new HashMap<String, EditBan>();
	private final KiwiAdminPlayerListener playerListener = new KiwiAdminPlayerListener(this);

	//public Configuration properties = new Configuration(new File("plugins/KiwiAdmin/config.yml"));
	public FileConfiguration config;
	public boolean autoComplete;

	// NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled

	public void setupPermissions() {
		Plugin plugin = this.getServer().getPluginManager().getPlugin("Permissions");

		if (CurrentPermissions == null) {
			// Permission plugin already registered
			return;
		}

		if (plugin != null) {
			CurrentPermissions = (Permissions) plugin;
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
	protected void setupConfig() {
		this.config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		
	}
	public void setupURL(){
		String mysqlDatabase = getConfig().getString("mysql-database","jdbc:mysql://localhost:3306/minecraft");
		String mysqlUser = getConfig().getString("mysql-user","root");
		String mysqlPassword = getConfig().getString("mysql-password","root");
	}

	public void onEnable() {
		new File(maindir).mkdir();

		setupConfig();

		//boolean useMysql = properties.getBoolean("mysql", false);
		this.autoComplete = getConfig().getBoolean("auto-complete", true);

		db = new MySQLDatabase();
		db.initialize(this);

		// Register our events   	
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);

		getCommand("editban").setExecutor(new EditCommand(this));
		
		PluginDescriptionFile pdfFile = this.getDescription();
		log.log(Level.INFO,pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}

	public String combineSplit(int startIndex, String[] string, String seperator) {
		StringBuilder builder = new StringBuilder();

		for (int i = startIndex; i < string.length; i++) {
			builder.append(string[i]);
			builder.append(seperator);
		}

		builder.deleteCharAt(builder.length() - seperator.length()); // remove
		return builder.toString();
	}

	public long parseTimeSpec(String time, String unit) {
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
		return sec;
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
		if(commandName.equals("warn")){
			return warnPlayer(sender,trimmedArgs);
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
		if(commandName.equals("ipban")){
			return ipBan(sender,trimmedArgs);
		}
		if(commandName.equals("exportbans")){
			return exportBans(sender);
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

		if(bannedPlayers.remove(p.toLowerCase())){
			db.removeFromBanlist(p);
			// Remove him from temporary bans if he's there
			if(tempBans.containsKey(p.toLowerCase()))
				tempBans.remove(p.toLowerCase());
			String ip = db.getAddress(p);
			if(bannedIPs.contains(ip)){
				bannedIPs.remove(ip);
				System.out.println("Also removed the IP ban!");
			}
			//Log in console
			log.log(Level.INFO, "[KiwiAdmin] " + kicker + " unbanned player " + p + ".");

			String kickerMsg = getConfig().getString("messages.unbanMsgGlobal", "test");
			String globalMsg = getConfig().getString("messages.unbanMsgGlobal", "test");
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
			String kickerMsg = getConfig().getString("messages.unbanMsgFailed", "test");
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
		boolean broadcast = true;
		if(args.length > 1){
			if(args[1].equalsIgnoreCase("-s")){
				broadcast = false;
				reason = combineSplit(2, args, " ");
			}else
				reason = combineSplit(1, args, " ");
		}

		if(p.equals("*")){
			if (sender instanceof Player)
				if (!Permissions.Security.permission(player, "kiwiadmin.kick.all")) return false;

			String kickerMsg = getConfig().getString("messages.kickAllMag");
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
			String kickerMsg = getConfig().getString("messages.kickMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}

		//Log in console
		log.log(Level.INFO, "[KiwiAdmin] " + kicker + " kicked player " + p + ". Reason: " + reason);

		//Send message to victim
		String kickerMsg = getConfig().getString("messages.kickMsgVictim");
		kickerMsg = kickerMsg.replaceAll("%player%", kicker);
		kickerMsg = kickerMsg.replaceAll("%reason%", reason);
		victim.kickPlayer(formatMessage(kickerMsg));

		if(broadcast){
			//Send message to all players
			String kickerMsgAll = getConfig().getString("messages.kickMsgBroadcast");
			kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
			kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
			kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
			this.getServer().broadcastMessage(formatMessage(kickerMsgAll));
		}
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
		boolean broadcast = true;
		if(args.length > 1){
			if(args[1].equalsIgnoreCase("-s")){
				broadcast = false;
				reason = combineSplit(2, args, " ");
			}else
				reason = combineSplit(1, args, " ");
		}

		if(bannedPlayers.contains(p.toLowerCase())){
			String kickerMsg = getConfig().getString("messages.banMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}

		bannedPlayers.add(p.toLowerCase()); // Add name to RAM

		// Add player to database
		db.addPlayer(p, reason, kicker, 0, 0);

		//Log in console
		log.log(Level.INFO, "[KiwiAdmin] " + kicker + " banned player " + p + ".");

		if(victim != null){ // If he is online, kick him with a nice message :)

			//Send message to victim
			String kickerMsg = getConfig().getString("messages.banMsgVictim");
			kickerMsg = kickerMsg.replaceAll("%player%", kicker);
			kickerMsg = kickerMsg.replaceAll("%reason%", reason);
			victim.kickPlayer(formatMessage(kickerMsg));
		}
		//Send message to all players
		if(broadcast){
			String kickerMsgAll = getConfig().getString("messages.banMsgBroadcast");
			kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
			kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
			kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
			this.getServer().broadcastMessage(formatMessage(kickerMsgAll));
		}

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
		boolean broadcast = true;
		if(args.length > 3){
			if(args[3].equalsIgnoreCase("-s")){
				broadcast = false;
				reason = combineSplit(4, args, " ");
			}else
				reason = combineSplit(3, args, " ");
		}

		if(bannedPlayers.contains(p.toLowerCase())){
			String kickerMsg = getConfig().getString("messages.banMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}

		long tempTime = parseTimeSpec(args[1],args[2]); //parse the time and do other crap below
		if(tempTime == 0)
			return false;
		bannedPlayers.add(p.toLowerCase()); // Add name to RAM
		tempTime = System.currentTimeMillis()/1000+tempTime;
		tempBans.put(p.toLowerCase(), tempTime); //put him in the temporary bans

		// Add to database
		db.addPlayer(p, reason, kicker, tempTime, 0);

		//Log in console
		log.log(Level.INFO, "[KiwiAdmin] " + kicker + " tempbanned player " + p + ".");

		if(victim != null){ // If he is online, kick him with a nice message :)

			//Send message to victim
			String kickerMsg = getConfig().getString("messages.tempbanMsgVictim");
			kickerMsg = kickerMsg.replaceAll("%player%", kicker);
			kickerMsg = kickerMsg.replaceAll("%reason%", reason);
			victim.kickPlayer(formatMessage(kickerMsg));
		}
		if(broadcast){
			//Send message to all players
			String kickerMsgAll = getConfig().getString("messages.tempbanMsgBroadcast");
			kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
			kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
			kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
			this.getServer().broadcastMessage(formatMessage(kickerMsgAll));
		}
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
	private boolean ipBan(CommandSender sender, String[] args){
		boolean auth = false;
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.ipban")) auth=true;
			kicker = player.getName();
		}else{
			auth = true;
		}
		if (!auth) return false;

		// Has enough arguments?
		if (args.length < 1) return false;

		String p = args[0]; // Get the victim's name
		if(autoComplete)
			p = expandName(p); //If the admin has chosen to do so, autocomplete the name!
		Player victim = this.getServer().getPlayer(p); // What player is really the victim?
		if(victim == null){
			sender.sendMessage("Couldn't find player.");
			return true;
		}
		// Reason stuff
		String reason = "undefined";
		boolean broadcast = true;
		if(args.length > 1){
			if(args[1].equalsIgnoreCase("-s")){
				broadcast = false;
				reason = combineSplit(2, args, " ");
			}else
				reason = combineSplit(1, args, " ");
		}

		if(bannedPlayers.contains(p.toLowerCase())){
			String kickerMsg = getConfig().getString("messages.banMsgFailed");
			kickerMsg = kickerMsg.replaceAll("%victim%", p);
			sender.sendMessage(formatMessage(kickerMsg));
			return true;
		}

		bannedPlayers.add(p.toLowerCase()); // Add name to RAM
		bannedIPs.add(victim.getAddress().getAddress().getHostAddress()); // Add ip to RAM

		// Add player to database
		db.addPlayer(p, reason, kicker, 0, 1);

		//Log in console
		log.log(Level.INFO, "[KiwiAdmin] " + kicker + " banned player " + p + ".");

		//Send message to victim
		String kickerMsg = getConfig().getString("messages.banMsgVictim");
		kickerMsg = kickerMsg.replaceAll("%player%", kicker);
		kickerMsg = kickerMsg.replaceAll("%reason%", reason);
		victim.kickPlayer(formatMessage(kickerMsg));

		if(broadcast){
			//Send message to all players
			String kickerMsgAll = getConfig().getString("messages.banMsgBroadcast");
			kickerMsgAll = kickerMsgAll.replaceAll("%player%", kicker);
			kickerMsgAll = kickerMsgAll.replaceAll("%reason%", reason);
			kickerMsgAll = kickerMsgAll.replaceAll("%victim%", p);
			this.getServer().broadcastMessage(formatMessage(kickerMsgAll));
		}

		return true;
	}	

	private boolean warnPlayer(CommandSender sender, String[] args){
		boolean auth = false;
		Player player = null;
		String kicker = "server";
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.warn")) auth=true;
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
		boolean broadcast = true;
		if(args.length > 1){
			if(args[1].equalsIgnoreCase("-s")){
				broadcast = false;
				reason = combineSplit(2, args, " ");
			}else
				reason = combineSplit(1, args, " ");
		}

		// Add player to database
		db.addPlayer(p, reason, kicker, 0, 2);

		//Log in console
		log.log(Level.INFO, "[KiwiAdmin] " + kicker + " warned player " + p + ".");


		//Send message to all players
		if(broadcast){
			this.getServer().broadcastMessage(ChatColor.RED + "Player " + p + " recieved a warning from " + kicker + ":");
			this.getServer().broadcastMessage(ChatColor.GRAY + "  " + reason);
		}else{
			if(victim != null){ // If he is online, kick him with a nice message :)
			victim.sendMessage(ChatColor.RED + "You have recieved a warning from " + kicker + ":");
			victim.sendMessage(ChatColor.GRAY + "  " + reason);
			}
		}

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

			log.log(Level.INFO, "[KiwiAdmin] " + kicker + " reloaded the banlist.");
			sender.sendMessage("§2Reloaded banlist.");
			return true;
		}
		return false;
	}

	private boolean exportBans(CommandSender sender){
		boolean auth = false;
		Player player = null;
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.export")) 
				auth=true;
		}else{
			auth = true;
		}
		if (auth) {
			try
			{
				BufferedWriter banlist = new BufferedWriter(new FileWriter("banned-players.txt",true));
				for(String p : bannedPlayers){
					banlist.newLine();
					banlist.write(p);
				}
				banlist.close();
			}
			catch(IOException e)          
			{
				KiwiAdmin.log.log(Level.SEVERE,"KiwiAdmin: Couldn't write to banned-players.txt");
			}
			sender.sendMessage("§2Exported banlist to banned-players.txt.");
			return true;
		}
		return false;
	}

}
