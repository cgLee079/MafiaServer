package com.cglee079.mafia.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JTextArea;

import com.cglee079.mafia.game.Play;
import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.model.User;

public class ClientConnector extends Thread {
	private MySocket mySocket;
	private Socket socket; // 연결소켓
	private ServerSocket serversocket;
	private NetworkManager networkManager; // 연결된 사용자를 저장할 벡터
	private Play play;
	
	public ClientConnector(){
		networkManager 	= new NetworkManager();
		play			= new Play();
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
				socket = serversocket.accept();
				Logger.i(">>>> User Connect!!\n");
				this.mySocket = new MySocket(socket);
				new MyNetwork(mySocket, networkManager, play).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // accept가 일어나기 전까지는 무한 대기중

		}

	}
}
