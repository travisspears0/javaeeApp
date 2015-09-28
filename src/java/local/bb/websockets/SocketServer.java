package local.bb.websockets;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.joda.time.DateTime;

@ServerEndpoint("/server")
public class SocketServer {
    
    //list of users who are not in any room
    public static List<User> users = new ArrayList<>();
    public static List<Session> pendingSessions = new ArrayList<>();
    public final static RoomsManager roomsManager = new RoomsManager();
    public final static RoomsChangeNotifier roomsChangeNotifier = new RoomsChangeNotifier(roomsManager);
    public static Room waitingRoom = new Room("Waiting Room");
    private static final ActionManager actionManager = new ActionManager() {
        {
            /*
             * ___ ACTION
             *
            this.getActions().add(new Action("___", new ActionCallBack() {
                @Override
                public void execute(Session session, String data) {
                    
                }
            }));
            /*
             * LOGIN ACTION
             */
            this.getActions().add(new Action("login", new ActionCallBack() {
                @Override
                public void execute(Session session, String data) {
                    if( SocketServer.loginAttempt(data) ) {
                        User newUser = new User(session, data);
                        SocketServer.users.add(newUser);
                        SocketServer.waitingRoom.addUser(newUser);
                        SocketServer.pendingSessions.remove(session);
                        SocketServer.sendToUser(newUser, newUser.getName(),"loggedIn");
                        SocketServer.log(newUser + " logged in as " + newUser.getName());
                        SocketServer.sendToUser(newUser, SocketServer.roomsManager.toString(), "rooms");
                        SocketServer.sendToAllInWaitingRoom(newUser.getName() + " joined");
                    } else {
                        SocketServer.sendToSession(session, "Could not log in as " + data + ", name taken", "error" );
                        SocketServer.log("Failed to log in as [" + data + "], name taken; by connection: " + session.getId());
                    }
                }
            }));
            /*
             * LOGOUT ACTION
             */
            this.getActions().add(new Action("logout", new ActionCallBack() {
                @Override
                public void execute(Session session, String data) {
                    try {
                        SocketServer.sendToSession(session, "loggedOut", "loggedOut");
                        session.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }));
            /*
             * ENTER ROOM ACTION
             */
            this.getActions().add(new Action("enterRoom", new ActionCallBack() {
                @Override
                public void execute(Session session, String data) {
                    User user = SocketServer.getUserBySession(session);
                    if( !SocketServer.waitingRoom.getUsers().contains(user) ) {
                        SocketServer.sendToSession(session, "Could not join the room as You are not in the waiting room");
                        return;
                    }
                    Room room = SocketServer.roomsManager.getRoomByName(data);
                    if( SocketServer.roomsManager.enterRoom(user,room) ) {
                        SocketServer.waitingRoom.getUsers().remove(user);
                        SocketServer.sendToUser(user, room.getName(), "joinedRoom");
                        SocketServer.roomsChangeNotifier.roomsChanged();
                        //room.getRoomChangeNotifier().roomChanged();
                        room.roomChangeNotify();
                        SocketServer.sendToAllInRoom(room, user.getName() + " joined " + room.getName());
                    } else {
                        System.out.println(user + " could not join the room " + room.getName());
                        SocketServer.sendToUser(user, "could not join the room " + room.getName() + " because it is full or does not exist", "error");
                    }
                }
            }));
            /*
             * LEAVE ROOM ACTION
             */
            this.getActions().add(new Action("leaveRoom", new ActionCallBack() {
                @Override
                public void execute(Session session, String data) {
                    User user = SocketServer.getUserBySession(session);
                    //SocketServer.kickUserFromRoom(user);
                    user.getRoom().kickUser(user);
                }
            }));
            /*
             * READY ACTION
             */
            this.getActions().add(new Action("ready", new ActionCallBack() {
                @Override
                public void execute(Session session, String data) {
                    User user = SocketServer.getUserBySession(session);
                    Room room = user.getRoom();
                    if( room.getUsers().size() < room.getSlots() ) {
                        SocketServer.sendToUser(user, "You can not be ready untill the room is full", "error");
                        return;
                    }
                    room.userReady(user);
                }
            }));
            /*
             * GAME ACTION
             */
            this.getActions().add(new Action("action", new ActionCallBack() {
                @Override
                public void execute(Session session, String data) {
                    User user = SocketServer.getUserBySession(session);
                    Room room = user.getRoom();
                    Game game = room.getGame();
                    if( game == null ) {
                        SocketServer.sendToUser(user, "error: game is not active", "error");
                        return;
                    }
                    if( user.getGameState() == null ) {
                        SocketServer.sendToUser(user, "error: you are not able to perform such actions", "error");
                        return;
                    }
                    switch(data) {
                        case "+": {
                            if( user.setAction(User.ACTION.ADD) ) {
                                game.userPickedAction();
                            }
                            break;
                        }
                        case "*": {
                            if( user.setAction(User.ACTION.MULTIPLE) ) {
                                game.userPickedAction();
                            }
                            break;
                        }
                        case "next": {
                            if( user.setAction(User.ACTION.NEXT) ) {
                                game.userPickedAction();
                            }
                            break;
                        }
                        case "pass": {
                            if( user.setAction(User.ACTION.PASS) ) {
                                game.userPickedAction();
                            }
                            break;
                        }
                        case "end": {
                            if( user.setAction(User.ACTION.END) ) {
                                game.userPickedAction();
                            }
                            break;
                        }
                        default: {
                            SocketServer.sendToUser(user, "error: unknown action", "error");
                        }
                    }
                }
            }));
            /*
             * MESSAGE ACTION
             */
            this.getActions().add(new Action("message", new ActionCallBack() {
                @Override
                public void execute(Session session, String data) {
                    User user = SocketServer.getUserBySession(session);
                    if( user == null ) {
                        SocketServer.sendToSession(session, "Could not send the message as You are not logged in", "error");
                        return;
                    }
                    String toBeSent = SocketServer.getCurrentTime() + user.getName() + ": " + data;
                    String toBeLogged = toBeSent;
                    if( user.getRoom() != null ) {
                        SocketServer.sendToAllInRoom(user.getRoom(), toBeSent);
                        toBeLogged += " | in " + user.getRoom().getName();
                    } else {
                        SocketServer.sendToAllInWaitingRoom(toBeSent);
                        toBeLogged += " | in Waiting room " + SocketServer.waitingRoom.getUsers().size();
                    }
                    SocketServer.log(toBeLogged);
                }
            }));
        }
    };
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("New Connection: " + session.getId());
        SocketServer.pendingSessions.add(session);
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        SocketServer.actionManager.callAction(session, message);
    }
    
    @OnClose
    public void onClose(Session session) {
        System.out.println("Closing connecion: " + session.getId());
        this.exit(session);
    }
    
    @OnError
    public void onError(Throwable error) {
        System.out.println("ERROR: " + error.getMessage());
        error.printStackTrace();
    }
    
    public void exit(Session session) {
        if( SocketServer.pendingSessions.remove(session) ) {
            return;
        }
        User user = SocketServer.getUserBySession(session);
        Room room = user.getRoom();
        if( room != null ) {
            Game game = room.getGame();
            if( game != null && game.isThisGameRunningWithUser(user) ) {
                if( game.getGameThread().isAlive() ) {
                    game.getGameThread().interrupt();
                }
            }
        }
        String userName = user.getName();
        SocketServer.users.remove(user);
        if( SocketServer.waitingRoom.getUsers().remove(user) ) {
            SocketServer.sendToAllInWaitingRoom(userName + " left");
            return;
        }
        user.getRoom().kickUser(user);
        room.roomChangeNotify();
        SocketServer.sendToAllInRoom(room, userName + " left");
        SocketServer.roomsChangeNotifier.roomsChanged();
    }

    /**
     * Checking if given login already exists
     */
    private static boolean loginAttempt(String login) {
        for( int i=0 ; i<SocketServer.users.size() ; ++i ) {
            User user = SocketServer.users.get(i);
            if( user.getName() == null ) {
                continue;
            }
            if( user.getName().equals(login) ) {
                return false;
            }
        }
        return true;
    }
    
    public static void sendToSession(Session session, String data) {
        SocketServer.sendToSession(session, data, "message");
    }
    
    public static void sendToSession(Session session, String data, String type) {
        try {
            session.getBasicRemote().sendText(SocketServer.jsonEncode(type, data));
        } catch(IOException e) {
            System.out.println("*There was an error during sending message to session " + session);
        }
    }

    public static void sendToUser(User user, String data, String type) {
        try {
            if( !user.getSession().isOpen() ) {
                return;
            }
            user.getSession().getBasicRemote().sendText(SocketServer.jsonEncode(type, data));
        } catch(IOException e) {
            System.out.println("*There was an error during sending message to user " + user);
        }
    }

    public static void sendToUser(User user, String data) {
        SocketServer.sendToUser(user, data, "message");
    }

    public static void sendToAllInRoom(Room room, String data, String type) {
        for( int i=0 ; i<room.getUsers().size() ; ++i ) {
            SocketServer.sendToUser(room.getUsers().get(i), data, type);
        }
    }
    
    public static void sendToAllInRoom(Room room, String data) {
        SocketServer.sendToAllInRoom(room, data, "message");
    }

    public static void sendToAllInWaitingRoom(String data) {
        SocketServer.sendToAllInWaitingRoom(data, "message");
    }

    public static void sendToAllInWaitingRoom(String data, String type) {
        for( int i=0 ; i<SocketServer.waitingRoom.getUsers().size() ; ++i ) {
            SocketServer.sendToUser(SocketServer.waitingRoom.getUsers().get(i), data, type);
        }
    }
    
    private static String getCurrentTime() {
        DateTime dateTime = new DateTime();
        String[] timeSplit = dateTime.toString().split("T");
        String date = timeSplit[0];
        String time = timeSplit[1].split("\\.")[0];
        return "[" + date + " " + time + "]";
    }
    
    public static JsonObject JsonDecode(String jsonString) {
        JsonReader reader = Json.createReader(new StringReader(jsonString));
        JsonObject ob = reader.readObject();
        reader.close();
        return ob;
    }
    
    public static String jsonEncode(String type, String data) {
        JsonObject ob = Json.createObjectBuilder()
                .add("type", type)
                .add("data", data)
                .build();
        return ob.toString();
    }
    
    public static void log(String info) {
        System.out.println(info);
    }
    
    private static User getUserBySession(Session session) {
        for( int i=0 ; i<SocketServer.users.size() ; ++i ) {
            User user = SocketServer.users.get(i);
            if( user.getSession() == session ) {
                return user;
            }
        }
        return null;
    }
    
}
