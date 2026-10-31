package de.blau.android;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;

import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.ViewBox;

/**
 * Runs every 15 minutes while there is network (constraint is set in Main):
 *
 * 1. uploads pending changes, if the user is logged in
 *
 * 2. if the loaded data is close to the "too much data" limit, removes unchanged data outside the screen so the limit
 * is not reached
 */
public class HourlyWorker extends Worker {

    private static final String TAG = "HourlyWorker";

    /** start cleaning up when this share of the limit is reached */
    private static final double PRUNE_THRESHOLD = 0.8;

    public HourlyWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logic logic = App.getLogic();
        if (logic == null) {
            Log.w(TAG, "Logic not initialized, skipping");
            return Result.success();
        }
        Context context = getApplicationContext();
        StorageDelegator delegator = App.getDelegator();
        boolean uploadFailed = false;
        boolean changed = false;

        if (logic.hasChanges()) {
            UploadState state = upload(logic, delegator);
            uploadFailed = state == UploadState.FAILED;
            changed = state == UploadState.OK;
        }

        int limit = logic.getPrefs().getDataWarnLimit();
        int nodeCount = delegator.getCurrentStorage().getNodeCount();
        if (nodeCount >= limit * PRUNE_THRESHOLD) {
            try {
                ViewBox pruneBox = new ViewBox(logic.getViewBox());
                pruneBox.scale(1.1);
                delegator.prune(pruneBox); // keeps modified and selected elements
                Log.i(TAG, "Pruned data: " + nodeCount + " -> " + delegator.getCurrentStorage().getNodeCount() + " nodes");
                changed = true;
            } catch (Exception e) {
                Log.e(TAG, "Pruning failed", e);
            }
        }

        if (changed) {
            logic.save(context);
            Map map = logic.getMap();
            if (map != null) {
                map.postInvalidate();
            }
        }
        // retry later (with backoff) if the upload failed, e.g. the connection dropped
        return uploadFailed ? Result.retry() : Result.success();
    }

    private enum UploadState {
        OK, SKIPPED, FAILED
    }

    /**
     * Upload all pending changes
     *
     * @param logic the current Logic instance
     * @param delegator the current StorageDelegator
     * @return the outcome
     */
    @NonNull
    private UploadState upload(@NonNull Logic logic, @NonNull StorageDelegator delegator) {
        Server server = logic.getPrefs().getServer();
        if (!server.isLoginSet() || server.needOAuthHandshake()) {
            Log.w(TAG, "Not logged in, skipping upload");
            return UploadState.SKIPPED;
        }
        try {
            server.getCapabilities(); // update status
            if (!(server.apiAvailable() && server.writableDB())) {
                Log.w(TAG, "API not available, skipping upload");
                return UploadState.FAILED;
            }
            // same changeset comment as before (dev used an empty one, which became this summary)
            delegator.uploadToServer(server, "Automatically generated summary", "", false, true, null, new ArrayList<>());
            delegator.clearUndo(); // only clear on successful upload, same as manual upload
            Log.i(TAG, "Periodic upload completed");
            return UploadState.OK;
        } catch (Exception e) {
            Log.e(TAG, "Periodic upload failed", e);
            return UploadState.FAILED;
        }
    }
}
