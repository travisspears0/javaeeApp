package local.bb.websockets;

import javax.websocket.Session;
import org.json.JSONException;
import org.json.JSONObject;

public class User {
    
    public static final String[] colors = {
        "#FF0000",  //red
        "#00FF00",  //green
        "#0000FF",  //blue
        "#990099",  //purple
        "#FF9900",  //orange
        "#996600",  //brown
        "#FF0099",  //pink
        "#FFFF33"   //yellow
    };
    public enum STATE {
        NOT_READY,
        READY
    }
    public enum GAME_STATE {
        PROCESSING
    }
    public enum ACTION {
        ADD,
        MULTIPLE,
        NEXT,
        PASS,
        END
    }
    private final Session session;
    private static int CURRENT_ID = 0;
    private final int id = User.CURRENT_ID++;
    private String name;
    private Room room = null;
    private int points = 0;
    private int currentNumber = 0;
    private User.STATE state = STATE.NOT_READY;
    private User.ACTION action = null;
    private User.GAME_STATE gameState = null;
    private boolean actionLocked = false;
    private int passCounter=0;
    private String color;
    private final int[] numbersStack = new int[3];
    private int numbersStackIndex = 0;
    private boolean blind = false;
    
    public User(Session session, String name) {
        for( int i=0 ; i<this.numbersStack.length ; ++i ) {
            this.numbersStack[i] = 0;
        }
        this.session = session;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }

    public Session getSession() {
        return session;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
    
    @Override
    public String toString() {
        JSONObject ob = new JSONObject();
        try {
            ob.put("name", this.name);
            ob.put("state", this.state);
            ob.put("points", this.points);
            ob.put("currentNumer", this.currentNumber);
            ob.put("action", this.action);
            ob.put("color", this.color);
            ob.put("blind",this.blind);
            String numbersStackString = "";
            for( int i : this.numbersStack ) {
                numbersStackString += i + ",";
            }
            numbersStackString = 
                    numbersStackString.substring(0, numbersStackString.length()-1);
            ob.put("numbersStack", numbersStackString);
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return ob.toString();
    }
    
    public void gameStarted() {
        if( this.state != User.STATE.READY ) {
            return;
        }
        this.gameState = GAME_STATE.PROCESSING;
        this.setCurrentNumber(1);
        for( int i=0 ; i<this.numbersStack.length ; ++i ) {
            this.numbersStack[i] = 0;
        }
        this.numbersStackIndex = 0;
        this.setCurrentNumberOnStack(this.getCurrentNumber());
    }
    
    public void gameEnded() {
        this.action = null;
        this.actionLocked = false;
        this.state = STATE.NOT_READY;
        this.gameState = null;
        this.passCounter = 0;
    }

    public ACTION getAction() {
        return action;
    }

    public boolean setAction(ACTION action, boolean force) {
        if( this.actionLocked && !force ) {
            return false;
        }
        this.action = action;
        return true;
    }
    
    public boolean setAction(ACTION action) {
        return this.setAction(action, false);
    }
    
    public User.STATE getState() {
        return this.state;
    }

    public void setState(STATE state) {
        this.state = state;
    }

    public GAME_STATE getGameState() {
        return gameState;
    }

    public void setGameState(GAME_STATE gameState) {
        this.gameState = gameState;
    }

    public boolean isActionLocked() {
        return actionLocked;
    }

    public void setActionLocked(boolean actionLocked) {
        this.actionLocked = actionLocked;
    }

    public int getCurrentNumber() {
        return currentNumber;
    }

    public void setCurrentNumber(int currentNumber) {
        this.currentNumber = currentNumber;
        this.setCurrentNumberOnStack(this.currentNumber);
    }

    public int getPassCounter() {
        return passCounter;
    }

    public void setPassCounter(int passCounter) {
        this.passCounter = passCounter;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
    
    public boolean nextOnNumbersStack() {
        try {
            if( this.numbersStack[numbersStackIndex+1] == 0 ) {
                ++this.numbersStackIndex;
                return true;
            }
            return false;
        } catch(ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }
    
    public void setCurrentNumberOnStack(int value) {
        this.numbersStack[this.numbersStackIndex] = value;
    }

    public int getNumbersStackIndex() {
        return numbersStackIndex;
    }

    public void setNumbersStackIndex(int numbersStackIndex) {
        this.numbersStackIndex = numbersStackIndex;
    }

    public boolean isBlind() {
        return blind;
    }

    public void setBlind(boolean blind) {
        this.blind = blind;
    }
    
    public int getBestResultFromStack(int house) {
        NumbersComparator comparator = new NumbersComparator(house);
        int result = 0;
        for( int i=0 ; i<this.numbersStack.length ; ++i ) {
            if( numbersStack[i] == 0 ) {
                continue;
            }
            if( comparator.compare(numbersStack[i], result) == 1 ) {
                result = this.numbersStack[i];
            }
        }
        return result;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
    
}
