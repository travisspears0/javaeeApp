package local.bb.websockets;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class RoomsManager {
    
    private static final int numRooms = 10;
    private final List<Room> rooms = new ArrayList<>();
    
    public RoomsManager() {
        for( int i=0 ; i<RoomsManager.numRooms ; ++i ) {
            this.rooms.add(new Room("ROOM no. " + i, i%2+1));
        }
    }

    public List<Room> getRooms() {
        return rooms;
    }
    
    public Room getRoomByName(String name) {
        for( int i=0 ; i<this.rooms.size() ; ++i ) {
            Room room = this.rooms.get(i);
            if( room.getName().equals(name) ) {
                return room;
            }
        }
        return null;
    }
    
    public boolean enterRoom(User user, Room room) {
        if( room == null || room.getUsers().size() >= room.getSlots() ) {
            return false;
        }
        room.addUser(user);
        user.setRoom(room);
        return true;
    }
    
    @Override
    public String toString() {
        JSONObject ob = new JSONObject();
        for( int i=0 ; i<this.rooms.size() ; ++i ) {
            try {
                Room room = this.rooms.get(i);
                ob.put(Integer.toString(i), room.toStringShort());
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        return ob.toString();
    }
    
}
