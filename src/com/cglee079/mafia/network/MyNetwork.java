package com.cglee079.mafia.network;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cglee079.mafia.cmd.ChatCmd;
import com.cglee079.mafia.cmd.Cmd;
import com.cglee079.mafia.cmd.GameoverCmd;
import com.cglee079.mafia.cmd.HiddenChatCmd;
import com.cglee079.mafia.cmd.LoginCmd;
import com.cglee079.mafia.cmd.PlayCmd;
import com.cglee079.mafia.cmd.WaitCmd;
import com.cglee079.mafia.game.Play;
import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.model.NetworksManager;
import com.cglee079.mafia.model.User;
import com.cglee079.mafia.util.C;

public class MyNetwork extends Thread {
	private Thread rcvMsgThd;
	private MySocket mySocket;
	private NetworksManager networksManager;
	private String myName;
	private Play play;

	public MyNetwork(MySocket mySocket, NetworksManager userNetManager, Play play) {
		this.mySocket 			= mySocket;
		this.networksManager	= userNetManager;
		this.play 				= play;
	}

	public void stopRecvmsg() {
		rcvMsgThd = null;
	}

	public void run() {
		try {
			rcvMsgThd = new Thread(new ReceiveMsgThread());
			rcvMsgThd.setDaemon(true);
			rcvMsgThd.start();
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	public void broadcast(String sndCmd, String sndNm, Object sndMsg) {
			JSONObject networks = networksManager.getUserNetworks();
			MyNetwork network = null;
			
			Iterator<String> iter = networks.keys();
			while(iter.hasNext()){
				network = (MyNetwork)networks.get(iter.next());
				network.sendMsg(sndCmd, sndNm, sndMsg);
			}
	}

	public void sndMsg_ToTargets(JSONArray tgUserNms, String sndCmd, String sndNm, Object sndMsg) {
		int size = tgUserNms.length();
		String username 	= null;
		MyNetwork network 	= null;
		for (int i = 0; i < size; i++) {
			username = tgUserNms.getString(i);
			network = networksManager.getUserNetwork(username);
			network.sendMsg(sndCmd, sndNm, sndMsg);
		}
	}

	public void sndMsg_ToTarget(String tgUserNm, String sndCmd, String sndNm, Object sndMsg) {
		MyNetwork network = networksManager.getUserNetwork(tgUserNm);
		network.sendMsg(sndCmd, sndNm, sndMsg);
	}
	
	public void sendMsg(String sndCmd, String sndNm, Object sndMsg) {
		JSONObject data = new JSONObject();
		data.put("cmd", sndCmd);
		data.put("name", sndNm);
		data.put("msg", sndMsg);
		
		try {
			mySocket.writeJSON(data);
			Logger.i("SEND  "+ sndCmd + ">>>>> to. " + myName +"\n");	
		} catch (IOException | ClassNotFoundException e) {
			Logger.i(sndCmd + ": " + "To." + myName + "메시지 송신 에러 발생" + e.getMessage() + "\n");
		} 
	}

	class ReceiveMsgThread implements Runnable {
		@SuppressWarnings("null")
		@Override
		public void run() {
			while (Thread.currentThread() == rcvMsgThd) {
				try {
					JSONObject data = mySocket.readJSON();

					String rcvCmd = data.getString("cmd");
					String rcvNm = data.getString("name");
					String rcvMsg = data.getString("msg");
					
					Logger.i("RECV " + rcvCmd + "  <<<<  By. " + rcvNm + "\n");
					Logger.i("\n");

					rcvMsgExcute(rcvCmd, rcvNm, rcvMsg);

				} catch (IOException | ClassNotFoundException e) {
					try {
						
						Logger.i(e.getMessage() + "\n");
						mySocket.close();
						
						networksManager.removeUserNetwork(myName);
						
						broadcast(Cmd.USERUPDATE, "server", play.getUsersStr());
						
//						Logger.i(networksManager.size() + " : 현재 벡터에 담겨진 사용자 수\n");
						Logger.i("사용자 접속 끊어짐 자원 반납\n");
						
						return;

					} catch (IOException ee) {
						return;
					}
				}
			}
		}

	}// run메소드 끝

	///// *받은 데이터의 , 로직 구현*/
	private void rcvMsgExcute(String rcvCmd, String rcvNm, Object rcvMsg) throws IOException {
		User user;
		String votedUserNm;
		String selectedUserNm;
		
		switch (rcvCmd) {

		//// * 이름 확인 요청 */
		case LoginCmd.REQUESTCONFIRMNAME:
			/* 동일 이름 확인, 접속가능 */
			if (networksManager.checkingName(rcvNm)) {
				Logger.i("동일 이름 확인되지 않음, 접속 가능!\n");
				sendMsg(LoginCmd.CONFIRMNAME, rcvNm, true);
				myName = rcvNm;
				
				play.addUser(myName);
				networksManager.addUserNetwork(myName, MyNetwork.this);
				
				Logger.i("유저 추가, 접속 가능!\n");
				
				sendMsg(LoginCmd.GOWAITROOM, rcvNm, true);
			} else { /* 동일 이름 확인, 접속 불가 */
				sendMsg(LoginCmd.CONFIRMNAME, rcvNm, false);
				Logger.i("동일 이름 확인, 접속 불가!\n");
			}
			break;

		//// * 유저 대기실로 입장 */
		case WaitCmd.IMWAITACTIVITY:
			broadcast(WaitCmd.NOTICE, "", rcvNm + " 님이 입장하셨습니다!");
			break;
			
		case WaitCmd.IMWAIT:
			play.setUserInfo(rcvNm, C.STATE, C.STATE_WAIT);
			broadcast(WaitCmd.NOTICE, "", rcvNm + " 님 WAIT!!");
			broadcast(Cmd.USERUPDATE, "", play.getUsersStr());
			break;

		//// * 유저 레디 */
		case WaitCmd.IMREADY:
			play.setUserInfo(rcvNm, C.STATE, C.STATE_READY);
			broadcast(WaitCmd.NOTICE, "", rcvNm + " 님 READY!!");
			broadcast(Cmd.USERUPDATE, "", play.getUsersStr());

			/* 모든 유저가 레디 상태인지 확인, 게임 상태 변경 */
			if (play.isAllUserReady()) { 
				play.setState(C.GAME_STATE_READY); 
			} else { 
				play.setState(C.GAME_STATE_WAIT);
			}

			/* 게임 상태가 레디이고, 유저 인원 수가 가능한 인원인지 확인 */
			if (( play.getState() == C.GAME_STATE_READY ) && play.isInsizeUserNum()) {

				/* 카운트 다운을 시작함 */
				new Thread() {
					public void run() {
						int count = 5;
						while ((count > 0) && ( play.getState() == C.GAME_STATE_READY ) && play.isInsizeUserNum()) {
							count--;
							broadcast(WaitCmd.NOTICE, "", "Count..." + count);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								return;
							}
						}

						/* 카운트 다운이 정상적으로 끝날경우, 게임을 시작함 */
						if (count == 0) {
							Logger.i("---- GAME START-------\n");
							broadcast(WaitCmd.NOTICE, "", "게임 스타트!!!!");

							/* 유저들에게 직업을 부여함 */
							if (play.updateCharacter()){
								Logger.i("---- Character update-------\n");
							}

							/* 요저 정보 갱신을 요청함 */
							broadcast(Cmd.USERUPDATE, "", play.getUsersStr());

							/* 게임을 시작 하기를 요청함 */
							broadcast(WaitCmd.STARTGAME, "", "");

						}

					}
				}.start();

			}

			break;

		//// * 유저 게임 시작 했음을 알림 */
		case PlayCmd.IMSTARTGAME:
			play.setUserInfo(rcvNm, C.STATE, C.STATE_PLAY);
			
			if (play.isAllUserPlay()){
				play.setState(C.GAME_STATE_PLAY); 
			} else { 
				play.setState(C.GAME_STATE_READY); 
			}

			/* 모든 유저가 게임 을 시작했다면, 직업별 직업 공지 */
			if (play.getState() == C.GAME_STATE_PLAY) {
				broadcast(Cmd.USERUPDATE, "", play.getUsersStr());
				
				sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTICE, "", "게임이 시작 되었습니다");
				sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.IMPOTANTNOTICE, "",
						"마피아 " + play.getNumberOfChracter("MAFIA") + "명, " + "경찰 "
								+ play.getNumberOfChracter("COP") + "명 , " + "의사 "
								+ play.getNumberOfChracter("DOCTOR") + "명, " + "시민 "
								+ play.getNumberOfChracter("CIVIL") + "명 입니다.");

				sndMsg_ToTargets(play.getUserNmsByCharacter(C.CHARACTOR_MAFIA), PlayCmd.IMPOTANTNOTICE, "server", "당신은 마피아 입니다.\n사람을 죽이십시오.");
				sndMsg_ToTargets(play.getUserNmsByCharacter(C.CHARACTOR_COP), PlayCmd.IMPOTANTNOTICE, "server", "당신은 경찰 입니다.\n마피아를 찾아주세요.");
				sndMsg_ToTargets(play.getUserNmsByCharacter(C.CHARACTOR_DOCTOR), PlayCmd.IMPOTANTNOTICE, "server","당신은 의사 입니다.\n마피아로 부터 구해주세요.");
				sndMsg_ToTargets(play.getUserNmsByCharacter(C.CHARACTOR_CIVIL), PlayCmd.IMPOTANTNOTICE, "server", "당신은 시민 입니다.\n");
				sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.GOSUNNY, "server", "");
			}
			break;

