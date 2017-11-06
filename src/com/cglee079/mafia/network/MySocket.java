package com.cglee079.mafia.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.json.JSONObject;

public class MySocket {
	private Socket socket;
	private InputStream is;
	private OutputStream os;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;

	public MySocket(Socket socket) throws IOException {
		this.socket = socket;
		is = socket.getInputStream();
		os = socket.getOutputStream();
		ois = new ObjectInputStream(is);
		oos = new ObjectOutputStream(os);
	}

	public synchronized void writeJSON(JSONObject data) throws ClassNotFoundException, IOException, EOFException {
		oos.writeUTF(data.toString());
		oos.reset();
	}

	public JSONObject readJSON() throws ClassNotFoundException, IOException, EOFException {
		return new JSONObject(ois.readUTF());
	}

	public void close() throws IOException {
		os.close();
		is.close();
		oos.close();
		ois.close();
		socket.close();
	}

	public Socket getSocket() {
		return socket;
	}

}
