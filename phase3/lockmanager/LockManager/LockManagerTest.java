package LockManager;

class LockManagerTest {
    public static void main(String[] args) {
        MyThread t1, t2;
        LockManager lm = new LockManager();
        t1 = new MyThread(lm, 1);
        t2 = new MyThread(lm, 2);
        t1.start();
        t2.start();
    }
}

class MyThread extends Thread {
    LockManager lm;
    int threadId;

    public MyThread(LockManager lm, int threadId) {
        this.lm = lm;
        this.threadId = threadId;
    }

    public void run() {
        if (threadId == 1) {
            try {
                lm.Lock(1, "a", LockManager.WRITE);
                System.out.println(threadId + " got WRITE lock on a");
            } catch (DeadlockException e) {
                System.out.println("Deadlock.... ");
            }

            try {
                this.sleep(4000);
            } catch (InterruptedException e) {
            }

            try {
                lm.Lock(1, "b", LockManager.READ);
                System.out.println(threadId + " got READ lock on b");
            } catch (DeadlockException e) {
                System.out.println("Deadlock.... ");
            }

            System.out.println(threadId + " unlocksAll");
            lm.UnlockAll(1);

        } else if (threadId == 2) {
            try {
                lm.Lock(2, "b", LockManager.READ);
                System.out.println(threadId + " got READ lock on b");
            } catch (DeadlockException e) {
                System.out.println("Deadlock.... ");
            }

            try {
                this.sleep(40000);
            } catch (InterruptedException e) {
            }

            try {
                lm.Lock(2, "a", LockManager.WRITE);
                System.out.println(threadId + " got WRITE lock on a");
            } catch (DeadlockException e) {
                System.out.println("Deadlock.... ");
            }

            System.out.println(threadId + " unlocksAll");
            lm.UnlockAll(2);
        }
    }
}
