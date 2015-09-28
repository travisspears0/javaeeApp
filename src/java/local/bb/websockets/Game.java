package local.bb.websockets;

import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

public class Game extends NextId implements Runnable {
    
    public enum STATE {
        NEW,
        WAITING_FOR_USERS_ACTIONS,
        WAITING_FOR_NEXT_ROUND,
        FINISHED
    }
    private static final int HOUSE_MIN=10,HOUSE_MAX=30 ;
    private final int NUMBERS_SIZE;
    private static final int PICK_ACTION_WAIT_SECONDS = 3;
    private static final int NEXT_ROUND_WAIT_SECONDS = 1;
    private STATE state = STATE.NEW;
    private final Random random = new Random();
    private final Room room;
    private final Queue<Number> numbers = new LinkedBlockingQueue<>();
    private final int HOUSE = this.random.nextInt(HOUSE_MAX-HOUSE_MIN)+HOUSE_MIN+1;
    private int blindPlayerIndex = 0;
    private boolean running = false;
    private Thread gameThread;
    
    public Game(Room room) {
        this.room = room;
        this.NUMBERS_SIZE = this.room.getSlots()*2;
        for( int i=0 ; i<this.NUMBERS_SIZE ; ++i ) {
            this.numbers.add(new Number(this.getRandomNumber(), this.HOUSE));
        }
        this.gameThread = new Thread(this);
        gameThread.start();
    }
    
    @Override
    public void run() {
        this.running = true;
        try {
            for( int i=0 ; i<this.room.getUsers().size() ; ++i ) {
                this.room.getUsers().get(i).gameStarted();
            }
            this.room.roomChangeNotify();
            /*
                przebieg gry:
                -na poczatku jest losowana dosc duza liczba(zalezy jak duza) nazywana HOUSE
                -jest 10 liczb w kolejce, jak sie wezmie jedna to sie dorabia nast na koncu
                -zaczyna sie runda kazdy gracz na poczatku ma currentNumber=1
                -kazy z graczy typuje co teraz zrobi(3 opcje: +,*,pass,end)
                    +    -currentNumber += pierwsza liczba z kolejki(odpada wybrana liczba(idzie do gracza))
                    *    -currentNumber *= pierwsza liczba z kolejki(odpada wybrana liczba(do gracza) i nastepna(do kosza))
                    pass -ominiecie kolejki(jesli ktos da pass 3 razy, to ma end!!)(zadna liczba nie odpada)
                    end  -zakonczenie gry z obecnie posiadana liczba(zadna liczba nie odpada)
                    new  -...potem
                -kazy gracz ma ile sekund na zdecydowanie co robi inaczej pass(default)
                -kiedy wszyscy zdecyduja(lub nie) po kolei leca liczby z kolejki
            do wszystkich, ktorzy cos robia(+,*) i te liczby sie uzupelniaja na biezaco
                -w kazdej rundzie ktos inny jest jako pierwszy
                -celem gry jest osiagniecie liczby jak najblizszej liczbie HOUSE
                -ktokolwiek spali(przekroczy HOUSE) automatycznie konczy
                -najpierw są liczeni ci, ktorzy byli najblizej HOUSE z dolu(ponizej),
            potem ci z gory(ci, ktorzy ja przekroczyli)
            */
            //game loop begin
            while( this.state != Game.STATE.FINISHED ) {
                for( int i=0 ; i<this.room.getUsers().size() ; ++i ) {
                    User user = this.room.getUsers().get(i);
                    user.setBlind(false);
                    if( user.getAction() != User.ACTION.END ) {
                        user.setAction(null);
                    }
                }
                this.room.getUsers().get(this.nextBlind()).setBlind(true);
                this.state = STATE.WAITING_FOR_USERS_ACTIONS;
                this.room.roomChangeNotify();
                this.gameChangeNotify();
                for( int i=Game.PICK_ACTION_WAIT_SECONDS ; i>=0 ; --i ) {
                    SocketServer.sendToAllInRoom(Game.this.room, i+"/"+Game.PICK_ACTION_WAIT_SECONDS, "timer");
                    TimeUnit.SECONDS.sleep(1);
                }
                this.forceEndActionPicking();
                this.room.roomChangeNotify();
                //actions picked, setting numbers' colors etc
                Iterator<Number> it = this.numbers.iterator();
                for( int i=0 ; i<this.room.getUsers().size() && it.hasNext() ; ++i ) {
                    User user = this.room.getUsers().get((i+this.blindPlayerIndex)%this.room.getUsers().size());
                    User.ACTION action = user.getAction();
                    switch( action ) {
                        case ADD: {
                            it.next().setColor(user.getColor());
                            break;
                        }
                        case MULTIPLE: {
                            it.next().setColor(user.getColor());
                            it.next().setColor(Number.DEFAULT_COLOR.BLACK.toString());
                            break;
                        }
                        case NEXT: {
                            if( !user.nextOnNumbersStack() ) {
                                user.setAction(User.ACTION.END, true);
                                break;
                            }
                            user.setCurrentNumber(1);
                            break;
                        }
                    }
                }
                //waiting for next round
                //(let users see numbers' colors and maybe perform some animations on client)
                this.state = Game.STATE.WAITING_FOR_NEXT_ROUND;
                this.room.roomChangeNotify();
                this.gameChangeNotify();
                for( int i=Game.NEXT_ROUND_WAIT_SECONDS ; i>=0 ; --i ) {
                    SocketServer.sendToAllInRoom(Game.this.room, i+"/"+Game.NEXT_ROUND_WAIT_SECONDS, "timer");
                    TimeUnit.SECONDS.sleep(1);
                }
                //spreading points etc.
                for( int i=0 ; i<this.room.getUsers().size() ; ++i ) {
                    User user = this.room.getUsers().get((i+this.blindPlayerIndex)%this.room.getUsers().size());
                    User.ACTION action = user.getAction();
                    if( action == User.ACTION.END ) {
                        continue;
                    }
                    if( action == User.ACTION.PASS || action == User.ACTION.NEXT ) {
                    } else {
                        int currentNumber = user.getCurrentNumber() ;
                        String oldNumbers = this.numbers.toString();
                        Number numberTaken = this.takeNumber();
                        String actionString = user.getName() +" -> "+ currentNumber;
                        if( action == User.ACTION.ADD ) {
                            user.setCurrentNumber(currentNumber+numberTaken.getValue());
                            actionString += "+" ;
                        } else if( action == User.ACTION.MULTIPLE ) {
                            user.setCurrentNumber(currentNumber*numberTaken.getValue());
                            actionString += "*" ;
                            this.takeNumber();
                        }
                        actionString += numberTaken.getValue() + "=" + user.getCurrentNumber();
                        SocketServer.sendToAllInRoom(this.room, actionString);
                    }
                    //user.setAction(null);
                    user.setActionLocked(false);
                    if( user.getCurrentNumber() >= this.HOUSE ) {
                        user.setAction(User.ACTION.END);
                    }
                }
                //deciding if it is the end of this game
                this.state = STATE.FINISHED;
                for( int i=0 ; i<this.room.getUsers().size() ; ++i ) {
                    if( this.room.getUsers().get(i).getAction() != User.ACTION.END ) {
                        this.state = STATE.WAITING_FOR_USERS_ACTIONS;
                        break;
                    }
                }
            }
            //game loop end
        } catch(InterruptedException e) {
            SocketServer.log("Game #" + this.id + " interrupted");
        } finally {
            this.finish();
            this.room.roomChangeNotify();
            this.gameChangeNotify();
            this.running = false;
        }
    }
    
