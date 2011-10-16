package se.kiwike.yottabyte;

public abstract class Database {

	protected KiwiAdmin plugin;

	public abstract void initialize(KiwiAdmin plugin);

	/*
	 * Remove a player from banlist
	 */
	public abstract boolean removeFromBanlist(String player);

	/*
	 * Add a new player
	 */
	public abstract void addPlayer(String p, String reason, String kicker, long tempTime);

	/*
	 * Get ban reason
	 */
	public abstract String getBanReason(String player);

	public abstract void addAddress(String p, String ip);

}