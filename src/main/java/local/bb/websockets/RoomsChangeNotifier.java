package local.bb.websockets;

public class RoomsChangeNotifier implements Runnable {
    
    private boolean roomsChanged = false;
    private final RoomsManager roomsManager;
    
    public RoomsChangeNotifier(RoomsManager roomsManager) {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
        this.roomsManager = roomsManager;
    }
    
    @Override
    public void run() {
        try {
            while( !Thread.interrupted() ) {
                this.waitForChange();
                SocketServer.sendToAllInWaitingRoom(this.roomsManager.toString(),"rooms");
                this.roomsChanged = false;
            }
        } catch( InterruptedException e ) {
            e.printStackTrace();
        }
    }
    
    private synchronized void waitForChange() throws InterruptedException {
        while( !this.roomsChanged ) {
            this.wait();
        }
    }
    
    public synchronized void roomsChanged() {
        this.roomsChanged = true;
        this.notifyAll();
    }
    
}
