package si.vegvamind.ftcuploader.utils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;

public class WebSocketUtils extends WebSocketClient {
	public WebSocketUtils(URI serverUri) {
		super(serverUri);
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
	}

	@Override
	public void onMessage(String message) {

	}

	@Override
	public void onClose(int code, String reason, boolean remote) {

	}

	@Override
	public void onError(Exception ex) {

	}
}
