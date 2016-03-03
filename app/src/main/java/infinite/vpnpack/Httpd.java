package infinite.vpnpack;



        import java.io.IOException;
        import java.util.Map;

        import fi.iki.elonen.NanoHTTPD;
        import android.app.Activity;
        import android.content.ComponentName;
        import android.content.Context;
        import android.content.Intent;
        import android.content.ServiceConnection;
        import android.net.VpnService;
        import android.os.Bundle;
        import android.os.Environment;
        import android.os.IBinder;
        import android.util.Log;
        import android.view.View;
        import android.widget.Toast;

        import java.io.*;
        import java.util.*;


public class Httpd extends Activity implements View.OnClickListener
{
    private MyProxyServer serverService;
    static boolean mBound =false;   //this keeps track of whether it is bound to the server service or not
    static boolean onStatus=false;  // this keeps track of the state of on off switch button, can be moved to shared prefs
    private WebServer server;
    Object obj;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

       Intent intent= new Intent(Httpd.this, MyProxyServer.class);
       bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

     //   server = new WebServer();
     //   try {
     //       server.start();
     //       Log.d("oal:::::", "Server must run now check!");
     //   } catch (IOException e) {

     //       e.printStackTrace();
      //  }
        //start service
       // Log.d("oal:::::", "Server must run now check!");

         obj=    startService(new Intent(getBaseContext(), MyProxyServer.class));
        Log.d("oal:::::", "service started workin!");

     /*   try {
            server.start();
        } catch(IOException ioe) {
            Log.d("e:",ioe.getMessage().toString());
            Log.w("Httpd", "The server could not start.");
        }
        Log.w("Httpd", "Web server initialized.");
*/
        Toast.makeText(this, "Server set, go connect!", Toast.LENGTH_SHORT);
        findViewById(R.id.connect).setOnClickListener(this);


    }
    //The method below is responsible or establishing connection with the service running Proxy Server
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            MyProxyServer.MyBinder b = (MyProxyServer.MyBinder) binder;
            serverService = b.getService();
            Toast.makeText(Httpd.this, "Server set", Toast.LENGTH_SHORT)
                    .show();
            mBound=true;
        }

        public void onServiceDisconnected(ComponentName className) {
            serverService = null;
            mBound= false;
        }
    };

    @Override
    public void onClick(View v) {
        Intent intent = VpnService.prepare(getApplicationContext());

        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

                Intent intent = new Intent(this, Vpn.class);
                startService(intent);

            }
            else
                Log.d("OOOO:","Result not ok!");

    }

    // DON'T FORGET to stop the server
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (server != null)
            server.stop();
    }

    private class WebServer extends NanoHTTPD {

        public WebServer()
        {
            super(8090);
        }



        @Override
        public Response serve(String uri, Method method,
                              Map<String, String> header,
                              Map<String, String> parameters,
                              Map<String, String> files) {
            String answer = "hello world";
            try {
                // Open file from SD Card
                File root = Environment.getExternalStorageDirectory();
                FileReader index = new FileReader(root.getAbsolutePath() +
                        "/www/index.html");
                BufferedReader reader = new BufferedReader(index);
                String line = "";
                while ((line = reader.readLine()) != null) {
                    answer += line;
                }
                reader.close();

            } catch(IOException ioe) {
                Log.w("Httpd", ioe.toString());
            }


            return new NanoHTTPD.Response(answer);
        }
    }

}