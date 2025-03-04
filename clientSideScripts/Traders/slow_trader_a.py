from exchange_client import ExchangeClient
import websocket
import json
import logging
import os
import time
import random
import threading

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class PriceTracker:
    def __init__(self):
        self.current_price = None
        self.last_update = 0
        self.ws = None
        
    def on_message(self, ws, message):
        now = time.time()
        if now - self.last_update >= 1.0:  # Update price once per second
            data = json.loads(message)
            if 'p' in data:
                self.current_price = float(data['p'])
                self.last_update = now
        
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

def place_single_order(client, price):
    side = "BUY" if random.random() > 0.5 else "SELL"
    price_mod = price * (1 + random.uniform(-0.01, 0.01))
    quantity = random.uniform(0.1, 1.0)
    client.place_order(side, price_mod, quantity)
    logger.info(f"Placed {side} order: {quantity:.4f} ETH @ {price_mod:.2f} USDC")

def main():
    try:
        logger.info("Starting Simple Trader...")
        
        # Get the correct path to config.json
        current_dir = os.path.dirname(os.path.abspath(__file__))  # Traders directory
        client_scripts_dir = os.path.dirname(current_dir)          # clientSideScripts directory
        meta_solver_root = os.path.dirname(client_scripts_dir)     # Meta-Solver root directory
        config_path = os.path.join(meta_solver_root, "Users", "config.json")
        
        logger.info(f"Loading config from: {config_path}")
        
        with open(config_path) as f:
            private_key = json.load(f)["privateKey_a"]
        
        client = ExchangeClient(private_key)
        client.create_session()
        
        price_tracker = PriceTracker()
        price_tracker.start()
        time.sleep(1)  # Brief wait for first price
        
        total_orders = 0
        start_time = time.time()
        
        while True:
            if price_tracker.current_price:
                place_single_order(client, price_tracker.current_price)
                total_orders += 1
                elapsed = time.time() - start_time
                rate = total_orders / elapsed
                logger.info(f"Total orders: {total_orders} (Rate: {rate:.2f} orders/sec)")
            
            # Wait for 1 second before next order
            time.sleep(1.0)

    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise

if __name__ == "__main__":
    main() 