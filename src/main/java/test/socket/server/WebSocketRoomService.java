package test.socket.server;

import java.util.Set;

import test.socket.bean.WebSocketRoom;

public interface WebSocketRoomService {
	/**
     * 查询所有房间
     * @return 所欲房间
     */
    Set<WebSocketRoom> findAll();
}
