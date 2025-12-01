# A Kubernetes and Ethereum-based Testbed for Task Offloading

This project provides a Kubernetes and Ethereum-based testbed for task offloading. In this testbed, we employed containerization technologies to deploy multiple simulated edge nodes on a single physical server. Each node has its own isolated computing and network space, and the nodes communicate with each other using real TCP/IP protocols, thereby replicating realistic network interactions in the edge environment. In addition, smart contracts were implemented using the Solidity language and successfully deployed onto a private Ethereum blockchain. Each edge node owns an Ethereum account, enabling authentic interactions and trust management under a real blockchain environment, which provides strong support for the proposed trust-enabled and intelligent task offloading.
> ✅ **This repository contains the open-source implementation of the testbed proposed in the following paper:**
> Trust-enabled Decentralized Task Offloading for Collaborative Edge Computing Using Blockchain and Deep Reinforcement Learning

## 🚀 How to Run

The system requires the following runtime environments. Detailed setup instructions will be added soon.

### ☕ Java Environment
- **Required Version:** Java 14+  
### ☸️ Kubernetes Environment



### 🐳 Docker Environment



### 🛢️ MySQL Environment



### 🧭 Nacos Configuration Center



### ⛓️ Ethereum Blockchain
1. Install Ganache ([https://archive.trufflesuite.com/ganache/](https://archive.trufflesuite.com/ganache/)), start a local blockchain, and create 10 predefined accounts.
2. Use the Remix website ([https://remix.ethereum.org/](https://remix.ethereum.org/)) to write smart contracts in Solidity, then compile and deploy them to the blockchain network. On this platform, you can perform initial testing of smart contracts.
3. Use Maven to install Web3j dependencies. Web3j is a Java library for interacting with the Ethereum blockchain.
```java
        <dependency>
            <groupId>org.web3j</groupId>
            <artifactId>core</artifactId>
            <version>4.9.4</version>
        </dependency>
```
4. Use Web3j can generate Java contract classes based on smart contracts.
---
