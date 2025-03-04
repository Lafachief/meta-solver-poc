import requests
import json
import time
from web3 import Web3
from eth_account.messages import encode_defunct
import random
from pycoingecko import CoinGeckoAPI
import logging

logger = logging.getLogger(__name__)

class ExchangeClient:
    def __init__(self, private_key, api_url="http://localhost:8080/order-book"):
        self.web3 = Web3(Web3.HTTPProvider('http://localhost:8545'))
        self.private_key = private_key
        self.api_url = api_url
        self.account = self.web3.eth.account.from_key(private_key)
        self.session_id = None
        self.cg = CoinGeckoAPI()
        self.ws = None  # For WebSocket connection
        
    def sign_message(self, message):
        """Sign a message using the private key"""
        message_hash = encode_defunct(text=message)
        signed_message = self.web3.eth.account.sign_message(message_hash, private_key=self.private_key)
        
        return {
            "message": message,
            "signature": signed_message.signature.hex(),
            "address": self.account.address,
            "v": hex(signed_message.v),
            "r": hex(signed_message.r),
            "s": hex(signed_message.s)
        }
        
    def create_session(self):
        """Create a new trading session"""
        message = f"Login to Meta-Solver Exchange at {int(time.time())}"
        signed_message = self.sign_message(message)
        
        payload = {
            "message": signed_message["message"],
            "signature": signed_message["signature"],
            "address": signed_message["address"],
            "v": signed_message["v"],
            "r": signed_message["r"],
            "s": signed_message["s"]
        }
        
        logger.info("Creating session with:")
        for key, value in payload.items():
            logger.info(f"{key}: {value}")
        
        try:
            response = requests.post(f"{self.api_url}/session", json=payload)
            
            if response.status_code == 200:
                response_data = response.json()
                self.session_id = response_data["sessionId"]
                logger.info(f"Session created successfully. ID: {self.session_id}")
                self.connect_websocket()
            else:
                logger.error(f"Session creation failed. Status: {response.status_code}")
                logger.error(f"Response: {response.text}")
                raise Exception(f"Failed to create session: {response.text}")
                
        except Exception as e:
            logger.error(f"Failed to create session: {e}")
            raise
            
    def place_order(self, side, price, quantity, prevent_self_trade=False):
        """
        Place an order on the exchange
        
        Args:
            side (str): "BUY" or "SELL"
            price (float): Price in USDC
            quantity (float): Quantity of ETH
            prevent_self_trade (bool): Whether to prevent self-trading
        """
        if not self.session_id:
            raise Exception("No active session")
            
        order = {
            "sessionId": self.session_id,
            "traderId": self.account.address,
            "type": "LIMIT",
            "side": side,
            "price": price,
            "quantity": quantity,
            "preventSelfTrade": prevent_self_trade
        }
        
        try:
            logger.info(f"Placing order: {order}")
            response = requests.post(f"{self.api_url}/order", json=order)
            
            if response.status_code == 200:
                logger.info(f"Order placed: {side} {quantity:.4f} ETH @ {price:.2f} USDC")
                return response.json()
            else:
                logger.error(f"Order placement failed. Status: {response.status_code}")
                logger.error(f"Response: {response.text}")
                raise Exception(f"Failed to place order: {response.text}")
                
        except Exception as e:
            logger.error(f"Failed to place order: {e}")
            raise
        
    def get_eth_price(self):
        """Get current ETH price from CoinGecko"""
        try:
            price_data = self.cg.get_price(ids='ethereum', vs_currencies='usd')
            return float(price_data['ethereum']['usd'])
        except Exception as e:
            logger.error(f"Failed to get ETH price: {e}")
            raise

    def connect_websocket(self):
        """Setup WebSocket connection for real-time updates"""
        # Implement WebSocket connection if needed
        pass

    def get_balance(self, asset):
        """Get current balance for an asset"""
        if not self.session_id:
            raise Exception("No active session")
        
        try:
            response = requests.get(
                f"{self.api_url}/balance",
                params={
                    "sessionId": self.session_id,
                    "traderId": self.account.address,
                    "asset": asset
                }
            )
            
            if response.status_code == 200:
                balance_data = response.json()
                logger.info(f"Got balance for {asset}: {balance_data['balance']}")
                return float(balance_data['balance'])
            else:
                logger.error(f"Failed to get balance. Status: {response.status_code}")
                logger.error(f"Response: {response.text}")
                raise Exception(f"Failed to get balance: {response.text}")
                
        except Exception as e:
            logger.error(f"Failed to get balance: {e}")
            raise