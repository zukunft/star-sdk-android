/*
 * *
 *  * Created by Ubique Innovation AG on 3/30/20 2:55 PM
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/30/20 2:54 PM
 *
 */

package ch.ubique.android.starsdk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;

import java.io.IOException;

import ch.ubique.android.starsdk.backend.CallbackListener;
import ch.ubique.android.starsdk.backend.ResponseException;
import ch.ubique.android.starsdk.backend.models.Exposee;
import ch.ubique.android.starsdk.crypto.STARModule;
import ch.ubique.android.starsdk.database.Database;
import ch.ubique.android.starsdk.util.LogHelper;
import ch.ubique.android.starsdk.util.ProcessUtil;

public class STARTracing {

	public static final String UPDATE_INTENT_ACTION = "ch.ubique.android.starsdk.UPDATE_ACTION";

	private static String appId;

	public static void init(Context context, String appId) {
		init(context, appId, false);
	}

	public static void init(Context context, String appId, boolean enableDevMode) {
		if (ProcessUtil.isMainProcess(context)) {
			STARTracing.appId = appId;
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			appConfigManager.setAppId(appId);
			appConfigManager.setDevModeEnabled(enableDevMode);
			appConfigManager.triggerLoad();

			STARModule.getInstance(context).init();

			boolean advertising = appConfigManager.isAdvertisingEnabled();
			boolean receiving = appConfigManager.isReceivingEnabled();
			if (advertising || receiving) {
				start(context, advertising, receiving);
			}
		}
	}

	private static void checkInit() {
		if (appId == null) {
			throw new IllegalStateException("You have to call STARTracing.init() in your application onCreate()");
		}
	}

	public static void start(Context context) {
		start(context, true, true);
	}

	public static void start(Context context, boolean advertise, boolean receive) {
		checkInit();
		AppConfigManager.getInstance(context).setAdvertisingEnabled(advertise);
		AppConfigManager.getInstance(context).setReceivingEnabled(receive);
		Intent intent = new Intent(context, TracingService.class).setAction(TracingService.ACTION_START);
		intent.putExtra(TracingService.EXTRA_ADVERTISE, advertise);
		intent.putExtra(TracingService.EXTRA_RECEIVE, receive);
		ContextCompat.startForegroundService(context, intent);
		SyncWorker.startSyncWorker(context);
	}

	public static boolean isStarted(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		return appConfigManager.isAdvertisingEnabled() || appConfigManager.isReceivingEnabled();
	}

	public static void sync(Context context) throws IOException, ResponseException {
		checkInit();
		SyncWorker.doSync(context);
	}

	public static TracingStatus getStatus(Context context) {
		checkInit();
		Database database = new Database(context);
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		TracingStatus.ErrorState errorState = null;
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!bluetoothAdapter.isEnabled()) {
			errorState = TracingStatus.ErrorState.BLE_DISABLED;
		}
		return new TracingStatus(
				database.getHandshakes().size(),
				appConfigManager.isAdvertisingEnabled(),
				appConfigManager.isReceivingEnabled(),
				database.wasContactExposed(),
				appConfigManager.getLastSyncDate(),
				appConfigManager.getAmIExposed(),
				errorState
		);
	}

	public static void sendIWasExposed(Context context, Object customData, CallbackListener<Void> callback) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAmIExposed(true);
		appConfigManager.getBackendRepository(context)
				.addExposee(new Exposee(STARModule.getInstance(context).getSecretKeyForBackend()), callback);
	}

	public static void sendIWasHealed(Context context, Object customData, CallbackListener<Void> callback) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAmIExposed(false);
		appConfigManager.getBackendRepository(context)
				.removeExposee(new Exposee(STARModule.getInstance(context).getSecretKeyForBackend()), callback);
		STARModule.getInstance(context).reset();
	}

	public static void stop(Context context) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAdvertisingEnabled(false);
		appConfigManager.setReceivingEnabled(false);

		Intent intent = new Intent(context, TracingService.class).setAction(TracingService.ACTION_STOP);
		context.stopService(intent);
		SyncWorker.stopSyncWorker(context);
	}

	public static void reset(Context context) {
		checkInit();
		//TODO clear all data
		STARModule.getInstance(context).reset();
	}

	public static IntentFilter getUpdateIntentFilter() {
		IntentFilter intentFilter = new IntentFilter(STARTracing.UPDATE_INTENT_ACTION);
		return intentFilter;
	}

	public static boolean isDevModeEnabled(Context context) {
		checkInit();
		return AppConfigManager.getInstance(context).isDevModeEnabled();
	}

	public static void setCalibrationTestDeviceName(Context context, String name) {
		checkInit();
		AppConfigManager.getInstance(context).setCalibrationTestDeviceName(name);
	}

	public static void disableCalibrationTestDeviceName(Context context) {
		checkInit();
		AppConfigManager.getInstance(context).setCalibrationTestDeviceName(null);
	}

	public static void clearData(Context context, Runnable onDeleteListener) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		if (appConfigManager.isAdvertisingEnabled() || appConfigManager.isReceivingEnabled()) {
			throw new IllegalStateException("Tracking must be stopped for clearing the local data");
		}

		appConfigManager.clearPreferences();
		LogHelper.clearLog(context);
		Database db = new Database(context);
		db.recreateTables(response -> onDeleteListener.run());
	}

}
