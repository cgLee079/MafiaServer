package com.example.changoo.mafia.log;

import javax.swing.JTextArea;

public class Logger {
	private static JTextArea textArea=new JTextArea();
	
	public static JTextArea getTextArea(){
		return textArea;
	}
	
	public static void append(String msg){
		textArea.append(msg);
		textArea.setCaretPosition(textArea.getText().length());
		
	}
}
