package infinite.vpnpack;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Enumeration;


public class Vpn extends VpnService implements Handler.Callback, Runnable {

    private static final String TAG = "ToyVpnService";
    DatagramChannel tunnel;
    private String mServerAddress;
    private String mServerPort;
 //   private byte[] mSharedSecret;
    private PendingIntent mConfigureIntent;

    private Handler mHandler;
    private Thread mThread;

    private ParcelFileDescriptor mInterface;
    private String mParameters;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }

        // Extract information from the intent.
        String prefix = getPackageName();
        mServerAddress = "127.0.0.1";
        mServerPort = "8080";
     //   mSharedSecret = "symphony";

        // Start a new session by creating a new thread.
        mThread = new Thread(this, "ToyVpnThread");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }



    @Override
    public synchronized void run() {

        try {
            Log.i(TAG, "Starting");

            // If anything needs to be obtained using the network, get it now.
            // This greatly reduces the complexity of seamless handover, which
            // tries to recreate the tunnel without shutting down everything.
            // In this demo, all we need to know is the server address.
            InetSocketAddress server = new InetSocketAddress(
                    mServerAddress, Integer.parseInt(mServerPort));

           // We try to create the tunnel for several times. The better way
            // is to work with ConnectivityManager, such as trying only when
            // the network is avaiable. Here we just use a counter to keep
            // things simple.
           /* for (int attempt = 0; attempt < 10; ++attempt) {
                mHandler.sendEmptyMessage(R.string.connecting);

                // Reset the counter if we were connected.
                if (run(server)) {
                    Log.d("TT:","RUN SERVER VPN Connected :D");
                    attempt = 0;
                    break;
                }

                // Sleep for a while. This also checks if we got interrupted.
                Thread.sleep(3000);
            }*/

            run(server);
            Thread.sleep(3000);
            Log.i(TAG, "Server set okay !");
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
             //   mInterface.close();
            } catch (Exception e) {
                // ignore
            }

        }
    }

    private boolean run(InetSocketAddress server) throws Exception {
        final int BUFFER_SIZE = 8192;  //largest size of MTU

         tunnel = null;
        boolean connected = false;
        try {
            // Create a DatagramChannel as the VPN tunnel.
            tunnel = DatagramChannel.open();
            Log.i(TAG, "tunnel open !");

            // Protect the tunnel before connecting to avoid loopback.
            if (!protect(tunnel.socket())) {
                throw new IllegalStateException("Cannot protect the tunnel");
            }

            // Connect to the server.
            tunnel.connect(server);

            Log.i(TAG, "Tunnel connects to server !");

            // For simplicity, we use the same thread for both reading and
            // writing. Here we put the tunnel into non-blocking mode.
            tunnel.configureBlocking(false);
            configure();
            // Authenticate and configure the virtual network interface.
            //    handshake(tunnel);
            Log.i(TAG, "Hand shaked!");

            // Now we are connected. Set the flag and show the message.
            connected = true;
            mHandler.sendEmptyMessage(R.string.connected);

            // Packets to be sent are queued in this input stream.
            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());

            // Packets received need to be written to this output stream.
            FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());

            // Allocate the buffer for a single packet.
            byte[] bytes = new byte[BUFFER_SIZE];

            // We use a timer to determine the status of the tunnel. It
            // works on both sides. A positive value means sending, and
            // any other means receiving. We start with receiving.
            int timer = 0;
            Log.i(TAG, "Starting frwding packets !");

