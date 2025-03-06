import java.util.Random;

/**
 * A simulation driver that stress-tests the MatchingEngine with concurrent random orders.
 * Creates multiple threads to generate buy/sell orders for different tickers with randomized
 * parameters, demonstrating the engine's behavior under concurrent load.
 */
public class RandomDriver {
    /**
     * Main simulation entry point. Creates a MatchingEngine and 5 worker threads that each
     * submit 100 randomized orders with short delays between submissions.
     * 
     * @param args Command-line arguments (not used in this simulation)
     */
    public static void main(String[] args) {
        MatchingEngine engine = new MatchingEngine();
        Random rand = new Random();

        for (int t = 0; t < 5; t++) {
            Thread worker = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    boolean isBuy = rand.nextBoolean();
                    int tickerId  = rand.nextInt(1024);  
                    int quantity  = 1 + rand.nextInt(100);  
                    double price  = 50.0 + rand.nextInt(50);

                
                    engine.addOrder(isBuy, tickerId, quantity, price);

                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                }
            });
            worker.start();
        }
    }
}