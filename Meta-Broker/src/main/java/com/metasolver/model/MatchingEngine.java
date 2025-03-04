package com.metasolver.model;
import java.util.*;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class MatchingEngine {
    private static final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);
    private final Map<String, Map<String, Double>> balances = new ConcurrentHashMap<>();
    private final List<Trade> executedTrades = new ArrayList<>();
    private ConcurrentSkipListMap<Double, List<Order>> buyOrders = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    private ConcurrentSkipListMap<Double, List<Order>> sellOrders = new ConcurrentSkipListMap<>();
    private final AtomicLong orderIdGenerator = new AtomicLong(0);
    private final Map<String, BigInteger> userBalances;
    private final List<Order> orderBook = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final int BATCH_SIZE = 1000;
    private final Queue<Order> orderQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queueSize = new AtomicInteger(0);

    // Inject SimpMessagingTemplate to broadcast messages
    private final SimpMessagingTemplate messagingTemplate;

    // Add at class level
    private final Set<String> loggedSelfTrades = new HashSet<>();

    @Autowired
    public MatchingEngine(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.userBalances = new ConcurrentHashMap<>();
    }

    public enum OrderType {
        MARKET, LIMIT
    }

    public enum OrderSide {
        BUY, SELL
    }

    @Getter
    @Setter
    public static class Order {
        private final long orderId;
        private final String traderId;
        private final OrderType type;
        private final OrderSide side;
        private final double price;
        private double quantity;
        private final LocalDateTime timestamp;
        private final boolean preventSelfTrade;

        public Order(long orderId, String traderId, OrderType type, OrderSide side, double price, double quantity, LocalDateTime timestamp) {
            this.orderId = orderId;
            this.traderId = traderId;
            this.type = type;
            this.side = side;
            this.price = price;
            this.quantity = quantity;
            this.timestamp = timestamp;
            this.preventSelfTrade = true; // Default to preventing self-trades
        }

        public boolean isPreventSelfTrade() { return preventSelfTrade; }

        public OrderSide getSide() {
            return side;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Trade {
        private final String buyer;
        private final String seller;
        private final double quantity;
        private final double price;
        private final LocalDateTime timestamp;
        private final OrderType buyType;
        private final OrderType sellType;

        public LocalDateTime getTimestamp() { return timestamp; }
        public String getBuyer() { return buyer; }
        public String getSeller() { return seller; }
        public double getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public OrderType getBuyType() { return buyType; }
        public OrderType getSellType() { return sellType; }
    }

    @Getter
    @Setter
    public static class OrderBookState {
        private List<Order> buyOrders = new ArrayList<>();
        private List<Order> sellOrders = new ArrayList<>();
        private List<Trade> recentTrades = new ArrayList<>();
        private Map<String, Map<String, Double>> balances = new HashMap<>();
    }

    public long submitOrder(String traderId, OrderType type, OrderSide side, 
                          double price, double quantity) {
        if (type == OrderType.MARKET) {
            price = (side == OrderSide.BUY) ? Double.MAX_VALUE : 0.0;
        }
        
        Order order = new Order(orderIdGenerator.incrementAndGet(), traderId, type, side, price, quantity, LocalDateTime.now());
        
        if (side == OrderSide.BUY) {
            matchBuyOrder(order);
        } else {
            matchSellOrder(order);
        }

        return order.orderId;
    }

    private void matchBuyOrder(Order buyOrder) {
        while (buyOrder.quantity > 0 && !sellOrders.isEmpty()) {
            Map.Entry<Double, List<Order>> bestSell = sellOrders.firstEntry();
            
            if (buyOrder.price >= bestSell.getKey()) {
                List<Order> ordersAtPrice = bestSell.getValue();
                Order sellOrder = ordersAtPrice.get(0);
                
                double matchedQuantity = Math.min(buyOrder.quantity, sellOrder.quantity);
                executeTrade(buyOrder, sellOrder, matchedQuantity, sellOrder.price);

                if (sellOrder.quantity == 0) {
                    ordersAtPrice.remove(sellOrder);
                    if (ordersAtPrice.isEmpty()) {
                        sellOrders.remove(bestSell.getKey());
                    }
                }
            } else {
                break;
            }
        }

        if (buyOrder.quantity > 0) {
            addToOrderBook(buyOrder, buyOrders);
        }
    }

    private void matchSellOrder(Order sellOrder) {
        while (sellOrder.quantity > 0 && !buyOrders.isEmpty()) {
            Map.Entry<Double, List<Order>> bestBuy = buyOrders.firstEntry();
            
            if (sellOrder.price <= bestBuy.getKey()) {
                List<Order> ordersAtPrice = bestBuy.getValue();
                Order buyOrder = ordersAtPrice.get(0);
                
                double matchedQuantity = Math.min(sellOrder.quantity, buyOrder.quantity);
                executeTrade(buyOrder, sellOrder, matchedQuantity, buyOrder.price);

                if (buyOrder.quantity == 0) {
                    ordersAtPrice.remove(buyOrder);
                    if (ordersAtPrice.isEmpty()) {
                        buyOrders.remove(bestBuy.getKey());
                    }
                }
            } else {
                break;
            }
        }

        if (sellOrder.quantity > 0) {
            addToOrderBook(sellOrder, sellOrders);
        }
    }

    private void addToOrderBook(Order order, ConcurrentSkipListMap<Double, List<Order>> orderBook) {
        orderBook.computeIfAbsent(order.price, k -> new ArrayList<>()).add(order);
        this.orderBook.add(order); // Add to main order history
    }

    private void executeTrade(Order buyOrder, Order sellOrder, double quantity, double price) {
        // Check for self-trade prevention
        if (buyOrder.isPreventSelfTrade() && buyOrder.traderId.equals(sellOrder.traderId)) {
            // Create a unique key for this self-trade pair
            String selfTradeKey = String.format("%s-%s-%s", 
                buyOrder.orderId, 
                sellOrder.orderId,
                buyOrder.traderId);
            
            // Only log if we haven't seen this pair before
            if (!loggedSelfTrades.contains(selfTradeKey)) {
                loggedSelfTrades.add(selfTradeKey);
                logger.debug("Skipping self-trade for user: {}", buyOrder.traderId);
            }
            return;
        }

        buyOrder.quantity -= quantity;
        sellOrder.quantity -= quantity;
        
        logger.info("Trade executed at {}: {} bought {} units from {} at price {} (Buy: {}, Sell: {})", 
            Instant.ofEpochMilli(buyOrder.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), 
            buyOrder.traderId, quantity, 
            sellOrder.traderId, price, buyOrder.type, sellOrder.type);
        
        updateBalances(buyOrder.traderId, "ETH", quantity);
        updateBalances(sellOrder.traderId, "ETH", -quantity);
        
        double usdcAmount = quantity * price;
        updateBalances(buyOrder.traderId, "USDC", -usdcAmount);
        updateBalances(sellOrder.traderId, "USDC", usdcAmount);
        
        Trade newTrade = new Trade(
            buyOrder.traderId,
            sellOrder.traderId,
            quantity,
            price,
            LocalDateTime.now(),
            buyOrder.type,
            sellOrder.type
        );
        
        executedTrades.add(newTrade);
        
        // Now broadcast this trade over /topic/matchedTrades
        broadcastMatchedTrade(newTrade);
    }

    // ---------------------------------------------------------------------------
    // Broadcast matched trades to WebSocket topic
    // ---------------------------------------------------------------------------
    private void broadcastMatchedTrade(Trade trade) {
        logger.info("‼️ Broadcasting trade: {}", trade);
        messagingTemplate.convertAndSend("/topic/matchedTrades", trade);
    }

    public void cancelOrder(long orderId, OrderSide side) {
        logger.debug("Cancelling order {} for side {}", orderId, side);
        ConcurrentSkipListMap<Double, List<Order>> orderBook = (side == OrderSide.BUY) ? buyOrders : sellOrders;
        
        for (List<Order> orders : orderBook.values()) {
            orders.removeIf(order -> order.orderId == orderId);
        }
        
        // Clean up empty price levels
        orderBook.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void displayOrderBook() {
        System.out.println("\nOrder Book:");
        System.out.println("Sell Orders:");
        sellOrders.descendingMap().forEach((price, orders) -> 
            logger.info("  {} Orders at {}: {} orders, quantity: {}", 
                orders.get(0).side, price, orders.size(), 
                orders.stream().mapToDouble(o -> o.quantity).sum()));
        
        System.out.println("Buy Orders:");
        buyOrders.forEach((price, orders) -> 
            System.out.printf("  Price %.2f: %d orders, total quantity: %.2f%n",
                price, orders.size(), orders.stream().mapToDouble(o -> o.quantity).sum()));
    }

    public void updateUserBalance(String userAddress, BigInteger newBalance) {
        userBalances.put(userAddress, newBalance);
        validateOpenPositions(userAddress);
    }
    
    private void validateOpenPositions(String userAddress) {
        BigInteger currentBalance = userBalances.get(userAddress);
        if (currentBalance.compareTo(BigInteger.ZERO) < 0) {
            logger.warn("User {} has insufficient balance: {}", userAddress, currentBalance);
            cancelAllOrders(userAddress);
        }
    }
    
    private void cancelAllOrders(String userAddress) {
        buyOrders.values().forEach(orders -> 
            orders.removeIf(order -> order.traderId.equals(userAddress)));
        sellOrders.values().forEach(orders -> 
            orders.removeIf(order -> order.traderId.equals(userAddress)));
    }
    
    public List<String> getActiveUsers() {
        // Return list of users with open positions or active orders
        return new ArrayList<>(userBalances.keySet());
    }

    public List<Order> getOrderHistory() {
        return new ArrayList<>(orderBook);
    }

    public OrderBookState getOrderBookState() {
        OrderBookState state = new OrderBookState();
        
        // Add all buy orders
        buyOrders.values().forEach(orders -> state.buyOrders.addAll(orders));
        
        // Add all sell orders
        sellOrders.values().forEach(orders -> state.sellOrders.addAll(orders));
        
        // Add recent trades (last 50)
        state.recentTrades = executedTrades.size() > 50 
            ? executedTrades.subList(executedTrades.size() - 50, executedTrades.size())
            : new ArrayList<>(executedTrades);
            
        // Add balances
        state.balances = new HashMap<>(balances);
        
        return state;
    }


    public void displayAllTrades() {
        logger.info("=== Complete Trade History ===");
        for (Trade trade : executedTrades) {
            logger.info("Trade at {}: Buyer {} bought {} units from {} at price {} (Buy: {}, Sell: {})", 
                trade.getTimestamp(),
                trade.getBuyer(),
                trade.getQuantity(),
                trade.getSeller(),
                trade.getPrice(),
                trade.getBuyType(),
                trade.getSellType()
            );
        }
        logger.info("=== Total Trades: {} ===", executedTrades.size());
    }

    public List<Trade> getAllTrades() {
        return new ArrayList<>(executedTrades);
    }

    public void submitOrderBatch(List<Order> orders) {
        executorService.submit(() -> {
            for (int i = 0; i < orders.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, orders.size());
                orders.subList(i, end).parallelStream().forEach(this::processOrder);
            }
        });
    }
    
    private void processOrder(Order order) {
        orderQueue.offer(order);
        queueSize.incrementAndGet();
        
        if (queueSize.get() >= BATCH_SIZE) {
            processQueuedOrders();
        }
    }
    
    private void processQueuedOrders() {
        List<Order> ordersToProcess = new ArrayList<>();
        Order order;
        while ((order = orderQueue.poll()) != null) {
            ordersToProcess.add(order);
            queueSize.decrementAndGet();
        }
        
        ordersToProcess.forEach(o -> {
            if (o.getSide() == OrderSide.BUY) {
                matchBuyOrder(o);
            } else {
                matchSellOrder(o);
            }
        });
    }

    public Map<String, Map<String, Double>> getBalances() {
        return new HashMap<>(balances);
    }

    public double getBalance(String traderId, String asset) {
        return balances
            .computeIfAbsent(traderId, k -> new ConcurrentHashMap<>())
            .getOrDefault(asset, asset.equals("ETH") ? 100.0 : 1000000.0); // Default initial balances
    }

    public void updateBalances(String traderId, String asset, double amount) {
        balances
            .computeIfAbsent(traderId, k -> new ConcurrentHashMap<>())
            .compute(asset, (k, v) -> (v == null ? 0.0 : v) + amount);
    }


}

