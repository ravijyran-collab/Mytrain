package com.mytrain.preview;
import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Small standalone Android preview; no WebView, server or network permissions. */
public class MainActivity extends Activity {
  private final int ink=Color.rgb(24,60,53), paper=Color.rgb(247,246,240);
  private LinearLayout content, results, details, savedList;
  private EditText query;
  private TextView message;
  private JSONObject data;
  private JSONArray trains;
  private SharedPreferences preferences;
  private Set<String> saved;
  private String lastQuery="";
  private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    preferences=getSharedPreferences("mytrain-demo-v1",MODE_PRIVATE);
    saved=new HashSet<>(preferences.getStringSet("saved",Collections.emptySet()));
    ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(paper);scroll.setFillViewport(true);
    content=column();content.setPadding(dp(20),dp(20),dp(20),dp(32));scroll.addView(content);
    // Modern target SDKs draw edge to edge; use actual system insets on every device.
    scroll.setOnApplyWindowInsetsListener((view,insets)->{
      if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
      else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
      return insets;
    });
    setContentView(scroll);scroll.requestApplyInsets();
    text(content,"▤ Mytrain",28,true);text(content,"NATIVE ANDROID PREVIEW",12,true);
    text(content,"Your journey,\non the right track.",34,true);
    LinearLayout notice=card();text(notice,"DEMO DATA · Fictional trains and stations",16,true);text(notice,"No real timetable or live status. Never use this demo for travel decisions.",15,false);content.addView(notice);
    text(content,"Bundled offline demo · No account or Internet connection needed.",14,false);
    try {
      try(InputStream input=getAssets().open("demo.json");ByteArrayOutputStream output=new ByteArrayOutputStream()){
        byte[] buffer=new byte[4096];int size;while((size=input.read(buffer))!=-1)output.write(buffer,0,size);
        data=new JSONObject(output.toString("UTF-8"));
      }
      if(!"demo".equals(data.getString("mode")))throw new JSONException("Wrong dataset mode");
      trains=data.getJSONArray("trains");
      text(content,"Source: "+data.getString("source"),12,false);
      LinearLayout search=card();text(search,"Find a demo train",21,true);text(search,"Train number or name",15,true);
      query=new EditText(this);query.setSingleLine(true);query.setTextColor(ink);query.setTextSize(16);query.setHint("e.g. 00001 or Express");query.setContentDescription("Train number or name");query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);query.setMinHeight(dp(52));search.addView(query);
      button(search,"Search demo trains",()->search());query.setOnEditorActionListener((v,id,event)->{if(id==EditorInfo.IME_ACTION_SEARCH){search();return true;}return false;});
      text(search,"Use the full number, including leading zeros, or part of a name.",13,false);content.addView(search);
      message=text(content,"Try 00001 or Express.",16,false);message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
      results=column();content.addView(results);details=column();content.addView(details);
      LinearLayout saves=card();text(saves,"Saved demo trains",21,true);text(saves,"Stored only on this device. Clearing app data removes bookmarks.",13,false);savedList=column();saves.addView(savedList);content.addView(saves);renderSaved();
      text(content,"Mytrain · Standalone native preview · Fictional routes only",12,false);
      if(state!=null){query.setText(state.getString("query",""));lastQuery=state.getString("submitted","");if(!lastQuery.isEmpty())renderResults();String selected=state.getString("selected","");if(!selected.isEmpty()){JSONObject train=find(selected);if(train!=null)showRoute(train);}}
    } catch(Exception error) {text(content,"Demo data could not be loaded. Search is unavailable.",16,true);}
  }
  private LinearLayout column(){LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);view.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return view;}
  private LinearLayout card(){LinearLayout view=column();view.setPadding(dp(16),dp(16),dp(16),dp(16));view.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.setMargins(0,dp(14),0,dp(14));view.setLayoutParams(layout);return view;}
  private TextView text(LinearLayout parent,String value,int size,boolean bold){TextView view=new TextView(this);view.setText(value);view.setTextColor(ink);view.setTextSize(size);view.setPadding(0,dp(6),0,dp(6));if(bold)view.setTypeface(null,Typeface.BOLD);parent.addView(view);return view;}
  private void button(LinearLayout parent,String title,Runnable action){Button view=new Button(this);view.setText(title);view.setAllCaps(false);view.setTextColor(Color.WHITE);view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ink));view.setMinHeight(dp(52));view.setOnClickListener(v->action.run());parent.addView(view,new LinearLayout.LayoutParams(-1,-2));}
  private void search(){lastQuery=query.getText().toString();selected="";details.removeAllViews();renderResults();}
  private void renderResults(){results.removeAllViews();int count=0;try{for(int i=0;i<trains.length();i++){JSONObject train=trains.getJSONObject(i);String number=train.getString("number");if(!TrainSearch.matches(number,train.getString("name"),lastQuery))continue;count++;LinearLayout card=card();text(card,"DEMO DATA · "+number,12,true);text(card,train.getString("name"),20,true);button(card,"View route for "+number,()->showRoute(train));button(card,(saved.contains(number)?"Unsave ":"Save ")+number,()->toggle(number));results.addView(card);}}catch(JSONException error){message.setText("Demo data is invalid.");return;}message.setText(lastQuery.trim().isEmpty()?"Enter a train number or name.":count==0?"No demo trains found. Try Express or 00001.":count+" demo train"+(count==1?"":"s")+" found.");}
  private JSONObject find(String number){try{for(int i=0;i<trains.length();i++){JSONObject train=trains.getJSONObject(i);if(number.equals(train.getString("number")))return train;}}catch(JSONException ignored){}return null;}
  private String selected="";
  private void showRoute(JSONObject train){details.removeAllViews();try{selected=train.getString("number");LinearLayout card=card();text(card,train.getString("name")+" · Demo route",21,true);JSONArray stops=train.getJSONArray("stops");text(card,"DEMO DATA · Fictional local times. Duration: "+(stops.getJSONObject(stops.length()-1).getInt("minute")-stops.getJSONObject(0).getInt("minute"))+" minutes.",14,false);JSONArray stations=data.getJSONArray("stations");for(int i=0;i<stops.length();i++){JSONObject stop=stops.getJSONObject(i);String name=stop.getString("stationId");for(int j=0;j<stations.length();j++){JSONObject station=stations.getJSONObject(j);if(name.equals(station.getString("id"))){name=station.getString("name");break;}}text(card,(i+1)+". "+name,17,true);text(card,TrainSearch.time(stop.getInt("minute")),16,false);}button(card,"Close route",()->{selected="";details.removeAllViews();});details.addView(card);details.post(()->details.getParent().requestChildFocus(details,details));}catch(JSONException error){text(details,"Demo route unavailable.",16,true);}}
  private void toggle(String number){if(saved.contains(number))saved.remove(number);else saved.add(number);boolean stored=preferences.edit().putStringSet("saved",new HashSet<>(saved)).commit();Toast.makeText(this,stored?"Saved demo trains updated.":"Storage failed. Changes are session-only.",Toast.LENGTH_SHORT).show();renderSaved();if(!lastQuery.isEmpty())renderResults();}
  private void renderSaved(){savedList.removeAllViews();int count=0;for(String number:new TreeSet<>(saved)){JSONObject train=find(number);if(train==null)continue;count++;try{text(savedList,number+" · "+train.getString("name"),16,true);}catch(JSONException ignored){}button(savedList,"Open "+number,()->showRoute(train));button(savedList,"Remove "+number,()->toggle(number));}if(count==0)text(savedList,"No saved demo trains yet.",16,false);}
  @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(query!=null){state.putString("query",query.getText().toString());state.putString("submitted",lastQuery);state.putString("selected",selected);}}
}
