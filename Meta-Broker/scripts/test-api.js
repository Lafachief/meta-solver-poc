const Web3 = require('web3');
const axios = require('axios');

const web3 = new Web3('http://localhost:8545');
const API_URL = 'http://localhost:8080/api/v1/orderbook';

async function createSession(privateKey) {
    const account = web3.eth.accounts.privateKeyToAccount(privateKey);
    const message = `Login to Meta-Solver Exchange at ${Date.now()}`;
    const signature = await web3.eth.accounts.sign(message, privateKey);

    const response = await axios.post(`${API_URL}/session`, {
        message: message,
        signature: signature.signature,
        address: account.address
    });

    return response.data.sessionId;
}

async function placeOrder(sessionId, privateKey, type, side, price, quantity) {
    const account = web3.eth.accounts.privateKeyToAccount(privateKey);
    
    const response = await axios.post(`${API_URL}/order`, {
        sessionId: sessionId,
        traderId: account.address,
        type: type,
        side: side,
        price: price,
        quantity: quantity
    });

    return response.data;
}

async function main() {
    const privateKey = '0x...'; // Add your private key here
    const sessionId = await createSession(privateKey);
    console.log('Session created:', sessionId);

    const orderId = await placeOrder(sessionId, privateKey, 'LIMIT', 'BUY', 1800.0, 1.5);
    console.log('Order placed:', orderId);
}

main().catch(console.error); 