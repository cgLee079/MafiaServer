package com.cglee079.mafia.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.model.User;
import com.cglee079.mafia.network.NetworksManager;
import com.cglee079.mafia.util.C;

public class Play {
	private final static int MINUSER = 1;
	private final static int MAXUSER = 8;

	private JSONObject users;
	
	private int state;
	private int when;
	private int wantnext;

	private HashMap<Integer, Integer[]> chractorOfUserSize; // 참여 인원 숫자별 직업수
	private HashMap<Integer, Integer> 	numOfChractor; // 직업별 인원 배정
	private HashMap<String, String> 	userVote; // 유저 투표
	
	private HashMap<String, String> 	mafiaChoice; // 마피아가 선택한 인원
	private String copChoice 	= "";
	private String doctorChoice = "";

	private NetworksManager userManager;

	public Play(){
		chractorOfUserSize 	= new HashMap<>();
		numOfChractor 		= new HashMap<>();
		userVote			= new HashMap<>();
		mafiaChoice 		= new HashMap<>();

		chractorOfUserSize.put(1, new Integer[] { 1, 0, 0, 0 });
		chractorOfUserSize.put(2, new Integer[] { 1, 1, 0, 0 });
		chractorOfUserSize.put(3, new Integer[] { 1, 1, 0, 1 });
		chractorOfUserSize.put(4, new Integer[] { 1, 1, 1, 1 });
		chractorOfUserSize.put(5, new Integer[] { 2, 1, 1, 1 });
		chractorOfUserSize.put(6, new Integer[] { 2, 1, 1, 2 });
		chractorOfUserSize.put(7, new Integer[] { 2, 1, 1, 3 });
		chractorOfUserSize.put(8, new Integer[] { 3, 1, 1, 3 });
	}
	
	public Play(NetworksManager userManager) {
		this();
		this.userManager = userManager;
	}
	
	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getWhen() {
		return when;
	}

	public void setWhen(int when) {
		this.when = when;
	}

	public int getWantnext() {
		return wantnext;
	}

	public void setWantnext(int wantnext) {
		this.wantnext = wantnext;
	}

	public boolean isInsizeUserNum() {
		int size = users.length();
		if (size >= MINUSER && size <= MAXUSER) { return true; }
		return false;
	}

	public boolean updateCharacter() {
		int length = users.length();
		Integer[] characterDivision = chractorOfUserSize.get(length);

		int numOfMafias = 0;
		int numOfCops 	= 0;
		int numOfDoctors= 0;
		int numOfCivils = 0;

		int maxOfMafias = characterDivision[0];
		int maxOfCops 	= characterDivision[1];
		int maxOfDoctors= characterDivision[2];
		int maxOfCivils = characterDivision[3];

		Iterator<String> iter = users.keys();
		while(iter.hasNext()){
			String username = users.getString(iter.next());
			while (true) {
				int random = (int) (Math.random() * 4);
				if (random == 0 && numOfMafias < maxOfMafias) {
					setUserInfo(username, C.CHARACTER, C.CHARACTOR_MAFIA);
					numOfMafias++;
					break;
				}

				else if (random == 1 && numOfCops < maxOfCops) {
					setUserInfo(username, C.CHARACTER, C.CHARACTOR_COP);
					numOfCops++;
					break;
				}

				else if (random == 2 && numOfDoctors < maxOfDoctors) {
					setUserInfo(username, C.CHARACTER, C.CHARACTOR_DOCTOR);
					numOfDoctors++;
					break;
				}

				else if (random == 3 && numOfCivils < maxOfCivils) {
					setUserInfo(username, C.CHARACTER, C.CHARACTOR_CIVIL);
					numOfCivils++;
					break;
				}
			}
		}
		
		numOfChractor.put(C.CHARACTOR_MAFIA, numOfMafias);
		numOfChractor.put(C.CHARACTOR_COP, numOfCops);
		numOfChractor.put(C.CHARACTOR_DOCTOR, numOfDoctors);
		numOfChractor.put(C.CHARACTOR_CIVIL, numOfCivils);

		return true;

	}

	public void initVote() {
		userVote.clear();
		JSONArray aliveUserNms = this.getAliveUserNms();
		int length = aliveUserNms.length();
		
		for (int i = 0; i < length; i++){
			userVote.put(aliveUserNms.getString(i), "");
		}

		Logger.i("새로운 투표가 시작되었습니다 . 투표가능 (생존) 유저 " + length + "명" + "\n");
	}

	public void initMafiaChoice() {
		mafiaChoice.clear();
		
		JSONArray aliveUserNms = this.getAliveUserNms();
		
		int length = aliveUserNms.length();
		String username = null;
		for (int i = 0; i < length; i++){
			username = aliveUserNms.getString(i);
			if (getUserInfo(username, C.CHARACTER) == C.CHARACTOR_MAFIA){
				mafiaChoice.put(username, "");
			}
		}
	}

