package com.cglee079.mafia.model;

import java.io.Serializable;

import org.json.JSONObject;

import com.cglee079.mafia.util.C;

public class User extends JSONObject implements Serializable {
	public User() {
		this.put(C.CHARACTER, C.CHARACTOR_NULL);
		this.put(C.STATE, C.STATE_NULL);
		this.put(C.WHEN, C.WHEN_NULL);
		this.put(C.ISWANTNEXT, false);
	}

	public User(String name) {
		this();
		this.put("name", name);
	}

	public void remake(User user) {
		String name = user.getString("name");
		int character = user.getInt("character");
		int state = user.getInt("state");
		int when = user.getInt("when");
		boolean wantnext = user.getBoolean("wantnext");
		
		this.put("name", name);
		this.put("character", character);
		this.put("state", state);
		this.put("when", when);
		this.put("wantnext", wantnext);
	}
	
}
