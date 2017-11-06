package com.cglee079.mafia.log;

import javax.swing.JTextArea;

public class Logger {
	private static final JTextArea TEXTAREA = new JTextArea();
	
	public synchronized static JTextArea getTextArea(){
		return TEXTAREA;
	}
	
	public synchronized static void i(String msg){
		TEXTAREA.append(msg);
		TEXTAREA.setCaretPosition(TEXTAREA.getText().length());
		
	}
}