//            Socket echoSocket = new Socket("::1", 8090);
            //  PrintWriter outputFromProxy =
            //        new PrintWriter(echoSocket.getOutputStream(), true);
  //          OutputStream inputToProxy =
    //                (echoSocket.getOutputStream());

            // We keep forwarding packets till something goes wrong.
            while (true) {


                // Read the outgoing packet from the input stream.
               // int bytesRead = in.read(bytes, 0, BUFFER_SIZE);

                ByteBuffer packet = ByteBuffer.allocate(8192);
            //    DatagramChannel tunnel = mTunnel;
         //       FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());
                int length;
             //   String destIP;

                try
                {

                        while ((length = in.read(packet.array())) > 0) {
                            Socket echoSocket = new Socket("::1", 8090);
                            //  PrintWriter outputFromProxy =
                            //        new PrintWriter(echoSocket.getOutputStream(), true);
                            OutputStream inputToProxy =
                                    (echoSocket.getOutputStream());

                        //    InputStream outputFromProxy =
                          //          (echoSocket.getInputStream());

                            packet.limit(length);
                            Log.d(TAG, "Total Length:" + length);

                         //    tunnel.write(packet);
                         //   packet.flip();



        //                        String response = new String(bytes, 0, bytesRead);
                            //    Log.d("Outside IPS Response: ", response);

                            TCP_IP TCP_debug = new TCP_IP(packet);
                            TCP_debug.debug();
                            String destIP = TCP_debug.getDestination();

                          //  Log.d("Host:", TCP_debug.getHostname());
                             InetAddress address = InetAddress.getByName(destIP);
                            System.out.println("Host address:" + address.getHostAddress()); // Gaunamas IP (185.11.24.36)
                              System.out.println("Host name:" + address.getHostName()); // www.15min.lt
                                                        inputToProxy.write(packet.array(), 0, length);
                           // Log.d(TAG, "Writing to tunnel now...");
                           // tunnel.write(packet);
                           //  out.write(packet.array(), 0, length);
                            packet.clear();

                            Thread.sleep(100);
                            InputStream outputFromProxy = (echoSocket.getInputStream());

                            int len=0;
                            while((len=outputFromProxy.read(packet.array()))>0)
                            {


                                String response = new String(packet.array(), 0, len);
                                //byte[] bufferOutput = new byte[32767];
                                //inBuffer.read(bufferOutput);
                               // if (bufferOutput.length > 0) {
                              //      String recPacketString = new String(packet,0,packet.array().length,"UTF-8");
                                    Log.d(TAG, "recPacketString");
                            //    FileOutputStream o = new FileOutputStream(mInterface.getFileDescriptor());
                             byte [] arr= {'O','K'};
                                out.write(arr);
                               //     out.write(packet.array());
                                Log.d("vpn fd op:", response);
                                packet.clear();

                                Thread.sleep(100);
                            }
                            echoSocket.close();
                        }




                }


                catch (IOException e)
                {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }



/*                if (bytesRead > 0) {
                    inputToProxy.write(bytes, 0, bytesRead);
                    String response = new String(bytes, 0, bytesRead);
                    Log.d("Outside IPS Response: ", response);
                }
                */
                //     if (length > 0) {
                // Write the outgoing packet to the tunnel.
                //   packet.limit(length);
                //  Log.i("TTT:", "Reading packet");
                // //  tunnel.write(packet);
                //  Log.i("Packet***:", bytes.toString());
                //  inputToProxy.write(packet.array());
                //  Log.i("TTT:", "Written to tunnel"+packet);

                //  packet.clear();

                // There might be more outgoing packets.
                // idle = false;

                // If we were receiving, switch to sending.
                // if (timer < 1) {
                //    timer = 1;
                // }
            //    echoSocket.close();
            }
/*

                // Read the incoming packet from the tunnel.
                length = tunnel.read(packet);
                if (length > 0) {
                    // Ignore control messages, which start with zero.
                    Log.i("TTT:", "Receiving packet");
                    if (packet.get(0) != 0) {
                        // Write the incoming packet to the output stream.
                        out.write(packet.array(), 0, length);
                        Log.i("TTT:", "Delivering packet");
                    }
                    packet.clear();

                    // There might be more incoming packets.
                    idle = false;

                    // If we were sending, switch to receiving.
                    if (timer > 0) {
                        timer = 0;
                    }
                }
*/
            // If we are idle or waiting for the network, sleep for a
            // fraction of time to avoid busy looping.
            //     if (idle) {
            //       Thread.sleep(100);

            // Increase the timer. This is inaccurate but good enough,
            // since everything is operated in non-blocking mode.
            //       timer += (timer > 0) ? 100 : -100;

            // We are receiving for a long time but not sending.
            //     if (timer < -15000) {
            // Send empty control messages.
            //       packet.put((byte) 0).limit(1);
            //     for (int i = 0; i < 3; ++i) {
            //       packet.position(0);
            //     tunnel.write(packet);
            //    }
            //  packet.clear();

            // Switch to sending.
            //     timer = 1;
            // }

            // We are sending for a long time but not receiving.
        //    if (timer > 20000) {
          //      throw new IllegalStateException("Timed out");
           // }

        }
        catch(Exception e) {
            e.printStackTrace();
        }
          // }

            // Assume that we did not make any progress in this iteration.
            //boolean idle = true;

            // Read the outgoing packet from the input stream.
           // int length = in.read(packet.array());


  /*          while (true) {

                byte[] bytes = new byte[32767];
                int bytesRead = in.read(bytes, 0, 32767);
                String request = new String(bytes);
                // String host = extractHost(request);
                // Log.d("**~~~** Request Host: ", host);
                /*
                String request = new String(packet);
                String host = extractHost(request); */

               /* Log.d(TAG, "************new packet");

                while (packet.hasRemaining()) {
                    Log.d(TAG, "" + packet.get());
                    //System.out.print((char) packet.get());
                }
                packet.limit(length);
                tunnel.write(packet);
                packet.clear();

                // There might be more outgoing packets.
                idle = false;

                Thread.sleep(50);
            }*/

     /*   } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            Log.i(TAG, "Inside server run !");

            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                tunnel.close();
            } catch (Exception e) {*/
                // ignore
        //    }
        //}
        return connected;
    }

    private void handshake(DatagramChannel tunnel) throws Exception {
        // To build a secured tunnel, we should perform mutual authentication
        // and exchange session keys for encryption. To keep things simple in
        // this demo, we just send the shared secret in plaintext and wait
        // for the server to send the parameters.

        // Allocate the buffer for handshaking.
        ByteBuffer packet = ByteBuffer.allocate(1024);

        // Control messages always start with zero.
//        packet.put((byte) 0).put(mSharedSecret).flip();

        // Send the secret several times in case of packet loss.
        for (int i = 0; i < 3; ++i) {
            packet.position(0);
            tunnel.write(packet);
        }
        packet.clear();

        // Wait for the parameters within a limited time.
        for (int i = 0; i < 50; ++i) {
            Thread.sleep(100);

            // Normally we should not receive random packets.
            int length = tunnel.read(packet);
            if (length > 0 && packet.get(0) == 0) {
                configure();
                return;
            }
        }
        throw new IllegalStateException("Timed out");
    }

    private void configure() throws Exception {
        // If the old interface has exactly the same parameters, use it!
        if (mInterface != null ) {
            Log.i(TAG, "Using the previous interface");
            return;
        }

        // Configure a builder while parsing the parameters.
        Builder builder = new Builder();

        // Create a new interface using the builder and save the parameters.
        // Configure a builder while parsing the parameters.
        builder = new Builder();
        builder.setMtu(1500);
        builder.addAddress("10.0.0.2", 24);
        builder.addRoute("0.0.0.0", 0);
      //  builder.addDnsServer("8.8.8.8");

        mInterface = builder.establish();

        Log.i(TAG, "New interface***");
    }
}

