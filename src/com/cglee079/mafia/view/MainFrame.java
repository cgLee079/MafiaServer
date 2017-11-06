package com.cglee079.mafia.view;

// Java Chatting Server

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.cglee079.mafia.log.Logger;
import com.cglee079.mafia.network.ClientConnector;

public class MainFrame extends JFrame {
	private JPanel contentPane;
	private JLabel portLabel;
	private JTextField portTextField; // 사용할 PORT번호 입력
	private JButton StartBtn; // 서버를 실행시킨 버튼
	private JScrollPane logScrollPane;
	private JTextArea logTextArea; // 클라이언트 및 서버 메시지 출력
	 
	private ServerSocket socket; // 서버소켓
	private int port; // 포트번호

	public static void main(String[] args) {
		new MainFrame();
	}

	public MainFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 410);
		setVisible(true);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(null);
		setContentPane(contentPane);

		logTextArea = Logger.getTextArea();
		logTextArea.setColumns(20);
		logTextArea.setRows(5);
		logTextArea.setEditable(false); // textArea를 사용자가 수정 못하게끔 막는다.
		
		logScrollPane = new JScrollPane();
		logScrollPane.setBounds(0, 0, 580, 254);
		logScrollPane.setViewportView(logTextArea);
		contentPane.add(logScrollPane);

		portTextField = new JTextField("30025");
		portTextField.setBounds(98, 264, 300, 37);
		portTextField.setColumns(10);
		portTextField.addActionListener(new Myaction());
		contentPane.add(portTextField);

		portLabel = new JLabel("Port Number");
		portLabel.setBounds(12, 264, 98, 37);
		contentPane.add(portLabel);

		StartBtn = new JButton("서버 실행");
		StartBtn.addActionListener(new Myaction()); // 내부클래스로 액션 리스너를 상속받은 클래스로
		StartBtn.setBounds(0, 325, 600, 37);
		contentPane.add(StartBtn);
	}


	class Myaction implements ActionListener { // 내부클래스로 액션 이벤트 처리 클래스
		@Override
		public void actionPerformed(ActionEvent e) {

			// 액션 이벤트가 sendBtn일때 또는 textField 에세 Enter key 치면
			if (e.getSource() == StartBtn || e.getSource() == portTextField) {
				if (portTextField.getText().equals("") || portTextField.getText().length() == 0)// textField에
				{
					portTextField.setText("포트번호를 입력해주세요");
					portTextField.requestFocus(); // 포커스를 다시 textField에 넣어준다
				} else {
					try {
						port = Integer.parseInt(portTextField.getText());
						serverStart(); // 사용자가 제대로된 포트번호를 넣었을때 서버 실행을위헤 메소드 호출
					} catch (Exception er) {
						// 사용자가 숫자로 입력하지 않았을시에는 재입력을 요구한다
						portTextField.setText("숫자로 입력해주세요");
						portTextField.requestFocus(); // 포커스를 다시 textField에 넣어준다
					}
				} // else 문 끝
			}

		}

	}

	private void serverStart() {
		try {
			socket = new ServerSocket(port); // 서버가 포트 여는부분
			StartBtn.setText("서버실행중");
			StartBtn.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
			portTextField.setEnabled(false); // 더이상 포트번호 수정못 하게 막는다

			if (socket != null) {/// socket 이 정상적으로 열렸을때
				new ClientConnector(socket).start();
			}

		} catch (IOException e) {
			logTextArea.append("소켓이 이미 사용중입니다...\n");
		}

	}
}
