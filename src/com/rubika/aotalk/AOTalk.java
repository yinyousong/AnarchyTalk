package com.rubika.aotalk;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import ao.misc.NameFormat;
import ao.protocol.CharacterInfo;
import ao.protocol.packets.toclient.CharacterListPacket;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gcm.GCMRegistrar;
import com.rubika.aotalk.adapter.ChannelAdapter;
import com.rubika.aotalk.adapter.CharacterAdapter;
import com.rubika.aotalk.adapter.ChatMessageAdapter;
import com.rubika.aotalk.adapter.FriendAdapter;
import com.rubika.aotalk.adapter.GridAdapter;
import com.rubika.aotalk.aou.AOU;
import com.rubika.aotalk.database.DatabaseHandler;
import com.rubika.aotalk.item.Account;
import com.rubika.aotalk.item.Channel;
import com.rubika.aotalk.item.Character;
import com.rubika.aotalk.item.ChatMessage;
import com.rubika.aotalk.item.Friend;
import com.rubika.aotalk.item.RKNAccount;
import com.rubika.aotalk.item.Tool;
import com.rubika.aotalk.itemsearch.ItemSearch;
import com.rubika.aotalk.map.Map;
import com.rubika.aotalk.market.Market;
import com.rubika.aotalk.recipebook.RecipeBook;
import com.rubika.aotalk.service.ClientService;
import com.rubika.aotalk.service.ServiceTools;
import com.rubika.aotalk.towerwars.Towerwars;
import com.rubika.aotalk.util.Logging;
import com.rubika.aotalk.util.RKNet;
import com.rubika.aotalk.util.Statics;
import com.rubika.aotalk.util.StorageTools;
import com.viewpagerindicator.TitlePageIndicator;

public class AOTalk extends SherlockFragmentActivity implements ViewPager.OnPageChangeListener, OnSharedPreferenceChangeListener {
	private static final String APP_TAG = "--> The Leet";

	public static Messenger service = null;
	private static Context context;
	private static Activity activity;
	private boolean serviceIsBound = false;	
	private static ProgressDialog loader;
	public static DatabaseHandler databaseHandler;
	public static ChatMessageAdapter messageAdapter;
	private static ChannelAdapter channelAdapter;
	public static GridAdapter gridAdapter;
	public static FriendAdapter friendAdapter;
	private static int selectedAccountToManage = 0;
	private static List<SherlockFragment> fragments;
	private static String GCMRegistrationId = null;
    private static Tracker tracker;

	public static boolean isPlaying = false;

	public static String currentTargetChannel = "";
	public static String currentTargetCharacter = "";
	private static int currentUserID = 0;
	public static String currentCharacterName = "";
	private static String currentShowChannel = Statics.CHANNEL_MAIN;
	
	private static SharedPreferences settings;
	public static SharedPreferences.Editor editor;
	
	public static ActionBar bar;
	public static ViewPager fragmentPager;
	private static TitlePageIndicator titleIndicator;
	
	private static List<Channel> currentChannels = new ArrayList<Channel>();
	public static List<Channel> channelList = new ArrayList<Channel>();
	public static List<Channel> privateList = new ArrayList<Channel>();
	private static List<Friend> friendList = new ArrayList<Friend>();
	
	public static String currentTrack = null;
	
	private static boolean online = false;
			
	private static ListHandler clearDBHandler = new ListHandler();
	private static ListHandler channelHandler = new ListHandler();
	private static ListHandler friendHandler = new ListHandler();
	private static ListHandler messageHandler = new ListHandler();
	
	public static OnItemLongClickListener chatFragmentClickListener;
	public static OnItemLongClickListener friendFragmentClickListener;
	public static View.OnClickListener inputChannelClick;
	public static OnKeyListener inputTextClick;
	
	static class ListHandler extends Handler {};
	
	public static final Messenger serviceMessenger = new Messenger(new ServiceHandler());

	public static void callFriendListUpdate() {
        friendHandler.removeCallbacks(friendListRunnable);
        friendHandler.removeCallbacks(friendListUpdate);
        friendHandler.postDelayed(friendListRunnable, Statics.HANDLER_DELAY);		
	}
	
	public static void callMessageListUpdate() {
        messageHandler.removeCallbacks(messageListRunnable);
        messageHandler.removeCallbacks(messageUpdateRunnable);
        messageHandler.postDelayed(messageListRunnable, Statics.HANDLER_DELAY);
	}
	
	public static void callChannelListUpdate() {
        channelHandler.removeCallbacks(channelListRunnable);
        channelHandler.postDelayed(channelListRunnable, Statics.HANDLER_DELAY);
	}
	