	public void updateVote(String name, String choice) {
		userVote.put(name, choice);
	}

	public void updateMafiaSelect(String name, String choice) {
		mafiaChoice.put(name, choice);
	}

	public void updateCopSelect(String name, String choice) {
		copChoice = choice;
	}

	public void updateDoctorSelect(String name, String choice) {
		doctorChoice = choice;
	}

	public boolean isAllUserVote() {
		boolean result 			= true;
		Set<String> set 		= userVote.keySet();
		Iterator<String> iter 	= set.iterator();
		
		Logger.i("--------------투표 중간결과 -----------------------\n");
		while (iter.hasNext()) {
			String username = iter.next();

			if (userVote.get(username).equals("")) {
				Logger.i(username + " 님은   " + "아직 투표를 하지 않았습니다!" + "\n");
				result = false;
			} else{
				Logger.i(username + " 님은   " + userVote.get(username) + " 님께 투표하였습니다!" + "\n");
			}
		}

		return result;
	}

	public boolean isAllChracterSelect() {

		if (numOfChractor.get("COP") != 0){
			if (copChoice.equals("")){return false;}
		}
		
		if (numOfChractor.get("DOCTOR") != 0){
			if (doctorChoice.equals("")){ return false;}
		}
		
		Iterator iter = mafiaChoice.keySet().iterator();
		while (iter.hasNext()) {
			if (mafiaChoice.get(iter.next()).equals(""))
				return false;
		}

		return true;
	}

	public boolean isAliveCharacter(int charactorCop) {
		return numOfChractor.get(charactorCop) != 0;
	}


	public String getMaxVotedUserNm() {
		HashMap<String, Integer> votedUser = new HashMap<>();
		String name	= null;
		String voted= null;
		String maxUser = null;
		int maxCnt = 0;	
		int cnt = 0;
		
		Set<String> set = userVote.keySet();
		Iterator<String> iter = set.iterator();
		
		/* 유저, 뽑혀진 숫자 */
		while (iter.hasNext()) {
			name	= iter.next();
			voted	= userVote.get(name);
			
			if (votedUser.get(voted) != null) {
				cnt = votedUser.get(voted);
				votedUser.put(voted, cnt + 1);
			} else{
				votedUser.put(voted, 1);
			}
		}

		/* 가장 많이 뽑혀진 저를 찾음 */
		Set<String> set2 = votedUser.keySet();
		Iterator<String> iter2 = set2.iterator();
		
		while (iter2.hasNext()) {
			name = iter2.next();
			cnt = votedUser.get(name);
			Logger.i(name + " :   " + cnt + "\n");
			if (cnt > maxCnt) {
				maxCnt 	= cnt;
				maxUser = name;
			}
		}

		return maxUser;
	}

	public String getMaxMafiaChoice() {
		HashMap<String, Integer> choicedUsers = new HashMap<>();
		String name;
		String choiced;
		String maxuser = null;
		int maxint = 0;
		int cnt = 0;
		
		Set<String> set = mafiaChoice.keySet();
		Iterator<String> iter = set.iterator();

		/* 유저, 뽑혀진 숫자 */
		while (iter.hasNext()) {
			name = iter.next();
			choiced = mafiaChoice.get(name);
			if (choicedUsers.get(choiced) != null) {
				cnt = choicedUsers.get(choiced);
				choicedUsers.put(choiced, cnt + 1);
			} else {
				choicedUsers.put(choiced, 1);
			}
		}

		/* 가장 많이 뽑혀진 저를 찾음 */
		Set<String> set2 = choicedUsers.keySet();
		Iterator<String> iter2 = set2.iterator();
		
		while (iter2.hasNext()) {
			name = iter2.next();
			cnt = choicedUsers.get(name);
			Logger.i(name + cnt + "\n");
			if (cnt > maxint) {
				maxint = cnt;
				maxuser = name;
			}
		}

		return maxuser;
	}

	public String getCopChoice() {
		return copChoice;
	}

	public String getDoctorChoice() {
		return doctorChoice;
	}

	public int getNumberOfChracter(int character) {
		return numOfChractor.get(character);
	}

	public void updateDieUser(String dieUserNm) {
		/* 가장 많이 투표된 유저는 사망 */
		setUserInfo(dieUserNm, C.STATE, C.STATE_DIE);

		/* 직업별 유저 수 갱신 */
		int dieuserCharacter = getUserInfo(dieUserNm, C.CHARACTER);
		int num = numOfChractor.get(dieuserCharacter);
		numOfChractor.put(dieuserCharacter, num - 1);
	}

