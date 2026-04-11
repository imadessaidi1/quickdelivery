package com.example.app;

import android.content.Context;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "FirebaseStatus")
public class FirebaseStatusPlugin extends Plugin {
    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject result = new JSObject();
        result.put("available", isFirebaseAvailable(getContext()));
        call.resolve(result);
    }

    private boolean isFirebaseAvailable(Context context) {
        try {
            int googleAppId = context.getResources().getIdentifier("google_app_id", "string", context.getPackageName());
            int googleApiKey = context.getResources().getIdentifier("google_api_key", "string", context.getPackageName());
            return googleAppId != 0 && googleApiKey != 0;
        } catch (Exception exception) {
            return false;
        }
    }
}
