package lab;

/** Runs every case of this module once and prints what BouncyCastle did. */
public final class MainBc187 {
    private MainBc187() {}

    private interface Case {
        String run() throws Exception;
    }

    private static int failed;

    private static void run(String id, Case test) {
        try {
            System.out.printf("%s  ok    %s%n", id, test.run());
        } catch (Exception | LinkageError e) {
            System.out.printf("%s  FAIL  %s%n", id, e);
            failed++;
        }
    }

    public static void main(String[] args) {
        run("N01", Aes187::gcm);
        run("N02", Pq187::kem);
        run("N03", Pq187::dsa);
        run("N04", Pq187::slh);
        System.exit(failed == 0 ? 0 : 1);
    }
}
