from clientSideScripts.clientSide_Traders.exchange_client import ExchangeClient
import time
import random
import json
import logging
import os

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def main():
    try:
        logger.info("Starting Trader B...")
        
        # Get path to Meta-Solver/Users/config.json
        current_dir = os.path.dirname(os.path.abspath(__file__))
        meta_solver_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        config_path = os.path.join(meta_solver_dir, "Users", "config.json")
        
        logger.info(f"Current directory: {current_dir}")
        logger.info(f"Meta-Solver directory: {meta_solver_dir}")
        logger.info(f"Looking for config at: {config_path}")
        
        with open(config_path) as f:
            config = json.load(f)
            private_key = config["privateKey_b"]
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
                
                # Add random deviation (-1% to +1%)
                deviation = random.uniform(-0.01, 0.01)
                price = eth_price * (1 + deviation)
                
                # Random quantity between 0.1 and 1.0 ETH
                quantity = random.uniform(0.1, 1.0)
                
                # Randomly choose buy or sell
                side = random.choice(["BUY", "SELL"])
                
                # Place order
                logger.info(f"Placing {side} order: {quantity:.4f} ETH at ${price:.2f}")
                order_id = client.place_order(side, price, quantity)
                logger.info(f"Order placed successfully. ID: {order_id}")
                
                # Random delay between 0.1 and 0.5 seconds
                delay = random.uniform(0.1, 0.5)
                logger.info(f"Waiting {delay:.2f} seconds before next order...")
                time.sleep(delay)
                
            except Exception as e:
                logger.error(f"Error during trading: {e}")
                time.sleep(1)
        
        logger.info("Trading completed")

    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise

if __name__ == "__main__":
    main()