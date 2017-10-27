package com.cglee079.mafia.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.network.MyNetwork;

public class UserManager {
	Vector<UserInfo> userinfos = new Vector<>();
	HashMap<String, MyNetwork> userNetworks = new HashMap<>();

	public Vector<UserInfo> getUsers() {
		return userinfos;
	}

	public void setUsers(Vector<UserInfo> users) {
		this.userinfos = users;
	}

	public void addUser(String userName) {
		UserInfo userInfo = new UserInfo(userName);
		userInfo.setState("wait");
		userinfos.add(userInfo);
	}

	public UserInfo getUser(int i) {
		return userinfos.get(i);
	}

	public UserInfo getUser(String username) {
		for (int i = 0; i < userinfos.size(); i++) {
			if (username.equals(userinfos.get(i).getName()))
				return userinfos.get(i);
		}
		return null;
	}

	public MyNetwork getUserNetwork(String username) {
		return userNetworks.get(username);
	}

	public void addUserNetwork(String username, MyNetwork network) {
		userNetworks.put(username, network);
	}

	public void removeUser(String userName) {
		userinfos.remove(getUser(userName));
		userNetworks.remove(userName);
	}

	public int size() {
		return userinfos.size();
	}

	public boolean checkingName(String userName) {
		for (int i = 0; i < userinfos.size(); i++) {
			UserInfo u = userinfos.get(i);
			if (userName.equals(u.getName()) == true)
				return false;
		}
		return true;
	}

	public boolean isAllUserReady() {
		Boolean result = true;
		for (int i = 0; i < userinfos.size(); i++) {
			UserInfo user = userinfos.get(i);
			if (user.getState().equals("ready") == false)
				result = false;
		}
		return result;
	}

	public boolean isAllUserPlay() {
		Boolean result = true;
		for (int i = 0; i < userinfos.size(); i++) {
			UserInfo user = userinfos.get(i);
			if (user.getState().equals("play") == false)
				result = false;
		}
		return result;
	}

	public boolean isAllUserWantNext() {
		Boolean result = true;
		for (int i = 0; i < userinfos.size(); i++) {
			UserInfo user = userinfos.get(i);
			if (user.isWantnext() == false && user.getState().equals("play"))
				result = false;
		}
		return result;
	}

	public boolean isAllUserInSunny() {
		Boolean result = true;
		for (int i = 0; i < userinfos.size(); i++) {
			UserInfo user = userinfos.get(i);
			if (user.getWhen().equals("sunny") == false && user.getState().equals("play"))
				result = false;
		}
		return result;
	}

	public boolean isAllUserInNight() {
		Boolean result = true;
		for (int i = 0; i < userinfos.size(); i++) {
			UserInfo user = userinfos.get(i);

			if (user.getWhen().equals("night") == false && user.getState().equals("play")) {
				Logger.append(user.getName() + "님은 WHEN :  " + user.getWhen() + "상태는 : " + user.getState() + "\n");
				result = false;
			}
		}
		return result;
	}

	public String[] getAliveUserNames() {
		ArrayList<String> usernames = new ArrayList<>();
		int size = userinfos.size();
		for (int i = 0; i < size; i++) {
			if (userinfos.get(i).getState().equals("play"))
				usernames.add(userinfos.get(i).getName());
		}
		String[] names = usernames.toArray((new String[usernames.size()]));
		return names;
	}

	public ArrayList<String> getMafias() {
		ArrayList<String> mafias = new ArrayList<>();
		for (int i = 0; i < this.size(); i++) {
			UserInfo userinfo = userinfos.get(i);
			if (userinfo.getCharacter().equals("MAFIA") && userinfo.getState().equals("play"))
				mafias.add(userinfo.getName());
		}
		return mafias;
	}

	public ArrayList<String> getCops() {
		ArrayList<String> cops = new ArrayList<>();
		for (int i = 0; i < this.size(); i++) {
			UserInfo userinfo = userinfos.get(i);
			if (userinfo.getCharacter().equals("COP") && userinfo.getState().equals("play"))
				cops.add(userinfo.getName());
		}
		return cops;
	}

	public ArrayList<String> getDoctors() {
		ArrayList<String> doctors = new ArrayList<>();
		for (int i = 0; i < this.size(); i++) {
			UserInfo userinfo = userinfos.get(i);
			if (userinfo.getCharacter().equals("DOCTOR") && userinfo.getState().equals("play"))
				doctors.add(userinfo.getName());
		}
		return doctors;
	}

	public ArrayList<String> getCivils() {
		ArrayList<String> civils = new ArrayList<>();
		for (int i = 0; i < this.size(); i++) {
			UserInfo userinfo = userinfos.get(i);
			if (userinfo.getCharacter().equals("CIVIL") && userinfo.getState().equals("play"))
				civils.add(userinfo.getName());
		}
		return civils;
	}

	public ArrayList<String> getAlive() {
		ArrayList<String> alives = new ArrayList<>();
		for (int i = 0; i < this.size(); i++) {
			UserInfo userinfo = userinfos.get(i);
			if (userinfo.getState().equals("play"))
				alives.add(userinfo.getName());
		}
		return alives;
	}
}
