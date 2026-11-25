package lab;

import java.util.Map;

/** Runs every case once and prints what the JDK or BouncyCastle did. It names no algorithm of its own. */
public final class Main {
    private Main() {}

    private interface Case {
        String run() throws Exception;
    }

    private static int passed;
    private static int failed;

    private static void run(String id, Case test) {
        try {
            System.out.printf("%s  ok    %s%n", id, test.run());
            passed++;
        } catch (Exception | LinkageError e) {
            System.out.printf("%s  FAIL  %s%n", id, e);
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("java " + Runtime.version() + ", " + System.getProperty("java.vendor"));

        run("J01", Jca::j01);
        run("J02", Jca::j02);
        run("J03", Jca::j03);
        run("J04", () -> Jca.j04(Map.of("digest", args.length > 0 ? args[0] : "SHA3-512")));
        run("J05", Jca::j05);
        run("J06", Jca::j06);
        run("J07", Jca::j07);
        run("J08", Jca::j08);
        run("J09", Jca::j09);
        run("J10", Jca::j10);
        run("J11", Jca::j11);
        run("J12", Jca::j12);
        run("J13", Jca::j13);
        run("J14", Jca::j14);
        run("J15", Jca::j15);
        run("J16", Jca::j16);
        run("J17", Jca::j17);
        run("J18", Jca::j18);
        run("J19", Jca::j19);
        run("J20", Jca::j20);
        run("J21", Jca::j21);
        run("J22", Jca::j22);
        run("J23", Jca::j23);
        run("J24", Jca::j24);
        run("J25", Jca::j25);
        run("J26", Jca::j26);
        run("J27", Jca::j27);

        run("P01", Pqc::p01);
        run("P02", Pqc::p02);
        run("P03", Pqc::p03);
        run("P04", Pqc::p04);

        run("T01", Tls::t01);
        run("T02", Tls::t02);
        run("T03", Tls::t03);
        run("T04", Tls::t04);
        run("T05", Tls::t05);

        run("B01", Bc::b01);
        run("B02", Bc::b02);
        run("B03", Bc::b03);
        run("B04", Bc::b04);
        run("B05", Bc::b05);
        run("B06", Bc::b06);
        run("B07", Bc::b07);
        run("B08", Bc::b08);
        run("B09", Bc::b09);
        run("B10", Bc::b10);
        run("B11", Bc::b11);
        run("B12", Bc::b12);
        run("B13", Bc::b13);
        run("B14", Bc::b14);
        run("B15", Bc::b15);
        run("B16", Bc::b16);
        run("B17", Bc::b17);
        run("B18", Bc::b18);
        run("B19", Bc::b19);

        System.out.printf("%d cases ran, %d failed%n", passed + failed, failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
