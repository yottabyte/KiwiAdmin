
package com.yottabyte.bukkit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.entity.Player;

import com.nijikokun.bukkit.Permissions.Permissions;
/**
 * Simple admin plugin for Bukkit.
 * @author yottabyte
 */
public class KiwiAdminPlayerListener extends PlayerListener {
    private final KiwiAdmin plugin;
    public KiwiAdminPlayerListener(KiwiAdmin instance) {
        plugin = instance;
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
   public void banPlayer(String name){
	   KiwiAdmin.bannedPlayers.put(name, true);
		try
        {
        	BufferedWriter banlist = new BufferedWriter(new FileWriter("plugins/KiwiAdmin/banlist.txt",true));
            banlist.newLine();                    
            banlist.write(name);
            banlist.close();
        }
        catch(IOException e)          
        {
        	System.out.println("KiwiAdmin: Couldn't write to banlist.txt");
        }
   }
	    	
    @Override
    public void onPlayerCommand(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String fullMsg[] = event.getMessage().split(" ");
        
        
        if(fullMsg[0].equalsIgnoreCase("/kick")){
        	if (Permissions.Security.permission(event.getPlayer(), "kiwiadmin.kick")) {
        		if (fullMsg.length > 1) {
	        		String p = fullMsg[1];
	        		Player victim = plugin.getServer().getPlayer(p);
	        		if(victim != null){
	        			if(fullMsg.length < 3){
	        				victim.kickPlayer("You have been kicked by " + player.getName() + ".");
	        				plugin.getServer().broadcastMessage("§6" + p + " was kicked by " + player.getName() + ".");
	        			}else{
	        				String reason = combineSplit(2, fullMsg, " ");
	        				victim.kickPlayer("You have been kicked by " + player.getName() + ". Reason: " + reason);
	        				plugin.getServer().broadcastMessage("§6" + p + " was kicked by " + player.getName() + ". Reason: " + reason);
	        			}
	        		}else{
	        			player.sendMessage("§cKick failed: " + p + " isn't online.");
	        		}
        		}else{
        			player.sendMessage("§eUsage: /kick [player] (reason)");
        		}
        	}
        }
	     if(fullMsg[0].equalsIgnoreCase("/ban")){
	    	if (Permissions.Security.permission(event.getPlayer(), "kiwiadmin.ban")) {
	    		if (fullMsg.length > 1) {
	        		String p = fullMsg[1];
	        		Player victim = plugin.getServer().getPlayer(p); // What player is really the victim?
	        		
	        		banPlayer(p); // Add him to banned-players.txt!
	        		
	        		if(victim != null){ // If he is online, kick him with a nice message :)
	        			
	        			if(fullMsg.length < 3){ //No reason, just kick.
	        				victim.kickPlayer("You have been banned by " + player.getName() + ".");
	        				plugin.getServer().broadcastMessage("§6" + p + " was banned by " + player.getName() + "!");
	        			}else{ // Look at that, a reason! Good admin :)
	        				String reason = combineSplit(2, fullMsg, " ");
	        				victim.kickPlayer("You have been banned by " + player.getName() + ". Reason: " + reason);
	        				plugin.getServer().broadcastMessage("§6" + p + " was banned by " + player.getName() + "! Reason: " + reason);
	        			}
	        		}else{ //The victim wasn't online, let's just notify the admin that he actually banned someone
	        			player.sendMessage("§6Successfully banned " + p + "!");
	        		}
	    		}else{
	    			player.sendMessage("§eUsage: /ban [player] (reason)");
	    		}
	    	}
	    }
    }
    public void onPlayerLogin(PlayerLoginEvent event){
    	Player player = event.getPlayer();
    	Boolean banned = KiwiAdmin.bannedPlayers.get(player.getName());
    	if(banned != null){
    		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "You are banned from this server.");
    		System.out.println(player.getName() + " is banned! Deny");
    	}else{
    		System.out.println(player.getName() + " is not banned. Allow");
    	}
    }
}
