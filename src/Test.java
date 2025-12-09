
import processing.core.PApplet;

public class Test extends PApplet {
    public void setup() {

    }

    public void draw() {
        print("s");
    }

    public void settings() {
        size(400, 400);
    }

    static public void main(String[] passedArgs) {
//        String[] appletArgs = new String[] { "com.jiakii.processtest.Test" };
        PApplet.main("Test");

    }
}

