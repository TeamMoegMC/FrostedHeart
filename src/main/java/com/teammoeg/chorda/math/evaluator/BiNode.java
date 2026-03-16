/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.math.evaluator;

/**
 * 二元运算节点的抽象基类，包含左右两个子节点。
 * <p>
 * Abstract base class for binary operation nodes, containing left and right child nodes.
 */
abstract class BiNode implements Node {
    Node left;
    Node right;

    /**
     * 构造一个二元节点。
     * <p>
     * Constructs a binary node.
     *
     * @param left 左子节点 / the left child node
     * @param right 右子节点 / the right child node
     */
    public BiNode(Node left, Node right) {
        super();
        this.left = left;
        this.right = right;
    }

}