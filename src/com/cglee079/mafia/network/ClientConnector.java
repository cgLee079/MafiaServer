package com.cglee079.mafia.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JTextArea;

import com.cglee079.mafia.game.Play;
import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.model.User;
import com.cglee079.mafia.model.NetworksManager;

public class ClientConnector extends Thread {
	private MySocket mySocket;
	private Socket soc; // 연결소켓
	private ServerSocket serversocket;
	private NetworksManager networksManager; // 연결된 사용자를 저장할 벡터
	private Play play;
	
	public ClientConnector(){
		networksManager = new NetworksManager();
		play		= new Play(networksManager);
	}
	
	public ClientConnector(ServerSocket socket) {
		this();
		this.serversocket = socket;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Logger.i("\n");
				Logger.i("---------------User waiting----------------\n");
				soc = serversocket.accept();
				Logger.i(">>>> User Connect!!\n");
				this.mySocket = new MySocket(soc);
				new MyNetwork(mySocket, networksManager, play).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // accept가 일어나기 전까지는 무한 대기중

		}

	}
}
