package com.mronlinecoder.advancedmutes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.mronlinecoder.advancedmutes.MuteInfo.MuteState;

public class AdvancedMutes extends JavaPlugin implements Listener{
	
	ArrayList<MuteRule> rules = new ArrayList<>();
	
	HashMap<String, MuteInfo> queue = new HashMap<>();
	HashMap<String, MuteInfo> muted = new HashMap<>();
	
	DateFormat f = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		rules.add(new MuteRule("Использование другого языка в чате, кроме как русского", "1h", "12h"));
		rules.add(new MuteRule("Выдавание себя за администратора или модератора","5h","24h"));
		rules.add(new MuteRule("Оскробление игроков / модераторов / администрации.","1h","24h"));
		rules.add(new MuteRule("Употребление ругательств, мата, нецензурных слов и эвфемизмов.","2h","32h"));
		rules.add(new MuteRule("Упоминание родных лиц в оскорбительной и/или унизительной форме.","3h","48h"));
		rules.add(new MuteRule("Унижение человеческого достоинства, угрозы расправы, угрозы баном, сообщения сексуального характера, угрозы расправой в реальной жизни, угрозы баном, дискриминация людей по религиозному или другим признакам, пропаганда/обсуждение нацизма, наркотиков, алкоголизма и табакокурения","1d","7d"));
		rules.add(new MuteRule("Флуд","15m","24h"));
		rules.add(new MuteRule("Обилие символов (повторение 5 или более раз)","15m","24h"));
		rules.add(new MuteRule("Реклама услуг/магазинов/варпов чаще чем раз в 5 минут","5h","24h"));
		rules.add(new MuteRule("Флуд командами сервера / обход мута","1d","7d"));
		rules.add(new MuteRule("Дискриминация неопытных игроков, дезинформирование, насмешки.","3h","7d"));
		rules.add(new MuteRule("Отправка провокационных сообщений, сомнительных или внешних ссылок.", "3h", "7d"));
		rules.add(new MuteRule("Подстрекание третьих лиц на нарушение правил, подстава игроков, отправка лживых сведений от любого игрока в третьем лице.", "7d", "7d"));
		rules.add(new MuteRule("Публичная критика/обсуждение действий администратора или модераторов", "1d", "1d"));
		rules.add(new MuteRule("Публичная критика проекта/серверов (серверный чат, комментарии на сайтах, форум).", "1h", "ban"));
		rules.add(new MuteRule("Ложные сведения о вайпе.", "12h", "12h"));
		
