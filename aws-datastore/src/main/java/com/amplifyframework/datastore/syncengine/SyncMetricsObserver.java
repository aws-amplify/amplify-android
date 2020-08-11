package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.events.ModelSyncedEvent;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class SyncMetricsObserver {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private Map<String, ModelSyncMetrics> metricsByModel;
    private final Cancelable itemChangeObserver;
    private final List<Class<? extends Model>> syncableModels;

    SyncMetricsObserver(LocalStorageAdapter localStorageAdapter,
                        List<Class<? extends Model>> syncableModels,
                        DataStoreConfigurationProvider dataStoreConfigurationProvider) {
        itemChangeObserver = localStorageAdapter.observe(
            this::onItemChange,
            this::onError,
            this::onComplete
        );
        metricsByModel = new ConcurrentHashMap<>();
        this.syncableModels = syncableModels;
    }

    private void onComplete() {
        LOG.debug("Initial sync metrics observer completed.");
        itemChangeObserver.cancel();
    }

    private void onError(DataStoreException dataStoreException) {
        LOG.warn("Unable to gather metrics for remote sync.", dataStoreException);
    }

    private void onItemChange(StorageItemChange<? extends Model> itemChange) {
        boolean isSyncProcessChange = StorageItemChange.Initiator.SYNC_ENGINE.equals(itemChange.initiator());
        if (isSyncProcessChange) {
            if (itemChange.item() instanceof LastSyncMetadata) {
                // If the item emitted is a LastSyncMetadata, that means the lastSyncDate has been
                // updated for a given model. This, in turn, means that the sync process has completed.
                // This is where we trigger the metrics for the associated model.
                LastSyncMetadata lastSyncMetadata = (LastSyncMetadata) itemChange.item();
                String modelClassName = lastSyncMetadata.getModelClassName();
                boolean isFullSync = SyncType.BASE.name().equals(lastSyncMetadata.getLastSyncType());
                // Sync is completed, so remote the ModelSyncMetrics instance for the model from the map.
                ModelSyncMetrics metricsToEmit = metricsByModel.remove(modelClassName);
                if (metricsToEmit != null) {
                    // Build event payload and emit
                    ModelSyncedEvent modelSyncedEvent = new ModelSyncedEvent(modelClassName,
                        isFullSync,
                        metricsToEmit.getCountFor(StorageItemChange.Type.CREATE),
                        metricsToEmit.getCountFor(StorageItemChange.Type.UPDATE),
                        metricsToEmit.getCountFor(StorageItemChange.Type.DELETE));
                    LOG.debug("Sync completed: " + modelSyncedEvent);
                    Amplify.Hub.publish(HubChannel.DATASTORE,
                        HubEvent.create(DataStoreChannelEventName.MODEL_SYNCED, modelSyncedEvent));
                }

            } else {
                // If it's not a sync-able model, just bail.
                if (!syncableModels.contains(itemChange.itemClass())) {
                    return;
                }
                // Since it's not LastSyncMetadata, then try to get the model by name.
                ModelSyncMetrics metricsForModel = metricsByModel.get(itemChange.itemClass().getSimpleName());
                if (metricsForModel == null) {
                    // A new sync just started and this is the first record, so we have to create the
                    // metrics object and put it in the map.
                    metricsForModel = new ModelSyncMetrics();
                    metricsByModel.put(itemChange.itemClass().getSimpleName(), metricsForModel);
                }
                metricsForModel.increment(itemChange.type());
            }
        }
    }

    private static final class ModelSyncMetrics {
        private final Map<String, AtomicInteger> syncMetrics;

        ModelSyncMetrics() {
            syncMetrics = new ConcurrentHashMap<>();
            syncMetrics.put(DataStoreItemChange.Type.CREATE.name(), new AtomicInteger(0));
            syncMetrics.put(DataStoreItemChange.Type.UPDATE.name(), new AtomicInteger(0));
            syncMetrics.put(DataStoreItemChange.Type.DELETE.name(), new AtomicInteger(0));
        }

        public void increment(StorageItemChange.Type changeType) {
            syncMetrics.get(changeType.name()).incrementAndGet();
        }

        public int getCountFor(StorageItemChange.Type changeType) {
            return syncMetrics.get(changeType.name()).get();
        }
    }
}
