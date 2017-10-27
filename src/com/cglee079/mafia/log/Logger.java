package com.cglee079.mafia.log;

import javax.swing.JTextArea;

public class Logger {
	private static JTextArea textArea = new JTextArea();
	
	public static JTextArea getTextArea(){
		return textArea;
	}
	
	public static void i(String msg){
		textArea.append(msg);
		textArea.setCaretPosition(textArea.getText().length());
		
	}
}
