from clientSideScripts.clientSide_Traders.exchange_client import ExchangeClient
import time
import random
import json
import logging
import os
import asyncio
import aiohttp
from concurrent.futures import ThreadPoolExecutor

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

async def place_orders(client, eth_price, num_orders=100):
    orders = []
    for _ in range(num_orders):
        deviation = random.uniform(-0.01, 0.01)
        price = eth_price * (1 + deviation)
        quantity = random.uniform(0.1, 1.0)
        side = random.choice(["BUY", "SELL"])
        orders.append((side, price, quantity))
    
    # Place orders concurrently
    tasks = [client.place_order(side, price, qty) for side, price, qty in orders]
    await asyncio.gather(*tasks)

def main():
    try:
        logger.info("Starting High-Frequency Trader A...")
        
        # Update path to point to the correct config location
        current_dir = os.path.dirname(os.path.abspath(__file__))
        meta_solver_dir = os.path.dirname(os.path.dirname(current_dir))  # Go up two levels to Meta-Solver root
        config_path = os.path.join(meta_solver_dir, "Users", "config.json")
        
        if not os.path.exists(config_path):
            raise FileNotFoundError(f"Could not find config file at: {config_path}")
        
        logger.info(f"Using config from: {config_path}")
        
        with open(config_path) as f:
            config = json.load(f)
            private_key = config["privateKey_a"]
            logger.info(f"Loaded private key for address: {private_key[:10]}...")
        
        logger.info("Initializing exchange client...")
        client = ExchangeClient(private_key)
        
        logger.info("Creating trading session...")
        client.create_session()
        
        logger.info("Starting trading loop...")
        start_time = time.time()
        duration = 600  # 10 minutes
        
        while time.time() - start_time < duration:
            try:
                # Get current ETH price
                logger.info("Fetching ETH price...")
                eth_price = client.get_eth_price()
                logger.info(f"Current ETH price: ${eth_price:.2f}")
                
                # Place 100 orders every second
                asyncio.run(place_orders(client, eth_price))
                
                # Wait for next second
                next_second = int(time.time()) + 1
                time.sleep(max(0, next_second - time.time()))
                
            except Exception as e:
                logger.error(f"Error during trading: {e}")
                time.sleep(1)
        
        logger.info("Trading completed")

    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise

if __name__ == "__main__":
    main()