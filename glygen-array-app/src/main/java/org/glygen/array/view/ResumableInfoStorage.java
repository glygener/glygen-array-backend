package org.glygen.array.view;

import java.util.HashMap;

/**
 * by fanxu
 */
public class ResumableInfoStorage {

    //Single instance
    private ResumableInfoStorage() {
    }
    private static ResumableInfoStorage sInstance;

    public static synchronized ResumableInfoStorage getInstance() {
        if (sInstance == null) {
            sInstance = new ResumableInfoStorage();
        }
        return sInstance;
    }

    //resumableIdentifier --  ResumableInfo
    private HashMap<String, ResumableFileInfo> mMap = new HashMap<String, ResumableFileInfo>();
    
    public synchronized void add (ResumableFileInfo info) {
    	if (mMap.get(info.resumableIdentifier) == null)
    		mMap.put (info.resumableIdentifier, info);
    }
    
    public synchronized ResumableFileInfo get (String resumableIdentifier) {
    	return mMap.get(resumableIdentifier);
    }

    /**
     * remove ResumableInfo
     * @param info
     */
    public void remove(ResumableFileInfo info) {
       mMap.remove(info.resumableIdentifier);
    }
}
