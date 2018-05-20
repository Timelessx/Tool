package o.o;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SemaphoreTest {

    public static void main(String[] args) {
        foo(10);
        bar(10);
    }

    public static void foo(final int n) {
        final Semaphore[] semaphore = new Semaphore[n];
        semaphore[0] = new Semaphore(1);
        for (int i = 0; i < n; i++) {
            if (i > 0)
                semaphore[i] = new Semaphore(0);
            final int cur = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (; ; ) {
                        try {
                            //System.out.println("线程 " + cur + " 准备执行");
                            semaphore[cur].acquire();
                            System.out.println("线程 " + cur + " 执行中");
                            Thread.sleep((long) (2000 * Math.random()));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            semaphore[(cur + 1) % n].release();
                        }
                    }
                }
            }).start();
        }
    }

    private static int next = 0;

    public static void bar(final int n) {
        final ReentrantLock lock = new ReentrantLock();
        final Condition[] conditions = new Condition[n];
        for (int i = 0; i < n; i++) {
            conditions[i] = lock.newCondition();
            final int cur = i;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    for (; ; ) {
                        //System.out.println("线程 " + cur + " 准备执行");
                        lock.lock();
                        try {
                            while (next != cur)
                                conditions[cur].await();
                            System.out.println("线程 " + cur + " 执行中");
                            Thread.sleep((long) (2000 * Math.random()));    //模拟耗时操作
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            next = (cur + 1) % n;
                            conditions[next].signal();
                            lock.unlock();
                        }
                    }
                }
            }).start();
        }
    }

}