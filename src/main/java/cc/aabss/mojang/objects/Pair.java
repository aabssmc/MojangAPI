package cc.aabss.mojang.objects;

public class Pair<T, R> {

    private T left;
    private R right;

    public static <T, R> Pair<T, R> of(T left, R right) {
        Pair<T, R> pair = new Pair<>();
        pair.left = left;
        pair.right = right;
        return pair;
    }

    public T getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}
