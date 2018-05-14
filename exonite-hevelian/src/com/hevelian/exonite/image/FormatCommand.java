package com.hevelian.exonite.image;

public class FormatCommand implements Comparable<FormatCommand> {

	public String CommandName			= null;
	public String[] CommandParams		= null;
	
	public FormatCommand(String name, String[] params) {
		CommandName			= name;
		CommandParams		= params;
	}

	@Override
	public int compareTo(FormatCommand to) {
		String s_from = this.CommandName;
		String s_to   = to.CommandName;
		
		return s_from.compareTo(s_to);
	}
}
