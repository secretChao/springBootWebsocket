package test.socket.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import test.socket.bean.WebSocketRoom;
import test.socket.handler.ChatTextWebSocketHandler;
import test.socket.interceptor.WebsocketInterceptor;
import test.socket.server.WebSocketRoomService;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	@Autowired
	private ChatTextWebSocketHandler chatTextWebSocketHandler;

	@Autowired
	private WebSocketRoomService webSocketRoomService;

	@Autowired
	private WebsocketInterceptor interceptor;

	/**
	 * 注册 websocket 处理器
	 * 
	 * @param registry WebSocketHandlerRegistry
	 */
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//		registry.addHandler(chatTextWebSocketHandler, "/connect").withSockJS();
		Set<WebSocketRoom> rooms = webSocketRoomService.findAll();
		List<String> roomConnects = rooms.stream().map(WebSocketRoom::getConnectPath).collect(Collectors.toList());
		registry.addHandler(chatTextWebSocketHandler, roomConnects.toArray(new String[] {}))
				.addInterceptors(interceptor);
	}

//	    /**
//	     * WebSocket服务器端点
//	     * @return ServerEndpointExporter
//	     */
//	    @Bean
//	    public ServerEndpointExporter serverEndpointExporter() {
//	        return new ServerEndpointExporter();
//	    }
}
