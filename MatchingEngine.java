import java.util.concurrent.atomic.AtomicInteger;

/**
 * A matching engine for handling the execution of buy and sell orders for various securities (tickers).
 * This engine matches orders based on price priority, executing trades when a buy order's price meets
 * or exceeds a sell order's price. It supports multiple tickers and maintains a record of executed trades.
 */
public class MatchingEngine {
    /** Maximum number of supported tickers */
    private static final int MAX_TICKERS = 1024;
    /** Maximum number of orders per ticker for both buy and sell sides */
    private static final int MAX_ORDERS_PER_TICKER = 10000;

    private final Order[][] buyOrders;
    private final AtomicInteger[] buyCount;
    private final Order[][] sellOrders;
    private final AtomicInteger[] sellCount;

    /**
     * Represents a trade executed by the matching engine.
     */
    public static class Trade {
        /** The ID of the ticker (security) associated with this trade */
        public final int tickerId;
        /** The price at which the trade was executed */
        public final double price;
        /** The quantity of shares/units traded */
        public final int quantity;

        /**
         * Constructs a new Trade instance.
         * @param tickerId The ID of the ticker/security
         * @param price The execution price per unit
         * @param quantity The number of units traded
         */
        public Trade(int tickerId, double price, int quantity) {
            this.tickerId = tickerId;
            this.price = price;
            this.quantity = quantity;
        }
    }

    /** Maximum number of trades to store in history */
    private static final int MAX_TRADES = 10000;
    private final Trade[] tradesExecuted;
    private final AtomicInteger tradeCount;

    /**
     * Initializes a new MatchingEngine with empty order books and trade history.
     * Prepares data structures for all supported tickers up to MAX_TICKERS.
     */
    public MatchingEngine() {
        buyOrders = new Order[MAX_TICKERS][MAX_ORDERS_PER_TICKER];
        sellOrders = new Order[MAX_TICKERS][MAX_ORDERS_PER_TICKER];
        
        buyCount = new AtomicInteger[MAX_TICKERS];
        sellCount = new AtomicInteger[MAX_TICKERS];
        for (int i = 0; i < MAX_TICKERS; i++) {
            buyCount[i] = new AtomicInteger(0);
            sellCount[i] = new AtomicInteger(0);
        }

        tradesExecuted = new Trade[MAX_TRADES];
        tradeCount = new AtomicInteger(0);
    }

    /**
     * Adds a new order to the matching engine and triggers immediate matching attempts.
     * @param isBuy True for buy order, false for sell order
     * @param tickerId The security identifier (0 <= tickerId < MAX_TICKERS)
     * @param quantity Number of units to trade (must be positive)
     * @param price Price per unit (must be non-negative)
     */
    public void addOrder(boolean isBuy, int tickerId, int quantity, double price) {
        if (tickerId < 0 || tickerId >= MAX_TICKERS) {
            return;
        }

        Order newOrder = new Order(isBuy, tickerId, quantity, price);

        if (isBuy) {
            int index = buyCount[tickerId].getAndIncrement();
            if (index < MAX_ORDERS_PER_TICKER) {
                buyOrders[tickerId][index] = newOrder;
            } else {
                System.err.println("Buy orders for ticker " + tickerId + " are full!");
                buyCount[tickerId].decrementAndGet();
            }
        } else {
            int index = sellCount[tickerId].getAndIncrement();
            if (index < MAX_ORDERS_PER_TICKER) {
                sellOrders[tickerId][index] = newOrder;
            } else {
                System.err.println("Sell orders for ticker " + tickerId + " are full!");
                sellCount[tickerId].decrementAndGet();
            }
        }

        matchOrders(tickerId);
    }

    private void matchOrders(int tickerId) {
        int currentSellCount = sellCount[tickerId].get();
        int currentBuyCount = buyCount[tickerId].get();

        double lowestSellPrice = Double.MAX_VALUE;
        int lowestSellIndex = -1;
        for (int i = 0; i < currentSellCount; i++) {
            Order s = sellOrders[tickerId][i];
            if (s != null && s.isActive()) {
                if (s.price < lowestSellPrice) {
                    lowestSellPrice = s.price;
                    lowestSellIndex = i;
                }
            }
        }

        if (lowestSellIndex == -1) return;

        double bestBuyPrice = -1;
        int bestBuyIndex = -1;
        for (int i = 0; i < currentBuyCount; i++) {
            Order b = buyOrders[tickerId][i];
            if (b != null && b.isActive()) {
                if (b.price >= lowestSellPrice && b.price > bestBuyPrice) {
                    bestBuyPrice = b.price;
                    bestBuyIndex = i;
                }
            }
        }

        if (bestBuyIndex == -1) return;

        Order sellOrder = sellOrders[tickerId][lowestSellIndex];
        Order buyOrder = buyOrders[tickerId][bestBuyIndex];
        if (sellOrder != null && sellOrder.isActive() &&
            buyOrder  != null && buyOrder.isActive() &&
            buyOrder.price >= sellOrder.price) {

            int matchedQuantity = Math.min(buyOrder.quantity, sellOrder.quantity);
            captureTrade(tickerId, sellOrder.price, matchedQuantity);

            sellOrder.deactivate();
            buyOrder.deactivate();
        }
    }

    private void captureTrade(int tickerId, double price, int quantity) {
        int idx = tradeCount.getAndIncrement();
        if (idx < MAX_TRADES) {
            tradesExecuted[idx] = new Trade(tickerId, price, quantity);
        } else {
            System.err.println("Max trades reached—cannot store more trade data.");
        }
    }

    /**
     * Retrieves all executed trades since engine initialization.
     * @return A copy of the trade history array
     */
    public Trade[] getAllTrades() {
        int size = tradeCount.get();
        Trade[] copy = new Trade[size];
        for (int i = 0; i < size; i++) {
            copy[i] = tradesExecuted[i];
        }
        return copy;
    }
}