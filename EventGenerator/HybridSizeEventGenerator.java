package events;
import org.apache.commons.text.RandomStringGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class HybridSizeEventGenerator implements EventGenerator {

    private List<Integer> sizes;
    private Integer delayMillis;
    private Random random;
    private RandomStringGenerator generator;

    public HybridSizeEventGenerator() {
        this.sizes = new ArrayList<Integer>();
        this.delayMillis = 0;
        this.random = new Random();
        this.generator = new RandomStringGenerator.Builder().build();
    }

    public List<Integer> getSize(){
        return sizes;
    }

    public void setSize(Integer size) {
        if (this.sizes == null) {
            this.sizes = new ArrayList<Integer>();
        }
        this.sizes.add(size);
    }

    public Integer getDelayMillis() {
        return this.delayMillis;
    }

    public void setDelayMillis(Integer delayMillis) {
        this.delayMillis = delayMillis;
    }

    @Override
    public GeneratedEvent nextEvent() {
        if (this.sizes.size() == 0) {
            throw new IllegalStateException("data size needs to be specified.");
        }
        if (this.sizes.size() == 1) {
            throw new IllegalStateException("at least a small and large event must be specified.");
        }
        if (this.delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ex) {
                // Expected
            }
        }
        Integer index = random.nextInt(this.sizes.size());
        return new GeneratedEvent(UUID.randomUUID().toString(), generator.generate(sizes.get(index)));
    }
}