class TCP_IP extends Vpn {

    private ByteBuffer packet;
    private String hostname;
    private String destIP;
    private String sourceIP;
    private int version;
    private int protocol;
    private int port;


    public TCP_IP(ByteBuffer pack) {
        this.packet = pack;
    }

    public void debug() {


        int buffer = packet.get();
        int headerlength;
        int temp;

        version = buffer >> 4;
        headerlength = buffer & 0x0F;
        headerlength *= 4;
        System.out.println("IP Version:"+version);
        System.out.println("Header Length:"+headerlength);
        String status = "";
        status += "Header Length:"+headerlength;

        buffer = packet.get();      //DSCP + EN
        buffer = packet.getChar();  //Total Length

        System.out.println( "Total Length:"+buffer);

        buffer = packet.getChar();  //Identification
        buffer = packet.getChar();  //Flags + Fragment Offset
        buffer = packet.get();      //Time to Live
        buffer = packet.get();      //Protocol

        protocol = buffer;
        System.out.println( "Protocol:"+buffer);

        status += "  Protocol:"+buffer;

        buffer = packet.getChar();  //Header checksum


        byte buff = (byte)buffer;

        sourceIP  = "";
        buff = packet.get();  //Source IP 1st Octet
        temp = ((int) buff) & 0xFF;
        sourceIP += temp;
        sourceIP += ".";

        buff = packet.get();  //Source IP 2nd Octet
        temp = ((int) buff) & 0xFF;
        sourceIP += temp;
        sourceIP += ".";

        buff = packet.get();  //Source IP 3rd Octet
        temp = ((int) buff) & 0xFF;
        sourceIP += temp;
        sourceIP += ".";

        buff = packet.get();  //Source IP 4th Octet
        temp = ((int) buff) & 0xFF;
        sourceIP += temp;

        System.out.println( "Source IP:"+sourceIP);

        status += "   Source IP:"+sourceIP;


        destIP  = "";


        buff = packet.get();  //Destination IP 1st Octet
        temp = ((int) buff) & 0xFF;
        destIP += temp;
        destIP += ".";

        buff = packet.get();  //Destination IP 2nd Octet
        temp = ((int) buff) & 0xFF;
        destIP += temp;
        destIP += ".";

        buff = packet.get();  //Destination IP 3rd Octet
        temp = ((int) buff) & 0xFF;
        destIP += temp;
        destIP += ".";

        buff = packet.get();  //Destination IP 4th Octet
        temp = ((int) buff) & 0xFF;
        destIP += temp;

        System.out.println( "Destination IP:" + destIP);
        status += "   Destination IP:"+destIP;




    }


    public String getDestination() {
        return destIP;
    }

    public int getProtocol() {
        return protocol;
    }

    public int getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }

    public int getIPversion() { return version; }

}

