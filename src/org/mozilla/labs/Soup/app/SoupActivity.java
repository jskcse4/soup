package org.mozilla.labs.Soup.app;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.R;

import com.phonegap.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public abstract class SoupActivity extends DroidGap {
	public static final String ACTION_WEBAPP = "org.mozilla.labs.webapp";
	static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
	    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL
	);
	
	private SoupChromeClient appClient;
	
	private class SoupChildViewClient extends WebViewClient {

		public SoupChildViewClient() {
		}
		
		/**
         * Give the host application a chance to take over the control when a new url 
         * is about to be loaded in the current WebView.
         * 
         * @param view          The WebView that is initiating the callback.
         * @param url           The url to be loaded.
         * @return              true to override, false for default behavior
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
        	Log.i("SoupChildViewClient", "Going to load " + url);
        	view.loadUrl(url);
        	
        	return true;
        }
		
		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onLoadResource(android.webkit.WebView, java.lang.String)
		 */
		@Override
		public void onLoadResource(WebView view, String url) {
			// Log.i("SoupChildViewClient", "onLoadResource " + url);
		}
		
		@Override
		public void onReceivedError(WebView view, int err, String desc, String url) {
			// Log.i("SoupChildViewClient", "onReceivedError " + url + ": " + desc);
		}
		
		/**
         * Give the host application a chance to take over the control when a new url 
         * is about to be loaded in the current WebView.
         * 
         * @param view          The WebView that is initiating the callback.
         * @param url           The url to be loaded.
         * @return              true to override, false for default behavior
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
        	Log.i("SoupChildViewClient", "Going to load " + url);

        	super.onPageStarted(view, url, favicon);
        	
        	injectJavaScript(view);
        }
		
	}

	/**
	 * SoupChildClient
	 * 
	 * WebChromeClient for child webkit
	 */
	private class SoupChildChromeClient extends WebChromeClient {
		
		public void onCloseWindow(WebView view) {
			// Closing our only dialog without checking what view is!
			appClient.onClick(view);
		}
		
	}
	
	/**
	 * SoupViewClient
	 * 
	 * WebViewClient for main webkit
	 */
	private class SoupViewClient extends GapViewClient {

		public SoupViewClient(DroidGap ctx) {
			super(ctx);
        }
		
		/**
         * Give the host application a chance to take over the control when a new url 
         * is about to be loaded in the current WebView.
         * 
         * @param view          The WebView that is initiating the callback.
         * @param url           The url to be loaded.
         * @return              true to override, false for default behavior
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
        	Log.i("SoupViewClient", "Going to load " + url);

        	super.onPageStarted(view, url, favicon);
        	
        	injectJavaScript(view);
        }
		
		/* (non-Javadoc)
		 * @see com.phonegap.DroidGap.GapViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
		 */
		@Override
		public void onPageFinished(WebView view, String url) {
			Log.i("SoupViewClient", "onPageFinished: " + url);
			
			SoupActivity.this.setTitle(view.getTitle());
			
			super.onPageFinished(view, url);
		}
	}
	
	/**
	 * SoupGapClient
	 * 
	 * WebChromeClient for main window
	 */
	private class SoupChromeClient extends GapClient implements OnClickListener {

		private View container;
		private WebView childView;
		
		/**
		 * @param context
		 */
		public SoupChromeClient(Context context) {
			super(context);
		}
		
		
		/* (non-Javadoc)
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		public void onClick(View v) {
			Log.i("onClick", "Close clicked, removing Child");
			
			childView.destroy();
			
			ViewGroup content = (ViewGroup)getWindow().getDecorView();
			content.removeView(container);
		}
		
		@Override
		public void onCloseWindow(WebView view) {
			// Closing our only dialog without checking what view is!
			// onClick(view);
		}
		
		@Override
		public boolean onCreateWindow(WebView view, boolean modal, boolean user, Message result) {
			// TODO Launch on UI thread
			createChildWindow();
			
			WebView.WebViewTransport transport = (WebView.WebViewTransport) result.obj;
			
			transport.setWebView(childView);
			result.sendToTarget();
			
			return true;
		}
		
		private void createChildWindow() {
			LayoutInflater inflater = LayoutInflater.from(SoupActivity.this);
			
			container = inflater.inflate(R.layout.popup, null);
			childView = (WebView) container.findViewById(R.id.webViewPopup);
			ImageButton close = (ImageButton) container.findViewById(R.id.subwindow_close);
			close.setOnClickListener(this);
			
			childView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
			childView.setMapTrackballToArrowKeys(false);
			// childView.zoomOut();

	        final WebSettings settings = childView.getSettings();
	        settings.setDomStorageEnabled(true);
	        settings.setBuiltInZoomControls(true);
	        settings.setJavaScriptEnabled(true);
	        settings.setAllowFileAccess(true);
	        
	        // settings.setSupportMultipleWindows(true);
	        settings.setJavaScriptCanOpenWindowsAutomatically(true);
			
	        ViewGroup content = (ViewGroup)getWindow().getDecorView();
			content.addView(container, COVER_SCREEN_PARAMS);
			
			childView.setWebViewClient(new SoupChildViewClient());
			childView.setWebChromeClient(new SoupChildChromeClient());
			
			childView.requestFocus(View.FOCUS_DOWN);
			childView.requestFocusFromTouch();
		}
	}
	
	private void injectJavaScript(WebView view) {
		// core
		injectSingleFile(view, "phonegap/phonegap-1.2.0.js");
		
		// plugins
		injectSingleFile(view, "phonegap/moz-id.js");
		injectSingleFile(view, "phonegap/moz-apps.js");
		injectSingleFile(view, "phonegap/moz-apps-mgmt.js");
		
		// soup bridge
		injectSingleFile(view, "soup-addon.js");
	}
	
	private void injectSingleFile(WebView view, String file) {
		String strContent;
		
		try {
			InputStream is = getAssets().open("www/js/" + file);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			// Read the entire asset into a local byte buffer.
			// Convert the buffer into a string.
			strContent = new String(buffer);
		} catch (IOException e) {
			return;
		}
		
		Log.d("SoupActivity", "injectSingleFile " + file);
		view.loadUrl("javascript:" + strContent);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
//        Log.i("SoupActivity", "onCreate called with Intent " + getIntent().getAction());

        // Resolve the intent

        this.onResolveIntent();
    }
    
    /**
     * Called when the activity receives a new intent
     **/
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        if (intent.equals(getIntent())) {
//            Log.i("SoupActivity", "onNewIntent equals current Intent: " + intent);
        	return;
        }

//        Log.i("SoupActivity", "onNewIntent called with new Intent " + intent);
        
        setIntent(intent);
        
        this.onResolveIntent();
    }
    
    /**
     * Init phonegap and create layout
     * 
     * @return true if the layout was freshly created
     */
    public boolean onCreateLayout() {
    	if (appView != null) {
    		Log.i("SoupActivity", "init skipped");
    		return false;
    	}
    	
    	super.setStringProperty("loadingDialog", "Loading App");
    	// super.setStringProperty("loadingPageDialog", "Loading App");
    	super.setStringProperty("errorUrl", "file:///android_asset/www/error.html");
    	// super.setIntegerProperty("splashscreen", R.drawable.splash);
    	
    	super.init();

        // Set our own extended webkit client and clientview
    	appClient = new SoupChromeClient(SoupActivity.this);
        appView.setWebChromeClient(appClient);
        setWebViewClient(this.appView, new SoupViewClient(this));
        
        final WebSettings settings = appView.getSettings();
        
        // Allow window.open, bridged by onCreateWindow
        settings.setSupportMultipleWindows(true);
        
        return true;
    }
    
    public JSONArray findAll() {
    	
    	JSONArray list = new JSONArray(); 
		
		try {
			// http://www.limejs.com/roundball.webapp
			JSONObject app1 = new JSONObject();
			app1.put("origin", "http://www.limejs.com");
			app1.put("manifest", new JSONObject("{\"name\":\"Roundball\",\"description\":\"Roundball is a fun match three puzzle game where you form horizontal or vertical lines of at least three similar objects by swapping two adjacent items. The more matches you make, the higher your score. Two game modes: Classic and Timed mode. Works on regular computer or on touchscreens.\",\"launch_path\":\"/static/roundball/index.html\",\"developer\":{\"name\":\"Digital Fruit\",\"url\":\"http://www.limejs.com/\"},\"icons\":{\"128\":\"/static/roundball_icon_128.png\"},\"installs_allowed_from\":[\"*\"]}"));
			list.put(app1);
			
			// http://sinuousgame.com/manifest.webapp
			JSONObject app2 = new JSONObject();
			app2.put("origin", "http://sinuousgame.com");
			app2.put("manifest", new JSONObject("{\"name\":\"Sinuous\",\"description\":\"Avoid the red dots in this fun and addictive game.\",\"launch_path\":\"/\",\"developer\":{\"name\":\"Hakim El Hattab\",\"url\":\"http://hakim.se/experiments/\"},\"icons\":{\"128\":\"/assets/images/icon_128.png\"},\"installs_allowed_from\":[\"*\"]}"));
			list.put(app2);
			
			// http://shazow.net
			// http://shazow.net/linerage/gameon/manifest.json
			JSONObject app3 = new JSONObject();
			app3.put("origin", "http://shazow.net");
			app3.put("manifest", new JSONObject("{\"name\":\"LineRage\",\"description\":\"You are a line. Don't hit things.\",\"launch_path\":\"/linerage/gameon/index.html\",\"developer\":{\"name\":\"Andrey Petrov\",\"url\":\"http://shazow.net\"},\"icons\":{\"16\":\"/linerage/gameon/icon_16.png\",\"32\":\"/linerage/gameon/icon_32.png\",\"128\":\"/linerage/gameon/icon_128.png\"},\"installs_allowed_from\":[\"*\"]}"));
			list.put(app3);
			
			// http://stillalivejs.t4ils.com
			// http://stillalivejs.t4ils.com/play/manifest.webapp
			JSONObject app4 = new JSONObject();
			app4.put("origin", "http://stillalivejs.t4ils.com");
			app4.put("manifest", new JSONObject("{\"name\":\"StillAliveJS\",\"description\":\"StillAliveJS, or SaJS, is a puzzle game inspired by Portal: The Flash Version which is a 2D renewal of Portal, developed by Valve Corporation.\n\nSaJS consists primarily in a series of platform puzzles that must be solved by teleporting the character and other simple objects using a Portal Gun. The unusual physics allowed by this device is the emphasis of StillAliveJS.\",\"launch_path\":\"/play/index.html\",\"developer\":{\"name\":\"t4ils and Zeblackos\",\"url\":\"http://stillalivejs.t4ils.com/\"},\"icons\":{\"128\":\"/play/images/icon128.png\"},\"installs_allowed_from\":[\"*\"]}"));
			list.put(app4);
			
			
		} catch (JSONException e) {}
		
		return list;
    }
    
    public JSONObject findOneByOrigin(String constrain) {
    	
    	JSONArray list = findAll();
    	
    	try {
	    	for (int i = 0; i < list.length(); i++) {
	    		JSONObject app = list.getJSONObject(i);
	    		String origin = app.getString("origin");
	    		
	    		if (constrain.equals(origin)) {
	    			return app;
	    		}
	    	}
    	} catch (JSONException e) {}
    	
    	return null;
    }

	protected abstract void onResolveIntent();

}
