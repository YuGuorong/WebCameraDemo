/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.permissionrequest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This fragment shows a {@link WebView} and loads a web app from the {@link SimpleWebServer}.
 */
public class PermissionRequestFragment extends Fragment
        implements ConfirmationDialogFragment.Listener {

    private static final String TAG = PermissionRequestFragment.class.getSimpleName();

    private static final String FRAGMENT_DIALOG = "dialog";

    /**
     * We use this web server to serve HTML files in the assets folder. This is because we cannot
     * use the JavaScript method "getUserMedia" from "file:///android_assets/..." URLs.
     */
    private SimpleWebServer mWebServer;

    /**
     * A reference to the {@link WebView}.
     */
    private WebView mWebView;

    /**
     * This field stores the {@link PermissionRequest} from the web application until it is allowed
     * or denied by user.
     */
    private PermissionRequest mPermissionRequest;

    /**
     * For testing.
     */
    private ConsoleMonitor mConsoleMonitor;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permission_request, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mWebView = (WebView) view.findViewById(R.id.web_view);
        // Here, we use #mWebChromeClient with implementation for handling PermissionRequests.
        mWebView.setWebChromeClient(mWebChromeClient);
        configureWebSettings(mWebView.getSettings());
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }
        });


        final EditText editText=(EditText)getActivity().findViewById(R.id.url);
        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                String surl = v.getText().toString();
                surl.trim();

                surl = checkUrlHeader(surl);
                if( getCompleteUrl(surl) ) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0); //强制隐藏键盘
                    mWebView.loadUrl(surl);
                }
                else {
                    Toast.makeText(getActivity(), "不能解析“" + v.getText() + "”", Toast.LENGTH_SHORT).show();

                }
                return false;
            }
        });
    }

    public static String checkUrlHeader(String text) {
        Pattern p = Pattern.compile("((http|ftp|https)://).+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(text);
        if( matcher.find() == false)
            text = "http://" + text;
        return text;
    }

    public static boolean getCompleteUrl(String text) {
        if( text.indexOf("localhost") >=0 )
            return true;
        Pattern p = Pattern.compile("((http|ftp|https)://)(([\\w\\.-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[\\w\\&%_\\./-~-\\ ]*)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(text);
        return ( matcher.find() );

    }

    @Override
    public void onResume() {
        super.onResume();
        final int port = 8080;
        mWebServer = new SimpleWebServer(port, getResources().getAssets());
        mWebServer.start();
        mWebView.loadUrl("http://localhost:" + port + "/sample.html");
        //mWebView.loadUrl("http://192.168.1.88:" + port + "/camera.html");
    }

    @Override
    public void onPause() {
        mWebServer.stop();
        super.onPause();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void configureWebSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
    }

    /**
     * This {@link WebChromeClient} has implementation for handling {@link PermissionRequest}.
     */
    private WebChromeClient mWebChromeClient = new WebChromeClient() {

        // This method is called when the web content is requesting permission to access some
        // resources.
        //当网页内容被请求允许访问某些资源，调用此方法。
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            Log.i(TAG, "onPermissionRequest");
            mPermissionRequest = request;
            ConfirmationDialogFragment.newInstance(request.getResources())
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }

        // This method is called when the permission request is canceled by the web content.
        //这个方法在授权请求被取消时被调用
        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            Log.i(TAG, "onPermissionRequestCanceled");
            // We dismiss the prompt UI here as the request is no longer valid.
            mPermissionRequest = null;
            DialogFragment fragment = (DialogFragment) getChildFragmentManager()
                    .findFragmentByTag(FRAGMENT_DIALOG);
            if (null != fragment) {
                fragment.dismiss();
            }
        }


        @Override
        public boolean onConsoleMessage(@NonNull ConsoleMessage message) {
            switch (message.messageLevel()) {
                case TIP:
                    Log.v(TAG, message.message());
                    break;
                case LOG:
                    Log.i(TAG, message.message());
                    break;
                case WARNING:
                    Log.w(TAG, message.message());
                    break;
                case ERROR:
                    Log.e(TAG, message.message());
                    break;
                case DEBUG:
                    Log.d(TAG, message.message());
                    break;
            }
            if (null != mConsoleMonitor) {
                mConsoleMonitor.onConsoleMessage(message);
            }
            return true;
        }

    };

    @Override
    public void onConfirmation(boolean allowed) {
        if (allowed) {
            mPermissionRequest.grant(mPermissionRequest.getResources());
            Log.d(TAG, "Permission granted.");
        } else {
            mPermissionRequest.deny();
            Log.d(TAG, "Permission request denied.");
        }
        mPermissionRequest = null;
    }

    public void setConsoleMonitor(ConsoleMonitor monitor) {
        mConsoleMonitor = monitor;
    }

    /**
     * For testing.
     */
    public interface ConsoleMonitor {
        public void onConsoleMessage(ConsoleMessage message);
    }

}
