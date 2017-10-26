package com.example.changoo.mafia.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JTextArea;

import com.example.changoo.mafia.log.Logger;
import com.example.changoo.mafia.logic.GameLogic;
import com.example.changoo.mafia.model.UserManager;
import com.example.changoo.mafia.model.UserInfo;

public class ClientConnector extends Thread {
	private MySocket mySocket;
	private Socket soc; // 연결소켓
	private ServerSocket serversocket;
	private UserManager userManager = new UserManager(); // 연결된 사용자를 저장할 벡터
	private GameLogic gameLogic = new GameLogic(userManager);
	public ClientConnector(ServerSocket socket) {
		this.serversocket = socket;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Logger.append("\n");
				Logger.append("---------------User waiting----------------\n");
				soc = serversocket.accept();
				Logger.append(">>>> User Connect!!\n");
				this.mySocket = new MySocket(soc);
				new MyNetwork(mySocket, userManager,gameLogic).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // accept가 일어나기 전까지는 무한 대기중

		}

	}
}
