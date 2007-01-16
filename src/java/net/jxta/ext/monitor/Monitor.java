package net.jxta.ext.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class Monitor
implements Runnable {
    
    private static final String MONITORS = "file:///tmp/monitor.properties";
    private static final String STATE_GOOD = "good";
    private static final String STATE_BAD = "bad";
    private static final int INTERVAL = 20 * 60 * 1000;
    private static final int WAIT = 20 * 1000;
    
    private Properties addresses = null;
    private List probes = null;
    private boolean running = false;
    private Thread runner = null;
    
    static {
         URL.setURLStreamHandlerFactory(new TCPURLStreamHandlerFactory());
    }
    
    public static void main(String[] args) {
        Monitor m = new Monitor();
        
        m.start();
    }
    
    public void start() {
        while (true) {
            if (this.runner == null) {
                this.runner = new Thread(this, getClass().getName() + ":thread");
        
                this.runner.setDaemon(true);
                this.runner.start();
            }
        
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
    
    public void stop() {
        if (this.runner != null) {
            this.runner.interrupt();
        }
    }
    
    public void run() {
System.out.println("date: " + new Date());
        initialize();
        probe();
        validate();
        persist();
        
        this.runner = null;
    }
    
    public boolean isRunning() {
        return this.runner != null;
    }
    
    public String toString() {
        Properties p = loadProperties();
        
        return p != null ? p.toString() : "";
    }
    
    private void initialize() {
        this.addresses = loadProperties();
    }
    
    private Properties loadProperties() {
        Properties p = new Properties();
        boolean isValid = false;
        
        try {
            p.load(new URI(MONITORS).toURL().openStream());
            
            isValid = true;
        } catch (URISyntaxException use) {
            use.printStackTrace();
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        return isValid ? p : null;
    }
    
    private void probe() {
        this.probes = new ArrayList();
        
        for (Iterator ai = this.addresses != null ?
            this.addresses.keySet().iterator() :
            Collections.EMPTY_SET.iterator();
            ai.hasNext(); ) {
            try {
                this.probes.add(new Probe(new URL((String)ai.next())));
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            }
        }
        
        List t = new ArrayList();
        
        for (Iterator pi = this.probes.iterator(); pi.hasNext(); ) {
            Thread pt = new Thread((Probe)pi.next(), "Probe:thread");
            
            pt.setDaemon(true);
            
            t.add(pt);
        }
        
        for (Iterator ti = t.iterator(); ti.hasNext(); ) {
            ((Thread)ti.next()).start();
        }
        
        // hard wait
        
        try {
            Thread.sleep(WAIT);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        
        for (Iterator ti = t.iterator(); ti.hasNext(); ) {
            ((Thread)ti.next()).interrupt();
        }
    }
    
    private void validate() {
        for (Iterator ai = this.addresses != null ?
            this.addresses.keySet().iterator() :
            Collections.EMPTY_SET.iterator();
            ai.hasNext(); ) {
            String a = (String)ai.next();
            URL u = null;
            
            try {
                u = new URL(a);
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            }
            
            boolean valid = false;
            
            for (Iterator pi = this.probes.iterator();
                pi.hasNext() && ! valid; ) {
                Probe p = (Probe)pi.next();
                
                valid = p.getURL().equals(u) && (p.getResponse() != null);
            }
            
            this.addresses.setProperty(a, valid ? STATE_GOOD : STATE_BAD);
//System.out.println("a: " + a + " " + valid);
        }
    }
    
    private void persist() {
        if (this.addresses != null) {
            try {
                this.addresses.store(new FileOutputStream(new File(new URI(MONITORS))), null);
            } catch (URISyntaxException use) {
                use.printStackTrace();
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}

class Probe
implements Runnable {
    
    private static final String HTTP_SCHEME = "http";
    private static final String HTTP_PREAMBLE = "GET / HTTP/1.0";
    private static final char NEW_LINE = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    
    private URL u = null;
    private String response = null;
    
    public Probe(URL u) {
        this.u = u;
    }
    
    public URL getURL() {
        return this.u;
    }
    
    public String getResponse() {
        return this.response;
    }
    
    public void run() {
        URLConnection c = null;
        
        try {
            c = u.openConnection();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        if (c != null) {
//            c.setDoOutput(true);
            c.setDoInput(true);
            
//            if (this.u.getProtocol().equalsIgnoreCase(HTTP_SCHEME)) {
//                OutputStream os = getOutputStream(c);
//
//                if (os != null) {
//                    try {
//                        write(os, HTTP_PREAMBLE + NEW_LINE + CARRIAGE_RETURN);
//                    } catch (IOException ioe) {
//                        ioe.printStackTrace();
//                    }
//                } else {
//                    System.out.println("invalid connection");
//                }
//            }

            InputStream is = getInputStream(c);

            if (is != null) {
                String r = null;

                try {
                    this.response = read(is);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }
    
    private OutputStream getOutputStream(URLConnection c) {
        OutputStream os = null;
        
        try {
            os = c.getOutputStream();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        return os;
    }
    
    private InputStream getInputStream(URLConnection c) {
        InputStream is = null;
        
        try {
            is = c.getInputStream();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        return is;
    }
    
    private void write(OutputStream os, String msg)
    throws IOException {
        os.write(msg.getBytes());
        os.flush();
    }
    
    private String read(InputStream is)
    throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader br= new BufferedReader(new InputStreamReader(is));
        int c = -1;
        
        while ((c = br.read()) != -1) {
            sb.append((char)c);
        }
        
        return sb.toString();
    }
}

class TCPURLStreamHandlerFactory
implements URLStreamHandlerFactory {
    
    private static final String TCP_SCHEME = "tcp";
    
    public URLStreamHandler createURLStreamHandler(String p) {
        TCPURLStreamHandler h = null;

        if (p.equalsIgnoreCase(TCP_SCHEME)) {
            h = new TCPURLStreamHandler();
        }

        return h;
    }
}

class TCPURLStreamHandler
extends URLStreamHandler {
    public URLConnection openConnection(URL u) {
        return new TCPURLConnection(u);
    }
}

class TCPURLConnection
extends URLConnection {
    
    private static final int DEFAULT_PORT = 80;
    private static final char DOT = '.';
    private static final int MAX_WAIT = 20 * 1000;
    
    private Socket socket = null;
    private Object lock = new Object();
    
    private boolean connected = false;
    
    public TCPURLConnection(URL u) {
        super(u);
    }
    
    public InputStream getInputStream()
    throws IOException {
        connect();
        
        return this.socket.getInputStream();
    }
    
    public OutputStream getOutputStream()
    throws IOException {
        connect();
        
        return this.socket.getOutputStream();
    }
    
    public void connect()
    throws IOException {
        SocketAddress a = getAddress(getURL());
        
        if (! this.connected &&
            a != null) {
            synchronized (this.lock) {
                this.socket = new Socket();
                
                try {
                    this.socket.connect(a, MAX_WAIT);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                
                this.connected = this.socket.isConnected();
            }
        }
    }
    
    private SocketAddress getAddress(URL u) {
        SocketAddress sa = null;
        String h = u != null ? u.getHost() : null;
        int p = u != null ? u.getPort() : DEFAULT_PORT;

        try {
            sa = new InetSocketAddress(InetAddress.getByAddress(toBytes(h)), p);
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
        }

        return sa;
    }

    private byte[] toBytes(String s) {
        byte[] b = null;
        byte[] a = s != null ? s.trim().getBytes() : null;

        if (a != null &&
            a.length > 0) {
            b = new byte[4];

            int j = 0;
            char c;

            for (int i = 0; i < a.length; i++ ) {
                c = (char)a[i];

                if (c != DOT) {
                    b[j] = (byte)(b[j] * 10 + Character.digit(c, 10));
                } else {
                    j++;
                }
            }
        }

        return b;
    }
}
