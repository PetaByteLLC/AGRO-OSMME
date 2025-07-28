package de.blau.android;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.blau.android.listener.UploadListener;
import de.blau.android.osm.Server;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.ScreenMessage;

public class HourlyWorker extends Worker {

    public HourlyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final Logic logic = App.getLogic();
        boolean hasDataChanges = logic.hasChanges();
        if (hasDataChanges) {
            UploadListener.UploadArguments arguments = new UploadListener.UploadArguments("", "",
                    false, true, null, de.blau.android.dialogs.Util.getElementsFromBundle(new Bundle()));
            logic.upload2(arguments);
        }
        return Result.success();
    }
}
