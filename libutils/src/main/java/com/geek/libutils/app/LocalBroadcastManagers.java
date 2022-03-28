package com.geek.libutils.app;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by efan on 2017/4/14.
 */

public final class LocalBroadcastManagers {
    private static final String TAG = "LocalBroadcastManagers";
    private static final boolean DEBUG = false;
    private final Context mAppContext;
    private final HashMap<BroadcastReceiver, ArrayList<IntentFilter>> mReceivers = new HashMap<BroadcastReceiver, ArrayList<IntentFilter>>();
    private final HashMap<String, ArrayList<ReceiverRecord>> mActions = new HashMap<String, ArrayList<ReceiverRecord>>();
    private final ArrayList<BroadcastRecord> mPendingBroadcasts = new ArrayList<BroadcastRecord>();
    static final int MSG_EXEC_PENDING_BROADCASTS = 1;
    private final Handler mHandler;
    private static final Object mLock = new Object();
    private static volatile LocalBroadcastManagers mInstance;

    public static LocalBroadcastManagers getInstance(Context context) {
        Object var1 = mLock;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new LocalBroadcastManagers(context.getApplicationContext());
//                mInstance = new AgentWebAgentWebLocalBroadcastManagers(context);
            }

            return mInstance;
        }
    }

    private LocalBroadcastManagers(Context context) {
        this.mAppContext = context;
        this.mHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        LocalBroadcastManagers.this.executePendingBroadcasts();
                        break;
                    default:
                        super.handleMessage(msg);
                }

            }
        };
    }

    public void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        HashMap var3 = this.mReceivers;
        synchronized (this.mReceivers) {
            LocalBroadcastManagers.ReceiverRecord entry = new LocalBroadcastManagers.ReceiverRecord(filter, receiver);
            ArrayList filters = this.mReceivers.get(receiver);
            if (filters == null) {
                filters = new ArrayList(1);
                this.mReceivers.put(receiver, filters);
            }

            filters.add(filter);

            for (int i = 0; i < filter.countActions(); ++i) {
                String action = filter.getAction(i);
                ArrayList entries = this.mActions.get(action);
                if (entries == null) {
                    entries = new ArrayList(1);
                    this.mActions.put(action, entries);
                }

                entries.add(entry);
            }

        }
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        HashMap var2 = this.mReceivers;
        synchronized (this.mReceivers) {
            ArrayList filters = this.mReceivers.remove(receiver);
            if (filters != null) {
                for (int i = 0; i < filters.size(); ++i) {
                    IntentFilter filter = (IntentFilter) filters.get(i);

                    for (int j = 0; j < filter.countActions(); ++j) {
                        String action = filter.getAction(j);
                        ArrayList receivers = this.mActions.get(action);
                        if (receivers != null) {
                            for (int k = 0; k < receivers.size(); ++k) {
                                if (((LocalBroadcastManagers.ReceiverRecord) receivers.get(k)).receiver == receiver) {
                                    receivers.remove(k);
                                    --k;
                                }
                            }

                            if (receivers.size() <= 0) {
                                this.mActions.remove(action);
                            }
                        }
                    }
                }

            }
        }
    }

    public boolean sendBroadcast(Intent intent) {
        HashMap var2 = this.mReceivers;
        synchronized (this.mReceivers) {
            String action = intent.getAction();
            String type = intent.resolveTypeIfNeeded(this.mAppContext.getContentResolver());
            Uri data = intent.getData();
            String scheme = intent.getScheme();
            Set categories = intent.getCategories();
            boolean debug = (intent.getFlags() & 8) != 0;
            if (debug) {
                Log.v("LocalBroadcastManagers", "Resolving type " + type + " scheme " + scheme + " of intent " + intent);
            }

            ArrayList entries = this.mActions.get(intent.getAction());
            if (entries != null) {
                if (debug) {
                    Log.v("LocalBroadcastManagers", "Action list: " + entries);
                }

                ArrayList receivers = null;

                int i;
                for (i = 0; i < entries.size(); ++i) {
                    LocalBroadcastManagers.ReceiverRecord receiver = (LocalBroadcastManagers.ReceiverRecord) entries.get(i);
                    if (debug) {
                        Log.v("LocalBroadcastManagers", "Matching against filter " + receiver.filter);
                    }

                    if (receiver.broadcasting) {
                        if (debug) {
                            Log.v("LocalBroadcastManagers", "  Filter\'s target already added");
                        }
                    } else {
                        int match = receiver.filter.match(action, type, scheme, data, categories, "AgentWebAgentWebLocalBroadcastManagers");
                        if (match >= 0) {
                            if (debug) {
                                Log.v("LocalBroadcastManagers", "  Filter matched!  match=0x" + Integer.toHexString(match));
                            }

                            if (receivers == null) {
                                receivers = new ArrayList();
                            }

                            receivers.add(receiver);
                            receiver.broadcasting = true;
                        } else if (debug) {
                            String reason;
                            switch (match) {
                                case -4:
                                    reason = "category";
                                    break;
                                case -3:
                                    reason = "action";
                                    break;
                                case -2:
                                    reason = "data";
                                    break;
                                case -1:
                                    reason = "type";
                                    break;
                                default:
                                    reason = "unknown reason";
                            }

                            Log.v("LocalBroadcastManagers", "  Filter did not match: " + reason);
                        }
                    }
                }

                if (receivers != null) {
                    for (i = 0; i < receivers.size(); ++i) {
                        ((LocalBroadcastManagers.ReceiverRecord) receivers.get(i)).broadcasting = false;
                    }

                    this.mPendingBroadcasts.add(new LocalBroadcastManagers.BroadcastRecord(intent, receivers));
                    if (!this.mHandler.hasMessages(1)) {
                        this.mHandler.sendEmptyMessage(1);
                    }

                    return true;
                }
            }

            return false;
        }
    }

    public void sendBroadcastSync(Intent intent) {
        if (this.sendBroadcast(intent)) {
            this.executePendingBroadcasts();
        }

    }

    private void executePendingBroadcasts() {
        while (true) {
            LocalBroadcastManagers.BroadcastRecord[] brs = null;
            HashMap i = this.mReceivers;
            synchronized (this.mReceivers) {
                int br = this.mPendingBroadcasts.size();
                if (br <= 0) {
                    return;
                }

                brs = new LocalBroadcastManagers.BroadcastRecord[br];
                this.mPendingBroadcasts.toArray(brs);
                this.mPendingBroadcasts.clear();
            }

            for (int var6 = 0; var6 < brs.length; ++var6) {
                LocalBroadcastManagers.BroadcastRecord var7 = brs[var6];

                for (int j = 0; j < var7.receivers.size(); ++j) {
                    var7.receivers.get(j).receiver.onReceive(this.mAppContext, var7.intent);
                }
            }
        }
    }

    private static class BroadcastRecord {
        final Intent intent;
        final ArrayList<ReceiverRecord> receivers;

        BroadcastRecord(Intent _intent, ArrayList<ReceiverRecord> _receivers) {
            this.intent = _intent;
            this.receivers = _receivers;
        }
    }

    private static class ReceiverRecord {
        final IntentFilter filter;
        final BroadcastReceiver receiver;
        boolean broadcasting;

        ReceiverRecord(IntentFilter _filter, BroadcastReceiver _receiver) {
            this.filter = _filter;
            this.receiver = _receiver;
        }

        @Override
        @NonNull
        public String toString() {
            StringBuilder builder = new StringBuilder(128);
            builder.append("Receiver{");
            builder.append(this.receiver);
            builder.append(" filter=");
            builder.append(this.filter);
            builder.append("}");
            return builder.toString();
        }
    }
}