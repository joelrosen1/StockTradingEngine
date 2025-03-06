import java.util.concurrent.atomic.AtomicInteger;

/**
 * A matching engine for matching stock buys and stock sells
 */
public class MatchingEngine {
    /** Maximum number of supported tickers */
    private static final int MAX_TICKERS = 1024;
    /** Maximum number of orders per ticker*/
    private static final int MAX_ORDERS_PER_TICKER = 10000;

    private final Order[][] buyOrders;
    private final AtomicInteger[] buyCount;
    private final Order[][] sellOrders;
    private final AtomicInteger[] sellCount;

    /**
     * Represents a trade executed by the matching engine.
     */
    public static class Trade {
        /** ID of the ticker associated with this trade */
        public final int tickerSymbol;
        /** Price at which the trade was executed */
        public final double price;
        /** Quantity of shares traded */
        public final int quantity;

        /**
         * Creates a new Trade instance.
         * @param tickerSymbol The ID of the ticker
         * @param price The execution price per stock
         * @param quantity The number of units traded
         */
        public Trade(int tickerSymbol, double price, int quantity) {
            this.tickerSymbol = tickerSymbol;
            this.price = price;
            this.quantity = quantity;
        }
    }

    /** Maximum number of trades to store in history */
    private static final int MAX_TRADES = 10000;
    private final Trade[] tradesExecuted;
    private final AtomicInteger tradeCount;

    /**
     * Initializes a new MatchingEngine with empty orders and trade history.
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
     * Adds a new order to the matching engine and starts immediate matching attempts.
     * @param isBuy True for buy order, false for sell order
     * @param tickerSymbol The security identifier
     * @param quantity Number of stocks to trade
     * @param price Price per unit
     */
    public void addOrder(String orderType, int tickerSymbol, int quantity, double price) {
        if (tickerSymbol < 0 || tickerSymbol >= MAX_TICKERS) {
            return;
        }
    
        boolean isBuy = orderType.equalsIgnoreCase("Buy");
        Order newOrder = new Order(isBuy, tickerSymbol, quantity, price);
    
        if (isBuy) {
            int index = buyCount[tickerSymbol].getAndIncrement();
            if (index < MAX_ORDERS_PER_TICKER) {
                buyOrders[tickerSymbol][index] = newOrder;
            } else {
                System.err.println("Buy orders for ticker " + tickerSymbol + " are full!");
                buyCount[tickerSymbol].decrementAndGet();
            }
        } else {
            int index = sellCount[tickerSymbol].getAndIncrement();
            if (index < MAX_ORDERS_PER_TICKER) {
                sellOrders[tickerSymbol][index] = newOrder;
            } else {
                System.err.println("Sell orders for ticker " + tickerSymbol + " are full!");
                sellCount[tickerSymbol].decrementAndGet();
            }
        }
    
        matchOrder(tickerSymbol);
    }

    private void matchOrder(int tickerSymbol) {
        int currentSellCount = sellCount[tickerSymbol].get();
        int currentBuyCount = buyCount[tickerSymbol].get();

        double lowestSellPrice = Double.MAX_VALUE;
        int lowestSellIndex = -1;
        for (int i = 0; i < currentSellCount; i++) {
            Order s = sellOrders[tickerSymbol][i];
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
            Order b = buyOrders[tickerSymbol][i];
            if (b != null && b.isActive()) {
                if (b.price >= lowestSellPrice && b.price > bestBuyPrice) {
                    bestBuyPrice = b.price;
                    bestBuyIndex = i;
                }
            }
        }

        if (bestBuyIndex == -1) return;

        Order sellOrder = sellOrders[tickerSymbol][lowestSellIndex];
        Order buyOrder = buyOrders[tickerSymbol][bestBuyIndex];
        if (sellOrder != null && sellOrder.isActive() &&
            buyOrder  != null && buyOrder.isActive() &&
            buyOrder.price >= sellOrder.price) {

            int matchedQuantity = Math.min(buyOrder.quantity, sellOrder.quantity);
            captureTrade(tickerSymbol, sellOrder.price, matchedQuantity);

            sellOrder.deactivate();
            buyOrder.deactivate();
        }
    }

    private void captureTrade(int tickerSymbol, double price, int quantity) {
        int idx = tradeCount.getAndIncrement();
        if (idx < MAX_TRADES) {
            tradesExecuted[idx] = new Trade(tickerSymbol, price, quantity);
        } else {
            System.err.println("Max trades reached—cannot store more trade data.");
        }
    }

    /**
     * Retrieves all executed trades
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