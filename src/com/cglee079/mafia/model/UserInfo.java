package com.cglee079.mafia.model;

import java.io.Serializable;

public class UserInfo implements Serializable {
	static final long serialVersionUID = 6880704790547550457L;
	private String name 		= "";
	private String character 	= "";
	private String state 		= "";
	private String when 		= "";
	private boolean wantnext 	= false;

	public UserInfo() {
	}

	public UserInfo(String name) {
		this.name = name;
	}

	public void change(UserInfo u) {
		name = u.getName();
		character = u.getCharacter();
		state = u.getState();
		when = u.getWhen();
		wantnext = u.isWantnext();
	}

	@Override
	public String toString() {
		return "UserInfo [name=" + name + ", character=" + character + ", state=" + state + ", when=" + when
				+ ", wantnext=" + wantnext + "]";
	}

	public boolean isWantnext() {
		return wantnext;
	}

	public void setWantnext(boolean wantnext) {
		this.wantnext = wantnext;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public String getCharacter() {
		return character;
	}

	public void setCharacter(String character) {
		this.character = character;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
