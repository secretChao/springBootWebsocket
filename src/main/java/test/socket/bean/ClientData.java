package test.socket.bean;

import java.util.HashMap;
import java.util.Map;

public class ClientData {

	public static final Map<String, String> CLIENT_MAP = new HashMap<>();

	public static String getClientName(String clientId) {
		return CLIENT_MAP.get(clientId);
	}

	public static void setClientData(String clientId, String clientName) {
		CLIENT_MAP.put(clientId, clientName);
	}

	public Map<String, String> getClientData() {
		return CLIENT_MAP;
	}

}
