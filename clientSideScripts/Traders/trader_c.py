from exchange_client import ExchangeClient
import websocket
import json
import logging
import os
import time
import random
import threading
from concurrent.futures import ThreadPoolExecutor
from queue import Queue

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

def place_orders_batch(client, price, batch_size=100):
    orders = []
    for _ in range(batch_size):
        side = "BUY" if random.random() > 0.5 else "SELL"
        price_mod = price * (1 + random.uniform(-0.01, 0.01))
        quantity = random.uniform(0.1, 1.0)
        orders.append((side, price_mod, quantity))
    
    with ThreadPoolExecutor(max_workers=10) as executor:
        executor.map(lambda x: client.place_order(*x), orders)

def main():
    try:
        logger.info("Starting Ultra High-Frequency Trader C...")
        
        # Fix the path to config.json by going up one more directory level
        current_dir = os.path.dirname(os.path.abspath(__file__))  # Traders directory
        client_scripts_dir = os.path.dirname(current_dir)         # clientSideScripts directory
        meta_solver_dir = os.path.dirname(client_scripts_dir)     # Meta-Solver root directory
        config_path = os.path.join(meta_solver_dir, "Users", "config.json")
        
        logger.info(f"Loading config from: {config_path}")
        with open(config_path) as f:
            private_key = json.load(f)["privateKey_b"]  # Changed from privateKey_a to privateKey_b
        
        client = ExchangeClient(private_key)
        client.create_session()
        
        price_tracker = PriceTracker()
        price_tracker.start()
        time.sleep(1)  # Brief wait for first price
        
        total_orders = 0
        start_time = time.time()
        batch_size = 100  # Orders per batch
        target_batches_per_sec = 100  # 100 batches * 100 orders = 10,000 orders/sec
        
        while True:
            batch_start = time.time()
            
            if price_tracker.current_price:
                # Launch multiple batches in parallel
                threads = []
                for _ in range(target_batches_per_sec):
                    t = threading.Thread(
                        target=place_orders_batch,
                        args=(client, price_tracker.current_price, batch_size)
                    )
                    t.start()
                    threads.append(t)
                
                # Wait for all batches to complete
                for t in threads:
                    t.join()
                
                total_orders += batch_size * target_batches_per_sec
                elapsed = time.time() - start_time
                rate = total_orders / elapsed
                
                logger.info(f"Orders/sec: {rate:.2f} (Total: {total_orders})")
            
            # Maintain timing
            elapsed_batch = time.time() - batch_start
            if elapsed_batch < 1.0:
                time.sleep(1.0 - elapsed_batch)

    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise

if __name__ == "__main__":
    main() 