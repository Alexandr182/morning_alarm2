package app.morningalarm;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import app.alarmmanager.AlarmSetter;
import app.database.AlarmDbUtilities;
import app.gcm.GcmRegisterer;
import app.network.ServerRequestComposer;
import app.utils.Alarm;
import app.utils.Constants;
import app.utils.Group;
import app.utils.Person;


/**
 * Created by alexandr on 1/9/14.
 */
public class GroupActivity extends Activity {

    private ArrayList<Person> personList;
    private PersonListAdapter personListAdapter;
    private int groupId;
    private Group group;
    private String alarmId;
    private Alarm alarm;
    Timer timer = new Timer();

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(GroupActivity.this, AlarmFragmentsSettingsActivity.class);
            i.putExtra("id", alarm.getId());
            GroupActivity.this.startActivityForResult(i, 0);
        }
    };

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            personList.removeAll(personList);
            personList.addAll(AlarmDbUtilities.fetchAllPersonsFromGroup(GroupActivity.this, groupId));
            personListAdapter.notifyDataSetChanged();
            emptyTextViewVisibility();
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_group);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        groupId = this.getIntent().getIntExtra("group_id", 0);
        alarmId = this.getIntent().getStringExtra("alarm_id");
        Log.d(Constants.TAG, alarmId);
        group = AlarmDbUtilities.fetchGroup(this, groupId);
        alarm = AlarmDbUtilities.fetchAlarm(this, alarmId);
        personList = AlarmDbUtilities.fetchAllPersonsFromGroup(this, groupId);

        emptyTextViewVisibility();


        LinearLayout alarmLayout = (LinearLayout) this.findViewById(R.id.LinearLayout2);
        alarmLayout.setOnClickListener(onClickListener);

        personListAdapter = new PersonListAdapter(this, R.layout.list_item_persons, personList);
        ListView lv = (ListView) findViewById(R.id.listView1);
        lv.setAdapter(personListAdapter);
        registerForContextMenu(lv);

        updateAlarmView();
        startTimer();
    }


    /**
     * metoda ce determina daca trebuie afisat sau nu textview cu textul ca nu sint persoane
     */
    private void emptyTextViewVisibility() {
        if (personList.size() > 0)
            findViewById(R.id.id_empty_list_text_view).setVisibility(View.GONE);
        else
            findViewById(R.id.id_empty_list_text_view).setVisibility(View.VISIBLE);
    }

    private void updateAlarmView() {
        ImageView iv = (ImageView) this.findViewById(R.id.alarm_iv);
        TextView tv_big = (TextView) this.findViewById(R.id.alarm_tv_big);
        TextView tv_small = (TextView) this.findViewById(R.id.alarm_tv_small);
        ToggleButton tb = (ToggleButton) this.findViewById(R.id.alarm_tb);
        tb.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                AlarmSetter aSetter = new AlarmSetter(GroupActivity.this);
                if (alarm.isEnabled() == Alarm.ALARM_ENABLED) {
                    alarm.setEnabled(Alarm.ALARM_DISABLED);
                    AlarmDbUtilities.updateAlarm(GroupActivity.this, alarm);
                    aSetter.removeAlarm(alarm.getId());
                } else {
                    alarm.setEnabled(Alarm.ALARM_ENABLED);
                    AlarmDbUtilities.updateAlarm(GroupActivity.this, alarm);
                    aSetter.setAlarm(alarm);
                }
            }

        });
        if (iv != null) {
            if (alarm.getWakeUpMode().equals("0"))
                iv.setImageResource(R.drawable.simple_test);
            if (alarm.getWakeUpMode().equals("1"))
                iv.setImageResource(R.drawable.mathtest);
            if (alarm.getWakeUpMode().equals("2"))
                iv.setImageResource(R.drawable.logic_test);
        }
        if (tv_big != null) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(alarm.getTime());
            DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
            String bigView = "<b>" + df.format(c.getTime()) + "</b>    ";
            String arr[] = {"S", "M", "T", "W", "T", "F", "S"};
            String daysOfWeek = alarm.getDaysOfWeek();
            if (daysOfWeek.contains("#ALL#")) {
                for (int i = 1; i < 8; i++) {
                    bigView += "<font color=\"blue\"<u>" + arr[i - 1] + "</u></font> ";
                }

            } else {
                for (int i = 1; i < 8; i++) {
                    if (daysOfWeek.contains(i + "")) {
                        bigView += "<font color=\"blue\"<u>" + arr[i - 1] + "</u></font> ";
                    } else {
                        bigView += "<font color=\"red\"<u>" + arr[i - 1] + "</u></font> ";
                    }

                }
            }
            tv_big.setText(Html.fromHtml(bigView));
        }

        if (tv_small != null) {
            tv_small.setText(alarm.getDescription());
        }
        if (tb != null) {
            if (alarm.isEnabled() == Alarm.ALARM_ENABLED)
                tb.setChecked(true);
            else
                tb.setChecked(false);
        }
    }


    protected void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                mHandler.obtainMessage(1).sendToTarget();
            }
        }, 0, 5000);
    }

    protected void stopTimer() {
        timer.cancel();
    }


    @Override
    public void onPause(){
        super.onPause();
        this.stopTimer();
    }

    @Override
    public void onResume(){
        super.onResume();
        this.startTimer();
    }

    @Override
    /**
     * creaza meniu cu optiuni
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_associations, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    /**
     * se apeleaza la alegerea unui element din meniul cu optiuni
     * si sterge toate alarmele
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menu_delete_all_option:
                removeAllPersons();
                break;
            case R.id.menu_new_person:
                addNewperson();
                break;
        }
        return true;
    }

    private void removeAllPersons() {
        AlarmDbUtilities.removeAllPersonsFromGroup(this, groupId);
        personList.removeAll(personList);
        AlarmDbUtilities.removeAllPersonsFromGroup(this, groupId);
        emptyTextViewVisibility();
        personListAdapter.notifyDataSetChanged();
    }

    private void addNewperson() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.person_add_dialog);
        dialog.setTitle("Add Person");
        dialog.show();

        final EditText emailEdit = (EditText) dialog.findViewById(R.id.email_edit);
        Button addButton = (Button) dialog.findViewById(R.id.add_button);
        Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ;
                String email = emailEdit.getText().toString();
                if (email.isEmpty()) {
                    Toast.makeText(dialog.getContext(), "Email field must be filled", Toast.LENGTH_SHORT).show();
                } else if (emailIsInPersonList(email)){
                    Toast.makeText(dialog.getContext(), "Email is already in that list", Toast.LENGTH_SHORT).show();
                }else{
                    Person newPerson = AlarmDbUtilities.createPerson(GroupActivity.this, email, groupId);
                    sendRequestToPersonInBackground(newPerson);
                    dialog.cancel();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        emptyTextViewVisibility();
    }

    private boolean emailIsInPersonList(String email){
        for(Person p : personList){
            if(p.getEmail().equals(email)){
                return true;
            }
        }
        return false;
    }

    private void sendRequestToPersonInBackground(final Person person) {
        final GcmRegisterer gcm = new GcmRegisterer(this);
        gcm.setUpGcm();
        Log.d(Constants.TAG,gcm.getRegistrationId(this)+"zzxzxzxzxzxzzxzxxz\n");
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try{
                    String responseString = ServerRequestComposer.sendRequestToPerson(group,person,alarm, gcm.getRegistrationId(GroupActivity.this));
                    if(responseString!=null && !responseString.startsWith("PERSON_EXISTS")){
                        GroupActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(GroupActivity.this, "This person is not registered", Toast.LENGTH_LONG).show();
                                AlarmDbUtilities.removePerson(GroupActivity.this, person.getEmail(), groupId);
                            }
                        });
                    }else{
                        if(responseString!=null){

                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    GroupActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(GroupActivity.this, "Connection problem, can't send request", Toast.LENGTH_LONG).show();
                            AlarmDbUtilities.removePerson(GroupActivity.this, person.getEmail(), groupId);
                        }
                    });
                }
                return "";
            }

            @Override
            protected void onPostExecute(String v) {
                //TODO
            }
        }.execute(null, null, null);
    }

    @Override
    /**
     * se apeleaza la crearea de meniu context
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.context_menu, menu);
    }


    @Override
    /**
     * se apeleaza la alegerea unui element din meniul context
     * si sterge elementul ales
     */
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        switch (item.getItemId()) {
            case R.id.delete_option:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                Person person = personList.get((int) info.id);
                personList.remove(person);
                AlarmDbUtilities.removePerson(this, person.getEmail(), person.getGroupId());
                emptyTextViewVisibility();
                personListAdapter.notifyDataSetChanged();
                break;
        }
        return true;
    }


    @Override
    /**
     * se apeleaza la revenirea din preferinte
     * seteaza alarma sau actualizeaza pe una existenta
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences sp = this.getSharedPreferences(alarm.getId(), Context.MODE_PRIVATE);

        Alarm newAlarm = getAlarmFromSharedPreferences(sp);

        alarm = newAlarm;

        if (newAlarm.isEnabled() == Alarm.ALARM_ENABLED) {
            AlarmSetter aSetter = new AlarmSetter(this);
            aSetter.setAlarm(newAlarm);
        }
        AlarmDbUtilities.updateAlarm(this, newAlarm);
        updateAlarmView();
    }

    private Alarm getAlarmFromSharedPreferences(SharedPreferences sp) {

        String description = sp.getString("description", null);
        String time = sp.getString("time", null);
        String daysOfWeek = sp.getString("days_of_week", null);
        String wakeUpMode = sp.getString("wake_up_mode", null);
        String ringtone = sp.getString("ringtone", null);

        Calendar when = Calendar.getInstance();
        when.set(Calendar.SECOND, 0);
        if (time != null) {
            String timeArgs[] = time.split(":");
            when.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeArgs[0]));
            when.set(Calendar.MINUTE, Integer.parseInt(timeArgs[1]));
        }

        Alarm newAlarm = AlarmDbUtilities.fetchAlarm(this, alarm.getId());
        newAlarm.setDescription(description);
        newAlarm.setTime(when.getTimeInMillis());
        newAlarm.setWakeUpMode(wakeUpMode);
        newAlarm.setDaysOfWeek(daysOfWeek);
        newAlarm.setRingtone(ringtone);

        return newAlarm;
    }


    /**
     * clasa adaptor care adapteaza aplicatia la interfata clasei ListView
     *
     * @author ALEXANDR
     */
    public static class PersonListAdapter extends ArrayAdapter<Person> {

        private ArrayList<Person> persons;

        public PersonListAdapter(Context context, int textViewResourceId,
                                 ArrayList<Person> objects) {
            super(context, textViewResourceId, objects);
            this.persons = objects;
        }

        /**
         * reprezinta metoda ce adapteaza vederile cu continutul alarmelor
         * la lista de alarme
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.list_item_persons, null);
            }

            final Person person = persons.get(position);
            if (person != null) {
                TextView email = (TextView) v.findViewById(R.id.textView);
                RadioButton accepted = (RadioButton) v.findViewById(R.id.radioButton);

                email.setText(person.getEmail());
                accepted.setChecked(person.isAccepted());
            }

            return v;
        }
    }

}
