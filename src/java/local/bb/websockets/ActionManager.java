package local.bb.websockets;

import java.util.ArrayList;
import java.util.List;
import javax.json.JsonObject;
import javax.websocket.Session;

public abstract class ActionManager {
    
    private final List<Action> actions = new ArrayList<>();
    
    public List<Action> getActions() {
        return actions;
    }
    
    public void callAction(Session session, String dataReceived) {
        JsonObject ob = SocketServer.JsonDecode(dataReceived);
        String type = ob.getString("type");
        String data = ob.getString("data");
        
        for( int i=0 ; i<this.actions.size() ; ++i ) {
            Action action = this.actions.get(i);
            if( action.getName().equals(type) ) {
                action.getActionCallback().execute(session, data);
            }
        }
    }
    
}
