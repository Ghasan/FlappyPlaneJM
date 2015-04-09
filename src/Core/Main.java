package Core;

import javax.microedition.lcdui.game.*;
import javax.microedition.lcdui.*;
import javax.microedition.media.Player;
import javax.microedition.media.Manager;
import java.util.Random;
import java.io.InputStream;
import javax.microedition.rms.RecordStore;
import java.util.Timer;

/**
 * 240w/320h is assumed for the screen size
 * @author Ghasan Al-Sakkaf
 */
public class Main extends GameCanvas implements Runnable {
    
    private TiledLayer background;
    private Sprite pipes[];
    private Sprite plane;
    private LayerManager lManager;
    
    private Sprite getReady;
    private Sprite gameOver;
    
    private Player win, lose;
    private InputStream winIs, loseIs;
    
    private TempPipe tempPipes;
    
    private Image backgroundImg;
    private Image planeImg;
    private Image[] pipesImgs;
    private Image gameOverImg;
    private Image getReadyImg;
    
    private Graphics graphics;
    
    private final int planeX = 40;
    private int planeY = 160;
    
    private int counter = 0;
    private int acc = 0;
    private int paceX, paceY;
    
    private Random random;
    private int temp;
    private int scoreCount;
    private byte pastScore;
    
    private Font font;
    
    private LayerManager startUi;
    private Sprite startItems[];
    private Image startImgs[];
    private int choice;
    
    private boolean pause;
    private boolean stopped;
    private boolean uiInitialized;
    private boolean startUiCanvas;
    private boolean destroy;
    
    private Midlet parent;
    private RecordStore record;
    private Clock clock;
    private Timer timer;
    
    private class TempPipe{
        Sprite pipe[];
        int pos[];
        boolean active[];
        int num;
        int latest, nearer;
        
        TempPipe(int n) {
           pipe = new Sprite[n*2];
           pos = new int[n];
           active = new boolean[n];
           num = n;
           latest = 0;
           nearer = 0;
        }
        
        int next() {
            for(int i = 0; i < num; ++i) {
                if(active[i] == false) return i;
            }
            return -1;
        }
        
        boolean clear(int n) {
           pipe = new Sprite[n*2];
           pos = new int[n];
           active = new boolean[n];
           num = n;
           latest = 0;
           nearer = 0;
           return true;
        }
    }
    
    public Main() {
        super(true);
    }
    
