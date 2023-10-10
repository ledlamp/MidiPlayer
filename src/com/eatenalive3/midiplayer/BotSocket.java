package com.eatenalive3.midiplayer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

public class BotSocket extends WebSocketClient {
	static long serverTimeOffset = 0;
	private String _roomName;
	
	private static Map<String, String> headers() {
		Random r = new Random();
		Map<String, String> h = new HashMap<String, String>();
		h.put("User-Agent", "MidiPlayer");
		String randomIP = Integer.toString(r.nextInt(256)) + '.' + Integer.toString(r.nextInt(256)) + '.' + Integer.toString(r.nextInt(256)) + '.' + Integer.toString(r.nextInt(256));
		h.put("X-Forwarded-For", randomIP);
		return h;
	}
	
	public BotSocket(String roomName) throws URISyntaxException {
		super(new URI("wss://game.multiplayerpiano.com"), headers());
		_roomName = roomName;
		
		System.out.println("before connect");

		connect();

		Play.socket = this;

		while (getReadyState() == ReadyState.NOT_YET_CONNECTED) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("after connect");
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					send("[{\"m\":\"t\",\"e\":" + System.currentTimeMillis() + "}]");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					}
				}
			}
		}).start();

		send("[{\"m\":\"hi\"}]");
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		System.out.println("handshake");
		System.out.println(handshakedata.getHttpStatusMessage());
	}

	@Override
	public void onMessage(String message) {
		if (message.contains("\"m\":\"hi\"")) {
			send("[{\"m\":\"ch\",\"_id\":\"" + _roomName + "\"}]");
			send("[{\"m\":\"userset\",\"set\":{\"name\":\"MidiPlayer\"}}]");
		}
		if (message.contains("m\":\"t")) {
			String t = message.substring(message.indexOf("t\":")+3);
			String e = t.substring(t.indexOf("e\":")+3);
			t = t.substring(0, t.indexOf(","));
			e = e.substring(0, e.indexOf("}"));
			//System.out.println(Long.parseLong(t) - Long.parseLong(e));
			serverTimeOffset = Long.parseLong(t) - Long.parseLong(e);
		}
		//MidiPlayer.log(System.currentTimeMillis()+ ": server message: " + message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		MidiPlayer.log("[BUG] NOTIFY BOSS: websocket connection closed: " + reason + ", " + code + ", " + remote);
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

	public void sendChat(String msg) {
		MidiPlayer.log(msg);
	}
}