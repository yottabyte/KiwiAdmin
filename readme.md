KiwiAdmin readme
=============
Updating it for the Bukkit community was too big of a hassle and I'd like to focus on developing for my own server instead.

KiwiAdmin broke in a Bukkit update, so here is the latest source that we are using.

It includes some pretty awesome stuff such as
- Editing bans
- Ban records
- Silent bans/kicks
- IP bans and unbans
- Warnings

Plugin Usage
-----
(Copy pasta from my moderator guide..)

Putting a -s before the reason will make the action silent, so people won't see the notification.

Kicking a player:
/kick player (-s) (reason)

Banning a player:
/ban player (-s) (reason)

Unbanning a player:
/unban player

Temporary banning a player:
/tempban player time min/hour/day (-s) (reason)

### Editing bans

You can edit bans using /editban OR /eban OR /eb

/eb <list/load/id/save/view/reason/time/cancel>

list <player> will list the 10 last legal actions for a player.

load <player> will load the last legal action for a player so you can edit it

id <player> will load the id of a ban. If you want to edit a specific ban, use the list command to find the id.

save will save the thing you are editing.

cancel will stop the editing so you can load another one WITHOUT saving.

view will show information about the action you are currently editing.

reason set <text> will set the reason to whatever.

reason add <text> will add more information to the reason of the action.

time set <amount> <format> will set the lasting of the ban from the initial time.

time add <amount> <format> will add more time to the ban.

Database setup
--------
The latest version only has support for MySQL since flatfile was a pain. I'm sure you can add SQLite support easily if you want that.

	CREATE TABLE  `banlist` (
	  `name` varchar(32) NOT NULL,
	  `reason` text NOT NULL,
	  `admin` varchar(32) NOT NULL,
	  `time` bigint(20) NOT NULL,
	  `temptime` bigint(20) NOT NULL,
	  `id` int(11) NOT NULL AUTO_INCREMENT,
	  `type` int(1) NOT NULL DEFAULT '0',
	  PRIMARY KEY (`id`) USING BTREE
	) ENGINE=InnoDB AUTO_INCREMENT=97 DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC;
	
You will also need a table for storing offline player IPs. We use our whitelist table for it.

	CREATE TABLE  `players` (
	  `name` varchar(32) NOT NULL,
	  `lastip` tinytext NOT NULL,
	  PRIMARY KEY (`name`)
	) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC;
