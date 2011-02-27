
package com.yottabyte.bukkit;

import java.util.logging.Level;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.entity.Player;

/**
 * Simple admin plugin for Bukkit.
 * @author yottabyte
 */
public class KiwiAdminPlayerListener extends PlayerListener {
    public KiwiAdminPlayerListener(KiwiAdmin instance) {
    }
    
    public void onPlayerLogin(PlayerLoginEvent event){
    	Player player = event.getPlayer();
    	if(KiwiAdmin.bannedPlayers.contains(player.getName().toLowerCase())){
    		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "You are banned from this server.");
    		KiwiAdmin.log.log(Level.INFO, player.getName() + " is banned! Deny!");
    	}
    }
}
