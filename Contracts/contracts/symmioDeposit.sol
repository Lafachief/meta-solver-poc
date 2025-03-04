// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/access/AccessControl.sol";

// Add interface for ZK proof verification
interface IZKVerifier {
    function verifyProof(
        bytes calldata proof,
        uint256[] calldata publicInputs
    ) external view returns (bool);
}

contract symmioDeposit is AccessControl {
    struct User {
        uint256 ethBalance;
        uint256 ethMarginBalance;
        uint256 usdcBalance;
        uint256 usdcMarginBalance;
        bool isRegistered;
    }
    
    // Only declare BROKER_ROLE
    bytes32 public constant BROKER_ROLE = keccak256("BROKER_ROLE");
    
    address public immutable owner;
    IERC20 public immutable usdc;
    mapping(address => User) public users;
    
    event Deposit(address indexed user, uint256 amount, bool isUSDC);
    event Withdrawal(address indexed user, uint256 amount);
    event UserRegistered(address indexed user);
    event MarginAccountDeposit(address indexed user, address indexed metaBroker, uint256 amount);
    event MovedToMargin(address indexed user, uint256 amount, bool isUSDC);
    event BalancesSettled(address indexed account, uint256 ethAmount, uint256 usdcAmount);
    
    IZKVerifier public zkVerifier;
    
    mapping(address => uint256) public marginAccounts;
    mapping(address => address) public userMetaBroker;
    
    constructor(address _usdc) {
        owner = msg.sender;
        usdc = IERC20(_usdc);
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(BROKER_ROLE, msg.sender);
    }
    
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this function");
        _;
    }
    
    function deposit() public payable {
        require(msg.value > 0, "Must deposit some ETH");
        
        User storage user = users[msg.sender];
        user.ethBalance += msg.value;
        user.isRegistered = true;
        
        emit Deposit(msg.sender, msg.value, false);
    }
    
    function depositUSDC(uint256 amount) public {
        require(amount > 0, "Must deposit some USDC");
        require(usdc.transferFrom(msg.sender, address(this), amount), "USDC transfer failed");
        
        User storage user = users[msg.sender];
        user.usdcBalance += amount;
        user.isRegistered = true;
        
        emit Deposit(msg.sender, amount, true);
    }
    
    function withdraw(uint256 amount) external {
        require(users[msg.sender].isRegistered, "User not registered");
        require(users[msg.sender].ethBalance >= amount, "Insufficient balance");
        users[msg.sender].ethBalance -= amount;
        (bool success, ) = payable(msg.sender).call{value: amount}("");
        require(success, "Transfer failed");
        emit Withdrawal(msg.sender, amount);
    }
    
    function setZKVerifier(address _verifier) external onlyOwner {
        require(_verifier != address(0), "Invalid verifier address");
        zkVerifier = IZKVerifier(_verifier);
    }
    
    function modifyBalanceWithProof(
        bytes memory proof,
        uint256[] memory publicInputs,
        address user,
        uint256 newBalance
    ) external {
        require(publicInputs.length >= 2, "Invalid public inputs");
        require(publicInputs[0] == uint256(uint160(user)), "Invalid user in proof");
        require(publicInputs[1] == newBalance, "Invalid balance in proof");
        
        // Verify the ZK proof
        require(zkVerifier.verifyProof(proof, publicInputs), "Invalid proof");
        
        // Update the user's balance
        users[user].ethBalance = newBalance;
        
        // If the user isn't registered, register them
        if (!users[user].isRegistered) {
            users[user].isRegistered = true;
            emit UserRegistered(user);
        }
    }
    
    function getBalance() external view returns (uint256) {
        return users[msg.sender].ethBalance;
    }
    
    function getContractBalance() external view onlyOwner returns (uint256) {
        return address(this).balance;
    }
    
    function moveToMargin(uint256 amount) public {
        User storage user = users[msg.sender];
        require(user.isRegistered, "User not registered");
        require(user.ethBalance >= amount, "Insufficient ETH balance");
        
        user.ethBalance -= amount;
        user.ethMarginBalance += amount;
        
        emit MovedToMargin(msg.sender, amount, false);
    }
    
    function moveUSDCToMargin(uint256 amount) public {
        User storage user = users[msg.sender];
        require(user.isRegistered, "User not registered");
        require(user.usdcBalance >= amount, "Insufficient USDC balance");
        
        user.usdcBalance -= amount;
        user.usdcMarginBalance += amount;
        
        emit MovedToMargin(msg.sender, amount, true);
    }
    
    function withdrawFromMarginAccount(
        uint256 amount,
        bytes calldata zkProof,
        bytes calldata signature
    ) external {
        require(amount > 0, "Amount must be greater than 0");
        require(marginAccounts[msg.sender] >= amount, "Insufficient margin balance");
        
        address metaBroker = userMetaBroker[msg.sender];
        require(metaBroker != address(0), "No meta-broker assigned");
        
        // Verify zkProof
        require(verifyZkProof(zkProof, msg.sender, amount), "Invalid zkProof");
        
        // Verify meta-broker signature
        bytes32 messageHash = keccak256(abi.encodePacked(msg.sender, amount));
        require(verifySignature(messageHash, signature, metaBroker), "Invalid signature");
        
        // Update balances
        marginAccounts[msg.sender] -= amount;
        users[msg.sender].ethBalance += amount;
    }
    
    function verifyZkProof(bytes calldata /* proof */, address /* user */, uint256 /* amount */) internal pure returns (bool) {
        // Implement zkProof verification logic
        // This is a placeholder - you'll need to implement actual ZK proof verification
        return true;
    }
    
    function verifySignature(bytes32 /* messageHash */, bytes calldata /* signature */, address /* signer */) internal pure returns (bool) {
        // Implement signature verification logic
        // This is a placeholder - you'll need to implement actual signature verification
        return true;
    }
    
    function getBalances(address userAddress) public view returns (
        uint256 ethBalance,
        uint256 ethMarginBalance,
        uint256 usdcBalance,
        uint256 usdcMarginBalance
    ) {
        User storage user = users[userAddress];
        return (
            user.ethBalance,
            user.ethMarginBalance,
            user.usdcBalance,
            user.usdcMarginBalance
        );
    }

    function settleBalances(address account) external onlyRole(BROKER_ROLE) {
        // Get current balances
        (/* uint256 ethBalance */, uint256 pendingEth, /* uint256 usdcBalance */, uint256 pendingUsdc) = getBalances(account);
        
        // Move pending balances to active
        if (pendingEth > 0) {
            users[account].ethBalance += pendingEth;
            users[account].ethMarginBalance -= pendingEth;
            pendingEth = 0;
        }
        
        if (pendingUsdc > 0) {
            users[account].usdcBalance += pendingUsdc;
            users[account].usdcMarginBalance -= pendingUsdc;
            pendingUsdc = 0;
        }

        emit BalancesSettled(account, pendingEth, pendingUsdc);
    }

    function hasBrokerRole(address account) external view returns (bool) {
        return hasRole(BROKER_ROLE, account);
    }
}

