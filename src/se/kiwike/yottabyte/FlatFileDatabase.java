package se.kiwike.yottabyte;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

public class FlatFileDatabase extends Database{

	/*
	 * 
	 * ABANDONED FILE LEFTOVERS...
	 * 
	 */
	
	public void initialize(KiwiAdmin plugin){

		this.plugin = plugin;

		try {

			File banlist = new File("plugins/KiwiAdmin/banlist.txt");

			if (banlist.exists()){ 
				BufferedReader in = new BufferedReader(new FileReader(banlist));
				String data = null;

				while ((data = in.readLine()) != null){
					//Checking for blank lines
					if (!data.startsWith("#")){
						if (data.length()>0){
							String[] values = data.split(">>");
							String player = values[0];
							long pTime = Long.parseLong(values[1]);
							if(pTime > 0)
								plugin.tempBans.put(player.toLowerCase(),pTime);


							plugin.bannedPlayers.add(player.toLowerCase());
						}
					}

				}
				in.close();
			}

			File banlistip = new File("plugins/KiwiAdmin/iplist.txt");

			if (banlistip.exists()){ 
				BufferedReader in = new BufferedReader(new FileReader(banlistip));
				String data = null;

				while ((data = in.readLine()) != null){
					//Checking for blank lines
					if (!data.startsWith("#")){
						if (data.length()>0){
							String[] values = data.split(">>");
							String ip = values[1];								
							plugin.bannedIPs.add(ip);
						}
					}

				}
				in.close();
			}

		}
		catch (IOException e) {

			e.printStackTrace();

		} 
	}

	@Override
	public boolean removeFromBanlist(String player) {

		try {

			String file = "plugins/KiwiAdmin/banlist.txt";
			File banlist = new File(file);

			File tempFile = new File(banlist.getAbsolutePath() + ".tmp");

			BufferedReader br = new BufferedReader(new FileReader(file));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

			String line = null;

			// Loops through the temporary file and deletes the player
			while ((line = br.readLine()) != null) {
				if (!line.trim().toLowerCase().startsWith(player.toLowerCase()) && line.length() > 0) {
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

			return true;

		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	@Override
	public void addPlayer(String p, String reason, String kicker, long tempTime) {

		try
		{
			BufferedWriter banlist = new BufferedWriter(new FileWriter("plugins/KiwiAdmin/banlist.txt",true));
			banlist.newLine();
			banlist.write(p+">>"+reason+">>"+kicker+">>"+System.currentTimeMillis()/1000+">>"+tempTime);
			banlist.close();
		}
		catch(IOException e)          
		{
			KiwiAdmin.log.log(Level.SEVERE,"KiwiAdmin: Couldn't write to banlist.txt");
		}

	}

	@Override
	public String getBanReason(String player) {
		try{
			BufferedReader in = new BufferedReader(new FileReader("plugins/KiwiAdmin/banlist.txt"));
			String data = null;

			while ((data = in.readLine()) != null){
				//Checking for blank lines
				if (!data.startsWith("#")){
					if (data.trim().toLowerCase().startsWith(player.toLowerCase()) && data.length()>0){

						String[] values = data.split(">>");

						return values[1];

					}
				}

			}
			in.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void addAddress(String p, String ip) {
		try
		{
			BufferedWriter banlist = new BufferedWriter(new FileWriter("plugins/KiwiAdmin/iplist.txt",true));
			banlist.newLine();
			banlist.write(p+">>"+ip);
			banlist.close();
		}
		catch(IOException e)          
		{
			KiwiAdmin.log.log(Level.SEVERE,"KiwiAdmin: Couldn't write to iplist.txt");
		}

	}

}
