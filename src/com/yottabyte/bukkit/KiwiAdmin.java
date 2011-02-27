
package com.yottabyte.bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
 * Admin plugin for Bukkit.
 *
 * @author yottabyte
 */

public class KiwiAdmin extends JavaPlugin {

	public static final Logger log = Logger.getLogger("Minecraft");

	static Permissions CurrentPermissions = null;
	static String maindir = "plugins/KiwiAdmin/";
	static File Settings = new File(maindir + "config.properties");
	static ArrayList<String> bannedPlayers = new ArrayList<String>();
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
	}


	public void onEnable() {
		new File(maindir).mkdir();
		if(!Settings.exists()){
			try {
				Settings.createNewFile();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		//load settings!
		LoadSettings.loadMain();

		// Register our events   	
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Highest, this);

		// EXAMPLE: Custom code, here we just output some info so we can check all is well
		PluginDescriptionFile pdfFile = this.getDescription();
		log.log(Level.INFO,pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );

		if(LoadSettings.useMysql){
			Connection conn = SQLConnection.getSQLConnection();
			if (conn == null) {
				log.log(Level.SEVERE, "[KiwiAdmin] Could not establish SQL connection. Disabling KiwiAdmin");
				getServer().getPluginManager().disablePlugin(this);
				return;
			} else {

				PreparedStatement ps = null;
				ResultSet rs = null;	
				try {
					ps = conn.prepareStatement("SELECT * FROM " + LoadSettings.mysqlTable);
					rs = ps.executeQuery();
					while (rs.next())
						bannedPlayers.add(rs.getString("name"));
				} catch (SQLException ex) {
					log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
				} finally {
					try {
						if (ps != null)
							ps.close();
						if (conn != null)
							conn.close();
					} catch (SQLException ex) {
						log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
					}
				}	

				try {
					conn.close();
					log.log(Level.INFO, "[KiwiAdmin] Initialized db connection" );
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}		
		}
		else{ //if not using mysql, use flatfile instead!

			//read the banlist txt file
			try {

				File banlist = new File("plugins/KiwiAdmin/banlist.txt");

				if (banlist.exists()){ 
					BufferedReader in = new BufferedReader(new FileReader(banlist));
					String data = null;

					while ((data = in.readLine()) != null){
						//Checking for blank lines
						if (data.length()>0){
							bannedPlayers.add(data.toLowerCase());
						}

					}
					in.close();
				}
			}
			catch (IOException e) {

				e.printStackTrace();

			} 
		}
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
		String kicker = "server";
		if (sender instanceof Player){
			player = (Player)sender;
			if (Permissions.Security.permission(player, "kiwiadmin.unban")) 
				auth=true;
			kicker = player.getName();
		}else{
			auth = true;
		}
		if (auth) {
			if (args.length > 1) {
				String p = args[1].toLowerCase();
				// First, lets remove him from the file
				if(KiwiAdmin.bannedPlayers.contains(p)){
					if(LoadSettings.useMysql){

						Connection conn = null;
						PreparedStatement ps = null;
						try {
							conn = SQLConnection.getSQLConnection();
							ps = conn.prepareStatement("DELETE FROM " + LoadSettings.mysqlTable + " WHERE name = ?");
							ps.setString(1, p);
							ps.executeUpdate();
						} catch (SQLException ex) {
							sender.sendMessage("§cError when unbanning " + p + "!");
							log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
							return true;
						} finally {
							try {
								if (ps != null)
									ps.close();
								if (conn != null)
									conn.close();
							} catch (SQLException ex) {
								log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
							}
						}

					}else{
						//flatfile setup
						try {
							String file = "plugins/KiwiAdmin/banlist.txt";
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
					}
					// Now lets remove him from the array
					bannedPlayers.remove(p);

					//Log in console
					log.log(Level.INFO, "[KiwiAdmin] " + kicker + " unbanned player " + p + ".");

					//Send a message!
					sender.sendMessage("§aSuccessfully unbanned player §e" + p + "§a!");
					//send a message to everyone!
					this.getServer().broadcastMessage("§e" + p + " §6was unbanned by §e" + kicker + "§6!");
					return true;

				}
				else{
					sender.sendMessage("§cPlayer §e" + p + "§c isn't banned!");
					return true;
				}

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
				String p = args[1].toLowerCase();
				Player victim = this.getServer().getPlayer(p);
				if(victim != null){
					//Log in console
					log.log(Level.INFO, "[KiwiAdmin] " + kicker + " kicked player " + p + ".");

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
					sender.sendMessage("§cKick failed: " + p + " isn't online.");
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
				String p = args[1].toLowerCase(); // Get the victim's name
				Player victim = this.getServer().getPlayer(p); // What player is really the victim?
				String reason = null; //no reason

				if(KiwiAdmin.bannedPlayers.contains(p)){
					sender.sendMessage("§cPlayer §e" + p + " §cis already banned!");
					return true;
				}

				if(args.length >= 3){ // ooh, more than 2 arguments, thats a reason! good boy
					reason = combineSplit(2, args, " ");
				}

				KiwiAdmin.bannedPlayers.add(p); // Remove name from RAM

				/*
				 * Do all that fun stuff, open file/table and remove their entry.
				 * 
				 */
				if(LoadSettings.useMysql){ // Using MySQL, bunch of crap here.

					Connection conn = null;
					PreparedStatement ps = null;
					try {
						conn = SQLConnection.getSQLConnection();
						if(reason == null)
							reason = "";
						java.util.Date date = new java.util.Date(); 
						Timestamp time = new Timestamp(date.getTime()); 
						ps = conn.prepareStatement("INSERT INTO " + LoadSettings.mysqlTable + " (name,reason,admin,time) VALUES(?,?,?,?)");
						ps.setString(1, p);
						ps.setString(2, reason);
						ps.setString(3, kicker);
						ps.setTimestamp(4, time);
						ps.executeUpdate();
					} catch (SQLException ex) {
						log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
					} finally {
						try {
							if (ps != null)
								ps.close();
							if (conn != null)
								conn.close();
						} catch (SQLException ex) {
							log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
						}
					}

				}else{ // Flatfile!
					try
					{
						BufferedWriter banlist = new BufferedWriter(new FileWriter("plugins/KiwiAdmin/banlist.txt",true));
						banlist.newLine();                    
						banlist.write(p);
						banlist.close();
					}
					catch(IOException e)          
					{
						log.log(Level.SEVERE,"KiwiAdmin: Couldn't write to banlist.txt");
						return false;
					}
				}

				//Log in console
				log.log(Level.INFO, "[KiwiAdmin] " + kicker + " banned player " + p + ".");

				if(victim != null){ // If he is online, kick him with a nice message :)

					if(reason == null){ //No reason, just ban.
						victim.kickPlayer("You have been banned by " + kicker + ".");
						this.getServer().broadcastMessage("§6" + p + " was banned by " + kicker + "!");
					}else{ // Look at that, a reason! Good admin :)
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

			KiwiAdmin.bannedPlayers.clear(); // Clear the arraylist

			if(LoadSettings.useMysql){ // Using MySQL
				Connection conn = null;
				PreparedStatement ps = null;
				ResultSet rs = null;	
				try {
					conn = SQLConnection.getSQLConnection();
					ps = conn.prepareStatement("SELECT * FROM " + LoadSettings.mysqlTable);
					rs = ps.executeQuery();
					while (rs.next())
						bannedPlayers.add(rs.getString("name"));
				} catch (SQLException ex) {
					log.log(Level.SEVERE, "[KiwiAdmin] Couldn't execute MySQL statement: ", ex);
					sender.sendMessage("Error when reloading. :c");
					return true;
				} finally {
					try {
						if (ps != null)
							ps.close();
						if (conn != null)
							conn.close();
					} catch (SQLException ex) {
						log.log(Level.SEVERE, "[KiwiAdmin] Failed to close MySQL connection: ", ex);
					}
				}	
			}else{ // Using flatfile
				try {
					File banlist = new File("plugins/KiwiAdmin/banlist.txt");
					BufferedReader in = new BufferedReader(new FileReader(banlist));
					String data = null;

					while ((data = in.readLine()) != null){
						//Checking for blank lines
						if (data.length()>0){
							KiwiAdmin.bannedPlayers.add(data.toLowerCase());
						}		
					}
					in.close();
				}
				catch (IOException e) {
					e.printStackTrace(); 
					return false;
				}
			}
			log.log(Level.INFO, "[KiwiAdmin] " + kicker + " reloaded the banlist.");
			sender.sendMessage("§2Reloaded banlist.");
			return true;
		}
		return false;
	}

}
