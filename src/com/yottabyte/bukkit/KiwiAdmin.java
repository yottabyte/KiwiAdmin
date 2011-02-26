
package com.yottabyte.bukkit;

import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * Chat plugin
 *
 * @author yottabyte
 */

public class KiwiAdmin extends JavaPlugin {

	public static final Logger log = Logger.getLogger("Minecraft");

	static Permissions CurrentPermissions = null;
	/*
	  public static DataSource ds;

	  public static String dataSource = "mysql";
	  public static String user = "root";
	  public static String pass = "";
	  public static String db = "jdbc:mysql://localhost:3306/minecraft";
	  public static String table = "banlist";
	  public static String destination = "mysql";
	 */	
	public static Map<String,Boolean> bannedPlayers = new ConcurrentHashMap<String,Boolean>();
	private final KiwiAdminPlayerListener playerListener = new KiwiAdminPlayerListener(this);

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
		// NOTE: All registered events are automatically unregistered when a plugin is disabled

		// EXAMPLE: Custom code, here we just output some info so we can check all is well
		System.out.println("Goodbye world! KiwiAdmin is going to sleep! :(");
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
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		String commandName = command.getName().toLowerCase();
		String[] trimmedArgs = args;

		if(commandName.equals("ka")){
			//sender.sendMessage(ChatColor.GREEN + trimmedArgs[0]);
			if(trimmedArgs.length >= 1){
				if(trimmedArgs[0].equalsIgnoreCase("reload")){
					return reloadKA(sender);
				}
				if(trimmedArgs[0].equalsIgnoreCase("unban")){
					return unBanPlayer(sender,trimmedArgs);
				}
				if(trimmedArgs[0].equalsIgnoreCase("ban")){
					return banPlayer(sender,trimmedArgs);
				}
				if(trimmedArgs[0].equalsIgnoreCase("kick")){
					return kickPlayer(sender,trimmedArgs);
				}
			}
		}
		return false;
	}
	private boolean unBanPlayer(CommandSender sender, String[] args){
		boolean auth = false;
		Player player = null;
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.unban")) auth=true;
		}else{
			auth = true;
		}
		if (auth) {
			if (args.length > 1) {
				String p = args[1];
				// First, lets remove him from the file
				String file = "plugins/KiwiAdmin/banlist.txt";
				try {
					File banlist = new File(file);

					File tempFile = new File(banlist.getAbsolutePath() + ".tmp");

					BufferedReader br = new BufferedReader(new FileReader(file));
					PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

					String line = null;

					// Loops through the temporary file and deletes the player
					while ((line = br.readLine()) != null) {
						if (!line.trim().equals(p)) {

							pw.println(line);
							pw.flush();
						}
					}
					// All done, SHUT. DOWN. EVERYTHING.
					pw.close();
					br.close();

					// Let's delete the old banlist.txt and change the name of our temporary list!
					banlist.delete();
					tempFile.renameTo(banlist);

				}
				catch (FileNotFoundException ex) {
					ex.printStackTrace();
				}
				catch (IOException ex) {
					ex.printStackTrace();
				}

				// Now lets remove him from the HashTable.
				KiwiAdmin.bannedPlayers.remove(p);

				//Send a message!
				sender.sendMessage("§aUnbanned " + p);

				return true;

			}
			return false;
		}
		return false;
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
		if (auth) {
			if (args.length > 1) {
				String p = args[1];
				Player victim = this.getServer().getPlayer(p);
				if(victim != null){
					if(args.length < 3){
						victim.kickPlayer("You have been kicked by " + kicker + ".");
						this.getServer().broadcastMessage("§6" + p + " was kicked by " + kicker + ".");
					}else{
						String reason = combineSplit(2, args, " ");
						victim.kickPlayer("You have been kicked by " + kicker + ". Reason: " + reason);
						this.getServer().broadcastMessage("§6" + p + " was kicked by " + kicker + ". Reason: " + reason);
					}
					return true;
				}else{
					player.sendMessage("§cKick failed: " + p + " isn't online.");
					return true;
				}
			}
			return false;
		}
		return false;
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
		if (auth) {
			if (args.length > 1) {
				String p = args[1];
				Player victim = this.getServer().getPlayer(p); // What player is really the victim?

				KiwiAdmin.bannedPlayers.put(p, true);
				try
				{
					BufferedWriter banlist = new BufferedWriter(new FileWriter("plugins/KiwiAdmin/banlist.txt",true));
					banlist.newLine();                    
					banlist.write(p);
					banlist.close();
				}
				catch(IOException e)          
				{
					System.out.println("KiwiAdmin: Couldn't write to banlist.txt");
					return false;
				}

				if(auth){
					log.log(Level.INFO, "Successfully banned " + p + "!");
				}

				if(victim != null){ // If he is online, kick him with a nice message :)

					if(args.length < 3){ //No reason, just kick.
						victim.kickPlayer("You have been banned by " + kicker + ".");
						this.getServer().broadcastMessage("§6" + p + " was banned by " + kicker + "!");
					}else{ // Look at that, a reason! Good admin :)
						String reason = combineSplit(2, args, " ");
						victim.kickPlayer("You have been banned by " + kicker + ". Reason: " + reason);
						this.getServer().broadcastMessage("§6" + p + " was banned by " + kicker + "! Reason: " + reason);
					}
					return true;
				}else{ //The victim wasn't online, let's just notify the admin that he actually banned someone
					sender.sendMessage("§6Successfully banned " + p + "!");
					return true;
				}
			}
			return false;
		}
		return false;
	}
	private boolean reloadKA(CommandSender sender){
		boolean auth = false;
		Player player = null;
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.reload")) auth=true;
		}else{
			auth = true;
		}
		if (auth) {
			try {
				KiwiAdmin.bannedPlayers.clear();
				File banlist = new File("plugins/KiwiAdmin/banlist.txt");
				BufferedReader in = new BufferedReader(new FileReader(banlist));
				String data = null;

				while ((data = in.readLine()) != null){
					//Checking for blank lines
					if (data.length()>0){
						KiwiAdmin.bannedPlayers.put(data, true);
					}		
				}
				in.close();
				player.sendMessage("§2Reloaded banlist.");
				return true;
			}
			catch (IOException e) {
				e.printStackTrace(); 
				return false;
			}
		}
		return false;
	}

	public void onEnable() {
		// Register our events   	
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Highest, this);

		// EXAMPLE: Custom code, here we just output some info so we can check all is well
		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );

		//read the banlist txt file
		try {

			File banlist = new File("plugins/KiwiAdmin/banlist.txt");

			if (banlist.exists()){ 
				BufferedReader in = new BufferedReader(new FileReader(banlist));
				String data = null;

				while ((data = in.readLine()) != null){
					//Checking for blank lines
					if (data.length()>0){
						bannedPlayers.put(data, true);
					}

				}
				in.close();
			}else{
				File file = new File("plugins/KiwiAdmin/banlist.txt");
				new File("plugins/KiwiAdmin").mkdir();
				file.createNewFile();
				System.out.println("Banlist not found, creating banlist.txt!");
			}
		}
		catch (IOException e) {

			e.printStackTrace();

		}  
		/*
      //mysql stuff

        if (destination.equalsIgnoreCase("mysql"))
            ds = new MySQL();
          else {
           // ds = new Flatfile();
          }
        if (!ds.init()) {
            log.severe("OnlineUsers: Could not init the datasource");
            getServer().getPluginManager().disablePlugin(this);
            return;
          }
		 */
	}
}
