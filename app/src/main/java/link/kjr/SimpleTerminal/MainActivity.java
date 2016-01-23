package link.kjr.SimpleTerminal;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import link.kjr.ndk_test.BuildConfig;
import link.kjr.ndk_test.R;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native");
    }
    public static native int get_pts();
    public static native void put_char(char c);
    public static native char get_char();

    public static native int get_buffer_size();
    public static native void read();

    Thread cpp_code;
    Thread input_thread;
    Thread reader;
    Thread update_textview;



    Handler handler;
    MainActivity activity;
    TextView textView;



    String text="";
    boolean text_has_changed=true;
    boolean is_vt_code=false;
    String vt_code="";
    public void processByte(byte c){


        char c1=(char)c;

        if(is_vt_code){

            if(!("0123456789([?;".contains(""+c1))){
                vt_code+=c1;
            }else {
                Log.e(BuildConfig.APPLICATION_ID,"got vt escape code to execute:"+vt_code);
                vt_code="";
                is_vt_code=false;
            }
        }else {
            if(c1=='\033'){
                is_vt_code=true;
            }else {
                text_has_changed=true;
                text+=c1;
            }
        }
    }
    public void trim_text(){
        int max_len=2000;
        if(text.length()>max_len){
            text=text.substring(text.length()-max_len);
        }
    }


    View main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity=this;
        RelativeLayout main=(RelativeLayout)findViewById(R.id.main);
        this.main=main;
        handler=new Handler(getMainLooper());

        main.requestFocus();

        main.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    put_char((char) event.getUnicodeChar());

                }
                return true;
            }
        });
        textView=(TextView)findViewById(R.id.terminal_view);

        textView.setMovementMethod(new ScrollingMovementMethod());

        init();
    }


    public void getkeyboard(View view) {
        main.setFocusableInTouchMode(true);
        main.setFocusable(true);
        main.requestFocus();

        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(main, InputMethodManager.SHOW_FORCED);

    }


    public void init() {
        text="";
        input_thread=new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(BuildConfig.APPLICATION_ID,"starting input thread");
                while (true){

                    String s="";
                    int size=get_buffer_size();
                    for(int i=0;i<size;i++){
                        char c=get_char();
                        s=s.concat(""+c);
                        processByte((byte)c);
                    }

                    if(s.length()>0){

                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    trim_text();
                }

            }
        });
        cpp_code=new Thread(new Runnable() {
            @Override
            public void run() {
                get_pts();
            }
        });
        reader=new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    read();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        update_textview=new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            if (text_has_changed) {
                                text_has_changed = false;
                                textView.setText(text);
                            }
                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        
        update_textview.start();
        cpp_code.start();
        input_thread.start();
        reader.start();
    }

    public void init_clicked(View view) {
        init();
    }
}
