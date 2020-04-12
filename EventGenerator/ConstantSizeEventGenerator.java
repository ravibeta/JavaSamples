package events;
import org.apache.commons.text.RandomStringGenerator;

import java.util.UUID;

public class ConstantSizeEventGenerator implements EventGenerator {

    private int size;
    private int delayMillis;
    private RandomStringGenerator generator;

    public ConstantSizeEventGenerator() {
        this.size = 100;
        this.delayMillis = 0;
        this.generator = new RandomStringGenerator.Builder().build();
    }

    public int getSize(){
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getDelayMillis() {
        return this.delayMillis;
    }

    public void setDelayMillis(int delayMillis) {
        this.delayMillis = delayMillis;
    }

    @Override
    public GeneratedEvent nextEvent() {
        if (this.delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ex) {
                // Expected
            }
        }
        return new GeneratedEvent(UUID.randomUUID().toString(), generator.generate(size));
    }
}
