package test.socket.handler;

import java.io.IOException;
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

import test.socket.bean.ClientData;

@Component
public class ChatTextWebSocketHandler extends TextWebSocketHandler {

	private static final Logger logger = LogManager.getLogger(ChatTextWebSocketHandler.class);

	private static final ConcurrentHashMap<String, AtomicInteger> ROOM_ONLINE_COUNTS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, Set<WebSocketSession>> ROOM_SESSIONS = new ConcurrentHashMap<>();

	/**
	 * 连接建立后调用的方法
	 *
	 * @param session 客户端 session
	 * @throws Exception 异常
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		String room = getRoom(session);

		Set<WebSocketSession> webSocketSessions = getSessions(room);
		webSocketSessions.add(session);

		String id = session.getId();
		String clientIp = session.getLocalAddress().getAddress().getHostAddress();
		String clientName = ClientData.getClientName(clientIp);

		AtomicInteger onlineCount = getOnlineCount(room);
		logger.info("client {}({}) join in room {}, now room online number is : {}", clientName, clientIp, room,
				onlineCount.incrementAndGet());

		session.sendMessage(new TextMessage(String.format("登入名稱: %s，已進入頻道%s", clientName, room)));
		groupSendMessage(room, id, String.format("%s 已進入頻道%s", clientName, room));
	}

	/**
	 * 连接断开后调用的方法
	 *
	 * @param session 客户端 session
	 * @param status  关闭状态， 1000 为正常关闭
	 * @throws Exception 异常
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		super.afterConnectionClosed(session, status);
		String room = getRoom(session);
		Set<WebSocketSession> webSocketSessions = getSessions(room);
		webSocketSessions.remove(session);
		AtomicInteger onlineCount = getOnlineCount(room);

		String clientIp = session.getLocalAddress().getAddress().getHostAddress();
		String clientName = ClientData.getClientName(clientIp);

		logger.info("client {}({}) was closed, now room {} online number is : {}", clientName, clientIp, room,
				onlineCount.decrementAndGet());
		groupSendMessage(room, session.getId(), String.format("%s 已中斷連接", clientName));
	}

	/**
	 * 获取到客户端发送的文本消息时调用的方法
	 *
	 * @param session 客户端 session
	 * @param message 消息内容
	 * @throws Exception 异常
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String room = getRoom(session);
		String textMessage = message.getPayload();

		String clientIp = session.getLocalAddress().getAddress().getHostAddress();
		String clientName = ClientData.getClientName(clientIp);

		logger.info("message from client {}({}): {}", clientName, clientIp, message);
		session.sendMessage(new TextMessage("我: " + textMessage));
		// 发送消息给其他客户端
		groupSendMessage(room, session.getId(), String.format("%s:%s", clientName, textMessage));
	}

	/**
	 * 客户端连接异常时调用的方法
	 *
	 * @param session   客户端 session
	 * @param exception 异常信息
	 * @throws Exception 异常
	 */
	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		session.close(CloseStatus.SERVER_ERROR);

		String clientIp = session.getLocalAddress().getAddress().getHostAddress();

		logger.error("server error, Session ID: {}, IP: {}, error info: {}", session.getId(), clientIp,
				exception.getMessage());
	}

	/**
	 * 向指定客户端发送消息
	 *
	 * @param room      房间号
	 * @param sessionId 被指定客户端 session
	 * @param message   消息内容
	 */
	public void sendMessage(String room, String sessionId, String message) {
		Set<WebSocketSession> sessions = getSessions(room);
		WebSocketSession targetSession = sessions.stream().filter(session -> sessionId.equals(session.getId()))
				.findAny().orElse(null);
		if (targetSession == null) {
			logger.warn("{} is offline", sessionId);
			return;
		}
		try {
			targetSession.sendMessage(new TextMessage(message));
		} catch (IOException e) {
			logger.error("client {} send message fail, error: {}", sessionId, e.getMessage());
		}
	}

	/**
	 * 群发消息
	 *
	 * @param room            房间号
	 * @param sourceSessionId 发出消息的 session 的 id
	 * @param message         消息内容
	 */
	public void groupSendMessage(String room, String sourceSessionId, String message) {
		Set<WebSocketSession> sessions = getSessions(room);
		sessions.stream().filter(WebSocketSession::isOpen).filter(session -> !session.getId().equals(sourceSessionId))
				.forEach(session -> sendMessage(room, session.getId(), message));
	}

	private String getRoom(WebSocketSession session) {
		String[] strings = StringUtils.split(session.getUri().getPath(), "/");
		return strings[1];
	}

	private Set<WebSocketSession> getSessions(String room) {
		Set<WebSocketSession> webSocketSessions = ROOM_SESSIONS.get(room);
		if (webSocketSessions == null) {
			webSocketSessions = new CopyOnWriteArraySet<>();
			ROOM_SESSIONS.put(room, webSocketSessions);
		}
		return webSocketSessions;
	}

	private AtomicInteger getOnlineCount(String room) {
		AtomicInteger onlineCount = ROOM_ONLINE_COUNTS.get(room);
		if (onlineCount == null) {
			onlineCount = new AtomicInteger(0);
			ROOM_ONLINE_COUNTS.put(room, onlineCount);
		}
		return onlineCount;
	}
}
