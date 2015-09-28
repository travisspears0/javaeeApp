package local.bb.websockets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

public class Room {
    
    private static final int MAX_SLOTS = 8;
    private static final int WAITING_READY_TIMEOUT_SECONDS = 5;
    private final String name;
    private final int slots;
    private List<User> users;
    private Thread readyWaiterThread = null;
    private boolean allReady = false;
    private Game game = null;
    private String[] colorsForUserNames;
    
    public Room(String name, int slots) {
        this.name = name;
        this.slots = Math.min(slots, Room.MAX_SLOTS);
        this.colorsForUserNames = new String[this.slots];
        this.users = new ArrayList<>();
    }
    
    public Room(String name) {
        this(name,0);
    }

    public String getName() {
        return name;
    }

    public List<User> getUsers() {
        return users;
    }

    public int getSlots() {
        return slots;
    }
    
    public boolean addUser(User user) {
        if( this.slots != 0 && this.users.size() >= this.slots ) {
            return false;
        }
        this.users.add(user);
        for( int i=0 ; i<this.slots ; ++i ) {
            if( this.colorsForUserNames[i] == null ) {
                this.colorsForUserNames[i] = user.getName();
                user.setColor(User.colors[i]);
                break;
            }
        }
        return true;
    }
    
    public String toStringShort() {
        JSONObject ob = new JSONObject();
        try {
            ob.put("name", this.name);
            ob.put("users", this.users.size());
            ob.put("slots", this.slots);
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return ob.toString();
    }
    
    @Override
    public String toString() {
        JSONObject ob = new JSONObject();
        for( int i=0 ; i<this.slots ; ++i ) {
            String index = Integer.toString(i);
            String userData = "empty";
            try {
                userData = this.users.get(i).toString();
            } catch( IndexOutOfBoundsException e) {}
            try {
                ob.put(index, userData);
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        return ob.toString();
    }
/*
    public RoomChangeNotifier getRoomChangeNotifier() {
        return roomChangeNotifier;
    }
*/
    public synchronized boolean areAllReady() {
        if( this.users.size() < this.slots ) {
            return false;
        }
        for( int i=0 ; i<this.slots ; ++i ) {
            if( this.users.get(i).getState() != User.STATE.READY ) {
                return false;
            }
        }
        return true;
    }
    
    public void userReady(User user) {
        if( this.game != null && this.game.getState() != Game.STATE.FINISHED ) {
            return;
        }
        user.setState(User.STATE.READY);
        this.roomChangeNotify();
        if( this.readyWaiterThread == null ) {
            this.readyWaiterThread = new Thread(){
                @Override
                public void run() {
                    try {
                        for( int i=WAITING_READY_TIMEOUT_SECONDS ; i>=0 ; --i ) {
                            if( !Room.this.allReady ) {
                                SocketServer.sendToAllInRoom(Room.this, i+"/"+WAITING_READY_TIMEOUT_SECONDS, "timer");
                            }
                            TimeUnit.SECONDS.sleep(1);
                        }
                        Room.this.kickUnreadyUsers();
                        SocketServer.sendToAllInRoom(Room.this, "game cancelled");
                    } catch(InterruptedException e) {
                        //starting game...
                        if( Room.this.game == null || Room.this.game.getState() == Game.STATE.FINISHED ) {
                            Room.this.game = null;
                            Room.this.game = new Game(Room.this);
                        }
                    } finally {
                        Room.this.readyWaiterThread = null;
                    }
                }
            };
            this.readyWaiterThread.start();
        }
        this.allReady = this.areAllReady();
        if( this.allReady ) {
            this.readyWaiterThread.interrupt();
        }
    }
    
    private void kickUnreadyUsers() {
        for( int i=0 ; i<this.users.size() ; ++i ) {
            try {
                User user = this.users.get(i);
                if( user.getState() == User.STATE.NOT_READY ) {
                    this.kickUser(user, false);
                    --i;
                } else {
                    user.setState(User.STATE.NOT_READY);
                }
            } catch(IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        this.roomChangeNotify();
        SocketServer.roomsChangeNotifier.roomsChanged();
    }
    
    public void roomChangeNotify() {
        SocketServer.sendToAllInRoom(this, this.toString(), "room");
    }
    
    public void kickUser(User user) {
        this.kickUser(user, true);
    }
    
    public void kickUser(User user, boolean roomNotification) {
        if( !this.users.contains(user) ) {
            return;
        }
        this.users.remove(this.users.get(this.users.indexOf(user)));
        user.setRoom(null);
        user.setState(User.STATE.NOT_READY);
        user.setColor(null);
        for( int i=0 ; i<this.slots ; ++i ) {
            if( this.colorsForUserNames[i] == null ) {
                continue;
            }
            if(  this.colorsForUserNames[i].equals(user.getName()) ) {
                this.colorsForUserNames[i] = null;
            }
        }
        SocketServer.waitingRoom.addUser(user);
        if( user.getSession().isOpen() ) {
            SocketServer.sendToUser(user, this.getName(), "leftRoom");
        }
        SocketServer.sendToAllInRoom(this, user.getName() + " left " + this.name);
        this.roomChangeNotify();
        if( roomNotification ) {
            SocketServer.roomsChangeNotifier.roomsChanged();
        }
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }
    
}
