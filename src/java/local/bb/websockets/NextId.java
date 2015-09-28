package local.bb.websockets;

public class NextId {
    
    private static int CURRENT_ID = 0;
    protected final int id = CURRENT_ID++;

    public int getId() {
        return id;
    }
    
}
