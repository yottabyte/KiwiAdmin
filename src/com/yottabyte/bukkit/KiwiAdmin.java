
package com.yottabyte.bukkit;

import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
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
	
	public static Map<String,Boolean> bannedPlayers = new HashMap<String,Boolean>();
    private final KiwiAdminPlayerListener playerListener = new KiwiAdminPlayerListener(this);

    public KiwiAdmin(PluginLoader pluginLoader, Server instance,
            PluginDescriptionFile desc, File folder, File plugin,
            ClassLoader cLoader) {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);
    }
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

    public void onEnable() {
        // Register our events   	
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Highest, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
        
        
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
	                if (!file.exists()) {
	                  new File("plugins/KiwiAdmin").mkdir();
	                  file.createNewFile();
	                  System.out.println("Banlist not found, creating banlist.txt!");
	                }
	            }
			}
        catch (IOException e) {

            e.printStackTrace();
            
            }
        
    }
}
