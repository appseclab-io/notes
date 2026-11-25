package lab;

import java.security.KeyPair;
import javax.crypto.SecretKey;

/** Runs every case of this module once and prints what the JDK did. */
public final class MainJdk {
    private MainJdk() {}

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

    public static void main(String[] args) throws Exception {
        System.out.println("java " + Runtime.version() + ", " + System.getProperty("java.vendor"));
        SecretKey callerKey = Inputs.aesKeyOf256Bits();
        KeyPair callerPair = Inputs.mlKem768KeyPair();

        run("S01", Sizes::s01);
        run("S02", Sizes::s02);
        run("S03", () -> Sizes.s03(callerKey));
        run("S04", Sizes::s04);
        run("S05", Sizes::s05);
        run("S06", Sizes::s06);
        run("S07", Sizes::s07);

        run("C01", () -> Coverage.c01(callerPair));
        run("C02", Coverage::c02);
        run("C03", Coverage::c03);
        run("C04", Coverage::c04);
        run("C05", Coverage::c05);

        System.exit(failed == 0 ? 0 : 1);
    }
}
