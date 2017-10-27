package com.cglee079.mafia.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import com.cglee079.mafia.cmd.ChatCmd;
import com.cglee079.mafia.cmd.Cmd;
import com.cglee079.mafia.cmd.GameoverCmd;
import com.cglee079.mafia.cmd.HiddenChatCmd;
import com.cglee079.mafia.cmd.LoginCmd;
import com.cglee079.mafia.cmd.PlayCmd;
import com.cglee079.mafia.cmd.WaitCmd;
import com.cglee079.mafia.game.Play;
import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.model.SocketData;
import com.cglee079.mafia.model.UserInfo;
import com.cglee079.mafia.model.UserManager;

public class MyNetwork extends Thread {
	private Thread rcvMsgThd;
	private MySocket mySocket;
	private UserManager userManager;
	private String myName;
	private Play play;

	public MyNetwork(MySocket mySocket, UserManager users, Play gameLogic) {
		this.mySocket = mySocket;
		this.userManager = users;
		this.play = gameLogic;
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

	public void broadcast(String sndCmd, String sndNm, Object sndObj) {
		for (int i = 0; i < userManager.size(); i++) {
			UserInfo userinfo = userManager.getUser(i);
			MyNetwork network = userManager.getUserNetwork(userinfo.getName());
			network.sendMsg(sndCmd, sndNm, sndObj);
		}
	}

	public void sndMsg_ToTargets(ArrayList<String> targets, String sndCmd, String sndNm, Object sndObj) {
		String 		userNm 	= null;
		MyNetwork 	network = null;
		int size = targets.size();
		
		for (int i = 0; i < size; i++) {
			userNm 	= targets.get(i);
			network = userManager.getUserNetwork(userNm);
			network.sendMsg(sndCmd, sndNm, sndObj);
		}
	}

	public void sndMsg_ToTarget(String target, String sndCmd, String sndNm, Object sndObj) {
		MyNetwork network = userManager.getUserNetwork(target);
		network.sendMsg(sndCmd, sndNm, sndObj);
		
//		for (int i = 0; i < userManager.size(); i++) {
//			UserInfo userinfo = userManager.getUser(i);
//			if (userinfo.getName().equals(target)) {
//				MyNetwork network = userManager.getUserNetwork(target);
//				network.sendMsg(sndCmd, sndNm, sndObj);
//			}			
//		}
	}
	
	public void sendMsg(String sndCmd, String sndNm, Object sndObj) {
		try {
			mySocket.writeObject(new SocketData(sndCmd, sndNm, sndObj));
			Logger.append("SEND  "+sndCmd + ">>>>> to. " +myName +"\n");	
		} catch (IOException | ClassNotFoundException e) {
			Logger.append(sndCmd + ": " + "To." + myName.toString() + "메시지 송신 에러 발생" + e.getMessage() + "\n");
		} 
	}

	class ReceiveMsgThread implements Runnable {
		@SuppressWarnings("null")
		@Override
		public void run() {
			while (Thread.currentThread() == rcvMsgThd) {
				try {

					SocketData socketData = (SocketData) mySocket.readObject();

					String rcvCmd = socketData.getCommand();
					String rcvNm = socketData.getName();
					Object rcvObj = socketData.getObject();
					
					Logger.append("RECV " + rcvCmd + "  <<<<  By. " + rcvNm + "\n");
					Logger.append("\n");

					rcvMsgExcute(rcvCmd, rcvNm, rcvObj);

				} catch (IOException | ClassNotFoundException e) {
					try {
						
						Logger.append(e.getMessage() + "\n");
						mySocket.close();
						userManager.removeUser(myName);
						
						broadcast(Cmd.USERUPDATE, "server", userManager.getUsers());
						
						Logger.append(userManager.size() + " : 현재 벡터에 담겨진 사용자 수\n");
						Logger.append("사용자 접속 끊어짐 자원 반납\n");
						
						return;

					} catch (IOException ee) {
						return;
					}
				}
			}
		}

	}// run메소드 끝

	///// *받은 데이터의 , 로직 구현*/
	private void rcvMsgExcute(String rcvCmd, String rcvNm, Object rcvObj) throws IOException {
		UserInfo userinfo;

		switch (rcvCmd) {

		//// * 이름 확인 요청 */
		case LoginCmd.REQUESTCONFIRMNAME:

			/* 동일 이름 확인, 접속가능 */
			if (userManager.checkingName(rcvNm) == true) {
				Logger.append("동일 이름 확인되지 않음, 접속 가능!\n");
				sendMsg(LoginCmd.CONFIRMNAME, rcvNm, true);
				myName = rcvNm;
				userManager.addUser(rcvNm);
				userManager.addUserNetwork(rcvNm, MyNetwork.this);
				Logger.append("유저 추가, 접속 가능!\n");
				sendMsg(LoginCmd.GOWAITROOM, rcvNm, true);

			}

			/* 동일 이름 확인, 접속 불가 */
			else {
				sendMsg(LoginCmd.CONFIRMNAME, rcvNm, false);
				Logger.append("동일 이름 확인, 접속 불가!\n");
			}
			break;

		//// * 유저 대기실로 입장 */
		case WaitCmd.IMWAITACTIVITY:
			broadcast(WaitCmd.NOTICE, "", rcvNm + " 님이 입장하셨습니다!");
			broadcast(Cmd.USERUPDATE, "", userManager.getUsers());
			break;

		//// * 유저 레디 */
		case WaitCmd.IMREADY:
			userinfo = userManager.getUser(rcvNm);
			userinfo.setState((String) rcvObj);
			broadcast(Cmd.USERUPDATE, "", userManager.getUsers());

			if (((String) rcvObj).equals("ready"))
				broadcast(WaitCmd.NOTICE, "", rcvNm + " 님 READY!!");
			else if (((String) rcvObj).equals("wait"))
				broadcast(WaitCmd.NOTICE, "", rcvNm + " 님 WAIT!!");

			/* 모든 유저가 레디 상태인지 확인, 게임 상태 변경 */
			if (userManager.isAllUserReady())
				play.setState("ready");
			else
				play.setState("wait");

			/* 게임 상태가 레디이고, 유저 인원 수가 가능한 인원인지 확인 */
			if (play.getState().equals("ready") && play.isInsizeUserNumber()) {

				/* 카운트 다운을 시작함 */
				new Thread() {
					public void run() {
						int count = 5;
						while (count > 0 && play.getState().equals("ready") 
								&& play.isInsizeUserNumber() == true) {
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
							Logger.append("---- GAME START-------\n");
							broadcast(WaitCmd.NOTICE, "", "게임 스타트!!!!");

							/* 유저들에게 직업을 부여함 */
							if (play.updateCharacter())
								Logger.append("---- Character update-------\n");

							/* 요저 정보 갱신을 요청함 */
							broadcast(Cmd.USERUPDATE, "", userManager.getUsers());

							/* 게임을 시작 하기를 요청함 */
							broadcast(WaitCmd.STARTGAME, "", "");

						}

					}
				}.start();

			}

			break;

		//// * 유저 게임 시작 했음을 알림 */
		case PlayCmd.IMSTARTGAME:
			userinfo = userManager.getUser(rcvNm);
			userinfo.setState((String) rcvObj);

			if (userManager.isAllUserPlay())
				play.setState("play");
			else
				play.setState("ready");

			/* 모든 유저가 게임 을 시작했다면, 직업별 직업 공지 */
			if (play.getState().equals("play")) {
				broadcast(Cmd.USERUPDATE, "", userManager.getUsers());
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTICE, "", "게임이 시작 되었습니다");
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.IMPOTANTNOTICE, "",
						"마피아 " + play.getNumberOfChracter("MAFIA") + "명, " + "경찰 "
								+ play.getNumberOfChracter("COP") + "명 , " + "의사 "
								+ play.getNumberOfChracter("DOCTOR") + "명, " + "시민 "
								+ play.getNumberOfChracter("CIVIL") + "명 입니다.");

				sndMsg_ToTargets(userManager.getMafias(), PlayCmd.IMPOTANTNOTICE, "server", "당신은 마피아 입니다.\n사람을 죽이십시오.");
				sndMsg_ToTargets(userManager.getCops(), PlayCmd.IMPOTANTNOTICE, "server", "당신은 경찰 입니다.\n마피아를 찾아주세요.");
				sndMsg_ToTargets(userManager.getDoctors(), PlayCmd.IMPOTANTNOTICE, "server","당신은 의사 입니다.\n마피아로 부터 구해주세요.");
				sndMsg_ToTargets(userManager.getCivils(), PlayCmd.IMPOTANTNOTICE, "server", "당신은 시민 입니다.\n");
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.GOSUNNY, "server", "");
			}
			break;

