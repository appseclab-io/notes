package lab;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * TLS as the JDK configures it. No case opens a connection or binds a port:
 * each one sets the options and reads them back.
 */
public final class Tls {
    private Tls() {}

    // [T01] TLS, protocol version named when the context is created
    static String t01() throws Exception {
        SSLContext context = SSLContext.getInstance("TLSv1.3");
        context.init(null, null, null);
        return context.getProtocol() + ", provider " + context.getProvider().getName();
    }

    // [T02] TLS, protocol and cipher suite set on the parameters of an engine
    static String t02() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLEngine engine = context.createSSLEngine();
        SSLParameters parameters = engine.getSSLParameters();
        parameters.setProtocols(new String[] {"TLSv1.2"});
        parameters.setCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"});
        engine.setSSLParameters(parameters);
        return String.join(",", engine.getEnabledProtocols()) + ", "
                + String.join(",", engine.getEnabledCipherSuites());
    }

    // [T03] TLS, protocol and cipher suite set on a server socket that is never bound
    static String t03() throws Exception {
        try (SSLServerSocket socket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket()) {
            socket.setEnabledProtocols(new String[] {"TLSv1.3"});
            socket.setEnabledCipherSuites(new String[] {"TLS_AES_256_GCM_SHA384"});
            return String.join(",", socket.getEnabledProtocols()) + ", "
                    + String.join(",", socket.getEnabledCipherSuites());
        }
    }

    // [T04] TLS, key exchange group named: the hybrid post-quantum group of JDK 27
    static String t04() throws Exception {
        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
        SSLParameters parameters = engine.getSSLParameters();
        parameters.setNamedGroups(new String[] {"X25519MLKEM768"});
        engine.setSSLParameters(parameters);
        return "named groups " + String.join(",", engine.getSSLParameters().getNamedGroups());
    }

    // [T05] TLS, nothing named: what the JDK offers by default
    static String t05() throws Exception {
        SSLParameters parameters = SSLContext.getDefault().getDefaultSSLParameters();
        return String.join(",", parameters.getProtocols()) + ", first group " + parameters.getNamedGroups()[0]
                + ", first suite " + parameters.getCipherSuites()[0];
    }
}
