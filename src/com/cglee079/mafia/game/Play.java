package com.cglee079.mafia.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.model.UserInfo;
import com.cglee079.mafia.model.UserManager;

public class Play {
	private final static int MINUSER = 1;
	private final static int MAXUSER = 8;

	private String state 	= "";
	private String when 	= "";
	private boolean wantnext = false;

	private HashMap<Integer, Integer[]> chractorOfUserSize; // 참여 인원 숫자별 직업수
	private HashMap<String, Integer> 	numberOfChractor; // 직업별 인원 배정
	private HashMap<String, String> 	userVote; // 유저 투표
	private HashMap<String, String> 	mafiaChoice; // 마피아가 선택한 인원
	
	private String copChoice 	= "";
	private String doctorChoice = "";

	private UserManager userManager;

	public Play(UserManager userManager) {
		this.userManager = userManager;

		chractorOfUserSize = new HashMap<>();
		numberOfChractor = new HashMap<>();
		userVote = new HashMap<>();
		mafiaChoice = new HashMap<>();

		chractorOfUserSize.put(1, new Integer[] { 1, 0, 0, 0 });
		chractorOfUserSize.put(2, new Integer[] { 1, 1, 0, 0 });
		chractorOfUserSize.put(3, new Integer[] { 1, 1, 0, 1 });
		chractorOfUserSize.put(4, new Integer[] { 1, 1, 1, 1 });
		chractorOfUserSize.put(5, new Integer[] { 2, 1, 1, 1 });
		chractorOfUserSize.put(6, new Integer[] { 2, 1, 1, 2 });
		chractorOfUserSize.put(7, new Integer[] { 2, 1, 1, 3 });
		chractorOfUserSize.put(8, new Integer[] { 3, 1, 1, 3 });
	}

	public boolean isInsizeUserNumber() {
		int size = userManager.size();
		if (size >= MINUSER && size <= MAXUSER) { return true; }
		return false;
	}

	public boolean updateCharacter() {
		Integer numberOfUsers = userManager.size();
		Integer[] characterDivision = chractorOfUserSize.get(numberOfUsers);

		int numberOfMafias 	= 0;
		int numberOfCops 	= 0;
		int numberOfDoctors = 0;
		int numberOfCivils 	= 0;

		int maxOfMafias = characterDivision[0];
		int maxOfCops 	= characterDivision[1];
		int maxOfDoctors= characterDivision[2];
		int maxOfCivils = characterDivision[3];

		for (int i = 0; i < numberOfUsers; i++) {
			UserInfo user = userManager.getUser(i);

			/* 시연용 인원배정 */
			if (numberOfUsers == 4) {
				if (user.getName().equals("left") || user.getName().equals("찬구")) {
					user.setCharacter("MAFIA");
					numberOfMafias++;
				}

				else if (user.getName().equals("center")) {
					user.setCharacter("COP");
					numberOfCops++;
				}

				else if (user.getName().equals("right")) {
					user.setCharacter("DOCTOR");
					numberOfDoctors++;
				}

				else {
					user.setCharacter("CIVIL");
					numberOfCivils++;
				}
			}

			/* 실제 게임 인원 배정 */
			if (numberOfUsers != 4) {
				while (true) {
					int random = (int) (Math.random() * 4);
					if (random == 0 && numberOfMafias < maxOfMafias) {
						user.setCharacter("MAFIA");
						numberOfMafias++;
						break;
					}

					else if (random == 1 && numberOfCops < maxOfCops) {
						user.setCharacter("COP");
						numberOfCops++;
						break;
					}

					else if (random == 2 && numberOfDoctors < maxOfDoctors) {
						user.setCharacter("DOCTOR");
						numberOfDoctors++;
						break;
					}

					else if (random == 3 && numberOfCivils < maxOfCivils) {
						user.setCharacter("CIVIL");
						numberOfCivils++;
						break;
					}
				}
			}
		}
		
		numberOfChractor.put("MAFIA", numberOfMafias);
		numberOfChractor.put("COP", numberOfCops);
		numberOfChractor.put("DOCTOR", numberOfDoctors);
		numberOfChractor.put("CIVIL", numberOfCivils);

		return true;

	}

	public void newVote() {
		userVote.clear();
		String[] aliveUsername = userManager.getAliveUserNames();
		int length = aliveUsername.length;
		for (int i = 0; i < length; i++){
			userVote.put(aliveUsername[i], "");
		}

		Logger.append("새로운 투표가 시작되었습니다 . 투표가능 (생존) 유저 " + length + "명" + "\n");
	}

	public void newMafiaChoice() {
		mafiaChoice.clear();
		
		String[] aliveUsername = userManager.getAliveUserNames();
		int length = aliveUsername.length;
		for (int i = 0; i < length; i++){
			if (userManager.getUser(aliveUsername[i]).getCharacter().equals("MAFIA")){
				mafiaChoice.put(aliveUsername[i], "");
			}
		}
	}

	public void updateVote(String name, String choice) {
		userVote.put(name, choice);
	}

	public void updateMafiaChoice(String name, String choice) {
		mafiaChoice.put(name, choice);
	}

	public void updateCopChoice(String name, String choice) {
		copChoice = choice;
	}

	public void updateDoctorChoice(String name, String choice) {
		doctorChoice = choice;
	}

