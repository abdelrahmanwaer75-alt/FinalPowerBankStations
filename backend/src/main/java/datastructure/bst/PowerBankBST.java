package datastructure.bst;

import datastructure.models.PowerBank;

public class PowerBankBST {
    private TreeNode root;

    public void insert(PowerBank bank) {
        if (bank == null || search(bank.getId()) != null) return;
        root = insertNode(root, bank);
    }

    private TreeNode insertNode(TreeNode node, PowerBank bank) {
        if (node == null) return new TreeNode(bank);
        if (bank.getId() < node.data.getId()) node.left = insertNode(node.left, bank);
        else if (bank.getId() > node.data.getId()) node.right = insertNode(node.right, bank);
        return node;
    }

    public PowerBank search(long id) {
        TreeNode current = root;
        while (current != null) {
            if (id == current.data.getId()) return current.data;
            if (id < current.data.getId()) current = current.left;
            else current = current.right;
        }
        return null;
    }

    public boolean update(long id, PowerBank updated) {
        if (updated == null || search(id) == null) return false;
        if (id != updated.getId() && search(updated.getId()) != null) return false;
        root = deleteNode(root, id);
        root = insertNode(root, updated);
        return true;
    }

    public boolean delete(long id) {
        if (search(id) == null) return false;
        root = deleteNode(root, id);
        return true;
    }

    private TreeNode deleteNode(TreeNode node, long id) {
        if (node == null) return null;
        if (id < node.data.getId()) node.left = deleteNode(node.left, id);
        else if (id > node.data.getId()) node.right = deleteNode(node.right, id);
        else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            TreeNode successor = findMin(node.right);
            node.data = successor.data;
            node.right = deleteNode(node.right, successor.data.getId());
        }
        return node;
    }

    private TreeNode findMin(TreeNode node) {
        while (node.left != null) node = node.left;
        return node;
    }

    public void displayInOrder() {
        if (root == null) {
            System.out.println("No PowerBanks found.");
            return;
        }
        displayInOrder(root);
    }

    /** Returns a snapshot of the inventory ordered by PowerBank ID. */
    public PowerBank[] inOrderTraversal() {
        PowerBank[] result = new PowerBank[count()];
        copyToArray(result, new int[]{0});
        return result;
    }

    /** Finds the lowest-ID available unit at the requested station. */
    public PowerBank findAvailable(long stationId) {
        return findAvailable(root, stationId);
    }

    /** Finds the lowest-ID available unit at any station. */
    public PowerBank findAvailable() {
        return findAvailable(root, null);
    }

    private PowerBank findAvailable(TreeNode node, Long stationId) {
        if (node == null) return null;
        PowerBank left = findAvailable(node.left, stationId);
        if (left != null) return left;
        PowerBank bank = node.data;
        if ("AVAILABLE".equals(bank.getStatus()) && (stationId == null || bank.getStationId() == stationId)) return bank;
        return findAvailable(node.right, stationId);
    }

    private void displayInOrder(TreeNode node) {
        if (node == null) return;
        displayInOrder(node.left);
        System.out.println(node.data);
        displayInOrder(node.right);
    }

    public void copyToArray(PowerBank[] array, int[] index) {
        copyToArray(root, array, index);
    }

    private void copyToArray(TreeNode node, PowerBank[] array, int[] index) {
        if (node == null) return;
        copyToArray(node.left, array, index);
        if (index[0] < array.length) array[index[0]++] = node.data;
        copyToArray(node.right, array, index);
    }

    public int count() {
        return count(root);
    }

    private int count(TreeNode node) {
        if (node == null) return 0;
        return 1 + count(node.left) + count(node.right);
    }
}
