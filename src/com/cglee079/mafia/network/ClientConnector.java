package com.cglee079.mafia.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JTextArea;

import com.cglee079.mafia.game.Play;
import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.model.UserInfo;
import com.cglee079.mafia.model.UserManager;

public class ClientConnector extends Thread {
	private MySocket mySocket;
	private Socket soc; // 연결소켓
	private ServerSocket serversocket;
	private UserManager userManager = new UserManager(); // 연결된 사용자를 저장할 벡터
	private Play play = new Play(userManager);
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
				new MyNetwork(mySocket, userManager, play).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // accept가 일어나기 전까지는 무한 대기중

		}

	}
}