		//// * 유저 낮에 있음 */
		case PlayCmd.IMINSUNNY:
			play.setUserInfo(rcvNm, C.WHEN, C.WHEN_SUNNY);
			play.setUserInfo(rcvNm, C.ISWANTNEXT, C.ISWANTNEXT_FALSE);
		
//			for (int i = 0; i < play.size(); i++) {
//				Logger.i("WHEN " + play.getUser(i).getName() + play.getUser(i).getWhen() + "\n");
//			}

			/* 모든 유저가 낮에 있음을 확인 */
			if (play.isAllUserInSunny()) { 
				play.setWhen(C.GAME_WHEN_SUNNY);
				play.setWantnext(C.GAME_ISWANTNEXT_FALSE);
			} else { 
				play.setWhen(C.GAME_WHEN_NULL);
				play.setWhen(C.GAME_ISWANTNEXT_NULL);
			}

			/* 낮 기간 타이머를 시작함 */
			if ((play.getWhen() == C.GAME_WHEN_SUNNY ) && (play.getState() == C.GAME_WHEN_NIGHT)) {
				broadcast(Cmd.USERUPDATE, "", play.getUsersStr());
				
				Logger.i("TIMER START " + rcvNm + "\n");
				
				sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTICE, "server", "아침이 밝았습니다");
				
