package lab;

/** Runs every case of this module once and prints what BouncyCastle did. */
public final class MainBc185 {
    private MainBc185() {}

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
        run("M02", Pq185New::kem);
        run("M03", Pq185New::dsa);
        run("M04", Pq185New::slh);
        run("O02", Pq185Old::kem);
        run("O03", Pq185Old::dsa);
        run("O04", Pq185Old::slh);
        System.exit(failed == 0 ? 0 : 1);
    }
}
