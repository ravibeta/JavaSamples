package com.rearview.timechart;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.collection.SimpleArrayMap;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import com.rearview.timechart.core.connectionInfo;
import com.rearview.timechart.core.connectionInfoList;
import com.rearview.timechart.ipc.IpcService;
import com.rearview.timechart.ipc.IpcService.ipcClientListener;
import com.rearview.timechart.ipc.ipcCategory;
import com.rearview.timechart.ipc.ipcData;
import com.rearview.timechart.ipc.ipcMessage;
import com.rearview.timechart.settings.Settings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;


public class ConnectionFragment extends ListFragment implements
        ipcClientListener {

    private final SimpleArrayMap<String, CacheQuery> CacheWhois = new SimpleArrayMap<String, CacheQuery>();
    // ipc client
    private final IpcService ipcService = IpcService.getInstance();
    private final boolean ipcStop = false;
    // data
    private final ArrayList<connectionInfo> data = new ArrayList<connectionInfo>();
    private final Settings settings = null;
    @SuppressLint("UseSparseArrays")
    private final SimpleArrayMap<Integer, String> map = new SimpleArrayMap<Integer, String>();
    private final SimpleArrayMap<String, Integer> countMap = new SimpleArrayMap<String, Integer>();
    // tablet
    private final boolean tabletLayout = false;
    private Fragment previousMap = null;
    private ProgressDialog procDialog = null;
    // stop or start
    private final boolean stopUpdate = false;
    private final MenuItem stopButton = null;

    @Override
    public void onRecvData(byte[] result) {

        // check
        if (ipcStop == true)
            return;

        if (stopUpdate == true || result == null) {
            byte[] newCommand = {ipcCategory.CONNECTION};
            ipcService.addRequest(newCommand, 1, this);
            return;
        }

        // clean up
        while (!data.isEmpty())
            data.remove(0);
        data.clear();

        map.clear();

        // convert data
        ipcMessage resultMessage = ipcMessage.getRootAsipcMessage(ByteBuffer.wrap(result));
        try {
            for (int index = 0; index < resultMessage.dataLength(); index++) {

                ipcData rawData = resultMessage.data(index);

                // prepare mapping table
                if (rawData.category() == ipcCategory.CONNECTION)
                    extractConnectionInfo(rawData);

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ((ConnectionListAdapter) getListAdapter()).refresh();

        // send command again
        byte[] newCommand = {ipcCategory.CONNECTION};
        ipcService.addRequest(newCommand, 1, this);
    }

    private void extractConnectionInfo(ipcData rawData) {
        // process connectionInfo
        connectionInfoList list = connectionInfoList.getRootAsconnectionInfoList(rawData.payloadAsByteBuffer().asReadOnlyBuffer());
        for (int count = 0; count < list.listLength(); count++) {
            connectionInfo cnInfo = list.list(count);
            data.add(cnInfo);
            String hostname = getHostName(cnInfo.remoteIP());
            if (countMap.containsKey(hostname)) {
                int value = countMap.get(hostname);
                countMap.remove(hostname);
                countMap.put(hostname, count + 1);
            } else {
                countMap.put(hostname, 1);
            }
        }
    }

    public SimpleArrayMap<String, Integer> getCountMap() {
        return countMap;
    }

    private void closeLoading() {

        // because activity may be destroyed by system, we need check it before
        // using.
        // Fix: java.lang.NullPointerException
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (procDialog != null)
                    procDialog.dismiss();
                procDialog = null;
            }
        });
    }

    private void cleanUp() {
        final FragmentTransaction transaction = getActivity()
                .getSupportFragmentManager().beginTransaction();
        if (previousMap != null) {
            transaction.remove(previousMap).commit();
            previousMap = null;
        }
    }

    private String getHostName(String QueryIP) {
        // nslookup
        String HostName = QueryIP;
        try {
            HostName = InetAddress.getByName(QueryIP).getHostName();
        } catch (UnknownHostException e) {
        }

        return HostName;
    }

    /**
     * implement viewholder class for connection list
     */
    private class ViewHolder {
        // main information
        ImageView icon;
        TextView type;
        TextView src;
        TextView dst;
        TextView owner;
        TextView status;
    }

    private class ConnectionListAdapter extends BaseAdapter {

        private LayoutInflater itemInflater = null;
        private final ViewHolder holder = null;

        public ConnectionListAdapter(Context mContext) {
            itemInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return data.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            View sv = null;
            return sv;
        }

        public void refresh() {
            this.notifyDataSetChanged();
        }

    }

    class CacheQuery {
        public String Msg;
        public float Longtiude;
        public float Latitude;
    }

    class PrepareQuery extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String QueryIP = params[0];
//      if (QueryIP != null)
//        new QueryWhois(QueryIP);
            return null;
        }

    }


}