	public boolean isAllUserVote() {
		boolean result 			= true;
		Set<String> set 		= userVote.keySet();
		Iterator<String> iter 	= set.iterator();
		
		Logger.append("--------------투표 중간결과 -----------------------\n");
		while (iter.hasNext()) {
			String name = iter.next();

			if (userVote.get(name).equals("")) {
				Logger.append(name + " 님은   " + "아직 투표를 하지 않았습니다!" + "\n");
				result = false;
			} else
				Logger.append(name + " 님은   " + userVote.get(name) + " 님께 투표하였습니다!" + "\n");
		}

		return result;
	}

	public boolean isAllChracterChoice() {

		if (numberOfChractor.get("COP") != 0){
			if (copChoice.equals("")){return false;}
		}
		
		if (numberOfChractor.get("DOCTOR") != 0){
			if (doctorChoice.equals("")){ return false;}
		}
		
		Iterator iter = mafiaChoice.keySet().iterator();
		while (iter.hasNext()) {
			if (mafiaChoice.get(iter.next()).equals(""))
				return false;
		}

		return true;
	}

	public boolean isAliveCop() {
		return numberOfChractor.get("COP") != 0;
	}

	public boolean isAliveDoctor() {
		return numberOfChractor.get("DOCTOR") != 0;
	}

	public String getDiedUserByVote() {
		HashMap<String, Integer> votedUser = new HashMap<>();

		Set<String> set = userVote.keySet();
		Iterator<String> iter = set.iterator();

		/* 유저, 뽑혀진 숫자 */
		while (iter.hasNext()) {
			String name	= iter.next();
			String value= userVote.get(name);
			if (votedUser.get(value) != null) {
				int choice = votedUser.get(value);
				votedUser.put(value, choice + 1);
			} else{
				votedUser.put(value, 1);
			}
		}

		/* 가장 많이 뽑혀진 저를 찾음 */
		Set<String> set2 = votedUser.keySet();
		Iterator<String> iter2 = set2.iterator();
		int maxint = 0;
		String maxuser = null;
		while (iter2.hasNext()) {
			String name = iter2.next();
			Integer choice = votedUser.get(name);
			Logger.append(name + choice + "\n");
			if (choice > maxint) {
				maxint 	= choice;
				maxuser = name;
			}
		}

		return maxuser;
	}

	public String getMaxMafiaChoice() {
		HashMap<String, Integer> choicedUser = new HashMap<>();

		Set<String> set = mafiaChoice.keySet();
		Iterator<String> iter = set.iterator();

		/* 유저, 뽑혀진 숫자 */
		while (iter.hasNext()) {
			String name = iter.next();
			String value = mafiaChoice.get(name);
			if (choicedUser.get(value) != null) {
				int choice = choicedUser.get(value);
				choicedUser.put(value, choice + 1);
			} else
				choicedUser.put(value, 1);
		}

		/* 가장 많이 뽑혀진 저를 찾음 */
		Set<String> set2 = choicedUser.keySet();
		Iterator<String> iter2 = set2.iterator();
		int maxint = 0;
		String maxuser = null;
		while (iter2.hasNext()) {
			String name = iter2.next();
			Integer choice = choicedUser.get(name);
			Logger.append(name + choice + "\n");
			if (choice > maxint) {
				maxint = choice;
				maxuser = name;
			}
		}

		return maxuser;
	}

	public int getNumberOfChracter(String character) {
		return numberOfChractor.get(character);
	}

	public void setDied(String dieUser) {
		/* 가장 많이 투표된 유저는 사망 */
		userManager.getUser(dieUser).setState("die");

		/* 직업별 유저 수 갱신 */
		String dieuserCharacter = userManager.getUser(dieUser).getCharacter();
		int num = numberOfChractor.get(dieuserCharacter);
		numberOfChractor.put(dieuserCharacter, num - 1);
	}

	public String isGameOver() {
		int numberOfMafia = numberOfChractor.get("MAFIA");
		int numberOfCop = numberOfChractor.get("COP");
		int numberOfDoctor = numberOfChractor.get("DOCTOR");
		int numberOfCivil = numberOfChractor.get("CIVIL");
		
		Logger.append("---------------------중간 결과 -------------------\n");
		Logger.append("마피아  " + numberOfMafia + "명 ," + "경찰 " + numberOfCop + "명, " + "의사 " + numberOfDoctor + "명 , " + "시민 " + numberOfCivil + "명 생존!!!\n");

		if (numberOfMafia == 0){
			return "MAFIALOSE";
		}

		if (numberOfMafia >= (numberOfCop + numberOfDoctor + numberOfCivil)){
			return "MAFIAWIN";
		}

		return "NOGAMEOVER";
	}

	public void gameOver() {
		state 		= "";
		when 		= "";
		wantnext 	= false;
		numberOfChractor.clear(); // 직업별 인원 배정
		userManager.getUsers().clear();
	}

	public void endNight() {
		copChoice 		= "";
		doctorChoice 	= "";
		mafiaChoice.clear();
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

	public boolean isWantnext() {
		return wantnext;
	}

	public void setWantnext(boolean wantnext) {
		this.wantnext = wantnext;
	}

	public String getCopChoice() {
		return copChoice;
	}

	public String getDoctorChoice() {
		return doctorChoice;
	}

}
