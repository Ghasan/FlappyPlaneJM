package Core;

import java.util.TimerTask;

/**
 * @author Ghasan Al-Sakkaf
 */
public class Clock extends TimerTask {

    private int seconds = 0;
    private int backup;
    
    public void run() {
        --seconds;
    }
    
    public Clock(int seconds) {
        this.seconds = seconds;
    }
    
    public int getTimeLeft() {
        return seconds;
    }
    
    public int setTimer(int sec) {
        return seconds = sec;
    }
    
    public void pause() {
        backup = seconds;
    }
    
    public void resume() {
        seconds = backup;
    }
    
}