		//// * 유저 낮에 있음 */
		case PlayCmd.IMINSUNNY:

			userinfo = userManager.getUser(rcvNm);
			userinfo.setWhen((String) rcvObj);
			userinfo.setWantnext(false);
		
			for (int i = 0; i < userManager.size(); i++) {
				Logger.append("WHEN " + userManager.getUser(i).getName() + userManager.getUser(i).getWhen() + "\n");
			}

			/* 모든 유저가 낮에 있음을 확인 */
			if (userManager.isAllUserInSunny()) { play.setWhen("sunny"); }
			else { play.setWhen("night"); }

			/* 낮 기간 타이머를 시작함 */
			if (play.getWhen().equals("sunny") && play.getState().equals("play")) {
				broadcast(Cmd.USERUPDATE, "", userManager.getUsers());
				
				Logger.append("TIMER START " + rcvNm + "\n");
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTICE, "server", "아침이 밝았습니다");
				new Thread() {
					public void run() {
						Integer timer = 360;
						while (timer > 0 && play.getWhen().equals("sunny") && play.isWantnext() == false) {
							timer--;
							sndMsg_ToTargets(userManager.getAlive(), PlayCmd.TIMER, "server", timer);

							if (timer <= 5) {
								sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTICE, "server", "투표까지" + timer);
							}
							
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
						}

						if (timer <= 0) {
							/* 모든 사용자에게 투표 시작을 요청함 */
							sndMsg_ToTargets(userManager.getAlive(), PlayCmd.STARTVOTE, "server", "");

							/* 새로운 투표 시작 */
							play.newVote();
						}
					}
				}.start();
			}
			break;

		//// * 유저 다음 턴으로 가기를 원함 */
		case PlayCmd.IWANTNEXT:
			userinfo = userManager.getUser(rcvNm);
			userinfo.setWantnext((boolean) rcvObj);

			if ((boolean) rcvObj == true){
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTICE, "server", rcvNm + "님이 밤으로 가길 원합니다.");
			} else{
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTICE, "server", rcvNm + "님이 밤으로 가길 원하지 않습니다.");
			}

			/* 모든 유저가 밤으로 가기를 원할경우 */
			if (userManager.isAllUserWantNext() == true){
				play.setWantnext(true);
			} else { /* 모든 유저가 밤으로 가기를 원하지 않은 경우 */
				play.setWantnext(false);
			}

			if (play.isWantnext()) {
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTICE, "server", "모든 유저가 투표를 원합니다.");

				/* 터치 불가한 상태로 전환 */
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTOUCHABLE, "server", "");

				/* 카운트 다운 시작 */
				new Thread() {
					public void run() {
						sndMsg_ToTargets(userManager.getAlive(), PlayCmd.IMPOTANTNOTICE, "server", "5초후 투표를 시작합니다");
						int count = 5;
						while (count > 0) {
							count--;
							sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTICE, "server", "투표까지    " + count);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								return;
							}
						}

						if (count == 0) {
							sndMsg_ToTargets(userManager.getAlive(), PlayCmd.IMPOTANTNOTICE, "server", "투표 중입니다..");

							/* 모든 사용자에게 투표 시작을 요청함 */
							sndMsg_ToTargets(userManager.getAlive(), PlayCmd.STARTVOTE, "server", "");

							/* 새로운 투표 시작 */
							play.newVote();
						}
					}
				}.start();
			}
			break;

		//// * 유저 투표함. 선택한 사람 전송 */
		case PlayCmd.VOTEUSER:

			/* 투표 업데이트 */
			play.updateVote(rcvNm, (String) rcvObj);

			/* 모든 유저가 투표를 완료함 */
			if (play.isAllUserVote()) {

				/* 가장 많은 표를 받은 유저(처형된 인원) 확인 */
				String dieUser = play.getDiedUserByVote();
				play.setDied(dieUser);

				/* 유저 정보 갱신, 사망자 공지 */
				broadcast(Cmd.USERUPDATE, "server", userManager.getUsers());
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.IMPOTANTNOTICE, "server", dieUser + "님이 투표로 처형 되었습니다.");
				sndMsg_ToTarget(dieUser, PlayCmd.IMPOTANTNOTICE, "server", "당신은 사망하였습니다.");
				sndMsg_ToTarget(dieUser, PlayCmd.YOUAREDIE, "server", "");
				sndMsg_ToTarget(dieUser, Cmd.USERUPDATE, "server", userManager.getUsers());

				/* 사망자는 터치 가는 상태로 전환 */
				sndMsg_ToTarget(dieUser, PlayCmd.TOUCHABLE, "", "");

				/* 게임 종료 조건 확인 */
				String gameover = play.isGameOver();
				if (!gameover.equals("NOGAMEOVER")) {
					Logger.append("--------------------게임 종료 -----------------\n");
					
					broadcast(Cmd.USERUPDATE, "server", userManager.getUsers());
					broadcast(PlayCmd.GAMEOVER, "server", gameover);
					
					play.gameOver();
					break;
				}

				/* 유저 터치 불가한 상태로 전환 */
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTOUCHABLE, "server", "");

				/* 밤이 오기 카운트 시작 */
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.IMPOTANTNOTICE, "server", "5초후 밤이 찾아옵니다.");
				new Thread() {
					public void run() {

						int count = 5;
						while (count > 0) {
							count--;
							sndMsg_ToTargets(userManager.getAlive(), PlayCmd.NOTICE, "server", "밤까지    " + count);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								return;
							}
						}

						if (count == 0) {
							sndMsg_ToTargets(userManager.getAlive(), PlayCmd.IMPOTANTNOTICE, "server", "밤이왔습니다..");

							/* 모든 사용자에게 밤으로 가기를 요청함 */
							sndMsg_ToTargets(userManager.getAlive(), PlayCmd.GONIGHT, "server", "");

							/* 마피아들의 선택을 초기화시킴 */
							play.newMafiaChoice();
						}
					}
				}.start();

			}
			break;

		/// * 유저가 밤에 있음을 알림 */
		case PlayCmd.IMINNIGHT:
			userManager.getUser(rcvNm).setWhen((String) rcvObj);
		
			if (userManager.isAllUserInNight()) {
				broadcast(Cmd.USERUPDATE, "server", userManager.getUsers());
				Logger.append("--------------모든 유저가 밤에 있습니다----------- \n");
				play.setWhen("night");
			} else {
				play.setWhen("sunny");
			}

			break;

		case PlayCmd.CHOICEUSERINNIGHT:
			switch (userManager.getUser(rcvNm).getCharacter()) {
			case "MAFIA":
				play.updateMafiaChoice(rcvNm, (String) rcvObj);
				break;
			case "COP":
				play.updateCopChoice(rcvNm, (String) rcvObj);
				break;
			case "DOCTOR":
				play.updateDoctorChoice(rcvNm, (String) rcvObj);
				break;
			}

			if (play.isAllChracterChoice() && play.getWhen().equals("night")) {
				String mafiasChoice = play.getMaxMafiaChoice();
				String copChoice 	= "";
				String doctorChoice = "";

				if (play.isAliveCop()) {
					copChoice = play.getCopChoice();
					if (userManager.getUser(copChoice).getCharacter().equals("MAFIA")){
						sndMsg_ToTargets(userManager.getCops(), PlayCmd.IMPOTANTNOTICE, "server", copChoice + "님은 마피아 입니다");
					} else{
						sndMsg_ToTargets(userManager.getCops(), PlayCmd.IMPOTANTNOTICE, "server", copChoice + "님은 마피아가 아닙니다");
					}
				}
				
				if (play.isAliveDoctor()){
					doctorChoice = play.getDoctorChoice();
				}

				Logger.append("---------------각 직업들의 선택 입니다 -------------------\n");
				Logger.append("마피아들은 " + mafiasChoice + "  선택하였습니다 " + "\n");
				Logger.append("경찰은" + copChoice + "  선택하였습니다 " + "\n");
				Logger.append("의사는" + doctorChoice + "  선택하였습니다 " + "\n");

				if (mafiasChoice != null){
					/* 마피아가 고른 유저와 , 의사가 고른 유저가 같은 경우, 아무도죽지 않음 */
					if (mafiasChoice.equals(doctorChoice)) {
						Logger.append("의사는 마피아로부터 " + doctorChoice + "를 구하였습니다" + "\n");
						sndMsg_ToTargets(userManager.getAlive(), PlayCmd.IMPOTANTNOTICE, "server", "지난 밤 마피아에 의해, " + "아무도 사망하지 않았습니다");
					} else { 	/* 마피아가 고른 유저와 , 의사고 고른 유저가 다른 경우, 마피아가 고른 인원 사망 */
						/* 사망자 갱신, 통보 */
						play.setDied(mafiasChoice);
						sndMsg_ToTargets(userManager.getAlive(), PlayCmd.IMPOTANTNOTICE, "server", "지난 밤 마피아에 의해, " + mafiasChoice + " 님이 사망하셨습니다");
						sndMsg_ToTarget(mafiasChoice, PlayCmd.IMPOTANTNOTICE, "server", "당신은 사망하였습니다.");
						sndMsg_ToTarget(mafiasChoice, PlayCmd.YOUAREDIE, "server", "");
						sndMsg_ToTarget(mafiasChoice, Cmd.USERUPDATE, "server", userManager.getUsers());
	
						/* 사망자는 터치 가는 상태로 전환 */
						sndMsg_ToTarget(mafiasChoice, PlayCmd.TOUCHABLE, "server", "당신은 사망하였습니다.");
					}
				}

				/* 밤이 끝났으므로 선택을 초기화 시킴 */
				play.endNight();

				/* 게임 종료 조건 확인 */
				String gameover = play.isGameOver();
				if (!gameover.equals("NOGAMEOVER")) {
					Logger.append("--------------------게임 종료 -----------------\n");
					broadcast(PlayCmd.GAMEOVER, "server", gameover);
					play.gameOver();
					break;
				}

				/* 밤이 끝나고 모든 유저에게 아침으로 가기를, 요청함 */
				sndMsg_ToTargets(userManager.getAlive(), PlayCmd.GOSUNNY, "server", "");

				/* 밤을 원하지 않도록 리셋 */
				for (int i = 0; i < userManager.size(); i++){
					userManager.getUser(i).setWantnext(false);
				}
				play.setWantnext(false);

				broadcast(Cmd.USERUPDATE, "server", userManager.getUsers());
			}

			break;

		case GameoverCmd.REGAME:
			userManager.addUser(rcvNm);
			broadcast(Cmd.USERUPDATE, "", userManager.getUsers());
			break;

		/* 유저 채팅을 보냄 */
		case ChatCmd.SENDMESSAGE:
			sndMsg_ToTargets(userManager.getAlive(), ChatCmd.SENDMESSAGE, rcvNm, rcvObj);
			break;

		case ChatCmd.SENDEMOTICON:
			sndMsg_ToTargets(userManager.getAlive(), ChatCmd.SENDEMOTICON, rcvNm, rcvObj);
			break;

		case HiddenChatCmd.SENDMESSAGE:
			sndMsg_ToTargets(userManager.getAlive(), HiddenChatCmd.SENDMESSAGE, rcvNm, rcvObj);
			break;

		case HiddenChatCmd.SENDEMOTICON:
			sndMsg_ToTargets(userManager.getAlive(), HiddenChatCmd.SENDEMOTICON, rcvNm, rcvObj);
			break;

		}
	}

}
