package test.socket.handler;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import test.socket.bean.ClientData;
import test.socket.bean.Message;
import test.socket.bean.SentType;

@Component
public class ChatTextWebSocketHandler extends TextWebSocketHandler {

	private static final Logger logger = LogManager.getLogger(ChatTextWebSocketHandler.class);

	private static final ConcurrentHashMap<String, AtomicInteger> ROOM_ONLINE_COUNTS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, Set<WebSocketSession>> ROOM_SESSIONS = new ConcurrentHashMap<>();
	private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("HH:mm:ss");

	/**
	 * 連接建立後調用的方法
	 *
	 * @param session 客戶端 session
	 * @throws Exception
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		String room = getRoom(session);

		Set<WebSocketSession> webSocketSessions = getSessions(room);
		webSocketSessions.add(session);

		String id = session.getId();
		String clientIp = session.getRemoteAddress().getAddress().getHostAddress();
		String clientName = ClientData.getClientName(clientIp);

		AtomicInteger onlineCount = getOnlineCount(room);
		logger.info("client {}({}) join in room {}, now room online number is : {}", clientName, clientIp, room,
				onlineCount.incrementAndGet());

		LocalTime lt = LocalTime.now();

		Message msgToClient = new Message();
		msgToClient.setSentName("系統訊息");
		msgToClient.setSentDate(lt.format(DTF));
		msgToClient.setSentType(SentType.SYS);
		msgToClient.setSentMsg(String.format("登入名稱: %s，已進入頻道%s", clientName, room));

		Message msgToGroup = new Message();
		msgToGroup.setSentName("系統訊息");
		msgToGroup.setSentDate(lt.format(DTF));
		msgToGroup.setSentType(SentType.SYS);
		msgToGroup.setSentMsg(String.format("%s 已進入頻道%s", clientName, room));

		session.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(msgToClient)));
		groupSendMessage(room, id, msgToGroup);
		sendOnlineUserMsg(room);
	}

	/**
	 * 連接斷開後調用的方法
	 *
	 * @param session 客戶端 session
	 * @param status  關閉狀態， 1000 為正常關閉
	 * @throws Exception
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		super.afterConnectionClosed(session, status);
		String room = getRoom(session);
		Set<WebSocketSession> webSocketSessions = getSessions(room);
		webSocketSessions.remove(session);
		AtomicInteger onlineCount = getOnlineCount(room);

		String clientIp = session.getRemoteAddress().getAddress().getHostAddress();
		String clientName = ClientData.getClientName(clientIp);
		ClientData.removeClientData(clientIp);

		logger.info("client {}({}) was closed, now room {} online number is : {}", clientName, clientIp, room,
				onlineCount.decrementAndGet());

		LocalTime lt = LocalTime.now();
		Message msgToGroup = new Message();
		msgToGroup.setSentName("系統訊息");
		msgToGroup.setSentDate(lt.format(DTF));
		msgToGroup.setSentType(SentType.SYS);
		msgToGroup.setSentMsg(String.format("%s 已中斷連接", clientName));

		groupSendMessage(room, session.getId(), msgToGroup);
		sendOnlineUserMsg(room);
	}

	/**
	 * 獲取到客戶端發送的文本消息時調用的方法
	 *
	 * @param session 客戶端 session
	 * @param message 訊息
	 * @throws Exception
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String room = getRoom(session);
		String textMessage = message.getPayload();

		String clientIp = session.getRemoteAddress().getAddress().getHostAddress();
		String clientName = ClientData.getClientName(clientIp);

		logger.info("message from client {}({}): {}", clientName, clientIp, message);

		LocalTime lt = LocalTime.now();
		Message msgToClient = new Message();
		msgToClient.setSentName("我");
		msgToClient.setSentDate(lt.format(DTF));
		msgToClient.setSentType(SentType.MSG);
		msgToClient.setSentMsg(textMessage);

		Message msgToGroup = new Message();
		msgToGroup.setSentName(clientName);
		msgToGroup.setSentDate(lt.format(DTF));
		msgToGroup.setSentType(SentType.MSG);
		msgToGroup.setSentMsg(textMessage);

		session.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(msgToClient)));
		// 發送訊息给其他人
		groupSendMessage(room, session.getId(), msgToGroup);
		sendOnlineUserMsg(room);
	}

	/**
	 * 客戶端連接異常時調用的方法
	 *
	 * @param session   客戶端 session
	 * @param exception 異常信息
	 * @throws Exception
	 */
	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		session.close(CloseStatus.SERVER_ERROR);

		String clientIp = session.getRemoteAddress().getAddress().getHostAddress();

		logger.error("server error, Session ID: {}, IP: {}, error info: {}", session.getId(), clientIp,
				exception.getMessage());
		sendOnlineUserMsg(getRoom(session));
	}

	/**
	 * 向指定客戶端發送訊息
	 *
	 * @param room      房間
	 * @param sessionId 被指定客戶端 session
	 * @param message   訊息
	 */
	public void sendMessage(String room, String sessionId, Message message) {
		Set<WebSocketSession> sessions = getSessions(room);
		WebSocketSession targetSession = sessions.stream().filter(session -> sessionId.equals(session.getId()))
				.findAny().orElse(null);
		if (targetSession == null) {
			logger.warn("{} is offline", sessionId);
			return;
		}
		try {
			targetSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(message)));
		} catch (IOException e) {
			logger.error("client {} send message fail, error: {}", sessionId, e.getMessage());
		}
	}

	/**
	 * 群发消息
	 *
	 * @param room            房間
	 * @param sourceSessionId 發出訊息者的 session id
	 * @param message         訊息
	 */
	public void groupSendMessage(String room, String sourceSessionId, Message message) {
		Set<WebSocketSession> sessions = getSessions(room);
		sessions.stream().filter(WebSocketSession::isOpen).filter(session -> !session.getId().equals(sourceSessionId))
				.forEach(session -> sendMessage(room, session.getId(), message));
	}

	/**
	 * 從session取出房間資料
	 * 
	 * @param session
	 * @return
	 */
	private String getRoom(WebSocketSession session) {
		String[] strings = StringUtils.split(session.getUri().getPath(), "/");
		return strings[1];
	}

	/**
	 * 取得session
	 * 
	 * @param room
	 * @return
	 */
	private Set<WebSocketSession> getSessions(String room) {
		Set<WebSocketSession> webSocketSessions = ROOM_SESSIONS.get(room);
		if (webSocketSessions == null) {
			webSocketSessions = new CopyOnWriteArraySet<>();
			ROOM_SESSIONS.put(room, webSocketSessions);
		}
		return webSocketSessions;
	}

	/**
	 * 
	 * @param room
	 * @return
	 */
	private AtomicInteger getOnlineCount(String room) {
		AtomicInteger onlineCount = ROOM_ONLINE_COUNTS.get(room);
		if (onlineCount == null) {
			onlineCount = new AtomicInteger(0);
			ROOM_ONLINE_COUNTS.put(room, onlineCount);
		}
		return onlineCount;
	}

	/**
	 * 發布在線清單
	 * 
	 * @param room
	 */
	public void sendOnlineUserMsg(String room) {
		try {
			LocalTime lt = LocalTime.now();
			Message message = new Message();
			message.setSentName("系統訊息");
			message.setSentDate(lt.format(DTF));
			message.setSentType(SentType.LIST);
			message.setSentMsg(ClientData.getOnlineClientJsonList());
			groupSendMessage(room, null, message);

		} catch (JsonProcessingException e) {
			logger.error("Sent online client list is error", e);
		}

	}
}
