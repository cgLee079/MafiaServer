package com.cglee079.mafia.network;

import org.json.JSONObject;

public class NetworksManager {
	private JSONObject userNetworks;

	public NetworksManager() {
		userNetworks = new JSONObject();
	}

	public JSONObject getUserNetworks() {
		return userNetworks;
	}

	public MyNetwork getUserNetwork(String username) {
		return (MyNetwork) userNetworks.get(username);
	}

	public void addUserNetwork(String username, MyNetwork network) {
		userNetworks.put(username, network);
	}

	public void removeUserNetwork(String userName) {
		userNetworks.remove(userName);
	}

	public boolean checkingName(String username) {
		return userNetworks.isNull(username);
	}
}