	static class ServiceHandler extends Handler {
	    @SuppressWarnings("unchecked")
		@Override
	    public void handleMessage(Message message) {
	    	switch (message.what) {
	            case Statics.MESSAGE_LOGIN_ERROR:
	            	Logging.toast(context, context.getString(R.string.login_error));
	                break;
	            case Statics.MESSAGE_CHANNEL:
	            	channelList = (List<Channel>)message.obj;
	            	
	            	if (channelList.size() > 0) {
	            		online = true;
	            	} else {
	            		online = false;
	            	}
	            	
	    	        updateInputHint();
	    	        callChannelListUpdate();
	    	        updateConnectionButton();
	    	        
	            	break;
	            case Statics.MESSAGE_FRIEND:
	            	friendList = (List<Friend>)message.obj;
	            	
            		callFriendListUpdate();
            		callMessageListUpdate();
	    	        
	                break;
	            case Statics.MESSAGE_IS_CONNECTED:
	            	online = true;
	            	
	    	        updateConnectionButton();
	    	        setDisconnect();
	    	        
	                break;
	            case Statics.MESSAGE_IS_DISCONNECTED:
	            	online = false;
	            	
	    	        updateConnectionButton();
	    	        setAccount();
	    	        
	                break;
	            case Statics.MESSAGE_UPDATE:
            		callMessageListUpdate();
	            	
	                break;
	            case Statics.MESSAGE_REGISTERED:
	    			currentUserID = message.arg1;
	    			
	    			List<Object> registerData = (ArrayList<Object>) message.obj;
	    	        
	    			friendList = (List<Friend>)registerData.get(0);
	    	        channelList = (List<Channel>)registerData.get(1);
	    	        privateList = (List<Channel>)registerData.get(8);
	    			currentTargetChannel = (String)registerData.get(2);
	    			currentTargetCharacter = (String)registerData.get(3);
	    			currentCharacterName = (String)registerData.get(4);
	    			currentShowChannel = (String)registerData.get(5);
	    			isPlaying = (Boolean)registerData.get(6);
	    	        currentTrack = (String) registerData.get(9);
	    	        online = (Boolean)registerData.get(10);
	    	        
            		callFriendListUpdate();
	    	        callChannelListUpdate();
            		callMessageListUpdate();

	    	        updateInputHint();
	    	        updateConnectionButton();
	    	        handleInvitations((List<Channel>)registerData.get(7));
	            	updatePlayer();
	    	        
	    	        if (channelList.size() > 0 && settings.getBoolean("showChatWhenOnline", true)) {
						fragmentPager.setCurrentItem(2);
	    	        }
	    	        
	                break;
	            case Statics.MESSAGE_STARTED:
	    	        hideLoader();
	    	        
					if (channelList.size() > 0 && settings.getBoolean("showChatWhenOnline", true)) {
						fragmentPager.setCurrentItem(2);
					}
					
	    			currentUserID = message.arg1;
	    			
	    			List<Object> startedData = (ArrayList<Object>) message.obj;
	    			
	    			currentTargetChannel = (String)startedData.get(0);
	    			currentTargetCharacter = (String)startedData.get(1);
	    			currentCharacterName = (String)startedData.get(2);
	    			currentShowChannel = (String)startedData.get(3);
	    			
	    			if (channelList.size() > 0) {
	    				online = true;
	    			} else {
	    				online = false;
	    			}
	    			
	    	        updateConnectionButton();
	    	        updateInputHint();

            		callMessageListUpdate();
	    			
	    	        break;
	            case Statics.MESSAGE_CHARACTERS:
	    	        hideLoader();
	    	        
	    			messageAdapter.clear();
	            	setCharacter((CharacterListPacket) message.obj);
	            	
	            	break;
	            case Statics.MESSAGE_CONNECTION_ERROR:
	    	        hideLoader();
	    	        
	    	        friendList.clear();
	    	        channelList.clear();
	    	        
	    	        online = false;
	    	        
	    	        ((EditText) activity.findViewById(R.id.input)).setHint(context.getString(R.string.disconnected));
	    	        
            		callMessageListUpdate();
	    	        callChannelListUpdate();
            		callFriendListUpdate();

	    	        updateConnectionButton();
	    	        
	            	Logging.toast(context, context.getString(R.string.connection_error));
	    	        
	                break;
	            case Statics.MESSAGE_CLIENT_ERROR:
	    	        hideLoader();
	            	Logging.log(APP_TAG, "Client error");
	    	        
	                break;
	            case Statics.MESSAGE_WHOIS:
	    	        List<String> whoisData = (ArrayList<String>) message.obj;
	    	        showWhoIs(whoisData.get(1), whoisData.get(0));
	    	        
	                break;
	            case Statics.MESSAGE_DISCONNECTED:
	            	Logging.log(APP_TAG, "Message: DISCONNECTED");
	            	hideLoader();
	            	
	    	        friendList.clear();
	    	        channelList.clear();
	    	        
	    	        ((EditText) activity.findViewById(R.id.input)).setHint(context.getString(R.string.disconnected));
	    			
	    	        online = false;
	    	        
            		callMessageListUpdate();
	    	        callChannelListUpdate();
            		callFriendListUpdate();
	            	
	            	if (message.arg1 == 0 && settings.getBoolean("clearMessagesOnDisconnect", true)) {
	            		clearDBHandler.postDelayed(clearDatabaseRunnable, Statics.HANDLER_DELAY);
	            	}

	    	        updateConnectionButton();
	            	
	                break;
	            case Statics.MESSAGE_PLAYER_ERROR:
	            	Logging.log(APP_TAG, "Player error");
	            	
	                break;
	            case Statics.MESSAGE_PLAYER_STARTED:
	            	isPlaying = true;
	            	updatePlayer();

	                break;
	            case Statics.MESSAGE_PLAYER_STOPPED:
	            	isPlaying = false;
	            	currentTrack = null;
	            	updatePlayer();

	                break;
	            case Statics.MESSAGE_PLAYER_TRACK:
	    	        List<Object> playerData = (ArrayList<Object>) message.obj;

	    	        isPlaying = (Boolean)playerData.get(0);
	            	currentTrack = (String) playerData.get(1);
	            	
	            	updatePlayer();
	            	
	            	break;
	            case Statics.MESSAGE_PRIVATE_INVITATION:
	            	handleInvitations((List<Channel>) message.obj);
	            	
	            	break;
	            case Statics.MESSAGE_PRIVATE_CHANNEL:
	            	privateList = (List<Channel>) message.obj;
	    	        callChannelListUpdate();
	            	
	            	break;
	            default:
	                super.handleMessage(message);
	        }
	    }
	}
	
	final static Runnable clearDatabaseRunnable = new Runnable()
	{
	    public void run() 
	    {
			databaseHandler.deleteAllPostsForUser(currentUserID);
	    }
	};
	
	final static Runnable channelListRunnable = new Runnable()
	{
	    public void run() 
	    {
			updateChannelList();
	    }
	};
		
	final static Runnable friendListRunnable = new Runnable()
	{
	    public void run() 
	    {
	    	updateFriendList();
	    }
	};

	final static Runnable friendUpdateRunnable = new Runnable()
	{
		public void run() {
			if (AOTalk.getActivity() != null) {
				Logging.log(APP_TAG, "starting friend list update runnable on main thread");
				AOTalk.getActivity().runOnUiThread(friendListUpdate);
			}
		}
	};

	final static Runnable friendListUpdate = new Runnable()
	{
		public void run() {
			friendAdapter.notifyDataSetChanged();
		}
	};

	final static Runnable messageListRunnable = new Runnable()
	{
	    public void run() 
	    {
	    	updateMessageList();
	    }
	};

	public final static Runnable messageUpdateRunnable = new Runnable()
	{
		public void run() {
			if (AOTalk.getActivity() != null) {
				Logging.log(APP_TAG, "starting message list update runnable on main thread");
				AOTalk.getActivity().runOnUiThread(messageListUpdate);
			}
		}
	};

	public final static Runnable messageListUpdate = new Runnable()
	{
		public void run() {
			messageAdapter.notifyDataSetChanged();
		}
	};
	
