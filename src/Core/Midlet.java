package Core;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.*;

/**
 * @author Ghasan Al-Sakkaf
 */
public class Midlet extends MIDlet {
    Main main  = new Main();
        
    public void startApp() {
        Display currentDisplay = Display.getDisplay(this);
        
        main.Start(this);
        currentDisplay.setCurrent(main);
    }
    
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
        notifyDestroyed();
    }
}
