// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract MockUSDC is ERC20 {
    constructor() ERC20("USD Coin", "USDC") {
        // No need to set decimals, we'll override the decimals() function
    }

    function decimals() public pure override returns (uint8) {
        return 6; // USDC uses 6 decimals
    }

    // Function to mint USDC for testing
    function mint(address to, uint256 amount) public {
        _mint(to, amount);
    }
} 