				new Thread() {
					public void run() {
						Integer timer = 360;
						while ((timer > 0) && (play.getWhen() == C.GAME_WHEN_SUNNY) && (play.getWantnext() == C.GAME_ISWANTNEXT_FALSE)) {
							timer--;
							sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.TIMER, "server", timer + "");

							if (timer <= 5) {
								sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTICE, "server", "투표까지" + timer);
							}
							
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
						}

						if (timer <= 0) {
							/* 모든 사용자에게 투표 시작을 요청함 */
							sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.STARTVOTE, "server", "");

							/* 새로운 투표 시작 */
							play.initVote();
						}
					}
				}.start();
			}
			break;
			
		case PlayCmd.IDONTWANTNEXT:
			play.setUserInfo(rcvNm, C.ISWANTNEXT, C.ISWANTNEXT_FALSE);
			sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTICE, "server", rcvNm + "님이 밤으로 가길 원하지 않습니다.");
			broadcast(Cmd.USERUPDATE, "", play.getUsersStr());
			break;
			
		//// * 유저 다음 턴으로 가기를 원함 */
		case PlayCmd.IWANTNEXT:
			play.setUserInfo(rcvNm, C.ISWANTNEXT, C.ISWANTNEXT_TRUE);
			sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTICE, "server", rcvNm + "님이 밤으로 가길 원합니다.");
			broadcast(Cmd.USERUPDATE, "", play.getUsersStr());
			
			/* 모든 유저가 밤으로 가기를 원할경우 */
			if (play.isAllUserWantNext()){
				play.setWantnext(C.GAME_ISWANTNEXT_TRUE);
			} else { /* 모든 유저가 밤으로 가기를 원하지 않은 경우 */
				play.setWantnext(C.GAME_ISWANTNEXT_FALSE);
			}

			if (play.getWantnext() == C.GAME_ISWANTNEXT_TRUE) {
				sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTICE, "server", "모든 유저가 투표를 원합니다.");

				/* 터치 불가한 상태로 전환 */
				sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTOUCHABLE, "server", "");

				/* 카운트 다운 시작 */
				new Thread() {
					public void run() {
						sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.IMPOTANTNOTICE, "server", "5초후 투표를 시작합니다");
						int count = 5;
						
						while (count > 0) {
							count--;
							sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTICE, "server", "투표까지    " + count);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								return;
							}
						}

						sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.IMPOTANTNOTICE, "server", "투표 중입니다..");

						/* 모든 사용자에게 투표 시작을 요청함 */
						sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.STARTVOTE, "server", "");

						/* 새로운 투표 시작 */
						play.initVote();
					}
				}.start();
			}
			break;

		//// * 유저 투표함. 선택한 사람 전송 */
		case PlayCmd.VOTEUSER:
			votedUserNm = (String) rcvMsg;
			
			/* 투표 업데이트 */
			play.updateVote(rcvNm, votedUserNm);

			/* 모든 유저가 투표를 완료함 */
			if (play.isAllUserVote()) {

				/* 가장 많은 표를 받은 유저(처형된 인원) 확인 */
				String dieUserNm = play.getMaxVotedUserNm();
				play.updateDieUser(dieUserNm);

				/* 유저 정보 갱신, 사망자 공지 */
				broadcast(Cmd.USERUPDATE, "server", play.getUsersStr());
				sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.IMPOTANTNOTICE, "server", dieUserNm + "님이 투표로 처형 되었습니다.");
				sndMsg_ToTarget(dieUserNm, PlayCmd.IMPOTANTNOTICE, "server", "당신은 사망하였습니다.");
				sndMsg_ToTarget(dieUserNm, PlayCmd.YOUAREDIE, "server", "");
				sndMsg_ToTarget(dieUserNm, Cmd.USERUPDATE, "server", play.getUsersStr());

				/* 사망자는 터치 가는 상태로 전환 */
				sndMsg_ToTarget(dieUserNm, PlayCmd.TOUCHABLE, "", "");

				/* 게임 종료 조건 확인 */
				int gameover = play.isGameOver();
				if (!(gameover == C.GAMEOVER_NO)) { //게임 종료
					Logger.i("--------------------게임 종료 -----------------\n");
					
					broadcast(Cmd.USERUPDATE, "server", play.getUsersStr());
					broadcast(PlayCmd.GAMEOVER, "server", gameover);
					
					play.gameOver();
					break;
				} else { // 게임 유지
					
					/* 유저 터치 불가한 상태로 전환 */
					sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTOUCHABLE, "server", "");
	
					/* 밤이 오기 카운트 시작 */
					sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.IMPOTANTNOTICE, "server", "5초후 밤이 찾아옵니다.");
					new Thread() {
						public void run() {
	
							int count = 5;
							while (count > 0) {
								count--;
								sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.NOTICE, "server", "밤까지    " + count);
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									return;
								}
							}
	
							sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.IMPOTANTNOTICE, "server", "밤이왔습니다..");
	
							/* 모든 사용자에게 밤으로 가기를 요청함 */
							sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.GONIGHT, "server", "");
	
							/* 마피아들의 선택을 초기화시킴 */
							play.initMafiaChoice();
						}
					}.start();
				}

			}
			break;

		/// * 유저가 밤에 있음을 알림 */
		case PlayCmd.IMINNIGHT:
			play.setUserInfo(rcvNm, C.WHEN, C.WHEN_NIGHT);
			
			if (play.isAllUserInNight()) {
				broadcast(Cmd.USERUPDATE, "server", play.getUsersStr());
				//##모든유저가 밤이니까 투푤르 시작하라고 보내야.
				Logger.i("--------------모든 유저가 밤에 있습니다----------- \n");
				play.setWhen(C.GAME_WHEN_NIGHT);
			} else {
				play.setWhen(C.GAME_WHEN_SUNNY);
			}

			break;

		case PlayCmd.CHOICEUSERINNIGHT:
			selectedUserNm 	= (String)rcvMsg;
			
			switch (play.getUserInfo(rcvNm, C.CHARACTER)) {
			case C.CHARACTOR_MAFIA:
				play.updateMafiaSelect(rcvNm, selectedUserNm);
				break;
			case C.CHARACTOR_COP:
				play.updateCopSelect(rcvNm, selectedUserNm);
				break;
			case C.CHARACTOR_DOCTOR:
				play.updateDoctorSelect(rcvNm, selectedUserNm);
				break;
			}

			if (play.isAllChracterSelect() && (play.getWhen() == C.GAME_WHEN_SUNNY)) {
				String selectedNmByMafias 	= play.getMaxMafiaChoice();
				String selectedNmByCop 		= "";
				String selectedNmByDoctor 	= "";

				if (play.isAliveCharacter(C.CHARACTOR_COP)) {
					selectedNmByCop = play.getCopChoice();
					if (play.getUserInfo(selectedNmByCop, C.CHARACTER) == C.CHARACTOR_MAFIA){
						sndMsg_ToTargets(play.getUserNmsByCharacter(C.CHARACTOR_COP), PlayCmd.IMPOTANTNOTICE, "server", selectedNmByCop + "님은 마피아 입니다");
					} else{
						sndMsg_ToTargets(play.getUserNmsByCharacter(C.CHARACTOR_COP), PlayCmd.IMPOTANTNOTICE, "server", selectedNmByCop + "님은 마피아가 아닙니다");
					}
				}
				
				if (play.isAliveCharacter(C.CHARACTOR_DOCTOR)){
					selectedNmByDoctor = play.getDoctorChoice();
				}

				Logger.i("---------------각 직업들의 선택 입니다 -------------------\n");
				Logger.i("마피아들은 " + selectedNmByMafias + "  선택하였습니다 " + "\n");
				Logger.i("경찰은" + selectedNmByCop + "  선택하였습니다 " + "\n");
				Logger.i("의사는" + selectedNmByDoctor + "  선택하였습니다 " + "\n");

				if (selectedNmByMafias != null){
					
					/* 마피아가 고른 유저와 , 의사가 고른 유저가 같은 경우, 아무도죽지 않음 */
					if (selectedNmByMafias.equals(selectedNmByDoctor)) {
						Logger.i("의사는 마피아로부터 " + selectedNmByDoctor + "를 구하였습니다" + "\n");
						sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.IMPOTANTNOTICE, "server", "지난 밤 마피아에 의해, " + "아무도 사망하지 않았습니다");
					} else { 	/* 마피아가 고른 유저와 , 의사고 고른 유저가 다른 경우, 마피아가 고른 인원 사망 */
						/* 사망자 갱신, 통보 */
						play.updateDieUser(selectedNmByMafias);
						sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.IMPOTANTNOTICE, "server", "지난 밤 마피아에 의해, " + selectedNmByMafias + " 님이 사망하셨습니다");
						sndMsg_ToTarget(selectedNmByMafias, PlayCmd.IMPOTANTNOTICE, "server", "당신은 사망하였습니다.");
						sndMsg_ToTarget(selectedNmByMafias, PlayCmd.YOUAREDIE, "server", "");
						sndMsg_ToTarget(selectedNmByMafias, Cmd.USERUPDATE, "server", play.getUsersStr());
	
						/* 사망자는 터치 가능 상태로 전환 */
						sndMsg_ToTarget(selectedNmByMafias, PlayCmd.TOUCHABLE, "server", "당신은 사망하였습니다.");
					}
				}

				/* 게임 종료 조건 확인 */
				int gameover = play.isGameOver();
				if (!(gameover == C.GAMEOVER_NO)) {
					Logger.i("--------------------게임 종료 -----------------\n");
					broadcast(PlayCmd.GAMEOVER, "server", gameover);
					play.gameOver();
					break;
				}

				play.setAllUserInfo(C.WHEN, C.CHARACTOR_NULL);
				play.setAllUserInfo(C.ISWANTNEXT, C.ISWANTNEXT_NULL);
				
				play.initSelect(); /* 밤이 끝났으므로 선택을 초기화 시킴 */
				play.setWantnext(C.GAME_ISWANTNEXT_NULL); /* 밤이 다음 턴으로가는 지 여부 초기화 시킴 */
				play.setWhen(C.GAME_WHEN_NULL);

				broadcast(Cmd.USERUPDATE, "server", play.getUsersStr());
				
				/* 밤이 끝나고 모든 유저에게 아침으로 가기를, 요청함 */
				sndMsg_ToTargets(play.getAliveUserNms(), PlayCmd.GOSUNNY, "server", "");
			}

			break;

		case GameoverCmd.REGAME:
			play.addUser(rcvNm);
			broadcast(Cmd.USERUPDATE, "", play.getUsersStr());
			break;

		/* 유저 채팅을 보냄 */
		case ChatCmd.SENDMESSAGE:
			sndMsg_ToTargets(play.getAliveUserNms(), ChatCmd.SENDMESSAGE, rcvNm, rcvMsg);
			break;

		case ChatCmd.SENDEMOTICON:
			sndMsg_ToTargets(play.getAliveUserNms(), ChatCmd.SENDEMOTICON, rcvNm, rcvMsg);
			break;

		case HiddenChatCmd.SENDMESSAGE:
			sndMsg_ToTargets(play.getAliveUserNms(), HiddenChatCmd.SENDMESSAGE, rcvNm, rcvMsg);
			break;

		case HiddenChatCmd.SENDEMOTICON:
			sndMsg_ToTargets(play.getAliveUserNms(), HiddenChatCmd.SENDEMOTICON, rcvNm, rcvMsg);
			break;

		}
	}

}
