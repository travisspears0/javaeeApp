package local.bb.websockets;

import java.util.Comparator;

public class NumbersComparator implements Comparator<Integer> {
    
    private final int houseNumber;
    
    public NumbersComparator(int houseNumber) {
        this.houseNumber = houseNumber;
    }

    @Override
    public int compare(Integer num1, Integer num2) {
        int number1 = num1 - this.houseNumber;
        int number2 = num2 - this.houseNumber;
        if( number1 == number2 ) {
            return 0;
        }
        if( number1*number2 > 0 ) {
            return ( number1 > number2 ) ? 1 : -1 ;
        } else if( number1*number2 < 0 ) {
            if( number1 < 0 ) {
                return 1;
            }
            return -1;
        } else {
            if( number1 == 0 ) {
                return 1;
            }
            return -1;
        }
    }
}