    public boolean Start(Midlet parent) {
        try {
            record = RecordStore.openRecordStore("score", true);
            if(record.getNumRecords() > 0) pastScore = (record.getRecord(1))[0];
            else {
                byte[] scores = {0};
                record.addRecord(scores, 0, 1);
                pastScore = 0;
            }
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }
        
        try {
            this.parent = parent;
            
            backgroundImg = Image.createImage("/Resources/back.png");
            planeImg = Image.createImage("/Resources/planesGreen.png");
            pipesImgs = new Image[8];
            
            pipesImgs[0] = Image.createImage("/Resources/u2.png");
            pipesImgs[1] = Image.createImage("/Resources/u3.png");
            pipesImgs[2] = Image.createImage("/Resources/u4.png");
            pipesImgs[3] = Image.createImage("/Resources/u5.png");
            pipesImgs[4] = Image.createImage("/Resources/d2.png");
            pipesImgs[5] = Image.createImage("/Resources/d3.png");
            pipesImgs[6] = Image.createImage("/Resources/d4.png");
            pipesImgs[7] = Image.createImage("/Resources/d5.png");
            
            getReadyImg = Image.createImage("/Resources/textGetReady.png");
            gameOverImg = Image.createImage("/Resources/textGameOver.png");
            
            background = new TiledLayer(1, 1, backgroundImg, 240, 320);
            background.setCell(0, 0, 1);
            
            plane = new Sprite(planeImg, 50, 41);
            plane.defineReferencePixel(24, 20);
            
            pipes = new Sprite[8];
            
            for(int i = 0; i < 8; ++i) {
                pipes[i] = new Sprite(pipesImgs[i], 64, 64 + (i % 4) * 32);
                if(i<4) pipes[i].defineReferencePixel(63, 0);
                else pipes[i].defineReferencePixel(63, 63 + (i % 4) * 32);
            }
            
            gameOver = new Sprite(gameOverImg, 165, 31);
            getReady = new Sprite(getReadyImg, 170, 31);
            gameOver.defineReferencePixel(82, 14);
            getReady.defineReferencePixel(84, 14);
            gameOver.setRefPixelPosition(119, 159);
            getReady.setRefPixelPosition(119, 100);
            
            winIs = getClass().getResourceAsStream("/Resources/gamewin.mp3");
            loseIs = getClass().getResourceAsStream("/Resources/gameover.mp3");
            
            win = Manager.createPlayer(winIs, "audio/mp3");
            lose = Manager.createPlayer(loseIs, "audio/mp3");
            
            startImgs = new Image[8];
            startImgs[0] = Image.createImage("/Resources/resume.png");
            startImgs[1] = Image.createImage("/Resources/resumes.png");
            startImgs[2] = Image.createImage("/Resources/level1.png");
            startImgs[3] = Image.createImage("/Resources/level1s.png");
            startImgs[4] = Image.createImage("/Resources/level2.png");
            startImgs[5] = Image.createImage("/Resources/level2s.png");
            startImgs[6] = Image.createImage("/Resources/stop.png");
            startImgs[7] = Image.createImage("/Resources/stops.png");
            
            startUi = new LayerManager();
            
            startItems = new Sprite[8];
            for(int i = 0, j = 1; i < startItems.length; ++i) {
                startItems[i] = new Sprite(startImgs[i], 100, 36);
                startItems[i].defineReferencePixel(49, 0);
                startUi.append(startItems[i]);
                if(i%2 != 0) {
                    startItems[i-1].setRefPixelPosition(120, (i-j)*36 + 72);
                    startItems[i].setRefPixelPosition(120, (i-j)*36 + 72);
                    ++j;
                }
            }
            
            graphics = getGraphics();
            lManager = new LayerManager();
            
            random = new Random();
            
            tempPipes = new TempPipe(2);
            startUiCanvas = true;
            choice = 0;
            pause = false;
            uiInitialized = false;
            scoreCount = 0;
            destroy = false;
            
            font = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            graphics.setColor(225, 225, 225);
            graphics.setFont(font);
            
            setFullScreenMode(true);
        }
        catch(Exception e) {
            System.out.print("Error initializing the canvas: " + e.getMessage());
            parent.notifyDestroyed();
            return false;
        }
        
        Thread gameThread = new Thread(this);
        gameThread.start();
        
        return true;
    }
    
    public void run() {
         clock = new Clock(30);
         timer = new Timer();
        
        if(destroy) return;
        
        while(true) {
            if(startUiCanvas) {
                checkUserInput();
                
                try {Thread.sleep(150);}
                catch(Exception e) { System.out.println(e.getMessage()); }
            }
            else {
                if(!pause && !stopped) verifyGameState();
                checkUserInput();
                if(!pause && !stopped) updateGame(graphics);
                try {
                    if(pause || stopped) {
                    Thread.sleep(30);
                    continue;
                }

                   Thread.sleep(30);
                   ++counter;
               }
               catch(java.lang.InterruptedException e) {
                   System.out.print("Error starting canvas thread: " + e.getMessage());
                   end();
               }
            }
        }
    }

    private void verifyGameState() {
        checkForCollision();
    }

    private void checkUserInput() {
        getKeys(getKeyStates());
    }
    
