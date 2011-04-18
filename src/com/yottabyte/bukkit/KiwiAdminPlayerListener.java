
package com.yottabyte.bukkit;

import java.util.Date;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.entity.Player;
import com.yottabyte.bukkit.KiwiAdmin;

/**
 * Simple admin plugin for Bukkit.
 * @author yottabyte
 */
public class KiwiAdminPlayerListener extends PlayerListener {
	private final KiwiAdmin plugin;

	public KiwiAdminPlayerListener(KiwiAdmin instance) {
		this.plugin = instance;
	}
	public void onPlayerLogin(PlayerLoginEvent event){
		Player player = event.getPlayer();
		if(KiwiAdmin.bannedPlayers.contains(player.getName().toLowerCase())){
			if(KiwiAdmin.tempBans.get(player.getName().toLowerCase()) != null){
				long tempTime = KiwiAdmin.tempBans.get(player.getName().toLowerCase());
				long now = System.currentTimeMillis();
				long diff = tempTime - now;
				if(diff <= 0){
					if(Database.removeFromBanlist(player.getName().toLowerCase())){
						KiwiAdmin.bannedPlayers.remove(player.getName().toLowerCase());
						if(KiwiAdmin.tempBans.containsKey(player.getName().toLowerCase()))
							KiwiAdmin.tempBans.remove(player.getName().toLowerCase());
					}
					return;
				}
				Date date = new Date();
				date.setTime(tempTime);
				plugin.properties.load();
				String kickerMsg = plugin.formatMessage(plugin.properties.getNode("messages").getString("LoginTempban"));
				kickerMsg = kickerMsg.replaceAll("%time%", date.toString());
				kickerMsg = kickerMsg.replaceAll("%reason%", Database.getReason(player.getName().toLowerCase()));
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickerMsg);
				return;
			}
			plugin.properties.load();
			String kickerMsg = plugin.formatMessage(plugin.properties.getNode("messages").getString("LoginBan"));
			kickerMsg = kickerMsg.replaceAll("%reason%", Database.getReason(player.getName().toLowerCase()));
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickerMsg);
		}
	}
}
