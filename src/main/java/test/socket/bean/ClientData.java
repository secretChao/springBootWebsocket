package test.socket.bean;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientData {

	/** Map for key=clientIp, value=clientName */
	public static final Map<String, String> CLIENT_MAP = new HashMap<>();

	public static String getClientName(String clientIp) {
		return CLIENT_MAP.get(clientIp);
	}

	public static void setClientData(String clientIp, String clientName) {
		CLIENT_MAP.put(clientIp, clientName);
	}

	public static void removeClientData(String clientIp) {
		CLIENT_MAP.remove(clientIp);
	}

	public static String getOnlineClientJsonList() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(CLIENT_MAP);
	}

}
