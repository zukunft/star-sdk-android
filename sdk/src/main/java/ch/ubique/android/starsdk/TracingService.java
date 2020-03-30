/*
 * *
 *  * Created by Ubique Innovation AG on 3/30/20 2:55 PM
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/30/20 2:54 PM
 *
 */

package ch.ubique.android.starsdk;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import ch.ubique.android.starsdk.gatt.BleClient;
import ch.ubique.android.starsdk.gatt.BleServer;
import ch.ubique.android.starsdk.util.LogHelper;

public class TracingService extends Service {

	public static final String ACTION_START = TracingService.class.getCanonicalName() + ".ACTION_START";
	public static final String ACTION_STOP = TracingService.class.getCanonicalName() + ".ACTION_STOP";

	public static final long SCAN_INTERVAL = 5 * 60 * 1000L;
	private static final long SCAN_DURATION = 30 * 1000L;

	private static String NOTIFICATION_CHANNEL_ID = "star_tracing_service";
	private static int NOTIFICATION_ID = 1827;
	private Handler handler;

	private PowerManager.WakeLock wl;

	private BleServer bleServer;
	private BleClient bleClient;

	public TracingService() { }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getAction() == null) {
			return START_STICKY;
		}

		if (wl == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getPackageName() + ":TracingServiceWakeLock");
			wl.acquire();
		}

		LogHelper.init(this);
		LogHelper.append("service started");

		Log.d("TracingService", "onHandleIntent() with " + intent.getAction());

		if (ACTION_START.equals(intent.getAction())) {
			startForeground(NOTIFICATION_ID, createForegroundNotification());
			start();
		} else if (ACTION_STOP.equals(intent.getAction())) {
			stopForegroundService();
		}

		return START_STICKY;
	}

	private Notification createForegroundNotification() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel();
		}

		Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
		PendingIntent contentIntent = null;
		if (launchIntent != null) {
			contentIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		}

		return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
				.setOngoing(true)
				.setContentTitle(getString(R.string.star_sdk_service_notification_title))
				.setContentText(getString(R.string.star_sdk_service_notification_text))
				.setPriority(NotificationCompat.PRIORITY_LOW)
				.setSmallIcon(R.drawable.ic_begegnungen)
				.setContentIntent(contentIntent)
				.build();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void createNotificationChannel() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String channelName = getString(R.string.star_sdk_service_notification_channel);
		NotificationChannel channel =
				new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
		channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		notificationManager.createNotificationChannel(channel);
	}

	private void start() {
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
		}
		handler = new Handler();

		startTracing();
	}

	private void startTracing() {
		Log.d("TracingService", "startTracing() ");

		try {
			Notification notification = createForegroundNotification();

			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(NOTIFICATION_ID, notification);

			startClient();
			startServer();
		} catch (Throwable t) {
			t.printStackTrace();
			LogHelper.append(t);
		}

		handler.postDelayed(() -> {scheduleNextRun(this);}, SCAN_DURATION);
	}

	public static void scheduleNextRun(Context context) {
		long now = System.currentTimeMillis();
		long delay = SCAN_INTERVAL - (now % SCAN_INTERVAL);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, TracingServiceBroadcastReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + delay, pendingIntent);
	}

	private void stopForegroundService() {
		stopClient();
		stopServer();
		stopForeground(true);
		wl.release();
		stopSelf();
	}

	@Override
	public void onDestroy() {
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
		}
		Log.d("TracingService", "onDestroy()");
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	private void startServer() {
		stopServer();
		bleServer = new BleServer(this);
		bleServer.start();
		bleServer.startAdvertising();
	}

	private void stopServer() {
		if (bleServer != null) {
			bleServer.stop();
			bleServer = null;
		}
	}

	private void startClient() {
		stopClient();
		bleClient = new BleClient(this);
		bleClient.start();
	}

	private void stopClient() {
		if (bleClient != null) {
			bleClient.stop();
			bleClient = null;
		}
	}

}