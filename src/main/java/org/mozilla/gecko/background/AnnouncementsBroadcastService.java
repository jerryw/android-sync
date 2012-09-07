  /* This Source Code Form is subject to the terms of the Mozilla Public
   * License, v. 2.0. If a copy of the MPL was not distributed with this
   * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mozilla.gecko.sync.Logger;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AnnouncementsBroadcastService extends IntentService {
  private static final String LOG_TAG = "GeckoSnippets";

  public AnnouncementsBroadcastService() {
    super("SnippetsBroadcastWorker");
  }

  private void toggleAlarm(final Context context, boolean enabled) {
    Logger.debug(LOG_TAG, (enabled ? "R" : "Unr") + "egistering Snippets broadcast receiver...");
    final AlarmManager alarm = getAlarmManager(context);

    final Intent service = new Intent(context, AnnouncementsStartReceiver.class);
    final PendingIntent pending = PendingIntent.getBroadcast(context, 0, service, PendingIntent.FLAG_CANCEL_CURRENT);

    if (!enabled) {
      alarm.cancel(pending);
      return;
    }

    final long firstEvent = System.currentTimeMillis();
    final long pollInterval = getPollInterval(context);
    Logger.debug(LOG_TAG, "Setting inexact repeating alarm for interval " + pollInterval);
    alarm.setInexactRepeating(AlarmManager.RTC, firstEvent, pollInterval, pending);
  }

  private static AlarmManager getAlarmManager(Context context) {
    return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  }

  private void recordLastLaunch(final Context context) {
    final SharedPreferences preferences = context.getSharedPreferences(BackgroundServiceConstants.PREFS_BRANCH, BackgroundServiceConstants.SHARED_PREFERENCES_MODE);
    preferences.edit().putLong(BackgroundServiceConstants.PREF_LAST_LAUNCH, System.currentTimeMillis()).commit();
  }

  public static long getPollInterval(final Context context) {
    SharedPreferences preferences = context.getSharedPreferences(BackgroundServiceConstants.PREFS_BRANCH, BackgroundServiceConstants.SHARED_PREFERENCES_MODE);
    return preferences.getLong(BackgroundServiceConstants.PREF_ANNOUNCE_FETCH_INTERVAL_MSEC, BackgroundServiceConstants.DEFAULT_ANNOUNCE_FETCH_INTERVAL_MSEC);
  }

  public static void setPollInterval(final Context context, long interval) {
    SharedPreferences preferences = context.getSharedPreferences(BackgroundServiceConstants.PREFS_BRANCH, BackgroundServiceConstants.SHARED_PREFERENCES_MODE);
    preferences.edit().putLong(BackgroundServiceConstants.PREF_ANNOUNCE_FETCH_INTERVAL_MSEC, interval).commit();
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    final String action = intent.getAction();
    Logger.debug(LOG_TAG, "Broadcast onReceive. Intent is " + action);

    if ("org.mozilla.gecko.SNIPPETS_PREF".equals(action)) {
      // Gecko is telling us something.
      Logger.debug(LOG_TAG, intent.getStringExtra("branch") + "/" +
                            intent.getStringExtra("pref")   + " = " +
                            (intent.hasExtra("enabled") ? intent.getBooleanExtra("enabled", true) : ""));

      recordLastLaunch(this);
      if (intent.hasExtra("enabled")) {
        Logger.debug(LOG_TAG, "Enabled set. Toggling alarm accordingly.");
        toggleAlarm(this, intent.getBooleanExtra("enabled", true));
        return;
      }

      // This intent comes with a few extras to allow us
      // to look up the value if it wasn't provided.
      Logger.debug(LOG_TAG, "Looking up enabled state in prefs.");
      String branch = intent.getStringExtra("branch");
      String pref = intent.getStringExtra("pref");

      if (branch == null || pref == null) {
        toggleAlarm(this, true);
        return;
      }

      SharedPreferences preferences = this.getSharedPreferences(branch, 0);
      if (preferences == null) {
        toggleAlarm(this, true);
        return;
      }

      final boolean enabled = preferences.getBoolean(pref, true);
      toggleAlarm(this, enabled);
      return;
    }

    if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
        Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
      // Translate this into a Gecko notification.
      try {
        Class<?> geckoPreferences = Class.forName(BackgroundServiceConstants.GECKO_PREFERENCES_CLASS);
        Method broadcastSnippetsPref = geckoPreferences.getMethod(BackgroundServiceConstants.GECKO_BROADCAST_METHOD, Context.class);
        broadcastSnippetsPref.invoke(null, this);
        return;
      } catch (ClassNotFoundException e) {
        Logger.error(LOG_TAG, "Class " + BackgroundServiceConstants.GECKO_PREFERENCES_CLASS + " not found!");
        return;
      } catch (NoSuchMethodException e) {
        Logger.error(LOG_TAG, "Method " + BackgroundServiceConstants.GECKO_PREFERENCES_CLASS + "/" + BackgroundServiceConstants.GECKO_BROADCAST_METHOD + " not found!");
        return;
      } catch (IllegalArgumentException e) {
        // Fall through.
      } catch (IllegalAccessException e) {
        // Fall through.
      } catch (InvocationTargetException e) {
        // Fall through.
      }
      Logger.error(LOG_TAG, "Got exception invoking " + BackgroundServiceConstants.GECKO_BROADCAST_METHOD + ".");
      return;
    }

    // Failure case.
    Logger.warn(LOG_TAG, "Unknown intent " + action);
  }
}