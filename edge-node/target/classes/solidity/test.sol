// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

// 声明 Solidity 编译器版本要求，这里表示使用 0.8.0 或更高的版本（直到不兼容的版本）

contract ReputationManagement {
    struct Node {
        uint256 alpha; // 正反馈计数
        uint256 beta; // 负反馈计数
        uint256 reputation; // 信誉值（以百分比表示）
        uint256 successfulTasks; // 成功任务计数
        uint256 totalTasks; // 总任务计数
    }

    Node[] public nodes; // 节点数组，存储所有节点的信息，从0开始
    mapping(address => uint256) public nodeIndex; // 节点地址到节点索引的映射，从1开始

    // Solidity不支持浮点数类型的，因此整个合约设计整体放大10倍，例如0.9的遗忘因子，被设定为9

    uint256 public constant SCALE = 100; // 精度缩放因子（2位小数）
    uint256 public Wf = 90; // 遗忘因子0.9（90 = 0.9*SCALE）

    // 每当节点的信誉值更新时，通过emit触发这个事件，记录下节点ID和新的信誉值
    event ReputationUpdated(uint256 indexed nodeId, uint256 newReputation);

    modifier onlyRegisteredNode() {
        require(nodeIndex[msg.sender] != 0, "Node not registered");
        _;
    }

    // 初始化节点信誉值
    function initializeNode(address _nodeAddress) public {
        require(nodeIndex[_nodeAddress] == 0, "Node already initialized");
        uint256 index = nodes.length;
        nodes.push(
            Node({
                alpha: 50, // 初始正反馈计数，0.5 * SCALE
                beta: 50, // 初始负反馈计数
                reputation: 50, // 初始信誉值
                successfulTasks: 0, // 初始成功任务计数
                totalTasks: 0 // 初始总任务计数
            })
        );
        nodeIndex[_nodeAddress] = index + 1; // 从1开始索引，0代表该节点未初始化
    }

    function updateReputation(
        address _nodeAddress,
        string memory result,
        bool _isSuccess,
        uint256 _Wd
    ) public {
        uint256 index = nodeIndex[_nodeAddress];
        require(index != 0, "Node not initialized");
        index -= 1; // 转换为从0开始的索引

        Node storage node = nodes[index];

        // 对节点提交的result，理论上应在此处进行结果验证来判断，从而判定_isSuccess是true还是false
        if (keccak256(bytes(result)) == keccak256(bytes(""))) {
            // ……
            // 但在本项目中，重点在于实现基于Beta分布的信誉更新机制
            // 因此任务结果的具体验证过程被省略，任务是否成功直接由节点通过_isSuccess参数告知智能合约
            // ……
        }

        uint256 mu = _isSuccess ? 1 : 0;

        // 更新任务计数
        node.totalTasks += 1;
        if (_isSuccess) {
            node.successfulTasks += 1;
        }

        // 计算历史任务完成率
        uint256 Wc = (node.successfulTasks * SCALE) / node.totalTasks;

        uint256 newAlpha = (node.alpha * Wf) / SCALE + (Wc * _Wd * mu) / SCALE;
        uint256 newBeta = (node.beta * Wf) /
                    SCALE +
            ((1 * SCALE - Wc) * (1 - mu)) /
            SCALE;

        node.alpha = newAlpha;
        node.beta = newBeta;

        // 计算新信誉值（自动保留缩放比例）
        uint256 total = node.alpha + node.beta;
        require(total > 0, "Division by zero");
        node.reputation = (node.alpha * SCALE) / total;
        emit ReputationUpdated(index, node.reputation);
    }

    // 获取节点信誉值
    function getReputations()
    public
    view
    onlyRegisteredNode
    returns (uint256[] memory)
    {
        uint256 nodeCount = nodes.length;
        uint256[] memory reputations = new uint256[](nodeCount);
        for (uint256 i = 0; i < nodeCount; i++) {
            reputations[i] = nodes[i].reputation;
        }
        return reputations;
    }

    function getNodeLength() public view returns (uint256) {
        return nodes.length;
    }
}
