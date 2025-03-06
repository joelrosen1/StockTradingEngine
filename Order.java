import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a trading order in the matching engine.
 */
public class Order {
    /** true if this is a buy order, false for sell order */
    public final boolean isBuy;
    /** The security identifier this order applies to */
    public final int tickerId;
    /** Number of units to trade */
    public final int quantity;
    /** Price per unit for this order */
    public final double price;

    /** Atomic flag indicating if this order is active/open */
    private final AtomicBoolean active;

    /**
     * Creates a new trading order.
     * @param isBuy True for buy order, false for sell order
     * @param tickerId The security identifier
     * @param quantity Number of units to trade
     * @param price Price per unit 
     */
    public Order(boolean isBuy, int tickerId, int quantity, double price) {
        this.isBuy = isBuy;
        this.tickerId = tickerId;
        this.quantity = quantity;
        this.price = price;
        this.active = new AtomicBoolean(true);
    }

    /**
     * Checks if the order is still active/open.
     * @return True if the order is active, false if it's been filled/canceled
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * Deactivates the order.
     */
    public void deactivate() {
        active.set(false);
    }
}