    public boolean isThisGameRunningWithUser(User user) {
        return !(!this.running || !this.room.getUsers().contains(user));
    }
    
    public void gameChangeNotify() {
        SocketServer.sendToAllInRoom(this.room, this.toString(), "gameState");
    }
    
    private void forceEndActionPicking() {
        for( int i=0 ; i<this.room.getUsers().size() ; ++i ) {
            User user = this.room.getUsers().get(i);
            if( user.getAction() == null ) {
                user.setAction(User.ACTION.PASS);
            }
            if( user.getAction() == User.ACTION.PASS ) {
                user.setPassCounter(user.getPassCounter()+1);
            } else {
                user.setPassCounter(0);
            }
            if( user.getPassCounter() >= 3 ) {
                user.setAction(User.ACTION.END);
            }
            user.setActionLocked(true);
        }
    }
    
    public synchronized void userPickedAction() {
        for( int i=0 ; i<this.room.getUsers().size() ; ++i ) {
            if( this.room.getUsers().get(i).getAction() == null ) {
                return;
            }
        }
        this.notifyAll();
    }
    
    public Number takeNumber() {
        this.numbers.add(new Number(this.getRandomNumber(), this.HOUSE));
        return this.numbers.poll();
    }
    
    @Override
    public String toString() {
        JSONObject ob = new JSONObject();
        try {
            ob.put("status", this.state);
            ob.put("house", this.HOUSE);
            JSONObject numbersObject = new JSONObject();
            int i=0;
            Iterator<Number> it = this.numbers.iterator();
            while( it.hasNext() ) {
                numbersObject.put(i+"", it.next().toString());
                ++i;
            }
            ob.put("numbers", numbersObject.toString());
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return ob.toString();
    }
    
    private void finish() {
        this.state = STATE.FINISHED;
        for( int i=0 ; i<this.room.getUsers().size() ; ++i ) {
            this.room.getUsers().get(i).gameEnded();
        }
        //counting points
        for( User user : this.room.getUsers() ) {
            user.setCurrentNumber(user.getBestResultFromStack(HOUSE));
        }
        UsersComparator usersComparator = new UsersComparator(HOUSE);
        for( int i=0 ; i<this.room.getUsers().size() ; ++i ) {
            User user = this.room.getUsers().get(i);
            user.setCurrentNumber(user.getBestResultFromStack(HOUSE));
            int receivedPoints = 0;
            for( User u : this.room.getUsers() ) {
                if( user != u && usersComparator.compare(user, u) == 1 ) {
                    receivedPoints += 2;
                }
            }
            user.setPoints(user.getPoints() + receivedPoints);
        }
        this.room.setGame(null);
    }

    public Game.STATE getState() {
        return state;
    }
    
    private int getRandomNumber() {
        return this.random.nextInt(9)+1;
    }

    public Room getRoom() {
        return room;
    }

    public int getBlindPlayerIndex() {
        return blindPlayerIndex;
    }
    
    private int nextBlind() {
        int newBlind = this.blindPlayerIndex;
        int safeCounter = 0 ;//prevents infinite loop
        while( newBlind == this.blindPlayerIndex || 
                this.room.getUsers().get(newBlind).getAction() == User.ACTION.END ) {
            ++newBlind;
            newBlind %= this.room.getUsers().size();
            if( safeCounter++ >= this.room.getUsers().size() ) {
                return this.blindPlayerIndex;
            }
        }
        this.blindPlayerIndex = newBlind;
        return this.blindPlayerIndex;
    }

    public Thread getGameThread() {
        return gameThread;
    }
}