		loadConfig();
	}
	
	public void loadConfig() {
		if (!getConfig().contains("mutes")) return;
		
		ConfigurationSection cfg = getConfig().getConfigurationSection("mutes");
		
		for(String key : cfg.getKeys(false)){
			MuteInfo info = new MuteInfo();
			info.duration = cfg.getString(key+".duration");
			info.end = cfg.getLong(key+".end");
			info.start = cfg.getLong(key+".start");
			info.reason = cfg.getString(key+".reason");
			info.issuer = cfg.getString(key+".issuer");
			info.state = MuteState.GIVEN;
			info.target = key;
			
			getLogger().info("Loaded mute for player "+key);
			muted.put(key, info);
		}
	}
	
	public void sendLastMuteInfo(Player ipl, String target) {
		if (getConfig().contains("stats."+target+".lastIssuer")) {
			String lis = getConfig().getString("stats."+target+".lastIssuer");
			String lrule = getConfig().getString("stats."+target+".lastRule");
			long lstart = getConfig().getLong("stats."+target+".lastStart");
			
			String lstartStr = f.format(new Date(lstart));
			ipl.sendMessage(ChatColor.GOLD+"Последний мут был выдан "+ChatColor.RED+lstartStr+ChatColor.GOLD+" игроком "+ChatColor.RED+lis);
			ipl.sendMessage(ChatColor.GOLD+"Причина: "+ChatColor.RED+lrule);
		}
	}
	
	public void saveMutes() {
		getLogger().info("Saving mutes...");
		
		if (!getConfig().contains("mutes")) {
			getConfig().createSection("mutes");
		} else {
			getConfig().set("mutes", null);
		}
		
		ConfigurationSection cfg = getConfig().getConfigurationSection("mutes");
		
		for (Map.Entry<String, MuteInfo> entry : muted.entrySet()) {
		    String k = entry.getKey();
		    MuteInfo info = entry.getValue();
		    
		    cfg.set(k+".duration", info.duration);
		    cfg.set(k+".end", info.end);
		    cfg.set(k+".start", info.start);
		    cfg.set(k+".reason", info.reason);
		    cfg.set(k+".issuer", info.issuer);
		    cfg.set(k+".target", k);
		    getLogger().info("Saved mute for "+k);
		}
	
	}
	
	public void onDisable() {
		saveMutes();
		saveConfig();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]) {
		if (label.equalsIgnoreCase("advmute")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED+"Only for ingame players! Use Essentials /mute from console.");
				return false;
			}
			
			Player ipl = (Player) sender;
			
			if (!ipl.hasPermission("advancedmutes.mute")) {
				ipl.sendMessage(ChatColor.RED+"У вас нет прав на выдачу мута!");
				return false;
			}
			
			if (args.length == 0) {
				ipl.sendMessage(ChatColor.GOLD+"AdvancedMutes by MrOnlineCoder");
				ipl.sendMessage(ChatColor.GOLD+"Использование: /advmute <игрок>");
				return false;
			}
			
			String target = args[0];
			Player pl = getServer().getPlayer(target);
			
			if (target.equals(ipl.getName())) {
				ipl.sendMessage(ChatColor.RED+"Вы не можете замутить самого себя!");
				return false;
			}
			
			if (pl == null) {
				ipl.sendMessage(ChatColor.RED+"Указанный игрок не в сети!");
				return false;
			}
			
			boolean flag = false;
			
			if (queue.get(ipl.getName()) != null) {
				flag = true;
				queue.remove(ipl.getName());
			}
			
			MuteInfo info = new MuteInfo();
			info.issuer = sender.getName();
			info.target = target;
			info.start = System.currentTimeMillis();
			info.state = MuteState.RULE;
			queue.put(sender.getName(), info);
			ipl.sendMessage(ChatColor.GOLD+"Введите в чат номер правила, которое было нарушено игроком или 0 для отмены:");
			
			for (int i=0;i<rules.size();i++) {
				MuteRule rule = rules.get(i);
				ipl.sendMessage(ChatColor.RED+"[ "+(i+1)+" ] "+ChatColor.GOLD+rule.rule);
			}
			
			if (flag) ipl.sendMessage(ChatColor.RED+"Внимание: вы не закончили выдачу предыдущего мута. Его выдача была отменена.");
			return false;
		}
		
		if (label.equalsIgnoreCase("unmute")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED+"Only for ingame players! Use Essentials /mute from console.");
				return false;
			}
			
			Player ipl = (Player) sender;
			
			if (!ipl.hasPermission("advancedmutes.unmute")) {
				ipl.sendMessage(ChatColor.RED+"У вас нет прав на снятие мута!");
				return false;
			}
			
			if (args.length == 0) {
				ipl.sendMessage(ChatColor.GOLD+"Использование: /unmute <игрок>");
				return false;
			}
		
			
			String target = args[0];
			
			if (target.equals(ipl.getName())) {
				ipl.sendMessage(ChatColor.RED+"Вы не можете размутить самого себя!");
				return false;
			}
			
			if (muted.get(target) == null) {
				ipl.sendMessage(ChatColor.RED+"У этого игрока нет мута!");
				return false;
			}
			
			getServer().broadcastMessage(ChatColor.GREEN+ipl.getName()+ChatColor.GOLD+" снял мут с игрока "+ChatColor.RED+target);
			updateMuteStat(muted.get(target));
			muted.remove(target);
			return false;
		}
		
		if (label.equalsIgnoreCase("muteinfo")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED+"Only for ingame players! Use Essentials /mute from console.");
				return false;
			}
			
			Player ipl = (Player) sender;
			
			if (args.length == 0) {
				ipl.sendMessage(ChatColor.GOLD+"Использование: /muteinfo <игрок>");
				return false;
			}
			
			String target = args[0];
			if (muted.get(target) == null) {
				ipl.sendMessage(ChatColor.GOLD+"Игрок "+ChatColor.RED+target+ChatColor.GOLD+" сейчас "+ChatColor.GREEN+"не в муте.");
				sendLastMuteInfo(ipl, target);
				return false;
			} else {
				if (muted.get(target).end <= System.currentTimeMillis()) {
					updateMuteStat(muted.get(target));
					muted.remove(target);
					ipl.sendMessage(ChatColor.GOLD+"Игрок "+ChatColor.RED+target+ChatColor.GOLD+" сейчас "+ChatColor.GREEN+"не в муте.");
					sendLastMuteInfo(ipl, target);
					return false;
				}
			}
			
			MuteInfo info = muted.get(target);
			
			ipl.sendMessage(" ");
			ipl.sendMessage(ChatColor.GOLD+"Игрок "+ChatColor.RED+target+ChatColor.GOLD+" сейчас в муте.");
			ipl.sendMessage(ChatColor.GOLD+"Мут был выдан: "+ChatColor.AQUA+info.issuer);
			ipl.sendMessage(ChatColor.GOLD+"За нарушение: "+ChatColor.RED+info.reason);
			ipl.sendMessage(ChatColor.GOLD+"Длительность: "+getDurationText(info.duration));
			ipl.sendMessage(ChatColor.GOLD+"Осталось: "+DateUtil.formatDateDiff(info.end));
			
			sendLastMuteInfo(ipl, target);
		}
		
		
		return false;
	}
	
	public String getDurationText(String arg) {
		long date = 0;
		try {
			date = DateUtil.parseDateDiff(arg, true);
		} catch (Exception e) {
			e.printStackTrace();
			return "прошлое";
		}
		
		return DateUtil.formatDateDiff(date);
	}
	
	public void updateMuteStat(MuteInfo info) {
		getConfig().set("stats."+info.target+".lastIssuer", info.issuer);
		getConfig().set("stats."+info.target+".lastRule", info.reason);
		getConfig().set("stats."+info.target+".lastStart", info.start);
		saveConfig();
	}
	
	public void giveMute(MuteInfo info, Player pl) {		
		info.state = MuteState.GIVEN;
		try {
			info.end = DateUtil.parseDateDiff(info.duration, true);
			long diff = info.end - System.currentTimeMillis();
			
			getServer().broadcastMessage(ChatColor.GREEN+pl.getName()+ChatColor.GOLD+" выдал мут игроку "+ChatColor.RED+info.target+ChatColor.GOLD+" на "+DateUtil.formatDateDiff(info.end)+" за нарушение: ");
			getServer().broadcastMessage(ChatColor.RED+info.reason);
			
			getConfig().set("stats."+info.target+".previousRuleIndex", info.ruleIndex);
			
			muted.put(info.target, info);
			
			queue.remove(pl.getName());
		} catch (Exception e) {
			e.printStackTrace();
			queue.remove(pl.getName());
			pl.sendMessage(ChatColor.RED+"Ошибка распознавания. Отмена.");
			return;
		}
	}
	
	public boolean checkMute(Player pl) {
		MuteInfo info = muted.get(pl.getName());
		
		long date = info.end;
		
		if (date <= System.currentTimeMillis()) {
			pl.sendMessage(ChatColor.GOLD+"Вы снова можете писать в чат.");
			updateMuteStat(info);
			muted.remove(pl.getName());
			return false;
		}
		
		pl.sendMessage(ChatColor.GOLD+"Вы были заглушены за "+ChatColor.RED+info.reason);
		pl.sendMessage(ChatColor.GOLD+"Вы не сможете писать еще "+DateUtil.formatDateDiff(date));
		return true;
	}
	
	public boolean hadSameRule(String target, int idx) {
		if (!getConfig().contains("stats."+target+".previousRuleIndex")) {
			return false;
		}
		
		if (getConfig().getInt("stats."+target+".previousRuleIndex") == idx) {
			return true;
		}
		
		return false;
	}
	
	@EventHandler
	public void onCommandPreprocess(PlayerCommandPreprocessEvent ev) {
		if (ev.isCancelled()) {
			return;
		}
		
		if (ev.getMessage().startsWith("/tell") || ev.getMessage().startsWith("/w ") || ev.getMessage().startsWith("/m ") || ev.getMessage().startsWith("/msg")) {
			Player pl = ev.getPlayer();
			if (muted.get(pl.getName()) != null) {
				if (checkMute(pl)) ev.setCancelled(true);
				return;
			}
		}
	
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent ev) {
		if (ev.isCancelled()) return;
		
		Player pl = ev.getPlayer();
		
		if (ev.getMessage().startsWith("/")) return;
		
		if (muted.get(pl.getName()) != null) {
			if (checkMute(pl)) ev.setCancelled(true);
			return;
		}
		
		if (queue.get(pl.getName()) != null) {
			
			ev.setCancelled(true);
			MuteInfo info = queue.get(pl.getName());
			
			if (info.state == MuteState.RULE) {
				if (ev.getMessage().length() > 3) {
					pl.sendMessage(ChatColor.RED+"Неверный номер правила. Отмена");
					queue.remove(pl.getName());
					return;
				}
				
				try {
					int n = Integer.parseInt(ev.getMessage());
					
					if (n == 0) {
						pl.sendMessage(ChatColor.GOLD+"Выдача мута отменена.");
						queue.remove(pl.getName());
						return;
					}
					
					if (n < 1 || n > rules.size()) {
						pl.sendMessage(ChatColor.RED+"Неверный номер правила. Отмена");
						queue.remove(pl.getName());
						return;
					}
					
					MuteRule rule = rules.get(n-1);
					
					
					info.reason = rule.rule;
					info.state = MuteState.DURATION;
					info.ruleIndex = n-1;
					queue.put(pl.getName(), info);
					
					
					pl.sendMessage(" ");
					pl.sendMessage(ChatColor.GOLD+"Выберите меру наказания:");
					if (hadSameRule(info.target, info.ruleIndex)) {
						pl.sendMessage(ChatColor.RED+"Игрок рецидивист! Предыдущий мут был за это же нарушение!");
					}
					pl.sendMessage(ChatColor.RED+"[ 1 ] Минимальная: "+ChatColor.GOLD+" мут на "+getDurationText(rule.min));
					pl.sendMessage(ChatColor.RED+"[ 2 ] Максимальная: "+ChatColor.GOLD+" мут на "+getDurationText(rule.max));
					pl.sendMessage(ChatColor.RED+"[ 3 ] "+ChatColor.GOLD+"Мут на 15 минут");
					pl.sendMessage(ChatColor.RED+"[ 4 ] "+ChatColor.GOLD+"Мут на 1 час");
					pl.sendMessage(ChatColor.RED+"[ 5 ] "+ChatColor.GOLD+"Мут на 4 часа");
					pl.sendMessage(ChatColor.RED+"[ 6 ] "+ChatColor.GOLD+"Мут на 12 часов");
					pl.sendMessage(ChatColor.RED+"[ 7 ] "+ChatColor.GOLD+"Мут на 24 часа");
					pl.sendMessage(ChatColor.RED+"[ X ] "+ChatColor.GOLD+"Или введите собственное время");
				} catch (NumberFormatException e) {
					queue.remove(pl.getName());
					pl.sendMessage(ChatColor.RED+"Введеное число не распознано. Отмена.");
					return;
				}
			} else if (info.state == MuteState.DURATION) {
				int n = 0;
				
				if (!ev.getMessage().matches("[0-9]+")) {
					info.duration = ev.getMessage();
					giveMute(info, pl);
				} else {
					try {
						n = Integer.parseInt(ev.getMessage());
						
						if (n == 0) {
							pl.sendMessage(ChatColor.GOLD+"Выдача мута отменена.");
							queue.remove(pl.getName());
							return;
						}
						
						if (n < 1 || n > 6) {
							pl.sendMessage(ChatColor.RED+"Неверный номер. Отмена");
							queue.remove(pl.getName());
							return;
						}
						
						MuteRule rule = rules.get(info.ruleIndex);
						
						if (n == 1) {
							info.duration = rule.min;
						}
						
						if (n == 2) {
							info.duration = rule.max;
						}
						
						if (n == 3) {
							info.duration = "15m";
						}
						
						if (n == 4) {
							info.duration = "1h";
						}
						
						if (n == 5) {
							info.duration = "4h";
						}
						
						if (n == 6) {
							info.duration = "12h";
						}
						
						if (n == 7) {
							info.duration = "24h";
						}
						
						giveMute(info, pl);
					} catch (NumberFormatException e) {
						queue.remove(pl.getName());
						pl.sendMessage(ChatColor.RED+"Введеное число не распознано. Отмена.");
						return;
					}
				}
				
			}
			
			
		}
	}
}
