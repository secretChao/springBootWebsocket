package test.socket.interceptor;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import test.socket.bean.ClientData;

@Component
public class WebsocketInterceptor implements HandshakeInterceptor {

	private Logger logger = LogManager.getLogger(WebsocketInterceptor.class);

	/**
	 * 握手前
	 *
	 * @param request
	 * @param response
	 * @param wsHandler
	 * @param attributes
	 * @return
	 * @throws Exception
	 */
	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws Exception {
		// 获得请求参数
		try {
			String paramString = request.getURI().getQuery();
			String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
			String clientName = paramString.substring(5);
//			ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
//			HttpSession session = servletRequest.getServletRequest().getSession();

			ClientData.setClientData(clientIp, clientName);
			logger.info("ip={}, name={}", clientIp, clientName);
			return true;
		} catch (Exception e) {
			logger.info("登入失敗");
			return false;
		}
//        String uid = paramMap.get("token");
//        if (StrUtil.isNotBlank(uid)) {
//            // 放入属性域
//            attributes.put("token", uid);
//            logger.info("用户 token " + uid + " 握手成功！");
//            return true;
//        }
	}

	/**
	 * 握手后
	 *
	 * @param request
	 * @param response
	 * @param wsHandler
	 * @param exception
	 */
	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {

	}
}
