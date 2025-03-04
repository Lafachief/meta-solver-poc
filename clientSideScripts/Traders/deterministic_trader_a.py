from exchange_client import ExchangeClient
import websocket
import json
import logging
import os
import time
import threading
from decimal import Decimal, ROUND_DOWN

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class PriceTracker:
    def __init__(self):
        self.current_price = None
        self.last_update = 0
        self.ws = None
        
    def on_message(self, ws, message):
        data = json.loads(message)
        if 'p' in data:
            self.current_price = float(data['p'])
            self.last_update = time.time()
        
    def start(self):
        self.ws = websocket.WebSocketApp(
            "wss://stream.binance.com:9443/ws",
            on_message=self.on_message,
            on_open=lambda ws: ws.send(json.dumps({
                "method": "SUBSCRIBE",
                "params": ["ethusdt@trade"],
                "id": 1
            }))
        )
        wst = threading.Thread(target=self.ws.run_forever)
        wst.daemon = True
        wst.start()

def execute_trade_sequence(client, base_price, quantity=0.1):
    """Execute matching trades for Trader A's sequence"""
    
    # First 10 trades at market price - place sell orders
    logger.info(f"Starting 10 trades at current ETH price: ${base_price:.2f}")
    for i in range(10):
        client.place_order(
            side="SELL",
            price=base_price,
            quantity=quantity,
            prevent_self_trade=True
        )
        logger.info(f"Trade {i+1}/10: SELL {quantity:.4f} ETH @ ${base_price:.2f}")
        time.sleep(1)
    
    # Sell at 10% above market to Trader A
    higher_price = base_price * 1.1
    logger.info(f"Selling at 10% above market: ${higher_price:.2f}")
    client.place_order(
        side="SELL",
        price=higher_price,
        quantity=quantity,
        prevent_self_trade=True
    )
    
    # Buy at market price from Trader A
    logger.info(f"Buying at market price: ${base_price:.2f}")
    client.place_order(
        side="BUY",
        price=base_price,
        quantity=quantity,
        prevent_self_trade=True
    )

def main():
    try:
        logger.info("Starting Deterministic Trader A...")
        
        # Get config and setup client
        current_dir = os.path.dirname(os.path.abspath(__file__))
        client_scripts_dir = os.path.dirname(current_dir)
        meta_solver_root = os.path.dirname(client_scripts_dir)
        config_path = os.path.join(meta_solver_root, "Users", "config.json")
        
        with open(config_path) as f:
            private_key = json.load(f)["privateKey_a"]
        
        client = ExchangeClient(private_key)
        client.create_session()
        
        # Print initial balances
        logger.info("=== Initial Balances ===")
        initial_eth = client.get_balance("ETH")
        initial_usdc = client.get_balance("USDC")
        logger.info(f"ETH: {initial_eth:.4f}")
        logger.info(f"USDC: {initial_usdc:.2f}")
        logger.info("=====================")
        
        price_tracker = PriceTracker()
        price_tracker.start()
        
        # Wait for first price
        while price_tracker.current_price is None:
            time.sleep(0.1)
        
        # Execute all trades
        logger.info("Starting trading sequence...")
        executed_trades = []
        
        if price_tracker.current_price:
            trades = execute_trade_sequence(client, price_tracker.current_price)
            executed_trades.extend(trades)
            logger.info("Trade sequence completed.")
        
        # Print trade summary
        logger.info("\n=== Trade Summary ===")
        for i, trade in enumerate(executed_trades, 1):
            logger.info(f"Trade {i}: {trade['side']} {trade['quantity']:.4f} ETH @ ${trade['price']:.2f}")
        logger.info("===================")
        
        # Print final balances
        logger.info("\n=== Final Balances ===")
        final_eth = client.get_balance("ETH")
        final_usdc = client.get_balance("USDC")
        logger.info(f"ETH: {final_eth:.4f} (Change: {final_eth - initial_eth:+.4f})")
        logger.info(f"USDC: {final_usdc:.2f} (Change: {final_usdc - initial_usdc:+.2f})")
        logger.info("====================")

    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise

if __name__ == "__main__":
    main() 