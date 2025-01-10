package com.example.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView mWebView;
    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            // Se houver páginas no histórico, voltar para a página anterior
            mWebView.goBack();
        } else {
            // Se não houver, comportar-se como o botão "voltar" padrão do Android
            super.onBackPressed();
        }
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = findViewById(R.id.activity_main_webview);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Definindo o WebViewClient para garantir que os links sejam abertos na WebView
        mWebView.setWebViewClient(new MyWebViewClient());

        // Definindo o WebChromeClient para lidar com uploads de arquivos
        mWebView.setWebChromeClient(new WebChromeClient() {
            // Para Android 5.0+
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILECHOOSER_RESULTCODE);
                } catch (Exception e) {
                    mFilePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        // REMOTE RESOURCE
        mWebView.loadUrl("https://example.com");

        // LOCAL RESOURCE
        mWebView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mFilePathCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        String dataString = data.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Se o URL for um arquivo de download, tratar manualmente o download
            if (url.endsWith(".pdf") || url.endsWith(".zip") || url.endsWith(".doc") || url.endsWith(".docx")) {
                // Mostrar um diálogo de confirmação antes de iniciar o download
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Download")
                        .setMessage("Deseja baixar o arquivo?")
                        .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                String cookies = CookieManager.getInstance().getCookie(url);
                                String fileName = URLUtil.guessFileName(url, null, null);
                                request.addRequestHeader("cookie", cookies);
                                request.addRequestHeader("User-Agent", view.getSettings().getUserAgentString());
                                request.setDescription("Baixando arquivo...");
                                request.setTitle(fileName);
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);
                                Toast.makeText(getApplicationContext(), "Baixando arquivo...", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_menu_save)
                        .show();
                return true; // Bloquear a WebView de carregar o URL de arquivo diretamente
            }

            // Permitir que URLs de outras páginas sejam carregadas na WebView
            return false;
        }
    }
}
