package local.bb.websockets;

public class Action {
    
    private final String name;
    private final ActionCallBack actionCallback;
    
    public Action(String name, ActionCallBack actionCallback) {
        this.name = name;
        this.actionCallback = actionCallback;
    }

    public String getName() {
        return name;
    }

    public ActionCallBack getActionCallback() {
        return actionCallback;
    }
    
    
    
}
