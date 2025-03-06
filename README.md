# Stock Trading Engine

A real-time stock trading engine that matches buy and sell orders.

## Features
- Supports 1,024 tickers (stocks)
- `addOrder` function to place buy/sell orders with:
  - Order Type (Buy/Sell)
  - Ticker Symbol
  - Quantity
  - Price
- Random order simulation for testing
- `matchOrder` function to match buy and sell orders:
  - Matches when buy price >= lowest sell price
  - Handles race conditions with lock-free data structures
  - O(n) time complexity for matching
- No use of dictionaries, maps, or similar data structures

## Usage
1. Create a `MatchingEngine` instance.
2. Use `addOrder` to place orders.
3. Orders are matched automatically based on price priority.

## Example
```java
MatchingEngine engine = new MatchingEngine();
engine.addOrder(true, 10, 100, 150.0);  // Buy order
engine.addOrder(false, 10, 50, 149.5);  // Sell order