    private void updateGame(Graphics graphics) {
         plane.nextFrame();
         plane.setRefPixelPosition(planeX, planeY);
         
         int next = 0;
         
         if(counter < 60) {
             lManager.append(getReady);
             lManager.append(plane);
             lManager.append(background);             
             lManager.paint(graphics, 0, 0);
             flushGraphics();
             return;
         }
         
         if(counter == 60) {
             lManager.remove(getReady);
             clock.setTimer(30);
             timer.schedule(clock, 0, 1000);
         }
         
         lManager.append(plane);
         
         if(!tempPipes.active[0] && !tempPipes.active[1]) {
             next = (temp = random.nextInt() % 4) < 0 ? -temp : temp;
             tempPipes.pipe[0] = new Sprite(pipes[next]);
             tempPipes.pipe[1] = new Sprite(pipes[7 - next]);
             tempPipes.pipe[0].setRefPixelPosition(240 + 64, 0);
             tempPipes.pipe[1].setRefPixelPosition(240 + 64, 320);
             tempPipes.active[0] = true;
             lManager.append(tempPipes.pipe[0]);
             lManager.append(tempPipes.pipe[1]);
             lManager.append(background);
             tempPipes.nearer = 0;
             lManager.paint(graphics, 0, 0);
             flushGraphics();
             return;
         }
         
         for(int i = 0; i < tempPipes.num; ++i) {
             if(tempPipes.active[i] == true) {
                 tempPipes.pos[i] += paceX + acc;
                 tempPipes.pipe[2*i].setRefPixelPosition(240 + 64 - tempPipes.pos[i], 0);
                 tempPipes.pipe[2*i+1].setRefPixelPosition(240 + 64 - tempPipes.pos[i], 320);
             }
             
             if(tempPipes.pos[i] >= 240 + 64) {
                 tempPipes.pos[i] = 0;
                 tempPipes.active[i] = false;
                 lManager.remove(tempPipes.pipe[i*2]);
                 lManager.remove(tempPipes.pipe[i*2 + 1]);
                 tempPipes.pipe[i*2] = tempPipes.pipe[i*2+1] = null;
                 tempPipes.nearer = tempPipes.latest;
                 scoreCount++;
             }
             
             next = (temp = random.nextInt() % 4) < 0 ? -temp : temp;
             if(tempPipes.pos[i] >= 120 + 64 && tempPipes.latest == i) {
                 int j = tempPipes.next() * 2;
                 tempPipes.pipe[j] = new Sprite(pipes[next]);
                 tempPipes.pipe[j + 1] = new Sprite(pipes[7 - next]);
                 tempPipes.pipe[j].setRefPixelPosition(240 + 64, 0);
                 tempPipes.pipe[j + 1].setRefPixelPosition(240 + 64, 320);
                 tempPipes.latest = tempPipes.next();
                 tempPipes.active[tempPipes.next()] = true;
             }
         }
         
         for(int i = 0; i < tempPipes.num; ++i) {
             if(tempPipes.active[i]){
                 lManager.append(tempPipes.pipe[i*2]);
                 lManager.append(tempPipes.pipe[i*2+1]);
             }
         }
         
         lManager.append(background);
         lManager.paint(graphics, 0, 0);
         graphics.setColor(255, 255, 255);
         graphics.fillRect(0,0, 239, 30);
         graphics.fillRect(0,304, 239, 319);
         graphics.setColor(0, 0, 0);
         graphics.drawString("Score is: " + scoreCount, 239, 0, Graphics.TOP | Graphics.RIGHT);
         graphics.drawString("Time left: " + clock.getTimeLeft(), 239, 319, Graphics.BOTTOM | Graphics.RIGHT);
         
         flushGraphics();
         
    }

