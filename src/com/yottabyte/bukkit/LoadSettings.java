package com.yottabyte.bukkit;

/*
 * Settings stuff by yottabyte! Will be handy :3
 * 
 */

public class LoadSettings {
	public static boolean useMysql;
	public static String mysqlDatabase;
	public static String mysqlUser;
	public static String mysqlPassword;
	public static String mysqlTable;
	
	public static void loadMain(){
		String propertiesFile = KiwiAdmin.maindir + "config.properties";
		PluginProperties properties = new PluginProperties(propertiesFile);
		properties.load();
		
		useMysql = properties.getBoolean("mysql",false);
		mysqlDatabase = properties.getString("mysql-database","jdbc:mysql://localhost:3306/minecraft");
		mysqlUser = properties.getString("mysql-user","root");
		mysqlPassword = properties.getString("mysql-password","root");
		mysqlTable = properties.getString("mysql-table","banlist");
		
		properties.save("### KiwiAdmin configuration ###");
	}
	

}
