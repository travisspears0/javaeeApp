package local.bb.websockets;

import java.util.Comparator;

public class UsersComparator implements Comparator<User> {

    private final int houseNumber;
    
    public UsersComparator(int houseNumber) {
        this.houseNumber = houseNumber;
    }
    
    @Override
    public int compare(User user1, User user2) {
        return new NumbersComparator(houseNumber)
                .compare(user1.getCurrentNumber(), user2.getCurrentNumber());
    }
    
}
