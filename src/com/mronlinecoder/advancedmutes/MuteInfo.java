package com.mronlinecoder.advancedmutes;

import java.util.Date;

public class MuteInfo {
	
	public enum MuteState {
		RULE, DURATION, GIVEN
	}
	
	public String issuer;
	public String target;
	public String duration;
	public String reason;
	
	int ruleIndex;
	
	public MuteState state;
	
	public long start;
	public long end;
}
