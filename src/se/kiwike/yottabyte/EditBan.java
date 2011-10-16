package se.kiwike.yottabyte;

public class EditBan {

	int id;
	String name;
	String reason;
	String admin;
	long time;
	long endTime;
	int type;
	
	EditBan(int id, String name, String reason, String admin, long time, long endTime, int type){
		this.id = id;
		this.name = name;
		this.reason = reason;
		this.admin = admin;
		this.time = time;
		this.endTime = endTime;
		this.type = type;
	}
		
}