	public int isGameOver() {
		int numberOfMafia = numOfChractor.get(C.CHARACTOR_MAFIA);
		int numberOfCop = numOfChractor.get(C.CHARACTOR_COP);
		int numberOfDoctor = numOfChractor.get(C.CHARACTOR_DOCTOR);
		int numberOfCivil = numOfChractor.get(C.CHARACTOR_CIVIL);
		
		Logger.i("---------------------중간 결과 -------------------\n");
		Logger.i("마피아  " + numberOfMafia + "명 ," + "경찰 " + numberOfCop + "명, " + "의사 " + numberOfDoctor + "명 , " + "시민 " + numberOfCivil + "명 생존!!!\n");

		if (numberOfMafia == 0){
			return C.GAMEOVER_MAFIA_LOSE;
		}

		if (numberOfMafia >= (numberOfCop + numberOfDoctor + numberOfCivil)){
			return C.GAMEOVER_MAFIA_WIN;
		}

		return C.GAMEOVER_NO;
	}

	public void gameOver() {
		state 		= C.GAME_STATE_NULL;
		when 		= C.GAME_WHEN_NULL;
		wantnext 	= C.GAME_ISWANTNEXT_NULL;
		numOfChractor.clear(); // 직업별 인원 배정
		users = new JSONObject();
	}

	public void initSelect() {
		copChoice 		= "";
		doctorChoice 	= "";
		mafiaChoice.clear();
	}
	
	public boolean isAllUserReady() {
		Boolean result = true;

		Iterator<String> iter = users.keySet().iterator();
		String username = null;
		while(iter.hasNext()){
			username = iter.next();
			if ( getUserInfo(username, C.STATE) == C.STATE_READY ){
				result = false;
				break;
			}
		}
		
		return result;
	}

	public boolean isAllUserPlay() {
		Boolean result = true;
		
		Iterator<String> iter = users.keySet().iterator();
		String username = null;
		while(iter.hasNext()){
			username = iter.next();
			if (getUserInfo(username, C.STATE) == C.STATE_PLAY){
				result = false;
				break;
			}
		}
		
		return result;
	}

	public boolean isAllUserWantNext() {
		Boolean result = true;
		
		Iterator<String> iter = users.keySet().iterator();
		String username = null;
		while(iter.hasNext()){
			username = iter.next();
			if ((getUserInfo(username, C.ISWANTNEXT) == C.ISWANTNEXT_FALSE)
					&& (getUserInfo(username, C.STATE) == C.STATE_PLAY)){
				result = false;
				break;
			}
		}
		return result;
	}

	public boolean isAllUserInSunny() {
		Boolean result = true;
		
		Iterator<String> iter = users.keySet().iterator();
		String username = null;
		while(iter.hasNext()){
			username = iter.next();
			if ((getUserInfo(username, C.WHEN) == C.WHEN_SUNNY) && (getUserInfo(username, C.STATE) == C.STATE_PLAY)){
				result = false;
				break;
			}
		}
		return result;
	}

	public boolean isAllUserInNight() {
		Boolean result = true;
		
		Iterator<String> iter = users.keySet().iterator();
		String username = null;
		while(iter.hasNext()){
			username = iter.next();
			if ((getUserInfo(username, C.WHEN) == C.WHEN_NIGHT) && (getUserInfo(username, C.STATE) == C.STATE_PLAY)){
				result = false;
				break;
			}
		}
		return result;
	}
	
	public JSONArray getAliveUserNms(){
		JSONArray usernms = new JSONArray();
		Iterator<String> iter = users.keys();
		
		String username = null;
		while(iter.hasNext()){
			username = iter.next();
			if(this.getUserInfo(username, C.STATE) == C.STATE_PLAY){
				usernms.put(username);
			}
		}
		
		return usernms;
	}
	
	public JSONArray getUserNmsByCharacter(int charactorId) {
		JSONArray usernms = new JSONArray();
		
		Iterator<String> iter = users.keySet().iterator();
		String username = null;
		while(iter.hasNext()){
			username = iter.next();
			if ((getUserInfo(username, C.CHARACTER) == charactorId) 
					&& (getUserInfo(username, C.CHARACTER) == C.STATE_PLAY)){
				usernms.put(username);
			}
		}
		
		return usernms;
	}

	public void addUser(String userName) {
		User user = new User(userName);
		users.put(userName, user);
	}
	
	public void removeUser(String username){
		users.remove(username);
	}

	public void setUserInfo(String username, String key, Object value){
		User user = (User)users.get(username);
		user.put(key, value);
	}
	
	public int getUserInfo(String username, String key){
		User user = (User)users.get(username);
		return (int)user.get(key);
	}
	
	public void setAllUserInfo(String key, int value) {
		JSONArray aliveUsers = new JSONArray();
		Iterator<String> iter = users.keys();
		
		String username = null;
		while(iter.hasNext()){
			username = iter.next();
			setUserInfo(username, key, value);
		}
	}
	
	public String getUsersStr(){
		return users.toString();
	}

}
