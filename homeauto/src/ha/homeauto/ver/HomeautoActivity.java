package ha.homeauto.ver;

import ha_util.HA_utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class HomeautoActivity extends Activity implements OnClickListener {

    private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private Pattern pattern;
    private Matcher matcher;

    /** Properties **/
    protected Button inc_cmd_number;
    protected Button dec_cmd_number;
    protected Button inc_option_number;
    protected Button dec_option_number;
    protected Button getIface;
    protected Button sendCMD;
    protected TextView commandLabel;
    protected TextView option_Label;
    protected TextView option_Text;
    protected TextView IPaddrLabel;
    protected TextView cmdResultLabel;
    protected TextView batteryLabel;

    protected int brewTime = 3;
    protected CountDownTimer brewCountDownTimer;
    protected int brewCount = 0;
    protected boolean isBrewing = false;
    protected boolean UDP_timeout = false;
    protected boolean IP_validation = false;
    protected boolean cmd_get = true;
    protected boolean cmd_set = false;

    boolean iface_get = true;
    int cmd_number = 0;
    int cmd_max_number = 0;
    String IMM_string = "";
    String[] cmd_List = { "ping", "", "", "", "", "", "", "", "", "" };
    String options_brief = "";
    int MTU_SIZE = 600;
    int CMD_SIZE = 600;

    String Config_file = "/udisk/lirc/lircd.conf";
    // String Config_file = "/LocalDisk/lircd.conf";
    String Error_string = "{" + Config_file + "}" + " not found";
    String[] IR_codes_List;
    String[] options_arr;

    int bat_u = 0;
    int mVoltage = 0;
    int IR_names_cnt = 0;
    int IR_names_max_number = 0;
    boolean IR_conf_flag = false;

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        inc_cmd_number = (Button) findViewById(R.id.cmd_increment);
        dec_cmd_number = (Button) findViewById(R.id.cmd_decrement);
        inc_option_number = (Button) findViewById(R.id.option_increment);
        dec_option_number = (Button) findViewById(R.id.option_decrement);
        option_Label = (TextView) findViewById(R.id.option_wiev);
        option_Text = (TextView) findViewById(R.id.option_text);
        sendCMD = (Button) findViewById(R.id.cmd_start);
        getIface = (Button) findViewById(R.id.cmd_get);
        commandLabel = (TextView) findViewById(R.id.command_label);
        batteryLabel = (TextView) findViewById(R.id.battery_label);
        IPaddrLabel = (TextView) findViewById(R.id.IP_label);
        cmdResultLabel = (TextView) findViewById(R.id.info_wiev);

        // Setup ClickListeners
        inc_option_number.setOnClickListener(this);
        dec_option_number.setOnClickListener(this);
        inc_cmd_number.setOnClickListener(this);
        dec_cmd_number.setOnClickListener(this);
        getIface.setOnClickListener(this);
        sendCMD.setOnClickListener(this);
        cmdResultLabel.setOnClickListener(this);

        try // Read configuration file
        {
            File myFile = new File(Config_file);
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(
                    fIn));
            HA_utils string_parser = new HA_utils();
            String aDataRow = "";
            String aBuffer = "";
            aDataRow = myReader.readLine();
            IPaddrLabel.setText(aDataRow);

            while ((aDataRow = myReader.readLine()) != null) {
                if (aDataRow.contains("raw_codes")) {
                    IR_conf_flag = true;
                }
                ;

                if (IR_conf_flag) {
                    if (aDataRow.contains("name")) {
                        if (0 != IR_names_max_number)
                            aBuffer += " ! ";
                        aBuffer += "\n" + aDataRow + ' ';
                        IR_names_max_number++;
                    }

                    if (string_parser.isNumericSpace(aDataRow)) {
                        aBuffer += aDataRow + ' ';
                    }
                }
                ;
            }
            myReader.close();

            IR_codes_List = string_parser.get_cmd_List(aBuffer, "[!]");

            String[] options_parsed;
            for (int i = 0; i < IR_names_max_number; i++) {
                // delete word "name" and extra spaces and tabs
                IR_codes_List[i] = IR_codes_List[i].replace("name", "");
                IR_codes_List[i] = IR_codes_List[i].replaceAll("\\s+", " ");
                IR_codes_List[i] = IR_codes_List[i].replaceAll("^\\s+", "");
                IR_codes_List[i] = IR_codes_List[i].replaceAll("\\s+$", "");
                options_parsed = IR_codes_List[i].split(" ", 2);
                options_brief += options_parsed[0] + " ";
            }
            Toast.makeText(getBaseContext(), options_brief, Toast.LENGTH_LONG)
                    .show();

            IR_names_max_number--;
            options_arr = string_parser.get_cmd_List(options_brief, "[ ]");
        }

        catch (Exception e) {
            Toast.makeText(getBaseContext(), Error_string, Toast.LENGTH_LONG)
                    .show();
        }

        IMM_string = "192.168.1.34";
        cmd_number = 0;
        IP_validation = true;

        IPaddrLabel.setText(IMM_string);
        cmdResultLabel.setText("Enter IP from soft keys");
        inc_cmd_number.setText(" + ");
        dec_cmd_number.setText(" - ");
        inc_option_number.setText(" >> ");
        dec_option_number.setText(" << ");
        option_Text.setText("Command Options");
        option_Label.setText(" ");
        getIface.setText("Get interface ");
        sendCMD.setText("Send Command");
        commandLabel.setText("Command # " + Integer.toString(cmd_number));

        // Setup Battery Listener
        BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
            int level = -1;

            @Override
            public void onReceive(Context context, Intent intent) {
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                batteryLabel.setText("Battery: " + String.valueOf(level) + "%");
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    } // end OnCreate

    // @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        HA_utils parser = new HA_utils();
        if (keyCode == KeyEvent.KEYCODE_BACK)
            finish();
        if (keyCode == KeyEvent.KEYCODE_0)
            IMM_string += "0";
        if (keyCode == KeyEvent.KEYCODE_1)
            IMM_string += "1";
        if (keyCode == KeyEvent.KEYCODE_2)
            IMM_string += "2";
        if (keyCode == KeyEvent.KEYCODE_3)
            IMM_string += "3";
        if (keyCode == KeyEvent.KEYCODE_4)
            IMM_string += "4";
        if (keyCode == KeyEvent.KEYCODE_5)
            IMM_string += "5";
        if (keyCode == KeyEvent.KEYCODE_6)
            IMM_string += "6";
        if (keyCode == KeyEvent.KEYCODE_7)
            IMM_string += "7";
        if (keyCode == KeyEvent.KEYCODE_8)
            IMM_string += "8";
        if (keyCode == KeyEvent.KEYCODE_9)
            IMM_string += "9";
        if (keyCode == KeyEvent.KEYCODE_DEL)
            IMM_string = parser.cutString(IMM_string);
        if (keyCode == KeyEvent.KEYCODE_PERIOD)
            IMM_string += ".";
        IPaddrLabel.setText(IMM_string);

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            inputManager.toggleSoftInput(0, 0);
            if (validate(IMM_string)) {
                IPaddrLabel.setText("IP=" + IMM_string);
                IP_validation = true;
            } else {
                IPaddrLabel.setText("bad IP");
                IMM_string = "";
                IP_validation = false;
            }
        }
        return false;
    }

    public void onClick(View v) {
        if (v == inc_option_number) {
            if (IR_names_max_number > IR_names_cnt)
                IR_names_cnt++;
            option_Label.setText(options_arr[IR_names_cnt]);
        }

        if (v == dec_option_number) {
            if (0 < IR_names_cnt)
                IR_names_cnt--;
            option_Label.setText(options_arr[IR_names_cnt]);
        }

        if (v == cmdResultLabel) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputManager.toggleSoftInput(0, 0);
        }
        if (v == inc_cmd_number) {
            if (cmd_max_number > cmd_number)
                cmd_number++;
            commandLabel.setText("Command # " + String.valueOf(cmd_number));
            cmdResultLabel.setText((cmd_List[cmd_number]));
        }

        else if (v == dec_cmd_number) {
            if (0 < cmd_number)
                cmd_number--;
            commandLabel.setText("Command # " + String.valueOf(cmd_number));
            cmdResultLabel.setText((cmd_List[cmd_number]));
        }

        else if (v == getIface) {
            if (IP_validation) {
                UDP_timeout = false;
                UDP_send(IMM_string, cmd_number, cmd_get);
            } else {
                cmdResultLabel.setText("enter valid IP");
            }
        }

        else if (v == sendCMD) {
            if (IP_validation) {
                UDP_send(IMM_string, cmd_number, cmd_set);
            } else {
                cmdResultLabel.setText("enter valid IP");
            }
        }
    }

    public void UDP_send(String IPadr, int command, boolean iface_get) {
        // MulticastSocket serverSocket = new MulticastSocket(14666);
        int UDPlistenPort = 14666;
        byte[] receiveData = new byte[64];
        int counter = 0;

        DatagramSocket serverSocket = null;
        InetAddress IPAddress = null;
        DatagramPacket receivePacket = null;
        boolean UDP_wait = true;
        HA_utils parser = new HA_utils();

        sendCMD.setText("Send Command");
        try {
            serverSocket = new DatagramSocket(UDPlistenPort);
            try {
                serverSocket.setSoTimeout(1000);
            } catch (SocketException e1) {
                cmdResultLabel.setText("Error 1!");
            }

            try {
                IPAddress = InetAddress.getByName(IPadr);
            } catch (UnknownHostException e) {
                cmdResultLabel.setText("Error 2!");
            }

            String query = new String("");
            String get = new String("get");
            String set = new String("set");
            String cmd = new String("  ");
            int items_number = 0;
            int[] ints_arr = new int[256];
            DatagramPacket sendResponse;

            commandLabel.setText("Command # " + String.valueOf(command));

            if (command >= 0) {
                switch (command) {
                case 0:
                    cmd = "0 ";
                    break;
                case 1:
                    cmd = "1 ";
                    break;
                case 2:
                    cmd = "2 ";
                    break;
                case 3:
                    cmd = "3 ";
                    break;
                case 4:
                    cmd = "4 ";
                    break;
                case 5:
                    cmd = "5 ";
                    break;
                case 6:
                    cmd = "6 ";
                    break;
                case 7:
                    cmd = "7 ";
                    break;
                case 8:
                    cmd = "8 ";
                    break;
                case 9:
                    cmd = "9 ";
                    break;
                default:
                    cmd = "0 ";
                    break;
                }
                ;
            }

            if (iface_get) {
                query = cmd + get;
            } else {
                query = cmd + set;
                if (cmd_List[command].contains("IR")
                        && (0 != IR_names_max_number)) {
                    String[] arr = IR_codes_List[IR_names_cnt].split(" ", 2);
                    items_number = parser.get_strings_count(arr[1]);
                    ints_arr = parser
                            .String2int(arr[1], ints_arr, items_number);
                }

                if (cmd_List[command].contains("IR")
                        && (0 == IR_names_max_number)) {
                    Toast.makeText(getBaseContext(), Error_string,
                            Toast.LENGTH_LONG).show();
                }
            }

            if (cmd_List[command].contains("IR") && (0 != IR_names_max_number)) {
                byte[] query2sent = new byte[MTU_SIZE];
                byte[] query_bytes = query.getBytes();
                int i;
                int offset;
                offset = query.length();

                for (i = 0; i < offset; i++) {
                    query2sent[i] = (byte) query_bytes[i];
                }

                offset++;
                query2sent[offset] = 0;

                for (i = 0; i <= items_number; i++) {
                    query2sent[(2 * i) + offset + 1] = (byte) ((ints_arr[i] >> 8) & 0xFF);
                    query2sent[(2 * i) + offset] = (byte) (ints_arr[i] & 0xFF);
                }

                sendResponse = new DatagramPacket(query2sent, offset
                        + (items_number + 1) * 2, IPAddress, UDPlistenPort);
            }

            else {
                byte[] query2sent = query.getBytes();
                sendResponse = new DatagramPacket(query2sent,
                        query2sent.length, IPAddress, UDPlistenPort);
            }

            try {
                serverSocket.send(sendResponse);
            } catch (IOException e) {
                cmdResultLabel.setText("Error 3!");
            }
        }

        catch (SocketException ex) {
            cmdResultLabel.setText("Error 4!");
        }
        ;

        while (UDP_wait) {
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                serverSocket.receive(receivePacket);
            } catch (InterruptedIOException e) {
                // cmdResultLabel.setText("Timeout!");
                UDP_timeout = true;
            } catch (IOException e) {
                cmdResultLabel.setText("Error 5!");
            }
            byte[] payload = receivePacket.getData();

            int cmd_counter = 0;

            cmd_counter = parser.get_List_entries_count(payload, (byte) '|');

            String sentence = new String(payload);
            cmdResultLabel.setText(sentence);
            IPaddrLabel.setText(IMM_string);
            sentence = sentence.trim();

            if (iface_get && (0 == command)) {
                if (sentence.contains("IR") && (0 < IR_names_max_number)) {
                    option_Text.setText("IR control has "
                            + Integer.toString(IR_names_max_number)
                            + " options");
                    option_Label.setText(options_arr[IR_names_cnt]);
                    option_Label.setVisibility(View.VISIBLE);
                    inc_option_number.setVisibility(View.VISIBLE);
                    dec_option_number.setVisibility(View.VISIBLE);
                } else {
                    option_Label.setVisibility(View.INVISIBLE);
                    inc_option_number.setVisibility(View.INVISIBLE);
                    dec_option_number.setVisibility(View.INVISIBLE);
                }

                cmd_max_number = cmd_counter - 1;
                if (cmd_max_number > 0) {
                    cmdResultLabel.setText("has "
                            + Integer.toString(cmd_max_number) + " commands");
                    cmd_List = parser.get_cmd_List(sentence, "[|]");
                }
            }

            else {
                sentence = sentence.trim();
                cmdResultLabel.setText(sentence);
            }
            counter++;
            if (1 == counter) {
                UDP_wait = false;
                serverSocket.close();
            }
            if (UDP_timeout) {
                cmd_max_number = 0;
                cmd_number = 0;
                commandLabel.setText("Command # " + String.valueOf(cmd_number));
                cmdResultLabel.setText("Timeout!");
            }
        }
    }

    public boolean validate(String ip) {
        pattern = Pattern.compile(IPADDRESS_PATTERN);
        matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}