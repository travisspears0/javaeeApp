package local.bb.websockets;

import javax.websocket.Session;

public interface ActionCallBack {
    
    public void execute(Session session, String data);
    
}