    private void getKeys(int keyStates) {
        acc = 0;
        
        if(startUiCanvas) {
            if(!uiInitialized) {
                for(int i = 0; i < startItems.length; ++i) {
                    startUi.append(startItems[i]);
                }
                
                if(pause) {
                    choice = 0;
                    startUi.append(startItems[0]);
                }
                else {
                    startUi.remove(startItems[0]);
                    startUi.remove(startItems[1]);
                    choice = 1;
                    startUi.append(startItems[2]);
                }
                
                uiInitialized = true;
                startUi.append(background);
                startUi.paint(graphics, 0, 0);
                flushGraphics();
                return;
            }
            
            if((keyStates & FIRE_PRESSED) != 0) {
                if(pause && choice == 0) {
                    pause = !pause;
                    startUiCanvas = false;
                    clock.resume();
                    return;
                }
                
                switch(choice) {
                    case 1: {
                        reInitializeGame(); paceX = 4; paceY = 4;
                        startUiCanvas = false; return;
                    }
                    case 2: {
                        reInitializeGame(); paceX = 6; paceY = 6;
                        startUiCanvas = false; return;
                    }
                    case 3: end(); return;
                    default: {
                        System.out.println("Error managing start ui.");
                        end();
                    }
                }
                
                return;
            }
            
            if((keyStates & DOWN_PRESSED) != 0) {
                startUi.append(startItems[choice * 2 + 1]);
                
                if(choice == 3) {
                    if(pause) choice = 0;
                    else choice = 1;
                }
                else choice++;
               
                startUi.append(startItems[choice*2]);
                
                return;
            }
            
            if((keyStates & UP_PRESSED) != 0) {
                startUi.append(startItems[choice * 2 + 1]);
                
                if(choice == 0 || (choice == 1 && !pause)) choice = 3;
                else choice--;
               
                startUi.append(startItems[choice*2]);
            }
            
            startUi.append(background);
            startUi.paint(graphics, 0, 0);
            flushGraphics();
            
            return;
        }
        
        if((keyStates & FIRE_PRESSED) != 0) {
            try{
                if(!stopped) {
                    pause = !pause;
                    clock.pause();
                    startUiCanvas = true;
                    uiInitialized = false;
                }
                else {
                    stopped = false;
                    startUiCanvas = true;
                    uiInitialized = false;
                    lManager.remove(gameOver);
                }
            }
            catch(Exception e) {
                System.out.println(e.getMessage());
            }
            
            return;
        }
        
        if((keyStates & DOWN_PRESSED) != 0) {
            planeY = Math.min(320 - 20, planeY + paceY);
        }
        if((keyStates & UP_PRESSED) != 0) {
            planeY = Math.max(20, planeY - paceY);
        }
        if((keyStates & RIGHT_PRESSED) != 0) {
            acc = 4;
        }
        if((keyStates & LEFT_PRESSED) != 0) {
            acc = -2;
        }
    }

    private void checkForCollision() {
        temp = clock.getTimeLeft();
        
        byte[] score = new byte[1];
        score[0] = (byte) scoreCount;
        
        try {
            if ((tempPipes.active[tempPipes.nearer] &&
                (plane.collidesWith(tempPipes.pipe[tempPipes.nearer*2], true) || 
                 plane.collidesWith(tempPipes.pipe[tempPipes.nearer*2 + 1], true)))
               )
            {
                
                lManager.insert(gameOver, 0);
                
                lManager.remove(plane);
                lManager.paint(graphics, 0, 0);
                flushGraphics();
                timer.cancel();
                stopped = true;
                lose.start();
                reInitializeGame();
            }
            else if (temp <= 0) {
                lManager.remove(plane);
                lManager.paint(graphics, 0, 0);
                graphics.setColor(255, 255, 255);
                graphics.fillRect(0,0, 240, 30);
                graphics.setColor(0, 0, 0);
                graphics.drawString("Score is: " + scoreCount, 239, 0, Graphics.TOP | Graphics.RIGHT);
                graphics.drawString("Highest score is: " + (pastScore > scoreCount ? pastScore : scoreCount), 0, 15, Graphics.TOP | Graphics.LEFT);
                graphics.setColor(255, 255, 255);
                if(pastScore < scoreCount) {
                    record.setRecord(1, score, 0, 1);
                    pastScore = (byte)scoreCount;
                }
                flushGraphics();
                timer.cancel();
                stopped = true;
                win.start();
                reInitializeGame();
            }
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private void reInitializeGame() {
        for(int i = 0; i < tempPipes.num * 2; ++i) {
            if(tempPipes.pipe[i] != null) lManager.remove(tempPipes.pipe[i]);
        }
        tempPipes.clear(2);
        planeY = 160;
        pause = false;
        scoreCount = 0;
        counter = 0;
        clock = new Clock(30);
        timer = new Timer();
    }
    
    private void end() {
        destroy = true;
        parent.destroyApp(true);
    }
}
