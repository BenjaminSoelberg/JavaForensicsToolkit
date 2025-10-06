package io.github.benjaminsoelberg.jft;

import java.util.*;
import java.util.stream.Collectors;

public final class ClassTree {
    private final Map<ClassLoader, Node> root = new HashMap<>();

    public static class Node {
        private final ClassLoader loader;
        private final List<Node> children = new ArrayList<>();
        private final Map<Class<?>, byte[]> classes = new HashMap<>();

        public Node(ClassLoader loader) {
            this.loader = loader;
        }

        public void add(Node node) {
            children.add(node);
        }

        public void add(Class<?> clazz, byte[] bytecode) {
            // "putIfAbsent" ensures uniqueness
            classes.putIfAbsent(clazz, bytecode);
        }

        public ClassLoader getLoader() {
            return loader;
        }

        public List<Node> getChildren() {
            return children;
        }

        public Map<Class<?>, byte[]> getClasses() {
            return classes.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(Class::getName)))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a, // Dummy merge
                            LinkedHashMap::new  // Preserve order
                    ));
        }
    }

    public ClassTree() {
        // Create explicit bootstrap root
        Node bootstrap = new Node(null);
        root.put(null, bootstrap);
    }

    public synchronized void add(Class<?> clazz, byte[] bytecode) {
        ClassLoader loader = clazz.getClassLoader();
        Node node = ensureNode(loader);
        node.add(clazz, bytecode);
    }

    private Node ensureNode(ClassLoader loader) {
        if (root.containsKey(loader)) {
            return root.get(loader);
        }

        Node node = new Node(loader);
        root.put(loader, node);

        if (loader != null) {
            Node parent = ensureNode(loader.getParent());
            parent.add(node);
        }

        return node;
    }

    public Node getRoot() {
        return root.get(null);
    }

}