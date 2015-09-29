package local.bb.websockets;

import org.json.JSONException;
import org.json.JSONObject;

public class Number {
    
    public enum DEFAULT_COLOR {
        BLACK,      //to be removed in next round
        WHITE       //not taken, not to be removed
    }
    private String color = DEFAULT_COLOR.WHITE.toString();
    private int value;
    private final int houseNumber;
    
    public Number(int value, int houseNumber) {
        this.value = value;
        this.houseNumber = houseNumber;
    }
    
    @Override
    public String toString() {
        JSONObject ob = new JSONObject();
        try {
            ob.put("value", Integer.toString(this.value));
            ob.put("color", this.color);
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return ob.toString();
    }
    
    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
    
}
