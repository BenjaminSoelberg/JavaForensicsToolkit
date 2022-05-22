package io.github.benjaminsoelberg.jft;

public class DummyRunner {

    public static void main(String[] args) throws InterruptedException {
        System.out.printf("Pid %d", ProcessHandle.current().pid());
        Thread.sleep(Long.MAX_VALUE);
    }
}