	private synchronized static void handleInvitations(List<Channel> invitations) {
		for (final Channel channel : invitations) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(context.getString(R.string.join_channel));
			builder.setMessage(String.format(context.getString(R.string.you_were_invited_to_channel), channel.getName().replace(Statics.PREFIX_PRIVATE_GROUP, "")));
			
			builder.setPositiveButton(context.getString(R.string.ok), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
			        Message msg = Message.obtain(null, Statics.MESSAGE_PRIVATE_CHANNEL_JOIN);
			        msg.replyTo = serviceMessenger;
			        msg.obj = channel;
			        
			        try {
						service.send(msg);
					} catch (RemoteException e) {
						Logging.log(APP_TAG, e.getMessage());
					}
				}
			});

			builder.setNegativeButton(context.getString(R.string.cancel), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
			        Message msg = Message.obtain(null, Statics.MESSAGE_PRIVATE_CHANNEL_DENY);
			        msg.replyTo = serviceMessenger;
			        msg.obj = channel;
			        
			        try {
						service.send(msg);
					} catch (RemoteException e) {
						Logging.log(APP_TAG, e.getMessage());
					}
				}
			});

			builder.create().show();
		}
	}
	
	private static void updatePlayer() {
    	FragmentTools fragment = (FragmentTools)fragments.get(1);

    	if (fragment != null) {
    		fragment.updatePlayer(isPlaying, currentTrack);
    	}
	}
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder ibinder) {
	        service = new Messenger(ibinder);

	        try {
	            Message message = Message.obtain(null, Statics.MESSAGE_CLIENT_REGISTER);
	            message.replyTo = serviceMessenger;
	            service.send(message);
	        } catch (RemoteException e) {
				Logging.log(APP_TAG, e.getMessage());
	        }
	    }

	    public void onServiceDisconnected(ComponentName className) {
	    	service = null;
	    }
	};

	private void bindService() {
		currentTargetChannel = settings.getString("lastUsedChannel", currentTargetChannel);
		currentTargetCharacter = settings.getString("lastUsedCharacter", currentTargetCharacter);

		if (!serviceIsBound) {
	    	Intent serviceIntent = new Intent(this, ClientService.class);
			bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		    serviceIsBound = true;
		    
		    startService(serviceIntent);
	    }
	}

	private void unbindService() {
		if (serviceIsBound) {
	        if (service != null) {
	            try {
	                Message msg = Message.obtain(null, Statics.MESSAGE_CLIENT_UNREGISTER);
	                msg.replyTo = serviceMessenger;
	                service.send(msg);
	            } catch (RemoteException e) {
					Logging.log(APP_TAG, e.getMessage());
	            }
	        }

	        unbindService(serviceConnection);
	        serviceIsBound = false;
	    }
    	
        messageHandler.removeCallbacks(messageListRunnable);
        messageHandler.removeCallbacks(messageUpdateRunnable);
        friendHandler.removeCallbacks(friendListRunnable);
        friendHandler.removeCallbacks(friendUpdateRunnable);
        channelHandler.removeCallbacks(channelListRunnable);
		
		editor.putString("lastUsedChannel", currentTargetChannel);
		editor.putString("lastUsedCharacter", currentTargetCharacter);
		editor.commit();
	}
	
	public static void sendMessage(String tell) {
		Logging.log(APP_TAG, "sendMessage\ncurrentTargetChannel: " + currentTargetChannel + "\ncurrentTargetCharacter: " + currentTargetCharacter);
		
		if (tell != null) {
			tell = tell.trim();
			
			if(tell.length() > 0) {
				ChatMessage chatMessage = null;
				
				List<Channel> tempList = new ArrayList<Channel>();
				tempList.addAll(channelList);
				tempList.addAll(privateList);
				
				if (currentTargetChannel.length() > 0) {
					for (Channel channel : tempList) {
			        	if (channel.getName().equals(currentTargetChannel)) {
				        	chatMessage = new ChatMessage(System.currentTimeMillis(), tell, "", channel.getName(), 0);
				        	break;
			        	}
			        }
				} else if (currentTargetCharacter.length() > 0) {
		        	chatMessage = new ChatMessage(System.currentTimeMillis(), tell, currentTargetCharacter, "", 0);			
				}
		        
				if (chatMessage != null) {
					Logging.log(APP_TAG, "Sending message to service");
					try {
			            Message message = Message.obtain(null, Statics.MESSAGE_SEND);
			            message.arg1 = 1;
			            message.obj = chatMessage;
			            message.replyTo = serviceMessenger;
			            
			            service.send(message);
			        } catch (RemoteException e) {
						Logging.log(APP_TAG, e.getMessage());
			        }
				}
			}
		}
	}
	
	public static void whoIs(final String name, final boolean manual) {
		((SherlockFragmentActivity) AOTalk.getActivity()).setSupportProgressBarIndeterminateVisibility(Boolean.TRUE);
		
        //if (settings.getBoolean("whoisFromWeb", true) || manual) {
			new Thread(new Runnable() { 
	            public void run(){
					Message msg = Message.obtain();
					msg.what = 0;
					
					List<String> whoisData = ServiceTools.getUserData(context, name);
	
					if (whoisData == null) {
						whoisData = new ArrayList<String>();
						whoisData.add("");
						whoisData.add("");
						whoisData.add(name);
					}
	
					msg.obj = whoisData;
					msg.arg1 = manual? 1 : 0;
					whoIsHandler.sendMessage(msg);
				}
			}).start();
        /*} else {
			ChatMessage chatMessage = new ChatMessage(
					System.currentTimeMillis(),
					String.format(Statics.WHOIS_MESSAGE, name),
					Statics.BOTNAME,
					Statics.CHANNEL_PM,
					0,
					0
				);

			Message message = Message.obtain();
			message.what = Statics.MESSAGE_SEND;
			message.arg1 = 0;
			message.obj = chatMessage;
			
			try {
				service.send(message);
			} catch (RemoteException e) {
				Logging.log(APP_TAG, e.getMessage());
			}
        	
        }*/
	}
	
	private static Handler whoIsHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			@SuppressWarnings("unchecked")
			List<String> whoisData = (ArrayList<String>) msg.obj;
			
			if (whoisData.get(0).equals("")) {
				/*if (settings.getBoolean("whoisFallbackToBot", true) && online && msg.arg1 != 1) {
					ChatMessage chatMessage = new ChatMessage(
							System.currentTimeMillis(),
							String.format(Statics.WHOIS_MESSAGE, whoisData.get(2)),
							Statics.BOTNAME,
							Statics.CHANNEL_PM,
							0,
							0
						);
	
					Message message = Message.obtain();
					message.what = Statics.MESSAGE_SEND;
					message.arg1 = 0;
					message.obj = chatMessage;
					
					try {
						service.send(message);
					} catch (RemoteException e) {
						Logging.log(APP_TAG, e.getMessage());
					}
				} else {*/
				showWhoIs(context.getString(R.string.no_char_data), context.getString(R.string.no_char_title));
				//}
			} else {
				showWhoIs(whoisData.get(1), whoisData.get(0));
			}
		}
	};
	
	@SuppressLint("NewApi")
	private static void showWhoIs(String message, String name) {
		((SherlockFragmentActivity) activity).setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.alert_whois, null);

		if (name.equals("")) {
			name = context.getString(R.string.no_char_title);
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(layout);
		builder.setTitle(Html.fromHtml(name));
		builder.setPositiveButton(
				context.getString(R.string.ok),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			}
		);

		final AlertDialog dialog = builder.create();

		final WebView webView = (WebView) layout.findViewById(R.id.whois);
		webView.setBackgroundColor(0);
		webView.loadData(Uri.encode(Statics.HTML_START
			+ message
			+ Statics.HTML_END), "text/html", "UTF-8"
		);
        
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			((View)webView.getParent()).setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
		
		WebViewClient webClient = new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				dialog.show();
			}
		};
		
		webView.setWebViewClient(webClient);
	}
	
	private void search() {
		LayoutInflater factory = LayoutInflater.from(context);
        final View view = factory.inflate(R.layout.alert_search, null);
        final EditText search = (EditText) view.findViewById(R.id.search);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.search);
        builder.setView(view);
        
        final Spinner spinner = (Spinner) view.findViewById(R.id.type);
        
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	hideSoftKeyboard(view);          	

            	Intent intent;
            	
            	if (search.getText().toString().length() > 0) {
	            	switch(spinner.getSelectedItemPosition()) {
					case 0:
						tracker.sendEvent("Seach", "Character", search.getText().toString().replace(" ", "-"), 0L);

						if (channelList.size() <= 1 && !settings.getBoolean("whoisFromWeb", true)) {
							Logging.toast(context, getString(R.string.not_connected));
						} else {
		            		whoIs(NameFormat.format(search.getText().toString().replace(" ", "-")), true);
						}
						
						break;
					case 1:
						tracker.sendEvent("Seach", "Item", search.getText().toString().trim(), 0L);

						intent = new Intent(context, ItemSearch.class);
						intent.putExtra("name", search.getText().toString().trim());
						startActivity(intent);
						
						break;
					case 2:
						tracker.sendEvent("Seach", "Guide", search.getText().toString().trim(), 0L);

						intent = new Intent(context, AOU.class);
						intent.putExtra("text", search.getText().toString().trim());
						startActivity(intent);
						
						break;
					case 3:
						tracker.sendEvent("Seach", "Recipe", search.getText().toString().trim(), 0L);

						intent = new Intent(context, RecipeBook.class);
						intent.putExtra("text", search.getText().toString().trim());
						startActivity(intent);
						
						break;
					default:
	            	}
            	} else {
            		Logging.toast(context, getString(R.string.search_not_empty));
            	}
            }
        });
        
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	hideSoftKeyboard(view);
            }
        });
        
        builder.create();
        builder.show();
	}
	
	private static void updateConnectionButton() {
		Logging.log(APP_TAG, "updateConnectionButton: " + online);
		
		if (online) {
			gridAdapter.getItem(0).setName(getActivity().getString(R.string.disconnect));
		} else {
			gridAdapter.getItem(0).setName(getActivity().getString(R.string.connect));
		}
		
		gridAdapter.notifyDataSetChanged();
	}

	private synchronized static void updateChannelList() {
		Channel applicationMessages = new Channel(context.getString(R.string.app_name), 0, true, false);
		Channel privateMessages = new Channel("Private messages", 0, true, false);

		channelAdapter.clear();

		List<Channel> tempList = new ArrayList<Channel>();
		tempList.addAll(channelList);

		Logging.log(APP_TAG, "Channel list size: " + channelList.size());
						
		Collections.sort(tempList, new Channel.CustomComparator());
		
		List<Channel> privList = new ArrayList<Channel>();
		privList.addAll(privateList);
		
		Collections.sort(privList, new Channel.CustomComparator());
		tempList.addAll(privList);

		tempList.add(0, applicationMessages);

		if (online) {
			tempList.add(1, privateMessages);
		}
				
		String lastShowChannel = currentShowChannel;
				
		for (Channel channel : tempList) {
			if (channel.getEnabled() && !channel.getMuted()) {
				channelAdapter.add(channel);
			}
		}
		
		channelAdapter.notifyDataSetChanged();
		
		int navigationItem = 0;
		int navigationCount = 2;
		
		for (Channel channel : tempList) {
			Logging.log(APP_TAG, "Found channel "  + channel.getName() + ", want channel "  + lastShowChannel);
			if (lastShowChannel.equals(channel.getName())) {
				navigationItem = navigationCount;
				break;
			}
			
			navigationCount++;
		}
		
		if (lastShowChannel.equals(Statics.CHANNEL_MAIN)) {
			navigationItem = 0;
		}
		
		if (lastShowChannel.equals(Statics.CHANNEL_PM) && tempList.size() > 0) {
			navigationItem = 1;
		}
		
		Logging.log(APP_TAG, "Setting channel to " + navigationItem);
		bar.setSelectedNavigationItem(navigationItem);
	}
	
	private synchronized static void updateFriendList() {
		Logging.log(APP_TAG, "updateFriendList");
        friendHandler.removeCallbacks(friendUpdateRunnable);
		
		final List<Friend> tempList = new ArrayList<Friend>();
		tempList.addAll(friendList);
		
		Collections.sort(tempList, new Friend.CustomComparator());
		
		if (settings.getBoolean("showOnlyOnline", false)) {
			for (int i = tempList.size() - 1; i >= 0; i--) {
				if (!tempList.get(i).isOnline()) {
					tempList.remove(i);
				}
			}
		}
		
		friendAdapter.clear();
		
		if (settings.getBoolean("enableFaces", true)) {
			for (int i = 0; i < tempList.size(); i++) {
	        	final Friend friend = tempList.get(i);
		        	
	        	if (friend.getIcon() == null || friend.getIcon().equals("0")) {
		        	new Thread(new Runnable()
		            {
						@Override
						public void run() {
							String imageName = ServiceTools.getUserImageName(context, friend.getName());
		                	
							friend.setIcon(imageName);
		                	
							friendHandler.removeCallbacks(friendUpdateRunnable);
							friendHandler.postDelayed(friendUpdateRunnable, Statics.HANDLER_DELAY);
						}
		            }).start();
	        	}
			}

			friendAdapter.addAll(tempList);
		} else {
			friendAdapter.addAll(tempList);
			
			friendHandler.removeCallbacks(friendUpdateRunnable);
			friendHandler.postDelayed(friendUpdateRunnable, Statics.HANDLER_DELAY);
		}
	}

	private synchronized static void updateMessageList() {
		if (currentUserID != 0) {
        	Logging.log(APP_TAG, "updateMessageList");

        	List<ChatMessage> newMessages = new ArrayList<ChatMessage>();
			boolean animate = false;
			
	    	if (currentUserID != 0 && messageAdapter.getCount() == 0) {
	    		newMessages = databaseHandler.getAllPostsForUser(
		    			currentUserID, 
		    			currentShowChannel
		    		);
	    	} else {
	    		newMessages = databaseHandler.getNewPostsForUser(
	    			currentUserID, 
	    			messageAdapter.getItem(messageAdapter.getCount() - 1).getId(), 
	    			currentShowChannel
	    		);
	    		animate = true;
	    	}
	    	
	    	final boolean doAnimation = animate;
	    	
	    	if (newMessages != null) {
		    	for (final ChatMessage message : newMessages) {
		        	if (settings.getBoolean("enableFaces", true)) {
			    		if (message.getCharacter() != null && !message.getCharacter().equals(Statics.CHANNEL_SYSTEM) && !message.getCharacter().equals(Statics.CHANNEL_APPLICATION)) {
			    			if (message.getIcon() == null || message.getIcon().equals("0")) {
		    					new Thread(new Runnable()
					            {
				                    public void run()
				                    {
				                    	String imageName = null;
				                    	
				                    	if (message.getChannel().equals(Statics.CHANNEL_PM) && message.getMessage().contains("to [")) {
				                    		imageName = ServiceTools.getUserImageName(context, currentCharacterName);
				                    	} else {
				                    		imageName = ServiceTools.getUserImageName(context, message.getCharacter());
				                    	}
				                    	
				                    	message.setIcon(imageName);
		
				                    	messageHandler.removeCallbacks(messageUpdateRunnable);
				                    	messageHandler.postDelayed(messageUpdateRunnable, Statics.HANDLER_DELAY);
				                    }
					            }).start();
			    			}
				    	}
			    		
					    message.showAnimation(doAnimation);
				        messageAdapter.add(message);
		        	} else {
					    message.showAnimation(doAnimation);
				        messageAdapter.add(message);
		        	}
		    	}
	    	}
			
			if (messageAdapter != null && (messageAdapter.getCount()) > 1000) {
				List<ChatMessage> remList = new ArrayList<ChatMessage>();
				
				for(int i = 0; i <= (messageAdapter.getCount() - 1001); i++){
					remList.add(messageAdapter.getItem(i));
				}
				
				for (ChatMessage m : remList) {
					messageAdapter.remove(m);
				}
			}
	    	
	    	if (!settings.getBoolean("enableFaces", true)) {
	    		messageAdapter.notifyDataSetChanged();
	    	} else {
		        messageHandler.removeCallbacks(messageUpdateRunnable);
		        messageHandler.postDelayed(messageUpdateRunnable, Statics.HANDLER_DELAY);	    		
	    	}
		}
		
		Logging.log(APP_TAG, "currentUserID: " + currentUserID + ", adapter size: " + messageAdapter.getCount());
	}

	public static void updateInputHint() {
	    boolean hintIsSet = false;
	    
	    List<Channel> tempList = new ArrayList<Channel>();
	    if (channelList != null) {
	    	tempList.addAll(channelList);
	    }
	    if (privateList != null) {
	    	tempList.addAll(privateList);
	    }
	    
	    FragmentChat fragment = (FragmentChat)fragments.get(2);
	    
	    //Logging.log(APP_TAG, "currentTargetChannel: '" + currentTargetChannel + "', currentTargetCharacter: '" + currentTargetCharacter + "'" + ", listsize: " + tempList.size());
	    
		if (currentTargetChannel.equals("") && currentTargetCharacter.equals("") && online) {
	    	if (currentShowChannel.equals(Statics.CHANNEL_PM)) {
	    		fragment.setHint(getActivity().getString(R.string.select_character));
	    	} else {
	    		fragment.setHint(getActivity().getString(R.string.select_channel));
	    	}
		} else if (!online) {
			fragment.setHint(getActivity().getString(R.string.disconnected));
		} else {
			if (!currentTargetChannel.equals("")) {
				if (tempList.size() > 0) {
		        	for (Channel channel : tempList) {
		        		if (channel.getName().equals(currentTargetChannel)) {
		        			fragment.setHint(channel.getName());
		    				hintIsSet = true;
		    				break;
		        		}
		        	}
		        	
		        	if (!hintIsSet) {
		        		fragment.setHint(getActivity().getString(R.string.select_channel));
		            }
		        }
			}
			
			if (!currentTargetCharacter.equals("")) {
				fragment.setHint(String.format(getActivity().getString(R.string.tell), currentTargetCharacter));
			}
	    }
	}
	
	private static void setAccount() {
		final List<Account> accounts = databaseHandler.getAllAccounts();
		int numberOfAccounts = 0;
		
		if (accounts != null) {
			numberOfAccounts = accounts.size();
		}
		
		final CharSequence[] listItems = new CharSequence[numberOfAccounts + 1];
		
		listItems[0] = getActivity().getString(R.string.add_account);
		
		for (int i = 0; i < numberOfAccounts; i++) {
			listItems[i + 1] = accounts.get(i).getUsername();
		}
	
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.select_account));
		builder.setItems(
			listItems,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, final int item) {
					if (item > 0) {
						showLoader(context.getString(R.string.connecting));
						final Account account = accounts.get(item - 1);
						
						new Thread() {
							public void run() {
				                Message msg = Message.obtain(null, Statics.MESSAGE_CONNECT);
				                msg.replyTo = serviceMessenger;
				                msg.obj = account;
				                
				                try {
									service.send(msg);
								} catch (RemoteException e) {
									Logging.log(APP_TAG, e.getMessage());
								}
							}
						}.start();
					} else {
						newAccount();
					}
				}
			}
		);
		
		if (numberOfAccounts > 0) {
			builder.setNeutralButton(context.getString(R.string.manage), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					manageAccount();
				}
			});
		}
	
		builder.setNegativeButton(context.getString(R.string.cancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
	
		builder.create().show();
	}

	private static void manageAccount() {
		final List<Account> accounts = databaseHandler.getAllAccounts();
		int numberOfAccounts = 0;
		
		if (accounts != null) {
			numberOfAccounts = accounts.size();
		}
		
		final CharSequence[] listItems = new CharSequence[numberOfAccounts];
		
		for (int i = 0; i < numberOfAccounts; i++) {
			listItems[i] = accounts.get(i).getUsername();
		}
	
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.select_account));
		builder.setSingleChoiceItems(listItems, 0, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	        	selectedAccountToManage = which;
	        }
	    });
		
		builder.setNeutralButton(context.getString(R.string.delete), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle(context.getString(R.string.delete_account));
				builder.setMessage(context.getString(R.string.confirm_delete_account));
				
				builder.setPositiveButton(context.getString(R.string.ok), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (accounts != null && accounts.get(selectedAccountToManage) != null) {
							DatabaseHandler.getInstance(context).deleteAccount(accounts.get(selectedAccountToManage));
						}
						manageAccount();
					}
				});
	
				builder.setNegativeButton(context.getString(R.string.cancel), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						manageAccount();
					}
				});
	
				builder.create().show();
	
			}
		});
	
		builder.setNegativeButton(context.getString(R.string.edit), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (accounts != null && accounts.get(selectedAccountToManage) != null) {
					editAccount(accounts.get(selectedAccountToManage));
				} else {
					Logging.toast(context, context.getString(R.string.account_not_found));
				}
			}
		});
	
		builder.setPositiveButton(context.getString(R.string.cancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				setAccount();
			}
		});
	
		builder.create().show();
	}

	private static void newAccount() {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		
		final View layout = inflater.inflate(R.layout.alert_account, null);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getResources().getString(R.string.login_title));
		builder.setView(layout);

		builder.setPositiveButton(context.getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (
							((EditText) layout.findViewById(R.id.username)).getText().toString().length() > 0
							&&
							((EditText) layout.findViewById(R.id.password)).getText().toString().length() > 0
						) {
							showLoader(context.getString(R.string.connecting));
	
							new Thread() {
								public void run() {
									Account account = new Account(
											((EditText) layout.findViewById(R.id.username)).getText().toString(),
											((EditText) layout.findViewById(R.id.password)).getText().toString(),
											false,
											0
									);
									
									if (((CheckBox) layout.findViewById(R.id.savepassword)).isChecked()) {
										databaseHandler.addAccount(account);
									}
									
					                Message msg = Message.obtain(null, Statics.MESSAGE_CONNECT);
					                msg.replyTo = serviceMessenger;
					                msg.obj = account;
					                
					                try {
										service.send(msg);
									} catch (RemoteException e) {
										Logging.log(APP_TAG, e.getMessage());
									}
								}
							}.start();
						} else {
							Logging.toast(context, context.getString(R.string.u_and_p_required));
						}
						
		            	hideSoftKeyboard(layout);          	
					}
				});

		builder.setNegativeButton(context.getString(R.string.cancel),new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setAccount();
            	hideSoftKeyboard(layout);          	
			}
		});
		
		builder.create().show();
	}
	
	private static void editAccount(final Account account) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		
		final View layout = inflater.inflate(R.layout.alert_account, null);
		
		((CheckBox) layout.findViewById(R.id.savepassword)).setVisibility(View.GONE);
		
		((EditText) layout.findViewById(R.id.username)).setText(account.getUsername());
		((EditText) layout.findViewById(R.id.password)).setText(account.getPassword());
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getResources().getString(R.string.login_title));
		builder.setView(layout);

		builder.setPositiveButton(context.getString(R.string.ok),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if (
						((EditText) layout.findViewById(R.id.username)).getText().toString().length() > 0
						&&
						((EditText) layout.findViewById(R.id.password)).getText().toString().length() > 0
					) {
						account.setUsername(((EditText) layout.findViewById(R.id.username)).getText().toString());
						account.setPassword(((EditText) layout.findViewById(R.id.password)).getText().toString());
						
						databaseHandler.updateAccount(account);
						manageAccount();
					} else {
						Logging.toast(context, context.getString(R.string.u_and_p_required));
					}
					
	            	hideSoftKeyboard(layout);          	
				}
			});

		builder.setNegativeButton(context.getString(R.string.cancel),new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
            	hideSoftKeyboard(layout);          	
				manageAccount();
			}
		});
		
		builder.create().show();
	}
	
	private static void setDisconnect() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(getActivity().getString(R.string.disconnect));
		builder.setMessage(getActivity().getString(R.string.confirm_disconnect));
		
		builder.setPositiveButton(getActivity().getString(R.string.ok), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
                Message msg = Message.obtain(null, Statics.MESSAGE_DISCONNECT);
                msg.replyTo = serviceMessenger;
                
                try {
					service.send(msg);
				} catch (RemoteException e) {
					Logging.log(APP_TAG, e.getMessage());
				}
			}
		});

		builder.setNegativeButton(getActivity().getString(R.string.cancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		builder.create().show();
	}
	
	private static void setCharacter(final CharacterListPacket charpacket) {
		final List<Character> listItems = new ArrayList<Character>();

		if (charpacket != null) {
			for (int i = 0; i < charpacket.getNumCharacters(); i++) {
				Character character = new Character(charpacket.getCharacter(i).getName(), 0, 0);
				character.setOrder(i);
				
				listItems.add(character);
			}
			
			if (settings.getBoolean("sortLoginCharacters", false)) {
				Collections.sort(listItems, new Character.CustomComparator());
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(context.getString(R.string.select_character));
			builder.setAdapter(
				new CharacterAdapter(context, R.layout.list_item, listItems),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, final int item) {
						showLoader(context.getString(R.string.connecting));

						final CharacterInfo character = charpacket.getCharacter(listItems.get(item).getOrder());

						new Thread() {
							public void run() {
				                Message msg = Message.obtain(null, Statics.MESSAGE_CHARACTER);
				                msg.replyTo = serviceMessenger;
				                msg.obj = character;
				                
				                try {
									service.send(msg);
								} catch (RemoteException e) {
									Logging.log(APP_TAG, e.getMessage());
								}
							}
						}.start();

						return;
					}
				}
			);
			
			builder.setNegativeButton(context.getString(R.string.cancel), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
	                Message msg = Message.obtain(null, Statics.MESSAGE_DISCONNECT);
	                msg.replyTo = serviceMessenger;
	                
	                try {
						service.send(msg);
					} catch (RemoteException e) {
						Logging.log(APP_TAG, e.getMessage());
					}
				}
			});

			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
	                Message msg = Message.obtain(null, Statics.MESSAGE_DISCONNECT);
	                msg.replyTo = serviceMessenger;
	                
	                try {
						service.send(msg);
					} catch (RemoteException e) {
						Logging.log(APP_TAG, e.getMessage());
					}
				}
			});

			builder.create().show();
		}
	}
	
	public static void setChannel() {
		if (online) {
			final List<Channel> tempList = new ArrayList<Channel>();
			
			for (Channel channel : channelList) {
				if (channel.getEnabled() && !channel.getMuted()) {
					tempList.add(channel);
				}
			}
			
			Collections.sort(tempList, new Channel.CustomComparator());
			
			List<Channel> privList = new ArrayList<Channel>();
			privList.addAll(privateList);
			Collections.sort(privList, new Channel.CustomComparator());
			
			tempList.addAll(privList);
			
			int itemSize = 0;
			
			if (!currentShowChannel.equals(Statics.CHANNEL_PM)) {
				itemSize = tempList.size();
			}
			
			final CharSequence[] listItems = new CharSequence[itemSize + 2];
	
			listItems[0] = context.getString(R.string.select_friend);
			listItems[1] = context.getString(R.string.enter_name);
			
			for (int i = 0; i < itemSize; i++) {
				listItems[i + 2] = tempList.get(i).getName();
			}
	
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(context.getString(R.string.select_channel));
			builder.setItems(
				listItems,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, final int item) {
						if (item == 0) {
							final List<Friend> tempList = new ArrayList<Friend>();
							tempList.addAll(friendList);
							
							if (settings.getBoolean("showOnlyOnline", false)) {
								for (int i = tempList.size() - 1; i >= 0; i--) {
									if (!tempList.get(i).isOnline()) {
										tempList.remove(i);
									}
								}
							}
							
							Collections.sort(tempList, new Friend.CustomComparator());
							
							AlertDialog.Builder builder = new AlertDialog.Builder(context);
							builder.setTitle(context.getString(R.string.select_friend));
							builder.setAdapter(new FriendAdapter(context, R.id.friendlist, tempList, false), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										currentTargetCharacter = tempList.get(which).getName();
										currentTargetChannel = "";
										setServiceTargets();
									}
								}
							);
							
							builder.setNegativeButton(context.getString(R.string.cancel), new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									setChannel();
								}
							});
					
							builder.create().show();
						} else if (item == 1) {
				            LayoutInflater factory = LayoutInflater.from(context);
				            View view = factory.inflate(R.layout.alert_character, null);
				            final EditText username = (EditText) view.findViewById(R.id.username);
				            
				            AlertDialog.Builder builder = new AlertDialog.Builder(context);
			                builder.setTitle(R.string.enter_name);
			                builder.setView(view);
			                
			                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			                    public void onClick(DialogInterface dialog, int whichButton) {
			                    	if (username.getText().toString().length() > 0) {
			                    		currentTargetCharacter = NameFormat.format(username.getText().toString());
										currentTargetChannel = "";
										setServiceTargets();
			                    	}
			                    }
			                });
			                
			                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			                    public void onClick(DialogInterface dialog, int whichButton) {
			                    	setChannel();
			                    }
			                });
			                
			                builder.create();
			                builder.show();
						} else if (item > 1) {
							currentTargetCharacter = "";
							currentTargetChannel = tempList.get(item - 2).getName();
						}
						
						setServiceTargets();
					}
				}
			);
			
			builder.setNegativeButton(context.getString(R.string.cancel), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
	
			builder.create().show();
		}
	}
	
	public static void setServiceTargets() {
		if (service != null) {
			Message msg = Message.obtain(null, Statics.MESSAGE_SET_CHARACTER);
	        msg.obj = currentTargetCharacter;
	        msg.replyTo = serviceMessenger;
	        
	        try {
				service.send(msg);
			} catch (RemoteException e) {
				Logging.log(APP_TAG, e.getMessage());
			}
			
	        msg = Message.obtain(null, Statics.MESSAGE_SET_CHANNEL);
	        msg.obj = currentTargetChannel;
	        msg.replyTo = serviceMessenger;
	        
	        try {
				service.send(msg);
			} catch (RemoteException e) {
				Logging.log(APP_TAG, e.getMessage());
			}
			
	        msg = Message.obtain(null, Statics.MESSAGE_SET_SHOW);
	        msg.obj = currentShowChannel;
	        msg.replyTo = serviceMessenger;
	        
	        try {
				service.send(msg);
			} catch (RemoteException e) {
				Logging.log(APP_TAG, e.getMessage());
			}
		}
		
		updateInputHint();		
	}
	
	private static void showLoader(String message) {
		loader.setMessage(message + activity.getString(R.string.dots));
		loader.show();
	}
	
	private static void hideLoader() {
    	if (loader != null) {
    		loader.dismiss();
    	}		
	}
	
	public static void hideSoftKeyboard(View v) {
	    InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
	    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}	
	
	private List<Tool> getToolItems() {
		List<Tool> toolList = new ArrayList<Tool>();
		
		toolList.add(new Tool(getString(R.string.connect), R.drawable.grid_icon_refresh, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Message msg = Message.obtain(null, Statics.MESSAGE_STATUS);
					msg.replyTo = serviceMessenger;
					service.send(msg);
				} catch (RemoteException e) {
					Logging.log(APP_TAG, e.getMessage());
				}
			}
		}));

		toolList.add(new Tool(getString(R.string.search), R.drawable.grid_icon_search, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				search();
			}
		}));
		
		toolList.add(new Tool(getString(R.string.market_monitor), R.drawable.grid_icon_shopping, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, Market.class);
				startActivity(intent);
			}
		}));
		
		toolList.add(new Tool(getString(R.string.ao_universe), R.drawable.grid_icon_puzzle, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, AOU.class);
				startActivity(intent);
			}
		}));
		
		toolList.add(new Tool(getString(R.string.recipebook), R.drawable.grid_icon_books, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, RecipeBook.class);
				startActivity(intent);
			}
		}));

		toolList.add(new Tool(getString(R.string.towerwars), R.drawable.grid_icon_chess, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, Towerwars.class);
				startActivity(intent);
			}
		}));
		

		toolList.add(new Tool(getString(R.string.map), R.drawable.grid_icon_globe, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, Map.class);
				startActivity(intent);
			}
		}));

		toolList.add(new Tool(getString(R.string.preferences), R.drawable.grid_icon_process, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, Preferences.class);
				startActivity(intent);
			}
		}));
		
		return toolList;
	}
	
	public static Context getContext() {
		return context;
	}
	
	public static Activity getActivity() {
		return activity;
	}
	
	public static String getDeviceIdentifier() {
		final TelephonyManager tm = (TelephonyManager) AOTalk.getContext().getSystemService(Context.TELEPHONY_SERVICE);
	    final String tmDevice, tmSerial, androidId;
	    
	    tmDevice = "" + tm.getDeviceId();
	    tmSerial = "" + tm.getSimSerialNumber();
	    androidId = "" + android.provider.Settings.Secure.getString(AOTalk.getContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

	    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
	    
	    return deviceUuid.toString();
	}
		
	public static String getGCMRegistrationId() {
		return GCMRegistrationId;
	}
	
	public static void gcmRegister(Context context) {
		if (android.os.Build.VERSION.SDK_INT > 7) {
			GCMRegistrar.checkDevice(context);
			GCMRegistrar.checkManifest(context);
			
			GCMRegistrationId = GCMRegistrar.getRegistrationId(context);
			
			if (GCMRegistrationId == null || GCMRegistrationId.equals("")) {
				GCMRegistrar.register(context, Statics.GCMSenderID);
			} else {
				Logging.log(APP_TAG, "Already registered");
			}
			
			Logging.log(APP_TAG, "RegID: " + GCMRegistrationId);
		} else {
			Logging.log(APP_TAG, "Unsupported Android version");
		}
	}
	
	public static void gcmUnregister(Context context) {
		if (android.os.Build.VERSION.SDK_INT > 7) {
			GCMRegistrar.unregister(context);
		} else {
			Logging.log(APP_TAG, "Unsupported Android version");
		}
	}
	
    public class CheckRKNetAccount extends AsyncTask<Void, Void, String> {
    	private AccountManager accountManager;
    	private android.accounts.Account[] accounts;
    	private RKNAccount rknetaccount = null;

        @Override    
        protected void onPreExecute() {
        }

        @Override 
		protected void onPostExecute(String result) {
	     }

		@Override
		protected String doInBackground(Void... params) {			
	        long loadTime = System.currentTimeMillis();
	    	
			HttpClient httpclient;
			HttpPost httppost;
	        
	    	HttpResponse response;
	    	HttpEntity entity;
	    	InputStream is;
	    	BufferedReader reader;
	    	StringBuilder sb;
	    	String line;
	    	String resultData;
	    	JSONObject json_data;
	    	
	        accountManager = AccountManager.get(context);
			accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
	    		    	
			if (accounts.length > 0) {
				try {
		    		httpclient = new DefaultHttpClient();
			        httppost = new HttpPost(RKNet.getApiAccountPath(RKNet.RKNET_ACCOUNT_LOGIN));
	
			        JSONObject j = new JSONObject();
			        
			        j.put("Username", accounts[0].name);
			        j.put("Password", accountManager.getPassword(accounts[0]));
			        			        
			        httppost.setEntity(new StringEntity(j.toString()));
			        httppost.setHeader("Accept", "application/json");
			        httppost.setHeader("Content-type", "application/json");
			        
			        response = httpclient.execute(httppost);
			        entity = response.getEntity();
			        is = entity.getContent();
			        
			    	try {
			    		reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
		    	        sb = new StringBuilder();
		    	        line = null;
		    	        
		    	        while ((line = reader.readLine()) != null) {
		    	        	sb.append(line + "\n");
		    	        }
		    	        
		    	        is.close();
		    	 
		    	        resultData = sb.toString();
			    	} catch(Exception e){
			    	    Logging.log(APP_TAG, "Error converting result " + e.toString());
			    	    resultData = null;
			    	}
		    	} catch(Exception e){
		    		Logging.log(APP_TAG, "Error in http connection " + e.toString());
		    		resultData = null;
		    	}
	
		    	try {
		    		if(resultData != null) {
		    			resultData = resultData.substring(0, resultData.lastIndexOf("}")).replace("{\"d\":", "");
			    		
		    			if((!resultData.startsWith("null"))) {
		    				json_data = new JSONObject(resultData);
		    				
		    				rknetaccount = new RKNAccount(
		                		json_data.getInt("Id"),
		                		json_data.getString("Username"),
		                		json_data.getString("Password")
			                );
		    				
		    				JSONArray registrations = json_data.getJSONArray("Registrations");
		    				
		    				boolean isRegistered = false;
		    				
		    				for(int i = 0; i < registrations.length(); i++){
		    					 JSONObject reg = registrations.getJSONObject(i);
		    					 if (reg.getString("Key").equals(AOTalk.getGCMRegistrationId())) {
		    						 isRegistered = true;
		    					 }
		    				}
		    				
		    				if (!isRegistered && AOTalk.getGCMRegistrationId() != null && !AOTalk.getGCMRegistrationId().equals("")) {
		    					Logging.log(APP_TAG, "Device not registered");
		    					
		    		    		try{
		    			    		httpclient = new DefaultHttpClient();
		    				        httppost = new HttpPost(RKNet.getApiAccountPath(RKNet.RKNET_ACCOUNT_SETKEYS));
		    		
		    				        JSONObject j = new JSONObject();
		    				        j.put("AccountId", rknetaccount.getAccountId());
		    				        j.put("Key", AOTalk.getGCMRegistrationId());
		    				        j.put("UUID", AOTalk.getDeviceIdentifier());
		    				        
		    				        Logging.log(APP_TAG, j.toString(1));

		    				        httppost.setEntity(new StringEntity(j.toString()));
		    				        httppost.setHeader("Accept", "application/json");
		    				        httppost.setHeader("Content-type", "application/json");
		    				        
		    				        response = httpclient.execute(httppost);
		    				        entity = response.getEntity();
		    				        is = entity.getContent();
		    				        
		    				    	try{
		    				    		reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
		    			    	        sb = new StringBuilder();
		    			    	        line = null;
		    			    	        
		    			    	        while ((line = reader.readLine()) != null) {
		    			    	        	sb.append(line + "\n");
		    			    	        }
		    			    	        
		    			    	        is.close();
		    			    	 
		    			    	        resultData = sb.toString();
		    				    	} catch(Exception e){
		    				    	    Logging.log(APP_TAG, "Error converting result " + e.toString());
		    				    	    resultData = null;
		    				    	}
		    			    	} catch(Exception e){
		    			    		Logging.log(APP_TAG, "Error in http connection " + e.toString());
		    			    		resultData = null;
		    			    	}
		    				} else {
		    					Logging.log(APP_TAG, "Device already registered");
		    				}
			    		}
		    			
		            	tracker.sendTiming("Loading", System.currentTimeMillis() - loadTime, "Check account data", null);
		    		}
		    	} catch(JSONException e){
		    		Logging.log(APP_TAG, "Error parsing data " + e.toString());
		    	}
			}
			
			return null;
		}
	};
	
	private void checkIfSoundsExists() {
		int filesAdded = 0;
		
		ArrayList<Integer> listId = new ArrayList<Integer>();
		ArrayList<String> listName = new ArrayList<String>();
		
		Field[] fields = R.raw.class.getFields();
		
		for(Field f : fields) {
			try {
				listId.add(f.getInt(null));
				listName.add(f.getName());
			} catch (IllegalArgumentException e) {
				Logging.log(APP_TAG, e.getMessage());
			} catch (IllegalAccessException e) {
				Logging.log(APP_TAG, e.getMessage());
			}
		}
		
		if (StorageTools.isExternalStorageAvailable() && !StorageTools.isExternalStorageReadOnly()) {
			for (int i = 0; i < listId.size(); i++) {
				File f = new File(
						android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + 
						File.separator + "Notifications" + 
						File.separator + listName.get(i) + ".mp3"
					);
				
				if (!f.exists()) {
					BufferedOutputStream stream;
					
					try {
						stream = new BufferedOutputStream((new FileOutputStream(f)));
						BufferedInputStream reader = new BufferedInputStream(getResources().openRawResource(listId.get(i)));
						
						byte[] buff = new byte[32 * 1024];
						int len;
						
						while( (len = reader.read(buff)) > 0 ){
							stream.write(buff,0,len);
						}
						
						stream.flush();
						stream.close();
						
						filesAdded++;
						Logging.log(APP_TAG, String.format("Added %s to the memory card", listName.get(i)));
					} catch (FileNotFoundException e) {
						Logging.log(APP_TAG, e.getMessage());
					} catch (IOException e) {
						Logging.log(APP_TAG, e.getMessage());
					}
				}
			}
		}
		
		if (filesAdded > 0) {
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory() + File.separator + "Notifications")));
			Logging.log(APP_TAG, "Broadcasting media update");
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		//super.setTheme(R.style.Theme_AOTalkTheme_Light);

        checkIfSoundsExists();
        
        context = this;
        activity = this;
        
        EasyTracker.getInstance().setContext(this);
        tracker = EasyTracker.getTracker();
		
        gcmRegister(context);
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();
        
        if (settings.getBoolean("runAfterMerge", true)) {
        	List<Account> accounts = DatabaseHandler.getInstance(this).getAllAccounts();
        	List<Account> keep = new ArrayList<Account>();
        	
        	if (accounts != null) {
            	boolean keepAccount = true;
            	
	        	for (Account account : accounts) {
	        		for (Account a : keep) {
	        			if (account.getUsername().equals(a.getUsername())) {
	        				keepAccount = false;
	        			}
	        		}
	        		
	        		if (keepAccount) {
	        			keep.add(account);
	        		}
	        	}
	        	
	        	for (Account account : accounts) {
	        		if (!keep.contains(account)) {
	            		DatabaseHandler.getInstance(this).updateAccount(account);
	        		} else {
	        			DatabaseHandler.getInstance(this).deleteAccount(account);       			
	        		}
	        	}  
        	}
        	
        	editor.clear();
        	editor.putBoolean("runAfterMerge", false);
        	editor.commit();
        }
        
        settings.registerOnSharedPreferenceChangeListener(this);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.main);
        setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
                		
		friendAdapter = new FriendAdapter(this, R.id.friendlist, new ArrayList<Friend>(), true);

		channelAdapter = new ChannelAdapter(this, R.id.messagelist, currentChannels);
		channelAdapter.add(new Channel(getString(R.string.app_name), 0, true, false));
				
		bar = getSupportActionBar();
		
		bar.setDisplayShowTitleEnabled(false);
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);		
		bar.setListNavigationCallbacks(channelAdapter, new OnNavigationListener() {
			@Override
			public boolean onNavigationItemSelected(int itemPosition , long itemId) {
				messageAdapter.clear();
				
				if (currentShowChannel == Statics.CHANNEL_MAIN) {
					editor.putString("lastUsedChannel", currentTargetChannel);
					editor.putString("lastUsedCharacter", currentTargetCharacter);
					editor.commit();
				}
				
				if (itemPosition == 0) {
					currentTargetChannel = settings.getString("lastUsedChannel", "");
					currentTargetCharacter = settings.getString("lastUsedCharacter", "");
					currentShowChannel = Statics.CHANNEL_MAIN;
				} else if (itemPosition == 1) {
					currentTargetChannel = "";
					currentTargetCharacter = settings.getString("lastUsedCharacter", "");
					currentShowChannel = Statics.CHANNEL_PM;
				} else {
					List<Channel> tempList = new ArrayList<Channel>();
					tempList.addAll(channelList);
					tempList.addAll(privateList);
					
					for (Channel channel : tempList) {
						if (channel.getName().equals(channelAdapter.getItem(itemPosition).getName())) {
							currentTargetChannel = channelAdapter.getItem(itemPosition).getName();
							currentTargetCharacter = "";
							currentShowChannel = channelAdapter.getItem(itemPosition).getName();
							
							break;
						}
					}
				}
				
				if (findViewById(R.id.channel) != null) {
					if (itemPosition > 1) {
						((ImageButton) findViewById(R.id.channel)).setVisibility(View.GONE);
					} else {
						((ImageButton) findViewById(R.id.channel)).setVisibility(View.VISIBLE);
					}
				}
				
				setServiceTargets();
				
    	        messageHandler.removeCallbacks(messageListRunnable);
    	        messageHandler.postDelayed(messageListRunnable, Statics.HANDLER_DELAY);
				
				return true;
			}
		});
		
		loader = new ProgressDialog(context);
		loader.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		loader.setCancelable(false);

        databaseHandler = DatabaseHandler.getInstance(context);
        messageAdapter = new ChatMessageAdapter(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<ChatMessage>(), settings.getBoolean("enableAnimations", true));
				
		gridAdapter = new GridAdapter(this, R.id.grid, getToolItems());

		fragments = new Vector<SherlockFragment>();
        fragments.add(FragmentRKN.newInstance());
        fragments.add(FragmentTools.newInstance());
        fragments.add(FragmentChat.newInstance());
       	
        if (this.getLayoutInflater().inflate(R.layout.fragment_chat, null, false).findViewById(R.id.friendsfragment) == null) {
        	fragments.add(FragmentFriends.newInstance());
        }
        
        FragmentAdapter fragmentAdapter = new FragmentAdapter(super.getSupportFragmentManager(), fragments);

        fragmentPager = (ViewPager) findViewById(R.id.fragmentpager);
        fragmentPager.setAdapter(fragmentAdapter);
        fragmentPager.setOnPageChangeListener(this);
        fragmentPager.setPageMargin(0);
        fragmentPager.setKeepScreenOn(settings.getBoolean("keepScreenOn", false));
        fragmentPager.setCurrentItem(1);

        titleIndicator = (TitlePageIndicator)findViewById(R.id.titles);
        titleIndicator.setViewPager(fragmentPager);

        setTitleIndicator();
        bindService();
        
        new CheckRKNetAccount().execute();
    }
    
    private static void setTitleIndicator() {
        if (settings.getBoolean("hideTitles", false)) {
        	titleIndicator.setVisibility(View.GONE);
        } else {
        	titleIndicator.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
    	super.onResume();
            	
    	setTitleIndicator();
        bindService();
        
        if (settings != null) {
	        if (fragmentPager != null) {
	        	fragmentPager.setKeepScreenOn(settings.getBoolean("keepScreenOn", false));
	        }
        
	        GoogleAnalytics.getInstance(this).setDebug(settings.getBoolean("enableDebug", false));
        }
        
        if (channelList.size() > 1) {
        	hideLoader();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        unbindService();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();

        unbindService();
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
    	try {
        	EasyTracker.getInstance().activityStart(this);
    	} catch (IllegalStateException e) {
    		Logging.log(APP_TAG, e.getMessage());
    	}
    }
    
    @Override
    protected void onStop() {
    	super.onStop();

    	try {
            EasyTracker.getInstance().activityStop(this);
    	} catch (IllegalStateException e) {
    		Logging.log(APP_TAG, e.getMessage());
    	}
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.connect) {
			try {
				Message msg = Message.obtain(null, Statics.MESSAGE_STATUS);
				msg.replyTo = serviceMessenger;
				service.send(msg);
			} catch (RemoteException e) {
				Logging.log(APP_TAG, e.getMessage());
			}
			return true;
		} else if (item.getItemId() == R.id.tower) {
			Intent intent = new Intent(context, Towerwars.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.market) {
			Intent intent = new Intent(context, Market.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.aou) {
			Intent intent = new Intent(context, AOU.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.aorb) {
			Intent intent = new Intent(context, RecipeBook.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.atlas) {
			Intent intent = new Intent(context, Map.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.preferences) {
			Intent intent = new Intent(context, Preferences.class);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int arg0) {
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("enableFaces") || key.equals("showTimestamp")) {
	        Logging.log(APP_TAG, "Changing faces");
			messageHandler.removeCallbacks(messageListRunnable);
			messageAdapter.clear();
			messageHandler.postDelayed(messageListRunnable, Statics.HANDLER_DELAY);			
		}
		
		if (key.equals("showOnlyOnline")) {
	        Logging.log(APP_TAG, "Changing online only");
	        friendHandler.removeCallbacks(friendListRunnable);
	        friendHandler.postDelayed(friendListRunnable, Statics.HANDLER_DELAY);
		}
	}
}