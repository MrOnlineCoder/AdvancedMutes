package com.mronlinecoder.advancedmutes;

public class MuteRule {
	public String rule;
	public String min;
	public String max;
	
	public MuteRule(String rule, String min, String max) {
		this.rule = rule;
		this.min = min;
		this.max = max;
	}
	
	/*
	 * 
	 * 
	 * public String getDurationText(String arg) {
		String str = "";
		
		if (arg.length() < 2) return str;
		
		String num = arg.substring(0, arg.length()-1);

		int q = Integer.parseInt(num);
		
		char a = arg.charAt(arg.length()-1);
		
		if (a == 'd') {
			if (q == 1) return "1 день";
			
			if (q < 5) {
				return ""+q+" дня";
			} 
			
			return ""+q+" дней";
		} 
		
		if (a == 'h') {
			if (q == 1) return "1 час";
			
			if (q < 5) {
				return ""+q+" часа";
			} 
			
			if (q == 24 || q == 48) return ""+q+" часа";
			
			return ""+q+" часов.";
		}
		
		if (a == 'm') {
			if (q == 1) return "1 минуту";
			
			if (q < 5) {
				return ""+q+" минуты";
			} 
			
			return ""+q+" минут.";
		}
		
		return str;
	}
	 */
}
