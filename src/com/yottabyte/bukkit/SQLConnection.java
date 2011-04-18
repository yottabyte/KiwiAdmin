package com.yottabyte.bukkit;
import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.logging.Level;

import org.bukkit.util.config.Configuration;

public class SQLConnection {

    public static Connection getSQLConnection() {
    	Configuration Config = new Configuration(new File("plugins/KiwiAdmin/config.yml"));
    	Config.load();
		String mysqlDatabase = Config.getString("mysql-database","jdbc:mysql://localhost:3306/minecraft");
		String mysqlUser = Config.getString("mysql-user","root");
		String mysqlPassword = Config.getString("mysql-password","root");
    	
        try {
        	
            return DriverManager.getConnection(mysqlDatabase + "?autoReconnect=true&user=" + mysqlUser + "&password=" + mysqlPassword);
        } catch (SQLException ex) {
            KiwiAdmin.log.log(Level.SEVERE, "Unable to retreive connection", ex);
        }
        return null;
    }
	
}
