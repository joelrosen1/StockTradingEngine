import static org.junit.Assert.*;
import org.junit.Test;

/**
 * JUnit test suite for verifying the functionality of the MatchingEngine.
 * Tests cover order matching logic, price priority enforcement, and concurrent order handling.
 */
public class MatchingEngineTest {

    /**
     * Tests basic matching between buy and sell orders.
     * Verifies that a buy order with higher price than existing sell order
     * results in an immediate trade at the sell order's price.
     */
    @Test
    public void testBuyMatchesSell() {
        MatchingEngine engine = new MatchingEngine();

        // Add a SELL at price 100.0
        engine.addOrder(false, 10, 50, 100.0);

        // Add a BUY at price 110.0 -> Should match instantly
        engine.addOrder(true, 10, 50, 110.0);

        MatchingEngine.Trade[] trades = engine.getAllTrades();

        // We expect exactly 1 trade
        assertEquals(1, trades.length);
        assertEquals(10, trades[0].tickerId);
        assertEquals(100.0, trades[0].price, 0.000001);
        assertEquals(50, trades[0].quantity);
    }

    /**
     * Tests non-matching scenario where buy price is below sell price.
     * Verifies that no trade occurs when orders don't meet price requirements.
     */
    @Test
    public void testBuyDoesNotMatchSell() {
        MatchingEngine engine = new MatchingEngine();

        // Add a SELL at price 100.0
        engine.addOrder(false, 5, 50, 100.0);

        // Add a BUY at price 99.0 -> Should NOT match
        engine.addOrder(true, 5, 50, 99.0);

        MatchingEngine.Trade[] trades = engine.getAllTrades();

        // We expect no trades
        assertEquals(0, trades.length);
    }

    /**
     * Tests price priority for multiple buy orders against a single sell order.
     * Verifies that the highest-priced buy order that meets the sell price gets matched.
     */
    @Test
    public void testMultipleBuysOneSell() {
        MatchingEngine engine = new MatchingEngine();

        // Add multiple BUY orders for ticker 8
        engine.addOrder(true, 8, 100, 90.0);
        engine.addOrder(true, 8, 100, 95.0);
        engine.addOrder(true, 8, 100, 93.0);

        // Add a SELL at price 92.0, which should match with the highest buy >= 92.0
        engine.addOrder(false, 8, 100, 92.0);

        MatchingEngine.Trade[] trades = engine.getAllTrades();

        // We expect exactly 1 trade
        assertEquals(1, trades.length);
        assertEquals(8, trades[0].tickerId);
        assertEquals(92.0, trades[0].price, 0.000001);
        assertEquals(100, trades[0].quantity);
    }

    /**
     * Tests price priority for multiple sell orders against a single buy order.
     * Verifies that the lowest-priced sell order that meets the buy price gets matched.
     */
    @Test
    public void testMultipleSellsOneBuy() {
        MatchingEngine engine = new MatchingEngine();

        // Add multiple SELL orders for ticker 9
        engine.addOrder(false, 9, 50, 105.0);
        engine.addOrder(false, 9, 50, 103.0);
        engine.addOrder(false, 9, 50, 110.0);

        // Add a BUY at 104.0, which should match with the lowest sell (103.0)
        engine.addOrder(true, 9, 50, 104.0);

        MatchingEngine.Trade[] trades = engine.getAllTrades();
        assertEquals(1, trades.length);
        assertEquals(9, trades[0].tickerId);
        assertEquals(103.0, trades[0].price, 0.000001);
        assertEquals(50, trades[0].quantity);
    }

    /**
     * Tests concurrent order submission and matching.
     * Verifies thread safety by submitting orders from multiple threads
     * and checking for at least one valid match in overlapping price ranges.
     * 
     * @throws InterruptedException if thread joining is interrupted
     */
    @Test
    public void testConcurrentOrders() throws InterruptedException {
        final MatchingEngine engine = new MatchingEngine();

        // We'll spin up threads that randomly add buy/sell around a certain price range
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                engine.addOrder(false, 100, 10, 100 + (i % 5)); 
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                engine.addOrder(true, 100, 10, 102 + (i % 3));
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // We expect at least 1 match if there's overlap in the price ranges.
        MatchingEngine.Trade[] trades = engine.getAllTrades();
        assertTrue("Expected at least 1 match", trades.length >= 1);
    }
}