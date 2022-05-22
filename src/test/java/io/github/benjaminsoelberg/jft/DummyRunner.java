package io.github.benjaminsoelberg.jft;

public class DummyRunner {

    /**
     * Can be used to start a "Nothing Burger" JVM while testing
     *
     * @param args not used
     * @throws InterruptedException should not happen
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.printf("Pid %d%n", ProcessHandle.current().pid());
        Thread.sleep(Long.MAX_VALUE);
    }
}
