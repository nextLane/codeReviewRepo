package infinite.vpnpack;

import android.net.VpnService;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ProxyConnectionHandler implements Runnable {
    private static final int BUFFER_SIZE = 8192;

    Socket mProxySocket;
    Socket mOutsideSocket;


    public ProxyConnectionHandler(Socket proxySocket) {
        mProxySocket = proxySocket;

    }



    @Override
    public void run() {
        try{

            VpnService vpnService= new VpnService();
            long startTimestamp = System.currentTimeMillis();

            InputStream proxyInputStream = mProxySocket.getInputStream();

          //  byte[] bytes = new byte[BUFFER_SIZE];
            ByteBuffer bytes = ByteBuffer.allocate(8192);
            int bytesRead = proxyInputStream.read(bytes.array(), 0, BUFFER_SIZE);
            TCP_IP TCP_debug = new TCP_IP(bytes);
            TCP_debug.debug();
            String destIP = TCP_debug.getDestination();

              Log.d("IPCHost:", destIP);
            InetAddress address = InetAddress.getByName(destIP);
            System.out.println(" *******from Proxy Host adress:" + address.getHostAddress()); // Gaunamas IP (185.11.24.36)
            System.out.println("from Proxy Host name:" + address.getHostName()); // www.15min.lt


               //  vpnService.protect(mOutsideSocket);
           // Log.d("***", "sock protected");
            //mOutsideSocket.setKeepAlive(true);

           // Log.d("***", "set alive" + mOutsideSocket.isConnected());

            // String request = new String(bytes);
             String host = address.getHostName();
            // Log.d("**~~~**psRequest Host: ", host);
        //     MyProxyServer.addToBag(host);
             int port = TCP_debug.getPort();

            if (port == 443) {
                Log.d("***:", "443");
                new Https443RequestHandler(mProxySocket).handle(host);
            } else {
                Log.d("***", "before sock "+port);
                Socket mOutsideSocket = SocketChannel.open().socket();
                if ((null !=  mOutsideSocket) && (null != vpnService)) {
                    vpnService.protect( mOutsideSocket);
                }
                mOutsideSocket.connect(new InetSocketAddress(address.getHostName(),80));

                //  mOutsideSocket = new Socket(address.getHostName(),80);
                Log.d("***", "sock created");



                mOutsideSocket.setKeepAlive(true);
             //   Log.d("***", "set alive"+mOutsideSocket.isConnected());
               // try{
                 //   mOutsideSocket.connect(new InetSocketAddress(host, 80));
             //       Log.d("***", "tried");
               // } catch (UnknownHostException e1) {
                 //   Log.d("***", "failed");
                   // e1.printStackTrace();
                //}
               // mOutsideSocket = new Socket(host, port);
                Log.d("***", "outside socket created "+mOutsideSocket.isConnected());
                OutputStream outsideOutputStream = mOutsideSocket.getOutputStream();
                outsideOutputStream.write(bytes.array(), 0, bytesRead);
                outsideOutputStream.flush();

                InputStream outsideSocketInputStream = mOutsideSocket.getInputStream();
                OutputStream proxyOutputStream = mProxySocket.getOutputStream();
                byte[] responseArray = new byte[BUFFER_SIZE];
                Log.d("80: ", "i/p o/p streams fetched!");

                do {
                    bytesRead = outsideSocketInputStream.read(responseArray, 0, BUFFER_SIZE);
                    Log.d("No of bytes read:",""+bytesRead+"***"+responseArray);

                    if (bytesRead > 0) {
                        proxyOutputStream.write(responseArray, 0, bytesRead);
                        String response = new String(bytes.array(), 0, bytesRead);
                        Log.d("Outside IPS Response: ", response);
                    }
                } while (bytesRead > 0);


                proxyOutputStream.flush();
                mOutsideSocket.close();

           }
           mProxySocket.close();

            Log.d("ACHTUNG", "Cycle: " + (System.currentTimeMillis() - startTimestamp));

        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    }
