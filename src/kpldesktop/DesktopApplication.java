/******************************************************************************
 * Copyright © 2013-2016 The kpl Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * kpl software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package kpldesktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;
import kpl.*;
import kpl.http.API;
import kpl.util.Convert;
import kpl.util.Logger;
import kpl.util.TrustAllSSLProvider;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DesktopApplication extends Application {

    private static final Set DOWNLOAD_REQUEST_TYPES = new HashSet<>(Arrays.asList("downloadTaggedData", "downloadPrunableMessage"));
    private static volatile boolean isLaunched;
    private static volatile Stage stage;
    private static volatile WebEngine webEngine;
    private JSObject KRS;

    public static void launch() {
        if (!isLaunched) {
            isLaunched = true;
            Application.launch(DesktopApplication.class);
            return;
        }
        if (stage != null) {
            Platform.runLater(() -> showStage(false));
        }
    }

    @SuppressWarnings("unused")
    public static void refresh() {
        Platform.runLater(() -> showStage(true));
    }

    private static void showStage(boolean isRefresh) {
        if (isRefresh) {
            webEngine.load(getUrl());
        }
        if (!stage.isShowing()) {
            stage.show();
        } else if (stage.isIconified()) {
            stage.setIconified(false);
        } else {
            stage.toFront();
        }
    }

    public static void shutdown() {
        System.out.println("shutting down JavaFX platform");
        Platform.exit();
        System.out.println("JavaFX platform shutdown complete");
    }

    @Override
    public void start(Stage stage) throws Exception {
        DesktopApplication.stage = stage;
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        WebView browser = new WebView();
        //browser.setOnContextMenuRequested(new WalletContextMenu());
        browser.setContextMenuEnabled(true);
        WebView invisible = new WebView();

        int height = (int) Math.min(primaryScreenBounds.getMaxY() - 100, 1000);
        int width = (int) Math.min(primaryScreenBounds.getMaxX() - 100, 1618);
        browser.setMinHeight(height);
        browser.setMinWidth(width);
        webEngine = browser.getEngine();
        webEngine.setUserDataDirectory(Kpl.getConfDir());
        webEngine.getOnAlert();

        Worker<Void> loadWorker = webEngine.getLoadWorker();
        loadWorker.stateProperty().addListener(
                (ov, oldState, newState) -> {
                    JSObject window = (JSObject)webEngine.executeScript("window");
                    window.setMember("java", new JavaScriptBridge());
                    Locale locale = Locale.getDefault();
                    String language = locale.getLanguage().toLowerCase() + "-" + locale.getCountry().toUpperCase();
                    window.setMember("javaFxLanguage", language);
                    webEngine.executeScript("console.log = function(msg) { java.log(msg); };");
                    //stage.setTitle("kpl Desktop - " + webEngine.getLocation());
                    if (newState == Worker.State.SUCCEEDED) {
                        KRS = (JSObject) webEngine.executeScript("krs");
                        BlockchainProcessor blockchainProcessor = Kpl.getBlockchainProcessor();
                        blockchainProcessor.addListener((block) ->
                                updateClientState(BlockchainProcessor.Event.BLOCK_PUSHED, block), BlockchainProcessor.Event.BLOCK_PUSHED);
                        blockchainProcessor.addListener((block) ->
                                updateClientState(BlockchainProcessor.Event.AFTER_BLOCK_APPLY, block), BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
                        Kpl.getTransactionProcessor().addListener((transaction) ->
                                updateClientState(TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS, transaction), TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
                    }
                });

        // Invoked by the webEngine popup handler
        // The invisible webView does not show the link, instead it opens a browser window
        invisible.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
            popupHandlerURLChange(newValue);
        });

        // Invoked when changing the document.location property, when issuing a download request
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            webViewURLChange(newValue);
        });

        // Invoked when clicking a link to external site like Help or API console
        webEngine.setCreatePopupHandler(
            config -> {
                Logger.logInfoMessage("popup request from webEngine");
                return invisible.getEngine();
            });

        webEngine.load(getUrl());

        Scene scene = new Scene(browser);
        String address = API.getServerRootUri().toString();
        stage.setTitle("kpl");
        stage.getIcons().add(new Image(address + "/img/KPL-icon-32x32.png"));
        stage.initStyle(StageStyle.DECORATED);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
        Platform.setImplicitExit(false); // So that we can reopen the application in case the user closed it
    }

    private void updateClientState(BlockchainProcessor.Event blockEvent, Block block) {
        BlockchainProcessor blockchainProcessor = Kpl.getBlockchainProcessor();
        if (blockEvent == BlockchainProcessor.Event.BLOCK_PUSHED && blockchainProcessor.isDownloading()) {
            if (!(block.getHeight() % 100 == 0)) {
                return;
            }
        }
        if (blockEvent == BlockchainProcessor.Event.AFTER_BLOCK_APPLY) {
            if (blockchainProcessor.isScanning()) {
                if (!(block.getHeight() % 100 == 0)) {
                    return;
                }
            } else {
                return;
            }
        }
        String msg = blockEvent.toString() + " id " + block.getStringId() + " height " + block.getHeight();
        updateClientState(msg);
    }

    private void updateClientState(TransactionProcessor.Event transactionEvent, List<? extends Transaction> transactions) {
        if (transactions.size() == 0) {
            return;
        }
        String msg = transactionEvent.toString() + " ids " +
                transactions.stream().map(Transaction::getStringId).collect(Collectors.joining(","));
        updateClientState(msg);
    }

    private void updateClientState(String msg) {
        Platform.runLater(() -> webEngine.executeScript("krs.getState(null, '" + msg + "')"));
    }

    private static String getUrl() {
        String url = API.getWelcomePageUri().toString();
        if (url.startsWith("https")) {
            HttpsURLConnection.setDefaultSSLSocketFactory(TrustAllSSLProvider.getSslSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TrustAllSSLProvider.getHostNameVerifier());
        }
        String defaultAccount = Kpl.getStringProperty("kpl.defaultDesktopAccount");
        if (defaultAccount != null && !defaultAccount.equals("")) {
            url += "?account=" + defaultAccount;
        }
        return url;
    }

    // Invoked from JavaScript
    @SuppressWarnings("WeakerAccess")
    public void popupHandlerURLChange(String newValue) {
        Logger.logInfoMessage("popup request for " + newValue);
        Platform.runLater(() -> {
            try {
                Desktop.getDesktop().browse(new URI(newValue));
            } catch (Exception e) {
                Logger.logInfoMessage("Cannot open " + newValue + " error " + e.getMessage());
            }
        });
    }

    private void webViewURLChange(String newValue) {
        Logger.logInfoMessage("webview address changed to " + newValue);
        URL url;
        try {
            url = new URL(newValue);
        } catch (MalformedURLException e) {
            Logger.logInfoMessage("Malformed URL " + newValue, e);
            return;
        }
        String query = url.getQuery();
        if (query == null) {
            return;
        }
        String[] paramPairs = query.split("&");
        Map<String, String> params = new HashMap<>();
        for (String paramPair : paramPairs) {
            String[] keyValuePair = paramPair.split("=");
            if (keyValuePair.length == 2) {
                params.put(keyValuePair[0], keyValuePair[1]);
            }
        }
        String requestType = params.get("requestType");
        if (DOWNLOAD_REQUEST_TYPES.contains(requestType)) {
            download(requestType, params);
        } else {
            Logger.logInfoMessage(String.format("requestType %s is not a download request", requestType));
        }
    }

    private void download(String requestType, Map<String, String> params) {
        long transactionId = Convert.parseUnsignedLong(params.get("transaction"));
        TaggedData taggedData = TaggedData.getData(transactionId);
        boolean retrieve = "true".equals(params.get("retrieve"));
        if (requestType.equals("downloadTaggedData")) {
            if (taggedData == null && retrieve) {
                if (Kpl.getBlockchainProcessor().restorePrunedTransaction(transactionId) == null) {
                    growl("Pruned transaction data not currently available from any peer");
                    return;
                }
                taggedData = TaggedData.getData(transactionId);
            }
            if (taggedData == null) {
                growl("Tagged data not found");
                return;
            }
            byte[] data = taggedData.getData();
            String filename = taggedData.getFilename();
            if (filename == null || filename.trim().isEmpty()) {
                filename = taggedData.getName().trim();
            }
            downloadFile(data, filename);
        } else if (requestType.equals("downloadPrunableMessage")) {
            PrunableMessage prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
            if (prunableMessage == null && retrieve) {
                if (Kpl.getBlockchainProcessor().restorePrunedTransaction(transactionId) == null) {
                    growl("Pruned message not currently available from any peer");
                    return;
                }
                prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
            }
            String secretPhrase = params.get("secretPhrase");
            byte[] sharedKey = Convert.parseHexString(params.get("sharedKey"));
            if (sharedKey == null) {
                sharedKey = Convert.EMPTY_BYTE;
            }
            if (sharedKey.length != 0 && secretPhrase != null) {
                growl("Do not specify both secret phrase and shared key");
                return;
            }
            byte[] data = null;
            if (prunableMessage != null) {
                try {
                    if (secretPhrase != null) {
                        data = prunableMessage.decrypt(secretPhrase);
                    } else if (sharedKey.length > 0) {
                        data = prunableMessage.decrypt(sharedKey);
                    } else {
                        data = prunableMessage.getMessage();
                    }
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Decryption of message to recipient failed: " + e.toString());
                    growl("Wrong secretPhrase or sharedKey");
                    return;
                }
            }
            if (data == null) {
                data = Convert.EMPTY_BYTE;
            }
            downloadFile(data, "" + transactionId);
        }
    }

    private void downloadFile(byte[] data, String filename) {
        Path folderPath = Paths.get(System.getProperty("user.home"), "downloads");
        Path path = Paths.get(folderPath.toString(), filename);
        Logger.logInfoMessage("Downloading data to " + path.toAbsolutePath());
        try {
            OutputStream outputStream = Files.newOutputStream(path);
            outputStream.write(data);
            outputStream.close();
            growl(String.format("File %s saved to folder %s", filename, folderPath));
        } catch (IOException e) {
            growl("Download failed " + e.getMessage(), e);
        }
    }

    public void stop() {
        System.out.println("DesktopApplication stopped"); // Should never happen
    }

    private void growl(String msg) {
        growl(msg, null);
    }

    private void growl(String msg, Exception e) {
        if (e == null) {
            Logger.logInfoMessage(msg);
        } else {
            Logger.logInfoMessage(msg, e);
        }
        KRS.call("growl", msg);
    }

}
