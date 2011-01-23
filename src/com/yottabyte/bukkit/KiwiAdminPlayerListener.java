
package com.yottabyte.bukkit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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
    
    public void removeLineFromFile(String file, String lineToRemove) {

        try {

          File inFile = new File(file);
          
          if (!inFile.isFile()) {
            System.out.println("KiwiAdmin: Can't find banlist.txt!");
            return;
          }
          
          File tempFile = new File(inFile.getAbsolutePath() + ".tmp");
          
          BufferedReader br = new BufferedReader(new FileReader(file));
          PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
          
          String line = null;

          while ((line = br.readLine()) != null) {
            
            if (!line.trim().equals(lineToRemove)) {

              pw.println(line);
              pw.flush();
            }
          }
          pw.close();
          br.close();
          
          //Delete the original file
          if (!inFile.delete()) {
            System.out.println("Could not delete file");
            return;
          } 
          
          //Rename the new file to the filename the original file had.
          if (!tempFile.renameTo(inFile))
            System.out.println("Could not rename file");
          
        }
        catch (FileNotFoundException ex) {
          ex.printStackTrace();
        }
        catch (IOException ex) {
          ex.printStackTrace();
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
	     if(fullMsg[0].equalsIgnoreCase("/unban")){
		    	if (Permissions.Security.permission(event.getPlayer(), "kiwiadmin.unban")) {
		    		if (fullMsg.length > 1) {
		    			String p = fullMsg[1];
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
			    	    player.sendMessage("§aUnbanned " + p);
		    			
		    		}else{
		    			player.sendMessage("§eUsage: /unban [player]");
		    		}
		    	}
		    }
	     if(fullMsg[0].equalsIgnoreCase("/reloadka")){
		    	if (Permissions.Security.permission(event.getPlayer(), "kiwiadmin.reload")) {
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
		    			}
		            catch (IOException e) {
		            e.printStackTrace(); 
		            }
		    	}
		    }
    }
    public void onPlayerLogin(PlayerLoginEvent event){
    	Player player = event.getPlayer();
    	Boolean banned = KiwiAdmin.bannedPlayers.get(player.getName());
    	if(banned != null){
    		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "You are banned from this server.");
    		System.out.println(player.getName() + " is banned! Deny!");
    	}else{
    		System.out.println(player.getName() + " is not banned. Allow!");
    	}
    }
}
